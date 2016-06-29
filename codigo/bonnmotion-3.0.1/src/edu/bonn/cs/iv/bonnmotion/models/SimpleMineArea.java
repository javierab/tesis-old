package edu.bonn.cs.iv.bonnmotion.models;
import java.text.*;
import java.lang.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.models.mine.Obstacle;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;
import edu.bonn.cs.iv.bonnmotion.models.mine.*;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.ScenarioLinkException;
import edu.bonn.cs.iv.bonnmotion.Waypoint;
import edu.bonn.cs.iv.util.PositionHashMap;
import edu.bonn.cs.iv.util.IntegerHashMap;

/** Application to create movement scenarios according to the Simple Mine Area model. */

public class SimpleMineArea extends RandomSpeedBase {
    private static ModuleInfo info;
    DecimalFormat df = new DecimalFormat("#.##");
    
    static {
        info = new ModuleInfo("SimpleMineArea");
        info.description = "Simulates an Underground Mine";
        
        info.major = 0;
        info.minor = 1;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 1 $");
        
        info.contacts.add("jborn@dcc.uchile.cl");
        info.contacts.add("scespedes@ing.uchile.cl");
        info.authors.add("Javiera Born");
        info.authors.add("Sandra Céspedes");
		info.affiliation = "Universidad de Chile - Departamento de Ciencias de la Computación";
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }
    
	private static boolean debug = false;


	/** Count the number of areas . */
	protected int n_areas = 0;
	protected int n_areas_aux = 0;
	protected int n_nodes = 0;
	protected int n_nodes_aux = 0;
	
	/** Count for each area**/
	protected LinkedList<Integer> i_area =  new LinkedList<Integer>();
	protected int[] n_each = new int[3];
	protected int[] n_max = {1, 2, 4}; /* 1 access, 2 ext, 4 mant*/
	
	/** Count for nodes**/
	protected int[] nod_each = new int[3];
	protected int n_max_per_ext = 4;
	protected int[] nod_max = {n_max_per_ext*n_max[1], 200, 100}; /* 4*ext maquina, 200 mantenimiento, 100 supervisor*/
	protected LinkedList<Integer> type_nodes =  new LinkedList<Integer>();
	protected double[] min_speed = {0.0,0.0,0.0};
	protected double[] max_speed = {0.0,0.0,0.0};
	protected int[] avg_pause = {0,0,0};
	protected int[] repetitions = {0,0,0};
	
	/** Data to build each area **/
	protected LinkedList<double[]> corners = new LinkedList<double[]>();
	protected LinkedList<double[]> entries = new LinkedList<double[]>(); 
	
	/** Dumps for extaction areas**/
	protected LinkedList<Integer> ndumps =  new LinkedList<Integer>();
	protected LinkedList<Position[]> dumps = new LinkedList<Position[]>();
	
	/** Manage the Areas and nodes . */
	public MineArea[] mineAreas = null;
	public Access accArea = null;
	public Extraction[] extAreas = null;
	public Maintenance[] manAreas = null;
	
	/**Manage nodes**/
	public MineNode[] mineNodes = null;
	
	boolean write_one_file = false;
	String write_one_name = "";

	public SimpleMineArea(int nodes, double x, double y, double duration, double ignore, long randomSeed, double minspeed, double maxspeed, double maxpause) {
		super(nodes, x, y, duration, ignore, randomSeed, minspeed, maxspeed, maxpause);
		generate();
	}

	public SimpleMineArea(String[] args) {
		go(args);
	}
	
	public void go(String[] args) {
		super.go(args);
		generate();
	}

	public MineArea currentArea(Position p) throws Exception{
		for(int i=0; 		/*NODOS: asumimos que están especificados los tipos que tenemos que crear y donde parten.*/
i < mineAreas.length; i++){
			if (mineAreas[i].contains(p.x, p.y));
			return mineAreas[i];
		}
		throw new Exception("ERROR: Position outside of all areas");
	}	

	public void generate() {
		
		this.processArguments();
		preGeneration();

		mineAreas = new MineArea[n_areas];
		extAreas = new Extraction[n_each[1]];
		manAreas = new Maintenance[n_each[2]];
		int e = 0;
		int m = 0;

		for(int i = 0; i < n_areas; i++){
			if(i_area.get(i) == 0){ /*access*/
				mineAreas[i] = MineArea.getInstance(corners.get(i), entries.get(i), 0);
				accArea = (Access) mineAreas[i];
				
			}
			else if(i_area.get(i) == 1){ /*extraction*/
				mineAreas[i] = MineArea.getInstance(corners.get(i), entries.get(i), 1);
				((Extraction)mineAreas[i]).setDumps(ndumps.get(e), dumps.get(e));
				extAreas[e] = (Extraction) mineAreas[i];
				e++;
				
			}
			else if(i_area.get(i) == 2){
				mineAreas[i] = MineArea.getInstance(corners.get(i), entries.get(i), 2);
				manAreas[m] = (Maintenance) mineAreas[i];
				m++;
			}

			//System.out.print("New MineArea completed: "); mineAreas[i].print();
		}
		System.out.println("Areas created");

		
		/*NODES*/
		//System.out.println("Creating Nodes...");
		mineNodes = new MineNode[n_nodes];
		Random r = new Random();
		for(int i = 0; i < n_nodes; i++){
			if(type_nodes.get(i) == 0){ /*Machine*/
				int area = r.nextInt(extAreas.length);
				int cnt = 100;
				Position p = extAreas[area].getRandomPosition();
				mineNodes[i] = new MachineNode(p, extAreas[area]);	
				extAreas[area].n_machines++;
			}
			else if(type_nodes.get(i) == 1){ /*Operator*/
				int area = r.nextInt(manAreas.length);
				Position p = manAreas[area].getRandomPosition();
				mineNodes[i] = new OperatorNode(p, manAreas, manAreas[area], accArea);
			}
			else if(type_nodes.get(i) == 2){ /*Supervisor*/
			 	//always starts in ACC = 0
				Position p = mineAreas[0].getRandomPosition();
				mineNodes[i] = new SupervisorNode(p, mineAreas, mineAreas[0], accArea);		
			}
			else{ 
				System.err.println("Node type doesn't exist:" + type_nodes.get(i));
				System.exit(1);
			}			
		}
		
		System.out.println("Nodes created");

		parameterData.nodes = mineNodes;
		/*Do the things*/

		double t = 0.0;
		Position p;
		PrintWriter pw = null;
		
		if(write_one_file){
			/* open files for each node type */
			//pw = new PrintWriter[3];
			File f0;
			//, f1, f2;
			
			f0 = new File(write_one_name + ".one");
			//f1 = new File(write_one_name + "_OP.csv");
			//f2 = new File(write_one_name + "_SU.csv");
			
	    	try{
	    		if(f0.createNewFile() == false){
	    			// || f1.createNewFile() == false || f2.createNewFile() == false){
	    			System.out.println("Files already exist -- aborting!");
	    			System.exit(0);
	    		}
	    		else{

		    		/*open pws*/
		    		pw = new PrintWriter (f0);
		    		//pw[1] = new PrintWriter (f1);
		    		//pw[2] = new PrintWriter (f2);		    
		    	}

		    	/*print first line on each*/
		    	pw.println(parameterData.ignore + " " + parameterData.duration + " 0 " + parameterData.x + " 0 " + parameterData.y + " 0 0");
		    	//pw[1].println(parameterData.ignore + " " + parameterData.duration + ", 0, " + parameterData.x + ", 0, " + parameterData.y + ", 0, 0");
		    	//pw[2].println(parameterData.ignore + " " + parameterData.duration + ", 0, " + parameterData.x + ", 0, " + parameterData.y + ", 0, 0");
		    }
	    	catch(Exception ex){
	    		System.out.println("Can't write files -- aborting!");
	    		ex.printStackTrace();
	    		System.exit(1);
	    	}
		}
			
		System.out.println("--Starting simulation--");
		while(t < parameterData.duration){
			for(int i=0; i< mineNodes.length; i++){
				p = mineNodes[i].getNextStep();

				mineNodes[i].add(t, p);
				parameterData.nodes[i].add(t, p);
				if(write_one_file){
					pw.println(t + " " + i + " " + mineNodes[i].current_position.x + " " + mineNodes[i].current_position.y);
				}
				//System.out.println("t: " + t + " node " + mineNodes[i].getType() + ":" + i + " pos:" + p.toString(2));
			}
			t+=1.0;
			System.out.println("t="+t);
		}
		if(write_one_file){
			pw.flush();
			pw.close();
//			pw[1].flush();
//			pw[1].close();
//			pw[2].flush();
//			pw[2].close();
		}
		postGeneration();		
	}

	protected void postGeneration() {
		for ( int i = 0; i < parameterData.nodes.length; i++ ) {

			Waypoint l = parameterData.nodes[i].getLastWaypoint();
			if (l.time > parameterData.duration) {
				Position p = parameterData.nodes[i].positionAt(parameterData.duration);
				parameterData.nodes[i].removeLastElement();
				parameterData.nodes[i].add(parameterData.duration, p);
			}
		}    
		super.postGeneration();
	}
	
	protected int areaToId(String s){
		if(s.equals("ACC") || s.equals("acc")) return 0;
		else if(s.equals("EXT") || s.equals("ext")) return 1;
		else if(s.equals("MAN") || s.equals("man")) return 2;
		else return -1;
	}
	
	protected int nodeToId(String s){
		if(s.equals("LH") || s.equals("lh")) return 0;
		else if(s.equals("OP") || s.equals("op")) return 1;
		else if(s.equals("SU") || s.equals("su")) return 2;
		else return -1;
	}
	
	protected boolean parseArg(char key, String val){
		String[] args;

		switch(key) {
		case 'O':
			write_one_file = true;
			write_one_name = val;
			System.out.println("Output set to true - Files: " + write_one_name);
			return true;
		case 'o':
			write_one_file = true;
			write_one_name = val;
			System.out.println("Output set to true - Files: " + write_one_name);
			return true;
		case 'A':
			n_areas = Integer.parseInt(val);
			return true;
			//-a EX1 c1 c3 n_entries e1 e2 e3 [n_dumps dump1 dump2]
		case 'a':
			/*String: TYPE CORNER1x CORNER1y CORNER3x CORNER3y nENTRIES ENTRYx ENTRYy ... nDUMPS DUMPx DUMPy ...*/
			/*area definition*/
			if(n_areas == 0){
				System.out.println("Must first declare a valid number of areas");
				System.exit(1);
			}
			try{
				/*check we have enough arguments */
				args = val.split(" ");
				if(args.length < 8){
					System.out.println("Not enough arguments in -a");
					System.exit(1);
				}
				else{					
					/*get area type*/
					int id = areaToId(args[0]);
					if(id == -1){ System.out.println("Area type unknown"); System.exit(1);}
					
					/*check we can create a new area of that type*/
					n_areas_aux++;
					if(n_max[id] < n_each[id]){System.out.println("Can't declare so many zones of type " + args[0]); System.exit(1);}
					else {n_each[id]++; i_area.add(id);} 
					if(n_areas_aux > n_areas){
							System.out.println("Number of areas declared do not match");
							System.exit(1);
						}
					/*add corners*/
					double c1x = Double.parseDouble(args[1]);
					double c1y = Double.parseDouble(args[2]);
					double c2x = Double.parseDouble(args[3]);
					double c2y = Double.parseDouble(args[4]);
					double[] cn = {c1x, c1y, c1x, c2y, c2x, c2y, c2x, c1y};
					corners.add(cn);
					
					/*add entries*/
					int ne = Integer.parseInt(args[5]);
					double[] en = new double[ne*2];
					for(int i=0; i <ne*2; i++){
						en[i] = Double.parseDouble(args[6+i]);
					}
					entries.add(en);
					
					/*EXT: add dumps*/
					if(id == 1){ 
						int nd = Integer.parseInt(args[6+2*ne]);
						ndumps.add(nd);
						Position[] p = new Position[nd*2];
						System.out.println(nd*2);
						for(int i = 0; i < nd*2; i=i+2){
							p[i/2] = new Position(Double.parseDouble(args[7+2*ne+i]), Double.parseDouble(args[8+2*ne+i]));
						}
						dumps.add(p);
					}
				}
			}
			catch(Exception e){
				System.out.println("Could not parse -a args");
				e.printStackTrace();
				System.exit(1);
			}
			return true;
		case 'C':
			n_nodes = Integer.parseInt(val);
			return true;
		case 'c':
			/*node definition*/
			int n_node_type;
			/*String: TYPE NUMBER V_MIN V_MAX PAUSE REPS*/
			if(n_nodes == 0){
				System.out.println("Must first declare a valid number of nodes");
				System.exit(1);
			}
			try{
				/*check we have enough arguments */
				args = val.split(" ");
				if(args.length < 2){
					System.out.println("Not enough arguments in -c");
					System.exit(1);
				}
				else if(args.length == 2 || args.length == 6){
					n_node_type = Integer.parseInt(args[1]);
					n_nodes_aux += n_node_type;
					System.out.println("n_nodes_aux: " + n_nodes_aux + ", n_node_type: " + n_node_type + ", n_nodes: " + n_nodes);
					if(n_nodes_aux > n_nodes){
						System.out.println("Too many nodes declared:" + args[0]);
						System.exit(1);
					}
					/*get node type*/
					int id = nodeToId(args[0]);
					if(id == -1){ System.out.println("Node type unknown"); System.exit(1);}
					
					/*create the number of nodes */
					if(nod_max[id] <= nod_each[id]){System.out.println("Can't declare so many areas of type " + args[0]); System.exit(1);}
					else {nod_each[id]+=n_node_type;} 		
					/*add to parsing list*/
					for(int k = 0; k < nod_each[id]; k++){
						type_nodes.add(id);
					}
					if(args.length == 6){
						/*set their speeds and pauses*/
						switch(id){
						case 0:
							MachineNode.min_speed = Double.parseDouble(args[2]);
							MachineNode.max_speed = Double.parseDouble(args[3]);
							MachineNode.pause = Integer.parseInt(args[4]);
							MachineNode.repetitions = Integer.parseInt(args[5]);
						case 1:
							OperatorNode.min_speed = Double.parseDouble(args[2]);
							OperatorNode.max_speed = Double.parseDouble(args[3]);
							OperatorNode.pause = Integer.parseInt(args[4]);
							OperatorNode.repetitions = Integer.parseInt(args[5]);
						case 2:
							SupervisorNode.min_speed = Double.parseDouble(args[2]);
							SupervisorNode.max_speed = Double.parseDouble(args[3]);
							SupervisorNode.pause = Integer.parseInt(args[4]);
							SupervisorNode.repetitions = Integer.parseInt(args[5]);
						}
					}
				}
				else{
					System.out.println("Invalid number of parameters");
					System.exit(1);
				}			
			}
			catch(Exception e){
				System.out.println("Could not parse -a args");
				System.exit(1);
			}
			return true;
		case 'w':
			write_one_file = true;
			write_one_name = val;
			return true;
		}
		return super.parseArg(key, val);
	}
	
	
	private void processArguments() {
//			
//		
//		/**TODO: do it for real. now just playing with it.**/
//		parameterData.ignore = 600;
//		n_areas = 6;
//		n_nodes = 38;
//
//		double[] a1_c = {0.0, 800.0, 0.0, 1000.0, 3400.0, 1000.0, 3400.0, 800.0};
//		double[] a1_e = {200.0, 800.0, 1000.0, 800.0, 1100.0, 800.0, 2600.0, 800.0, 2780.0, 800.0, 400.0, 800.0, 700.0, 800.0, 2000.0, 800.0};
//		entries.add(a1_e);
//		corners.add(a1_c);
//		
//		double[] e1_c = {300.0, 0.0, 300.0, 800.0, 900.0, 800.0, 900.0, 0.0};
//		double[] e1_e = {400.0, 800.0, 700.0, 800.0};
//		entries.add(e1_e);
//		corners.add(e1_c);
//		double[] e2_c = {1500.0, 300.0, 1500.0, 800.0, 2400.0, 800.0, 1500.0, 800.0 };
//		double[] e2_e = {2000.0, 800.0};
//		entries.add(e2_e);
//		corners.add(e2_c);
//
//		//int[] nd = {2, 2};
//		//ndumps = nd;
//		
//		Position[] e1 = new Position[2];
//		Position[] e2 = new Position[2];
//		
//		e1[0] = new Position(500,0);
//		e1[1] = new Position(600,800);
//		e2[0] = new Position(2000, 300);
//		e2[1] = new Position(2300,800);
//	
//		dumps.add(e1);
//		dumps.add(e2);
//		
//		double[] m1_c = {0.0, 350.0, 0.0, 800.0, 300.0, 800.0, 300.0, 350.0};
//		double[] m1_e = {200.0, 800.0};
//		entries.add(m1_e);
//		corners.add(m1_c);
//		double[] m2_c = {950.0, 500.0, 950.0, 800.0, 1500.0, 800.0, 1500.0, 500.0};
//		double[] m2_e = {1000.0, 800.0, 1100.0, 800.0};
//		entries.add(m2_e);
//		corners.add(m2_c);
//		double[] m3_c = {2500.0, 600.0, 2500.0, 600.0, 2800.0, 600.0, 2800.0, 600.0};
//		double[] m3_e = {2600.0, 800.0, 2780.0, 800.0};
//		entries.add(m3_e);
//		corners.add(m3_c);
//
//		int[] array = {0,1,1,2,2,2};
//		i_area = array;
//		n_each[0] = 1;
//		n_each[1] = 2;
//		n_each[2] = 3;
//
//		nod_each[0] = 8;
//		nod_each[1] = 20;
//		nod_each[2] = 10;
//		
//		
//		/**UNO DE CADA UNO PARA TESTING**/
//		
//		int[] array2 = {0,1,2};
//		type_nodes = array2;
//		n_nodes = 3;
//		start_pos_nodes = new Position[n_nodes];
//		start_pos_nodes[0] = new Position(600.0, 400.0);
//		start_pos_nodes[1] = new Position(1000.0, 550.0);
//		start_pos_nodes[2] = new Position(500.0, 900.0);	
//		
//		/** Count for nodes**/
//		int[] array2 = {0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,2,2};
//		type_nodes = array2;		
//		
//		start_pos_nodes = new Position[n_nodes];
//		
//		start_pos_nodes[0] = new Position(600.0, 400.0);
//		start_pos_nodes[1] = new Position(600.0, 500.0);
//		start_pos_nodes[2] = new Position(600.0, 600.0);
//		start_pos_nodes[3] = new Position(600.0, 700.0);
//		start_pos_nodes[4] = new Position(1800.0, 400.0);
//		start_pos_nodes[5] = new Position(1800.0, 600.0);
//		start_pos_nodes[6] = new Position(2100.0, 400.0);
//		start_pos_nodes[7] = new Position(2100.0, 600.0);
//		
//		start_pos_nodes[8] = new Position(1000.0, 550.0);
//		start_pos_nodes[9] = new Position(1000.0, 580.0);
//		start_pos_nodes[10] = new Position(1000.0, 610.0);
//		start_pos_nodes[11] = new Position(1000.0, 640.0);
//		start_pos_nodes[12] = new Position(1000.0, 670.0);
//		start_pos_nodes[13] = new Position(1300.0, 550.0);
//		start_pos_nodes[14] = new Position(1300.0, 580.0);
//		start_pos_nodes[15] = new Position(1300.0, 610.0);
//		start_pos_nodes[16] = new Position(1300.0, 640.0);
//		start_pos_nodes[17] = new Position(1300.0, 670.0);
//		
//		start_pos_nodes[18] = new Position(2600.0, 600.0);
//		start_pos_nodes[19] = new Position(2600.0, 625.0);
//		start_pos_nodes[20] = new Position(2600.0, 650.0);
//		start_pos_nodes[21] = new Position(2600.0, 675.0);
//		start_pos_nodes[22] = new Position(2600.0, 700.0);
//		start_pos_nodes[23] = new Position(2700.0, 600.0);
//		start_pos_nodes[24] = new Position(2700.0, 625.0);
//		start_pos_nodes[25] = new Position(2700.0, 650.0);
//		start_pos_nodes[26] = new Position(2700.0, 675.0);
//		start_pos_nodes[27] = new Position(2700.0, 700.0);
//
//		start_pos_nodes[28] = new Position(500.0, 900.0);
//		start_pos_nodes[29] = new Position(800.0, 900.0);
//		start_pos_nodes[30] = new Position(1100.0, 900.0);
//		start_pos_nodes[31] = new Position(1400.0, 900.0);
//		start_pos_nodes[32] = new Position(1700.0, 900.0);
//		start_pos_nodes[33] = new Position(2000.0, 900.0);
//		start_pos_nodes[34] = new Position(2300.0, 900.0);
//		start_pos_nodes[35] = new Position(2600.0, 900.0);
//		start_pos_nodes[36] = new Position(2900.0, 900.0);
//		start_pos_nodes[37] = new Position(3200.0, 900.0);

	}
	
	
	public void write( String _name ) throws FileNotFoundException, IOException {
		super.write(_name);
	}

	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		RandomSpeedBase.printHelp();
		System.out.println( getInfo().name + ":" );
		System.out.println("\t-x <Scenario width>");
		System.out.println("\t-y <Scenario height>");
		System.out.println("\t-d <duration>");
		System.out.println("\t-i <ignore time>");
		System.out.println("\t-A <number of areas>");
		System.out.println("\t-a <area definition - EXT-MAN-ACC + 2 corners + n_entries + entries [+ n_dumps + dumps]>");
		System.out.println("\t-C <number of nodes>");
		System.out.println("\t-c <node definition - LH-OP-SU + [min_speed max_speed avg_pause repetitions]");
		System.out.println("\t-w <write addition theOne movement file>");
	}

/**TODO: write this again**/
//write coordinates of obstacles, areas and node movements to a file
//	public void mywrite(){
//		int t = 4;         // use obstacles from 3, cause TEL does not contain moving nodes
//		try {
//	        PrintWriter mywriter = new PrintWriter(new BufferedWriter(new FileWriter("MineOutput.txt")));
//			System.out.println("printing to " + "MineOutput.txt");
//			for(int i=0; i < mineAreas.length;i++){
//				mywriter.write("<-----Area----->");
//				mywriter.write(mineAreas[i].toString());
//				mywriter.write("\n");
//			}
//			for(int i=0; i < mineAreas[t].obstacles.length; i++){
//				mywriter.write("<-----Obstacle----->");
//				mywriter.write(mineAreas[t].obstacles[i].VerticesToString());
//				mywriter.write("\n");
//			}
//			if (write_vis) { //write information for VisibilityGraph
//				for(int i = 0; i < ((LinkedList)mineAreas[t].Graph.get(0)).size(); i++){
//					Position key = (Position)((LinkedList)mineAreas[t].Graph.get(0)).get(i);
//
//					for(int j = 0; j < ((LinkedList)((PositionHashMap)mineAreas[t].Graph.get(1)).get(key)).size(); j++){
//						mywriter.write("<-----VisibilityGraph-----> ");
//						mywriter.write(((Line2D.Double)((LinkedList)((PositionHashMap)mineAreas[t].Graph.get(1)).get(key)).get(j)).x1 + " " 
//								 	 + ((Line2D.Double)((LinkedList)((PositionHashMap)mineAreas[t].Graph.get(1)).get(key)).get(j)).y1 + " " 
//									 + ((Line2D.Double)((LinkedList)((PositionHashMap)mineAreas[t].Graph.get(1)).get(key)).get(j)).x2 + " " 
//									 + ((Line2D.Double)((LinkedList)((PositionHashMap)mineAreas[t].Graph.get(1)).get(key)).get(j)).y2);
//						mywriter.write("\n");
//					}
//				}
//				System.out.println("<--VisibilityGraph-->");
//				//end VisibilityGraph
//			}
//			if (write_moves) {
//				for(int i=0; i < parameterData.nodes.length; i++){
//					String movement = parameterData.nodes[i].movementString(parameterData.outputDim);
//					mywriter.write("Movement ");
//					mywriter.write(movement);
//					mywriter.write("\n");
//				}
//			}
//			mywriter.close();
//		} catch (Exception e) {
//			System.out.println("Something is wrong with the File specified");
//			System.exit(0);
//		}
//	}



}