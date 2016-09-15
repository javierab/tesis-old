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

/**
 * Class for storing and manipulating the meeting probabilities for the Javi
 * router module.
 */
public class GlobalMap {
	
	int myID;
	int[] myNeighbors;
	Map<Integer, Coord> myGlobalMap, newMap;
	double creationTime;
	
	public GlobalMap(Map<Integer, Coord> myGlobalMap, int myID){
	
		this.myID = myID;
		this.myNeighbors = toInt(myGlobalMap.keySet());
		this.myGlobalMap = myGlobalMap;
		this.creationTime = SimClock.getTime();
	}

	public GlobalMap(){
		this.myID = 0;
		this.myNeighbors = new int[1];
		this.myGlobalMap = new HashMap();
		this.creationTime = SimClock.getTime();
	}

	public void setID(int myid){
		this.myID = myid;
	}
	
	public void updateMap(Map<Integer, Coord> map){
		myGlobalMap = map;
	}
	
	public Map<Integer, Coord> getMap(){
		if(myGlobalMap == null){
			return new HashMap<Integer, Coord>();
		}
		return myGlobalMap;
	}
	
	public void setMap(Map<Integer, Coord> m){
		myGlobalMap = m;
		myNeighbors = myGlobalMap.getKeys();
	}

	public int mixMap(GlobalMap other){
		if((myGlobalMap == null) || (myGlobalMap.size() == 0) || (other.myGlobalMap.size() == 0)){
			core.Debug.p("need a starting map -- aborting");
			return -1;
		}
		if(newMap == null){
			newMap = new HashMap<Integer, Coord>();
		}
		else{
			newMap.clear();
		}
		/*see if we can have angle correction: we need */
		/* 1) i \in LVS_j and j \in LVS_i*/
		/* 2) k != i,j, k \in LVS_i and k \in LVS_j */
		
		int j = -1;

		if(containsInt(myNeighbors, other.myID) && containsInt(other.myNeighbors, myID)){
			/*find k*/
		
			for(int i : myNeighbors){ /*not neighbors*/
				if(containsInt(myNeighbors, i) && containsInt(other.myNeighbors, i)){
					j = i;
					break;
				}			
			}
			if(j < 0){ /*no  neighbors*/
				core.Debug.p("couldn't merge maps");
				return -1;
			}
			else{
				core.Debug.p("can merge maps");
			}
		}
		
		double alpha_j = getAngle(myGlobalMap.get(myID), myGlobalMap.get(j));
		double alpha_k = getAngle(myGlobalMap.get(myID), myGlobalMap.get(other.myID));
		double beta_i = getAngle(other.myGlobalMap.get(other.myID), myGlobalMap.get(myID));
		double beta_j = getAngle(other.myGlobalMap.get(other.myID), myGlobalMap.get(j));
		double corr, corr_dist, dist;
		boolean needMirr;
		
		if(((alpha_j - alpha_k < Math.toRadians(Math.PI)) && (beta_j - beta_i > Math.toRadians(Math.PI))) 
				|| ((alpha_j - alpha_k > Math.toRadians(Math.PI)) && (beta_j - beta_i < Math.toRadians(Math.PI)))){
			corr = beta_i - alpha_j + Math.toRadians(Math.PI);
			needMirr = false;
		}
		else if(((alpha_j - alpha_k < Math.toRadians(Math.PI)) && (beta_j - beta_i < Math.toRadians(Math.PI)))
				|| ((alpha_j - alpha_k > Math.toRadians(Math.PI)) && (beta_j - beta_i > Math.toRadians(Math.PI)))){
			corr = beta_i + alpha_k;
			needMirr = true;
		}
		else{
			core.Debug.p("angles don't match -- aborting");
			return -1;
		}
		corr_dist = myGlobalMap.get(myID).distance(myGlobalMap
.get(other.myID));
		
		/*TODO: pick the node with more neighbors */
		newMap.clear();
		newMap.putAll(myGlobalMap
);
			
		Coord old_0 = other.myGlobalMap.get(other.myID);
		/*first adjust the other map*/
		Map<Integer, Coord> fixedMap = fixMap(other.myGlobalMap
, corr, needMirr);
		
		for(Map.Entry<Integer, Coord> m : fixedMap.entrySet()){
			int i = m.getKey();
			Coord c_i = m.getValue();
			newMap.put(i, new Coord(c_i.getX() + old_0.getX(), c_i.getY() + old_0.getY()));
		}
		myNeighbors = toInt(newMap.keySet());
		if((newMap.size() > myGlobalMap
.size()) || (creationTime < SimClock.getTime() - JaviRouter.GLOBAL_TIMEOUT)){
			core.Debug.p("new map added");
			myGlobalMap
 = newMap;
			creationTime = SimClock.getTime();
			return 0;
		}
		else{
			return 1;
		}		
	
	}
	
	public static boolean containsInt(int[] a, int b){
		for(int i = 0; i < a.length; i++){
			if (a[i] == b) return true;
		}
		return false;
		
	}
	
	public int getNB(){
		return this.myNeighbors.length;
	}

	public double getCT(){
		return this.creationTime;
	}
	
	public static double getAngle(Coord orig, Coord i){
		return Math.acos(orig.distance(new Coord(0, i.getY()))/orig.distance(i));
	}
	
	public static int[] toInt(Set<Integer> set) {
		  int[] a = new int[set.size()];
		  int i = 0;
		  for (Integer val : set) a[i++] = val;
		  return a;
	}
	
	public Map<Integer, Coord> fixMap(Map<Integer,Coord> map, double angle, boolean mirr){
		double dx,dy, dist;
		Coord c;
		Map<Integer, Coord> newmap = new HashMap();
		for(Map.Entry<Integer, Coord> m : map.entrySet()){
			
			c = m.getValue();
			dist = c.distance(new Coord(0,0));
			dx = dist*Math.cos(angle);
			dy = dist*Math.sin(angle);
			
			if(mirr){
				newmap.put(m.getKey(), new Coord(c.getX()+dx, c.getY()+dy));
			}
			else{
				newmap.put(m.getKey(), new Coord(-(c.getX()+dx),c.getY()+dy));
			}
		}
		return newmap;
	}
	
	public int pickMap(GlobalMap m2){
		if(this.myNeighbors.length > m2.myNeighbors.length) return -1;
		else if(this.myNeighbors.length < m2.myNeighbors.length) return 1;
		else if(this.myID < m2.myID) return -1;
		else return 1;
	}
	
	
	
	
	
	
}
