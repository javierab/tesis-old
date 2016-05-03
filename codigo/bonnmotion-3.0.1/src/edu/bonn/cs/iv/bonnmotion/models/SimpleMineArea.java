package edu.bonn.cs.iv.bonnmotion.models;
import java.text.*;

import java.io.FileNotFoundException;
import java.io.IOException;
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
	protected int n_nodes = 0;
	
	/** Count for each area**/
	protected int[] i_area;
	protected int[] n_each = new int[3];
	protected int[] n_max = {1, 2, 4}; /* 1 access, 2 ext, 4 mant*/
	
	/** Count for nodes**/
	protected int[] nod_each = new int[3];
	protected int[] nod_max = {4*n_max[1], 50, 10}; /* 4*ext maquina, 50 mantenimiento, 10 supervisor*/
	protected int[] type_nodes = null;
	protected Position[] start_pos_nodes = null;

	
	/** Data to build each area **/
	protected LinkedList<double[]> corners = new LinkedList<double[]>();
	protected LinkedList<double[]> entries = new LinkedList<double[]>(); 
	
	/** Dumps for extaction areas**/
	protected int[] ndumps;
	protected LinkedList<Position[]> dumps = new LinkedList<Position[]>();
	
	/** Manage the Areas and nodes . */
	public MineArea[] mineAreas = null;
	public Access accArea = null;
	public Extraction[] extAreas = null;
	public Maintenance[] manAreas = null;

	public MineNode[] mineNodes = null;
	
	/**Area indices for each type **/
	protected int[] candidate_areas_acc =  null;
	protected int[] candidate_areas_ext =  null;
	protected int[] candidate_areas_man =  null;

	
	/** temporary saves the arguments for the catastrophe areas */
	//private LinkedList<String> miningAreaArgs = new LinkedList<String>();
	
	/** initialize dst . */
	Position dst = new Position(0,0);
	/** remember maxpause . */
	double oldmaxpause = 0;
	/** remember maxdist . */
	double oldmaxdist = 0;
	/** decide whether to write in file or not */
	boolean shallwrite = false;
	boolean write_moves = false;
	boolean write_vis = false;
	/** areas are also seen as obstacles for APP */
	boolean no_knock_over = false;
	/** number of vertices to approximate circle */
	int circlevertices = 6;
	/** remember nodes' status changes */

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
		/*AREAS: asumimos que los arreglos de corners, entries, y n_areas ahora existen y tienen valores válidos.*/
		preGeneration();
		
		/**TODO: sacar esto!!**/
		parameterData.ignore = 0.0;
		
		/*TODO: verificar condiciones*/
		
		mineAreas = new MineArea[n_areas];
		extAreas = new Extraction[n_each[1]];
		manAreas = new Maintenance[n_each[2]];
		int e = 0;
		int m = 0;
		
		for(int i = 0; i < n_areas; i++){
			if(i_area[i] == 0){ /*access*/
				mineAreas[i] = MineArea.getInstance(corners.get(i), entries.get(i), 0);
				accArea = (Access) mineAreas[i];
			}
			else if(i_area[i] == 1){ /*extraction*/
				mineAreas[i] = MineArea.getInstance(corners.get(i), entries.get(i), 1);
				((Extraction)mineAreas[i]).setDumps(ndumps[e], dumps.get(e));
				extAreas[e] = (Extraction) mineAreas[i];
				e++;
				
			}
			else if(i_area[i] == 2){
				mineAreas[i] = MineArea.getInstance(corners.get(i), entries.get(i), 2);
				manAreas[m] = (Maintenance) mineAreas[i];
				m++;
			}
			
			//System.out.println("New MineArea completed: " + mineAreas[i].getType());
		}
		
		
		/*NODES*/
		//System.out.println("Creating Nodes...");
		mineNodes = new MineNode[n_nodes];
		Random r = new Random();
		for(int i = 0; i < n_nodes; i++){
			if(type_nodes[i] == 0){ /*Machine*/
				int area = r.nextInt(extAreas.length);
				Position p = extAreas[area].getRandomPosition();
				mineNodes[i] = new MachineNode(p, extAreas[area]);		
			}
			else if(type_nodes[i] == 1){ /*Operator*/
				int area = r.nextInt(manAreas.length);
				Position p = manAreas[area].getRandomPosition();
				mineNodes[i] = new OperatorNode(p, manAreas, manAreas[area], accArea);		
			}
			else if(type_nodes[i] == 2){ /*Supervisor*/
			 	//always starts in ACC = 0
				Position p = mineAreas[0].getRandomPosition();
				mineNodes[i] = new SupervisorNode(p, mineAreas, mineAreas[0], accArea);		
			}
			else{ 
				System.err.println("Node type doesn't exist:" + type_nodes[i]);
				System.exit(1);
			}
			System.out.println("Node created: " + mineNodes[i].getType());
			
		}

		parameterData.nodes = mineNodes;
		/*Do the things*/

		double t = 0.0;
		Position p;
		
		while(t < parameterData.duration){
			for(int i=0; i< mineNodes.length; i++){
				p = mineNodes[i].getNextStep();
				mineNodes[i].add(t, p);
				parameterData.nodes[i].add(t, p);
				//System.out.println("t: " + t + " node " + mineNodes[i].getType() + ":" + i + " pos:" + p.toString(2));
			}
			t+=1.0;
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

	
	protected boolean parseArg(String key, String value) {
		return super.parseArg(key, value);
	}
	
	
	private void processArguments() {
//		
		/**TODO: do it for real. now just playing with it.**/
		parameterData.ignore = 600;
		n_areas = 6;
		n_nodes = 38;

		double[] a1_c = {0.0, 800.0, 0.0, 1000.0, 3400.0, 1000.0, 3400.0, 800.0};
		double[] a1_e = {200.0, 800.0, 1000.0, 800.0, 1100.0, 800.0, 2600.0, 800.0, 2780.0, 800.0, 400.0, 800.0, 700.0, 800.0, 2000.0, 800.0};
		entries.add(a1_e);
		corners.add(a1_c);
		
		double[] e1_c = {300.0, 0.0, 300.0, 800.0, 900.0, 800.0, 900.0, 0.0};
		double[] e1_e = {400.0, 800.0, 700.0, 800.0};
		entries.add(e1_e);
		corners.add(e1_c);
		double[] e2_c = {1500.0, 300.0, 1500.0, 800.0, 2400.0, 800.0, 1500.0, 800.0 };
		double[] e2_e = {2000.0, 800.0};
		entries.add(e2_e);
		corners.add(e2_c);

		int[] nd = {2, 2};
		ndumps = nd;
		
		Position[] e1 = new Position[2];
		Position[] e2 = new Position[2];
		
		e1[0] = new Position(500,0);
		e1[1] = new Position(600,800);
		e2[0] = new Position(2000, 300);
		e2[1] = new Position(2300,800);
	
		dumps.add(e1);
		dumps.add(e2);
		
		double[] m1_c = {0.0, 350.0, 0.0, 800.0, 300.0, 800.0, 300.0, 350.0};
		double[] m1_e = {200.0, 800.0};
		entries.add(m1_e);
		corners.add(m1_c);
		double[] m2_c = {950.0, 500.0, 950.0, 800.0, 1500.0, 800.0, 1500.0, 500.0};
		double[] m2_e = {1000.0, 800.0, 1100.0, 800.0};
		entries.add(m2_e);
		corners.add(m2_c);
		double[] m3_c = {2500.0, 600.0, 2500.0, 600.0, 2800.0, 600.0, 2800.0, 600.0};
		double[] m3_e = {2600.0, 800.0, 2780.0, 800.0};
		entries.add(m3_e);
		corners.add(m3_c);

		int[] array = {0,1,1,2,2,2};
		i_area = array;
		n_each[0] = 1;
		n_each[1] = 2;
		n_each[2] = 3;

		nod_each[0] = 8;
		nod_each[1] = 20;
		nod_each[2] = 10;
		
		
		/**UNO DE CADA UNO PARA TESTING**/
		
//		int[] array2 = {0,1,2};
//		type_nodes = array2;
//		n_nodes = 3;
//		start_pos_nodes = new Position[n_nodes];
//		start_pos_nodes[0] = new Position(600.0, 400.0);
//		start_pos_nodes[1] = new Position(1000.0, 550.0);
//		start_pos_nodes[2] = new Position(500.0, 900.0);
//		
		
		/** Count for nodes**/
		int[] array2 = {0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,2,2};
		type_nodes = array2;
		
		
		start_pos_nodes = new Position[n_nodes];
		
		start_pos_nodes[0] = new Position(600.0, 400.0);
		start_pos_nodes[1] = new Position(600.0, 500.0);
		start_pos_nodes[2] = new Position(600.0, 600.0);
		start_pos_nodes[3] = new Position(600.0, 700.0);
		start_pos_nodes[4] = new Position(1800.0, 400.0);
		start_pos_nodes[5] = new Position(1800.0, 600.0);
		start_pos_nodes[6] = new Position(2100.0, 400.0);
		start_pos_nodes[7] = new Position(2100.0, 600.0);
		
		start_pos_nodes[8] = new Position(1000.0, 550.0);
		start_pos_nodes[9] = new Position(1000.0, 580.0);
		start_pos_nodes[10] = new Position(1000.0, 610.0);
		start_pos_nodes[11] = new Position(1000.0, 640.0);
		start_pos_nodes[12] = new Position(1000.0, 670.0);
		start_pos_nodes[13] = new Position(1300.0, 550.0);
		start_pos_nodes[14] = new Position(1300.0, 580.0);
		start_pos_nodes[15] = new Position(1300.0, 610.0);
		start_pos_nodes[16] = new Position(1300.0, 640.0);
		start_pos_nodes[17] = new Position(1300.0, 670.0);
		
		start_pos_nodes[18] = new Position(2600.0, 600.0);
		start_pos_nodes[19] = new Position(2600.0, 625.0);
		start_pos_nodes[20] = new Position(2600.0, 650.0);
		start_pos_nodes[21] = new Position(2600.0, 675.0);
		start_pos_nodes[22] = new Position(2600.0, 700.0);
		start_pos_nodes[23] = new Position(2700.0, 600.0);
		start_pos_nodes[24] = new Position(2700.0, 625.0);
		start_pos_nodes[25] = new Position(2700.0, 650.0);
		start_pos_nodes[26] = new Position(2700.0, 675.0);
		start_pos_nodes[27] = new Position(2700.0, 700.0);

		start_pos_nodes[28] = new Position(500.0, 900.0);
		start_pos_nodes[29] = new Position(800.0, 900.0);
		start_pos_nodes[30] = new Position(1100.0, 900.0);
		start_pos_nodes[31] = new Position(1400.0, 900.0);
		start_pos_nodes[32] = new Position(1700.0, 900.0);
		start_pos_nodes[33] = new Position(2000.0, 900.0);
		start_pos_nodes[34] = new Position(2300.0, 900.0);
		start_pos_nodes[35] = new Position(2600.0, 900.0);
		start_pos_nodes[36] = new Position(2900.0, 900.0);
		start_pos_nodes[37] = new Position(3200.0, 900.0);

	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
		case 'h': //maxspeed
			System.out.println("In this model you can't specify maxspeed using area dependend speed");
			return true;
		case 'l': //minspeed
			System.out.println("In this model you can't specify minspeed using area dependend speed");
			return true;
		case 'w': //write all important aspects to file
			this.shallwrite = true;
			this.write_moves = true;
			return true;
		case 'v': //write all important aspects to file
			this.shallwrite = true;
			this.write_vis = true;
			return true;
		default:
			return super.parseArg(key, val);
		}
	}
	
	
	public void write( String _name ) throws FileNotFoundException, IOException {
		super.write(_name);
	}

	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		RandomSpeedBase.printHelp();
		System.out.println( getInfo().name + ":" );
		System.out.println("\t-a <average no. of nodes per group>");
		System.out.println("\t-b <catastrophe area (can be used multiple times for several catastrophe areas)>");
		System.out.println("\t-c <group change probability>");
		System.out.println("\t-e <max catastrophe areas>");
		System.out.println("\t-r <max. distance to group center>");
		System.out.println("\t-s <group size standard deviation>");
		System.out.println("\t-O <obstacle for only one group (specified in last param)>");
		System.out.println("\t-w <write vis. info to file & show movements>");
		System.out.println("\t-v <write vis. info to file & show vis.graph>");
		System.out.println("\t-K <do not knock over pedestrians - no ambulances in areas beside APP>");
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