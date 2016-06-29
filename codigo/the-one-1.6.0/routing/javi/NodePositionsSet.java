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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.AbstractMap.SimpleEntry;

import core.Coord;
import core.SimClock;

/**
 * Class for storing and manipulating the meeting probabilities for the Javi
 * router module.
 */
public class NodePositionsSet {
	
	int myID;
	int[] myNeighbors;
	Map<Integer, NodeDistancesSet> allNeighbors;
	Map<Integer, Coord> myMap, myMapOld;
	double creationTime;
	
	public NodePositionsSet(Map<Integer, NodeDistancesSet> allNeighbors, int myID){
	
		this.allNeighbors = allNeighbors;
		this.myID = myID;
		this.myNeighbors = allNeighbors.get(myID).knownNeighbors();
	}

	public NodePositionsSet(){
		this.myID = 0;
	}

	public void setId(int myid){
		this.myID = myid;
	}
	
	public void updateNeighbors(Map<Integer, NodeDistancesSet> nb){
		this.allNeighbors = nb;
		this.myNeighbors = nb.get(myID).knownNeighbors();
	}
	
	public Map<Integer, Coord> getMap(){
		if(myMap == null){
			int i = computeMap();
			if (i<0){ /*couldn't be computed*/
				return new HashMap<Integer, Coord>();
			}
		}
		return myMap;
	}
	
	
	public int computeMap(){
		
		if(allNeighbors.size() < 2){
			return -1;
		}
		
		SortedSet<Map.Entry<Integer, Integer>> sortedset = new TreeSet<Map.Entry<Integer, Integer>>(
	            new Comparator<Map.Entry<Integer, Integer>>() {
	                @Override
	                public int compare(Map.Entry<Integer, Integer> e1,
	                        Map.Entry<Integer, Integer> e2) {
	                	int v = e1.getValue().compareTo(e2.getValue());
	                	int k = e1.getKey().compareTo(e2.getKey());
	                    if(v==0 && k != 0) return 1;
	                    else return v;
	                }
	            });

		/** ordenar mis vecinos por los que más vecinos tienen**/
		/** pues asumimos que tienen mejores probabilidades **/

		/*list of neighbors of each neighbor*/
		Map <Integer, Integer> nb_nbs= new TreeMap<Integer, Integer>();
		int nb;
		for(int i = 0; i < myNeighbors.length; i++){
			nb = myNeighbors[i];
			NodeDistancesSet s = allNeighbors.get(nb);
			if(s != null){
				sortedset.add(new SimpleEntry<Integer, Integer>(nb, s.knownNeighbors().length));
			}
		}

		/* verify my 2 max are also neighbors. if not, go through it again finding a good one --or failing */
		

		if(sortedset.size() < 2){
			//core.Debug.p("--I don't have 2 neighbors:" + sortedset.size() + "--");
			return -1;
		}
		
		Map.Entry<Integer, Integer> n_p = sortedset.last();
		int node_p = n_p.getKey();
		sortedset.remove(sortedset.last());
		Map.Entry<Integer, Integer> n_q = sortedset.last();
		int node_q = n_q.getKey();

		if (!containsInt(allNeighbors.get(node_p).knownNeighbors(), node_q)){ /*not neighbors*/
			while(true){
				if(sortedset.size()==0){ /*completed the loop with no results*/
					return -1; 
				}
				else{
					node_q = node_p;
					n_q = n_p;
					n_p = sortedset.last();
					node_p = n_p.getKey(); 
					sortedset.remove(n_p);
					if(containsInt(allNeighbors.get(node_p).knownNeighbors(), node_q)) break;
				}
			}			
		}	
		
		/*prepare the map*/
		if(myMap == null){
			myMap = new HashMap<Integer, Coord>();
		}else{
			myMapOld = myMap;
			myMap.clear();
		}
		
		/*get distances and angle*/
		double x, y, alpha, beta, ang;
		
		double dist_i_p = allNeighbors.get(myID).getDistFor(node_p);
		double dist_i_q = allNeighbors.get(myID).getDistFor(node_q);
		double dist_p_q = allNeighbors.get(node_p).getDistFor(node_q);
		
		if(dist_i_p < 0 || dist_i_q < 0|| dist_p_q < 0){
			return -1;
		}
		ang = Math.acos( //angulo PIQ
				(dist_i_p*dist_i_p + dist_i_q*dist_i_q - dist_p_q*dist_p_q)/(2*dist_i_p*dist_i_q));
		
		//core.Debug.p("dip:" + dist_i_p + ", diq:" + dist_i_q + ", dpq:" + dist_p_q + ", ang" + ang);

		/*add first values*/
		
		this.myMap.put(myID, new Coord(0.0, 0.0));
		this.myMap.put(node_p, new Coord(dist_i_p, 0.0));
		this.myMap.put(node_q, new Coord(dist_i_q*Math.cos(ang), dist_i_q*Math.sin(ang)));


		
		/*add the rest-- if i can*/
		for(int n : myNeighbors){
			if (n==myID || n==node_p || n==node_q){
				continue;
			}
			else{
				double dist_p_n = -1.0, dist_q_n = -1.0;
				double dist_i_n = allNeighbors.get(myID).getDistFor(n);
				NodeDistancesSet dp = allNeighbors.get(node_p);
				if(dp != null)
					dist_p_n = dp.getDistFor(n);
				NodeDistancesSet dq = allNeighbors.get(node_q);
				if(dq != null)
					dist_q_n = dq.getDistFor(n);
				
				if(dist_i_n > 0 && dist_p_n > 0 && dist_q_n > 0){
					
					alpha = Math.acos(
							(dist_i_n*dist_i_n + dist_i_p*dist_i_p - dist_p_n*dist_p_n)/(2*dist_i_n*dist_i_p));
					
					beta = Math.acos(
							(dist_i_n*dist_i_n + dist_i_q*dist_i_q - dist_q_n*dist_q_n)/(2*dist_i_n*dist_i_q));
					
					x = dist_i_n*Math.cos(alpha);
					
					if(Math.abs(alpha+beta-ang) < 0.5){
						y = dist_i_n*Math.sin(alpha);
					}
					else{
						y = -dist_i_n*Math.sin(alpha);
					}
					this.myMap.put(n, new Coord(x, y));
				}
			}
		}
		creationTime = SimClock.getTime();
		return this.myMap.size();
	}

	
	public static boolean containsInt(int[] a, int b){
		for(int i = 0; i < a.length; i++){
			if (a[i] == b) return true;
		}
		return false;
		
	}
	
	
	
}