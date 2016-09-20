/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.javi;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.AbstractMap.SimpleEntry;

import routing.JaviRouter;
import core.Coord;
import core.SimClock;


import java.util.ArrayList;

/**
 * Class for storing and manipulating the global maps computed.
 */
public class GlobalMap {
	
	/**
	 * synced:
	 * -1 if not synced -> must compute new map
	 * 0 if synced with my own map
	 * 1 if not synced, using someone else's map -> must merge maps
	 */
	public int myID, synced, mapCount, ref_id;
	public Coord ref_c;
	public boolean mapExists;
	private Map<Integer, Double> dists;
	private static HashMap<Integer, Coord> staticNodes;
	private ArrayList<Integer> myMapNodes;
	private NodePositionsSet myMap;
	
	public Map<Integer, NodePositionsSet> allMaps;
	public Map<Integer, Coord> globalMap, oldMap;
	
	private double creationTime, updateTime;
	
	public GlobalMap(NodePositionsSet myMap, int myID, HashMap<Integer, Coord> staticNodes){
	
		this.myID = myID;
		this.myMap = myMap;
		this.synced = -1;
		this.ref_id = -1;
		this.mapExists = false;
		this.staticNodes = staticNodes;
		
		this.dists = new HashMap<Integer, Double>();
		this.myMapNodes = new ArrayList<Integer>();
		this.allMaps = new HashMap<Integer, NodePositionsSet>();
		this.globalMap = new HashMap<Integer, Coord>();
		this.creationTime = SimClock.getTime();
		this.updateTime = SimClock.getTime();
	}

	public GlobalMap(){
		this.myID = 0;
		this.myMap = null;
		this.synced = -1;
		this.ref_id = -1;
		this.mapExists = false;
		this.staticNodes = new HashMap<Integer, Coord>();
		
		this.dists = new HashMap<Integer, Double>();
		this.myMapNodes = new ArrayList<Integer>();
		this.allMaps = new HashMap<Integer, NodePositionsSet>();
		this.globalMap = new HashMap<Integer, Coord>();
		this.creationTime = SimClock.getTime();
		this.updateTime = SimClock.getTime();

	}

	/*update my own local map and recalculate the global map*/
	public int updateMap(NodePositionsSet myMap){
		/*check updated ID*/
		if(myID == 0){
			core.Debug.p("must set myID in global map before updating");
			return -1;
		}
		this.myMap = myMap;
		synced=-1;
		return 0;
	}	
	
	public void setDists(Map<Integer, Double> dists){
		this.dists = dists;
	}
	
	/*update my map with someone else's local map*/
	public int addMap(NodePositionsSet otherMap, int otherID){
		allMaps.put(otherID, otherMap);
		synced = -1;
		return 0;
	}
	
	/*update my map with someone else's global map*/
	public int addMap(GlobalMap otherMap){
		/*if i don't have a map yet, i use the one i got*/
		if(mapExists == false){
			globalMap = otherMap.globalMap;
			myMapNodes = otherMap.myMapNodes;
			synced = 1;
			mapExists = true;
			return 1;
		}
		/*TODO: try to update my map if i need to*/
		
		/*if i have an updated map, just join them preferring the one that used newer data*/
		else if(synced == 0 || synced == 1){
			boolean newer = this.updateTime < otherMap.updateTime ? true : false;
			for(Map.Entry<Integer, Coord> node : otherMap.globalMap.entrySet()){
				if(globalMap.containsKey(node.getKey())){
					if(newer) globalMap.replace(node.getKey(), node.getValue());
				}
				else{
					globalMap.put(node.getKey(), node.getValue());
					myMapNodes.add(node.getKey());
				}
			}
			return myMapNodes.size();
		}
		else return -1;
	}

	public void setID(int myid){
		this.myID = myid;
	}
	
	public int getID(){
		return this.myID;
	}
	
	public void setStaticNodes(HashMap<Integer, Coord> nodes){
		staticNodes = nodes;
	}
	
	public Map<Integer, Coord> getMap(){
		if(!mapExists || globalMap == null){
			return new HashMap<Integer, Coord>();
		}
		return globalMap;
	}
	
	/*make my global map. first create a map to adjust my map to real coordinates, then 'add' the nodes from the other localmaps*/
	public int makeGlobal(){
		
		//TODO: usar 3 nodos estáticos para calcular la posicion de mi ID en mi mapa 'real' y agregarlo antes de computar los otros.
		Map<Integer, Coord> m = new HashMap<Integer, Coord>();
		ArrayList<Integer> staticNBs = new ArrayList<Integer>();
		/*first find the static nodes i have in my map*/
		if(this.myMap == null || this.myMap.getMap() == null) return -1;
		for(Map.Entry<Integer, Coord> nb : this.myMap.getMap().entrySet()){
			/*add them to my map*/
			if (staticNodes.keySet().contains(nb.getKey())){
				staticNBs.add(nb.getKey());	
				m.put(nb.getKey(), staticNodes.get(nb.getKey()));
			}
		}
		if(staticNBs.size() < 3){
			//core.Debug.p("can't determine global coordinates");
			return -1;
		}
		else{
			double d1 = dists.get(staticNBs.get(0));
			double d2 = dists.get(staticNBs.get(1));
			double d3 = dists.get(staticNBs.get(2));
			if(d1< 0 || d2< 0 || d3< 0 ) return -1;
			Integer def = -1;
			Coord c1 = staticNodes.get(staticNBs.get(0));
			Coord c2 = staticNodes.get(staticNBs.get(1));
			Coord c3 = staticNodes.get(staticNBs.get(2));
			if(c1 == null || c2 == null || c3 == null) return -1;
			//core.Debug.p(c1.toString()+ "-d:" + d1);
			//core.Debug.p(c2.toString()+ "-d:" + d2);
			//core.Debug.p(c3.toString()+ "-d:" + d3);
			Coord myCoord = findCoordinate(c1, c2, c3, d1, d2, d3);
			if(myCoord != null){
				core.Debug.p("coord not null! wohoo! " + myCoord.toString());
				/*now mix this map*/
				globalMap = NodePositionsSet.mixMap(m, myMap.getMap(), this.ref_id, myID);
				
				for(Map.Entry<Integer, NodePositionsSet> local : this.allMaps.entrySet()){
					globalMap = NodePositionsSet.mixMap(globalMap, myMap.getMap(), ref_id, myID);
				}
				if(globalMap == null) return -1;
				else return 0;
			}
			else{
				//core.Debug.p("can't determine global coordinates");
				return -1;
			}
		}
	}
	
	
	public int mixMap(NodePositionsSet other){
		//core.Debug.p("mixing maps");
		
		if(!mapExists){
			globalMap = new HashMap<Integer, Coord>();
		}
		else{
			oldMap = globalMap;
		}
		int j = -1;
		globalMap = NodePositionsSet.mixMap(myMap.getMap(), other.getMap(), this.myID, other.getID());
		if(globalMap == null) return -1;
		else{
			mapExists = true;
			synced = 0;
		}
		return globalMap.size();
	}
	
	/*how many localmaps make my globalmap*/
	public int mapCount(){
		return this.mapCount;
	}

	public double getCT(){
		return this.creationTime;
	}

	public double updateTime(){
		return this.updateTime;
	}

	public int mapSize(){
		return this.globalMap.size();
	}
	
	
/*get the coord of my point*/
private static final double EPSILON = 3;
private static Coord findCoordinate(Coord p0, Coord p1, Coord p2, double r0, double r1, double r2){

	double x0, y0, x1, y1, x2, y2;
	x0 = p0.getX(); y0 = p0.getY();
	x1 = p1.getX(); y1 = p1.getY();
	x2 = p2.getX(); y2 = p2.getY();
	//core.Debug.p("x0:" + x0 + ",y0:" + y0 + ",r0:" + r0);
	//core.Debug.p("x1:" + x1 + ",y1:" + y1 + ",r1:" + r1);
	//core.Debug.p("x2:" + x2 + ",y2:" + y2 + ",r2:" + r2);
	double a, dx, dy, d, h, rx, ry;
	double point2_x, point2_y;

	/* dx and dy are the vertical and horizontal distances between
	 * the circle centers.
	 */	
	dx = x1 - x0;
	dy = y1 - y0;

	/* Determine the straight-line distance between the centers. */
	d = Math.sqrt((dy*dy) + (dx*dx));

	/* Check for solvability. */
	if (d > (r0 + r1)){
		/* no solution. circles do not intersect. */
		//core.Debug.p("no solution1");
		return null;
	}
	if (d < Math.abs(r0 - r1)){
		/* no solution. one circle is contained in the other */
		//core.Debug.p("no solution2");
		return null;
	}

	/* 'point 2' is the point where the line through the circle
	 * intersection points crosses the line between the circle
	 * centers.
	 */

	/* Determine the distance from point 0 to point 2. */
	a = ((r0*r0) - (r1*r1) + (d*d)) / (2.0 * d) ;

	/* Determine the coordinates of point 2. */
	point2_x = x0 + (dx * a/d);
	point2_y = y0 + (dy * a/d);

	/* Determine the distance from point 2 to either of the
	 * intersection points.
	 */	
	h = Math.sqrt((r0*r0) - (a*a));

	/* Now determine the offsets of the intersection points from
	 * point 2.
	 */
	rx = -dy * (h/d);
	ry = dx * (h/d);

	/* Determine the absolute intersection points. */
	double intersectionPoint1_x = point2_x + rx;
	double intersectionPoint2_x = point2_x - rx;
	double intersectionPoint1_y = point2_y + ry;
	double intersectionPoint2_y = point2_y - ry;

	/* Lets determine if circle 3 intersects at either of the above intersection points. */
	dx = intersectionPoint1_x - x2;
	dy = intersectionPoint1_y - y2;
	double d1 = Math.sqrt((dy*dy) + (dx*dx));

	dx = intersectionPoint2_x - x2;
	dy = intersectionPoint2_y - y2;
	double d2 = Math.sqrt((dy*dy) + (dx*dx));

	//core.Debug.p("d1: " + d1 + ", d2: " + d2 + ", r2: " + r2);
	//core.Debug.p("diff1: " + Math.abs(d1-r2));
	//core.Debug.p("point1: (" + intersectionPoint1_x + ", " + intersectionPoint1_y + ")");
	//core.Debug.p("diff2: " + Math.abs(d2-r2));
	//core.Debug.p("point2: (" + intersectionPoint2_x + ", " + intersectionPoint2_y + ")");


	if(Math.abs(d1 - r2) < EPSILON) {
		//core.Debug.p("point: (" + intersectionPoint1_x + ", " + intersectionPoint1_y + ")");
		return new Coord(intersectionPoint1_x, intersectionPoint1_y);
	}
	else if(Math.abs(d2 - r2) < EPSILON) {
		//core.Debug.p("point: (" + intersectionPoint2_x + ", " + intersectionPoint2_y + ")");
		return new Coord(intersectionPoint2_x, intersectionPoint2_y);}
	else {
		//core.Debug.p("no intersection");
		return null;
	}
}

}