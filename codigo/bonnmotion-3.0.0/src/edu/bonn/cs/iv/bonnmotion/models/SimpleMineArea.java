package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Random;

import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;
import edu.bonn.cs.iv.bonnmotion.models.mine.*;

/** Application to create movement scenarios according to the Simple Mine Area model. */

public class SimpleMineArea extends RandomSpeedBase {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("SimpleMineArea");
        info.description = "Simulates an Underground Mine";
        
        info.major = 0;
        info.minor = 1;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 1 $");
        
        info.contacts.add("jborn@dcc.uchile.cl");
        info.authors.add("Javiera Born");
        info.authors.add("");
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
	protected double[][] corners;
	protected double[][] entries; 
	
	/** Manage the Areas and nodes . */
	protected MineArea[] mineAreas = null;
	protected MineNode[] mineNodes = null;
	
	/**Area indices for each type **/
	protected int[] candidate_areas_acc =  MineArea.findArea(0,mineAreas);
	protected int[] candidate_areas_ext =  MineArea.findArea(1,mineAreas);
	protected int[] candidate_areas_man =  MineArea.findArea(2,mineAreas);

	
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

	public SimpleMineArea( String[] args ) {
		super.go(args);
		generate();
	}

	public MineArea currentArea(Position p) throws Exception{
		for(int i=0; i < mineAreas.length; i++){
			if (mineAreas[i].contains(p.x, p.y));
			return mineAreas[i];
		}
		throw new Exception("ERROR: Position outside of all areas");
	}
	
	

	public void generate() {
		
		this.processArguments();
		/*AREAS: asumimos que los arreglos de corners, entries, y n_areas ahora existen y tienen valores válidos.*/
		/*NODOS: asumimos que están especificados los tipos que tenemos que crear y donde parten.*/
		preGeneration();
		
		/*TODO: verificar condiciones*/
		
		mineAreas = new MineArea[n_areas];
		
		for(int i = 0; i < n_areas; i++){
			mineAreas[i] = MineArea.getInstance(corners[i], entries[i], i_area[i]);
			System.out.println("New MineArea completed: " + MineArea.getArea(i_area[i]));
		}
		
		/*NODES*/
		System.out.println("Creating Nodes areas...");
		mineNodes = new MineNode[n_nodes];
		Random r = new Random();
		for(int i = 0; i < n_nodes; i++){
			switch(type_nodes[i]){
			case 0: /*Machine*/
			{	//pick an extraction area
				int area = r.nextInt(this.candidate_areas_ext.length);
				Position p = mineAreas[area].getRandomPosition();
				/*TODO: get a real position in the area */
				mineNodes[i] = new MachineNode(p, mineAreas[area]);				
			}
			case 1: /*Operator*/
			{ 	//pick maintenance areas
				MineArea[] areas = (MineArea[]) Arrays.stream(mineAreas).filter(x -> x.type==1).toArray();
				int start = r.nextInt(areas.length);
				Position p = mineAreas[start].getRandomPosition();
				mineNodes[i] = new OperatorNode(p, areas, mineAreas[start]);		
			}
				
			case 2: /*Supervisor*/
			{ 	//always starts in ACC = 0
				Position p = mineAreas[0].getRandomPosition();
				mineNodes[i] = new SupervisorNode(p, mineAreas, mineAreas[0]);		
			}
				
			default:
			{ 
				System.err.println("Node type doesn't exist:" + type_nodes[i]);
				System.exit(1);
			}
			}
			
		}

		/*Do the things*/

		int t = 0;
		MineNode ref = null;
			while (t < parameterData.duration) {
				
			for(int i=0; i< mineNodes.length; i++){
				ref = mineNodes[i];
				ref.print();
				/*DO THE THINGS*/
			}
			}

				
		// write the nodes into our base
		//this.parameterData.nodes = node;
		//if(shallwrite) mywrite();
		postGeneration();
	}

	protected boolean parseArg(String key, String value) {
		return true;
		/**
		if ( key.equals("groupsize_E") ) {
			this.avgMobileNodesPerGroup = Double.parseDouble(value);
			System.out.println("avgMobileNodesPerGroup will not be considered in this model, because the group sizes depend on the areas");
			return true;
		} else if (	key.contains("catatastropheArea") ) {
			this.miningAreaArgs.add(value);
			return true;
		} else if (	key.equals("pGroupChange") ) {
			this.pGroupChange = Double.parseDouble(value);
			System.out.println("Group Change will not be considered in this model, because the groups belong to areas");
			return true;
		} else if (	key.equals("maxCataAreas") ) {
			this.maxCatastropheAreas = (int) Double.parseDouble(value);
			return true;
		} else if (	key.equals("circleVertices") ) {
			this.circlevertices = (int) Double.parseDouble(value);
			return true; // TODO: Fehlt bei parseArgs(-g) ?
		} else if (	key.equals("maxspeed") ) {
			System.out.println("In this model you can't specify maxspeed using area dependend speed");
			return true;
		} else if (	key.equals("factor") ) {
			this.factor = Double.parseDouble(value);
			return true;
		} else if (	key.equals("minspeed") ) {
			System.out.println("In this model you can't specify minspeed using area dependend speed");
			return true;
		} else if (	key.equals("obstacleForAllGrps") ) {
			double[] obstacleParams;
			obstacleParams = parseDoubleArray(value);
			for(int i = 0; i < obstacleParams.length; i = i+2){
				if(obstacleParams[i] > parameterData.x){
					System.out.println("Obstacles' x-coordinates should be in scenario range");
					System.exit(0);
				}
			}
			for(int i = 1; i < obstacleParams.length; i = i+2){
				if(obstacleParams[i] > parameterData.y){
					System.out.println("Obstacles' y-coordinates should be in scenario range");
					System.exit(0);
				}
			}
			Obstacle newone = new Obstacle(obstacleParams);
			for (int i = 0; i < 5; i++) { // for each type of area
				obstacles[i].add(newone);
			}
			onlyOneObstacle = true;
			return true;
		} else if (	key.contains("obstacleForOnlyOneGroup") ) {
			double[] help_param = parseDoubleArray(value);
			double[] obstacleParams_group = new double[help_param.length-1];
			// last parameter is the number of the group to add the obstacle
			System.arraycopy(help_param, 0, obstacleParams_group, 0, help_param.length - 1);
			for(int i = 0; i < obstacleParams_group.length; i = i+2) {
				if(obstacleParams_group[i] > parameterData.x){
					System.out.println("Obstacles' x-coordinates should be in scenario range");
					System.exit(0);
				}
			}
			for(int i = 1; i < obstacleParams_group.length; i = i+2){
				if(obstacleParams_group[i] > parameterData.y){
					System.out.println("Obstacles' y-coordinates should be in scenario range");
					System.exit(0);
				}
			}
			Obstacle newone_group = new Obstacle(obstacleParams_group);
			this.obstacles[(int)(help_param[help_param.length-1])].add(newone_group);
			onlyOneObstacle = false;
			if (help_param[help_param.length-1] == 4) {
				addObsT4 = true;
			}
			return true;
		} else if (	key.equals("mindist") ) {
			this.mindist = Double.parseDouble(value);
			return true;
		} else if (	key.equals("maxdist") ) {
			this.maxdist = Double.parseDouble(value);
			this.oldmaxdist = maxdist;
			return true;
		} else if (	key.equals("groupsize_S") ) {
			this.groupSizeDeviation = Double.parseDouble(value);
			System.out.println("Group Size Deviation will not be considered in this model, because the group sizes depend on the areas");
			return true;
		} else if (	key.equals("writeMoves") ) {
			boolean writeMoves = Boolean.parseBoolean(value);
			if (writeMoves) {
				this.shallwrite = true;
				this.write_moves = true;
			}
			return true;
		} else if (	key.equals("writeVisibilityGraph") ) {
			boolean writeVisibilityGraph = Boolean.parseBoolean(value);
			if (writeVisibilityGraph) {
				this.shallwrite = true;
				this.write_vis = true;
			}
			return true;
		} else if (	key.equals("noKnockOver") ) {
			boolean knock = Boolean.parseBoolean(value);
			if (knock) {
				this.no_knock_over = true;
			}
			return true;
		} else {
			return super.parseArg(key, value);
		}
		**/
	}

	
	
	public void write( String _name ) throws FileNotFoundException, IOException {
		/**
		int obsCount = 1;
		if (!onlyOneObstacle) {
			obsCount = 0;
			for (int i = 0; i < 5; i++) {
				if (obstacles[i].size() != 0) obsCount++;
			}
			if (!addObsT4) {
				obsCount--;
			}
		}
		
		String[] p = new String[11 + maxCatastropheAreas + obsCount];
		
		int idx = 0;
		
		p[idx++] = "groupsize_E=" + avgMobileNodesPerGroup;
		
		for (int i = 0; i < mineAreas.length; i++) {
			CatastropheArea area = mineAreas[i];
			double[] params = area.getPolygonParams();
			String paramsAsString = "";
			for (int j = 0; j < params.length; j++) {
				paramsAsString += (int)params[j] + ",";
			}
			String APPBorderEntryExit = "";
			if (area.type == 4) {
				APPBorderEntryExit = (int)area.borderentry.x + "," + (int)area.borderentry.y + "," + 
										(int)area.borderexit.x + "," + (int)area.borderexit.y + ",";
			}
			p[idx++] = "catatastropheArea" + i + "=" + paramsAsString + APPBorderEntryExit + 
						(int)area.entry.x + "," + (int)area.entry.y + "," + 
						(int)area.exit.x  + "," + (int)area.exit.y + "," + 
						area.type + "," + area.wantedgroups + "," + area.numtransportgroups;
		}

		p[idx++] = "pGroupChange=" + pGroupChange;
		p[idx++] = "maxCataAreas=" + maxCatastropheAreas;
		p[idx++] = "circleVertices=" + circlevertices;
		p[idx++] = "factor=" + factor;
		
		if (obstacles != null && obstacles.length > 0) {
			
			if (onlyOneObstacle) {
				
				double[] obsVertices = obstacles[0].getFirst().getVertices();
				String obsVerticesAsString = "";
				for (int i = 0; i < obsVertices.length; i++) {
					obsVerticesAsString += (int) obsVertices[i];
					if (i < obsVertices.length - 1) {
						obsVerticesAsString += ",";
					}
				}
				p[idx++] = "obstacleForAllGrps=" + obsVerticesAsString;
				
			} else {
				
				for (int i = 0; i < obstacles.length; i++) {
					
					if (obstacles[i].size() != 0) {
						
						if (i == 4 && !addObsT4) {
							continue;
						}
						
						double[] obsVertices = obstacles[i].getFirst().getVertices();
						String obsVerticesAsString = "";
						for (int j = 0; j < obsVertices.length; j++) {
							obsVerticesAsString += (int) obsVertices[j];
							if (j < obsVertices.length - 1) {
								obsVerticesAsString += ",";
							}
						}
						p[idx++] = "obstacleForOnlyOneGroup" + i + "=" + obsVerticesAsString + "," + i;
						
					} 
				}
				
			}
		}
		p[idx++] = "mindist=" + mindist;
		p[idx++] = "maxdist=" + maxdist;
		p[idx++] = "groupsize_S=" + groupSizeDeviation;		
		p[idx++] = "writeMoves=" + (shallwrite && write_moves);
		p[idx++] = "writeVisibilityGraph=" + (shallwrite && write_vis);
		p[idx] = "noKnockOver=" + no_knock_over;

		PrintWriter changewriter = new PrintWriter(new BufferedWriter(new FileWriter(_name + ".changes")));
		Iterator<Map.Entry<Integer, Object>> it = statuschanges.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Integer, Object> entry = it.next();
			Integer key = (entry.getKey());
			changewriter.write(key.toString());
			changewriter.write(" ");
		}
		changewriter.close();

		super.write(_name, p);
		//super.write(_name, p, statuschanges);
		 **/
	}

	/**
	 * Processes all arguments that depend on each other in some way
	 * that were skipped parseArg(char, String)
	 */
	private void processArguments() {
//		
//		// check if there are not too many catastrophe areas specified
//		if(this.miningAreaArgs.size() > this.maxCatastropheAreas){
//			System.out.println("Only " + maxCatastropheAreas + " CatastropheAreas permitted in this model");
//			System.exit(0);
//		}
//		// reserve space for catastrophe areas
//		this.mineAreas = new CatastropheArea[maxCatastropheAreas];
//		// process catastrophe area arguments
//		for (String arg : this.miningAreaArgs) {
//                        ++this.nummineAreas;
//			/** fetch params for Area. */
//			double[] areaParams = parseDoubleArray(arg);
//			// last three params are no coordinates
//			int check_till = areaParams.length - 3;
//			if (areaParams[check_till] == 4) {
//				// type is ambulance parking point, the last 12 parameters are no coordinates
//				check_till = areaParams.length - 12;
//			}
//			// check if coordinates are valid for scenario
//			for(int i = 0; i < check_till; i = i+2){
//				if(areaParams[i] > parameterData.x - this.maxdist || areaParams[i] < 0 + this.maxdist){
//					System.out.println("Areas' x-coordinates should be in scenario range and not too near to border");
//					System.out.println("Area-Type: " + areaParams[areaParams.length - 3]+"maxdist " + maxdist + " Params " + areaParams[i]);
//					System.exit(0);
//				}
//			}
//			for(int i = 1; i < check_till; i = i+2){
//				if(areaParams[i] > parameterData.y - this.maxdist || areaParams[i] < 0 + this.maxdist){
//					System.out.println("Areas' y-coordinates should be in scenario range and not too near to border");
//					System.out.println("Area-Type: " + areaParams[areaParams.length - 3] + " maxdist " + maxdist + " Params " + areaParams[i]);
//					System.exit(0);
//				}
//			}
//			// get the current catastrophe area instance
//			mineAreas[nummineAreas-1] = CatastropheArea.GetInstance(areaParams);
//		}
//		// arguments not needed anymore
//		this.miningAreaArgs = null;
	}

	protected boolean parseArg(char key, String val) {
		return false;
//		switch (key) {
//		case 'a': 
//			// "avgMobileNodesPerGroup"
//			this.avgMobileNodesPerGroup = Double.parseDouble(val);
//			System.out.println("avgMobileNodesPerGroup will not be considered in this model, because the group sizes depend on the areas");
//			return true;
//		case 'b': // "specify catastrophe areas"
//			this.miningAreaArgs.add(val);
//			// further processing will be done later in processArguments
//			return true;
//		case 'c': // "change"
//			this.pGroupChange = 0;
//			System.out.println("Group Change will not be considered in this model, because the groups belong to areas");
//			return true;
//		case 'e': // "MaxCatastropheAreas"
//			this.maxCatastropheAreas = (int)Double.parseDouble(val);
//			// to avoid errors space for catastrophe areas will be reserved in processArguments
//			return true;
//		case 'g': // "Vertices to approximate circle"
//			this.circlevertices = (int)Double.parseDouble(val);
//		case 'h': //maxspeed
//			System.out.println("In this model you can't specify maxspeed using area dependend speed");
//			return true;
//		case 'j': //factor to multiply MinCObstacle paths with
//			this.factor = Double.parseDouble(val);
//			return true;
//		case 'l': //minspeed
//			System.out.println("In this model you can't specify minspeed using area dependend speed");
//			return true;
//		case 'o': // obstacle for all groups
//			/** fetch params for Obstacle. */
//			double[] obstacleParams;
//			obstacleParams = parseDoubleArray(val);
//			for(int i = 0; i < obstacleParams.length; i = i+2){
//				if(obstacleParams[i] > parameterData.x){
//					System.out.println("Obstacles' x-coordinates should be in scenario range");
//					System.exit(0);
//				}
//			}
//			for(int i = 1; i < obstacleParams.length; i = i+2){
//				if(obstacleParams[i] > parameterData.y){
//					System.out.println("Obstacles' y-coordinates should be in scenario range");
//					System.exit(0);
//				}
//			}
//			Obstacle newone = new Obstacle(obstacleParams);
//			for (int i = 0; i < 5; i++) { // for each type of area
//				obstacles[i].add(newone);
//			}
//			onlyOneObstacle = true;
//			return true;
//		case 'O': //obstacle for only one group
//			/** fetch params for Obstacle. */
//			double[] help_param = parseDoubleArray(val);
//			double[] obstacleParams_group = new double[help_param.length-1];
//			// last parameter is the number of the group to add the obstacle
//			System.arraycopy(help_param, 0, obstacleParams_group, 0, help_param.length - 1);
//			for(int i = 0; i < obstacleParams_group.length; i = i+2) {
//				if(obstacleParams_group[i] > parameterData.x){
//					System.out.println("Obstacles' x-coordinates should be in scenario range");
//					System.exit(0);
//				}
//			}
//			for(int i = 1; i < obstacleParams_group.length; i = i+2){
//				if(obstacleParams_group[i] > parameterData.y){
//					System.out.println("Obstacles' y-coordinates should be in scenario range");
//					System.exit(0);
//				}
//			}
//			Obstacle newone_group = new Obstacle(obstacleParams_group);
//			this.obstacles[(int)(help_param[help_param.length-1])].add(newone_group);
//			onlyOneObstacle = false;
//			if (help_param[help_param.length-1] == 4) {
//				addObsT4 = true;
//			}
//			return true;
//		case 'q': // "min space for group to accept way as valid"
//			this.mindist = Double.parseDouble(val);
//			return true;
//		case 'r': // "random vector max length"
//			this.maxdist = Double.parseDouble(val);
//			this.oldmaxdist = maxdist;
//			return true;
//		case 's': // "groupSizeDeviation"
//			this.groupSizeDeviation = Double.parseDouble(val);
//			System.out.println("Group Size Deviation will not be considered in this model, because the group sizes depend on the areas");
//			return true;
//		case 'w': //write all important aspects to file
//			this.shallwrite = true;
//			this.write_moves = true;
//			return true;
//		case 'v': //write all important aspects to file
//			this.shallwrite = true;
//			this.write_vis = true;
//			return true;
//		case 'K': // do not knock over pedestrians - no ambulances in areas
//			this.no_knock_over = true;
//			return true;
//		default:
//			return super.parseArg(key, val);
//		}
	}

//	public static void printHelp() {
//	    System.out.println(getInfo().toDetailString());
//		RandomSpeedBase.printHelp();
//		System.out.println( getInfo().name + ":" );
//		System.out.println("\t-a <average no. of nodes per group>");
//		System.out.println("\t-b <catastrophe area (can be used multiple times for several catastrophe areas)>");
//		System.out.println("\t-c <group change probability>");
//		System.out.println("\t-e <max catastrophe areas>");
//		System.out.println("\t-r <max. distance to group center>");
//		System.out.println("\t-s <group size standard deviation>");
//		System.out.println("\t-O <obstacle for only one group (specified in last param)>");
//		System.out.println("\t-w <write vis. info to file & show movements>");
//		System.out.println("\t-v <write vis. info to file & show vis.graph>");
//		System.out.println("\t-K <do not knock over pedestrians - no ambulances in areas beside APP>");
//	}

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
