/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2012 University of Bonn                                **
 ** Copyright (C) 2012-2015 University of Osnabrueck                          **
 **                                                                           **
 ** This program is free software; you can redistribute it and/or modify      **
 ** it under the terms of the GNU General Public License as published by      **
 ** the Free Software Foundation; either version 2 of the License, or         **
 ** (at your option) any later version.                                       **
 **                                                                           **
 ** This program is distributed in the hope that it will be useful,           **
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of            **
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             **
 ** GNU General Public License for more details.                              **
 **                                                                           **
 ** You should have received a copy of the GNU General Public License         **
 ** along with this program; if not, write to the Free Software               **
 ** Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA **
 *******************************************************************************/

package edu.bonn.cs.iv.bonnmotion.models.mine;

import java.util.Random;

import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Waypoint;
import java.util.LinkedList;


/** Mine node */

public class SupervisorNode extends MineNode {
	public int rep_t;
	
	int START = -1;
	
	int TO_DEST = 0;
	int PAUSE = 1;
	int NEW_AREA = 2;
	int GO_OUTSIDE = 3;
	int OUTSIDE = 4;
	int GO_INSIDE = 5;
	
	public MineArea[] areas;
	public MineArea dest_area;
	public int area_change = 0;
		
	/* * * * * * * * * * *
	 * area:
	 * 0: global 
	 * 1: extraction
	 * 2: maintenance
	 * * * * * * * * * * *
	 * type:
	 * 0: maquina: movimiento circular, solo en area de extraccion
	 * 1: mantenimiento: movimiento entre areas de mantencion
	 * 2: supervisor: se mueve por todas las areas
	 * * * * * * * * * * */
	
	
	public SupervisorNode(Position start, MineArea[] areas, MineArea start_area, Access access) {
		super(start);
		this.type = 2;
		this.areas = areas;
		this.access = access;
		this.current_area = start_area;
//		this.min_speed = 1.0;
//		this.max_speed = 1.5;		
//		this.pause = 8;
//		this.repetitions = 3;
		this.state = START;
		this.rep_t = 0;
	}

	public void add(Position start) {
		this.start = start;
	}
	
	public Position getNextStep(){
		
		//System.out.println("nextstep");
		if(state == START){
			//System.out.println("start");
			state = PAUSE;
			timeout = pause -1;
			rep_t = 0;
			/*next iteration it will start moving*/
		}
		else if(state == TO_DEST){ /*moving to dest, follow route*/
			step++;
			//System.out.println("step " + step + "todest?");
			if(step >= route.size()-1){/*i'm here*/
				//System.out.println("ondest");
				rep_t++;
				current_position = dest_position;
				state = PAUSE;
				timeout = 0;
			}
			else{ /*next step*/
				//System.out.println("todest");
				current_position =  route.get(step);
			}
		}
		else if(state == PAUSE){
			timeout++;
				if(rep_t == repetitions){ /*must find new area*/
				//	System.out.println("newarea");
					state = NEW_AREA;
					step = 0;
					rep_t = 0;
					getNextDestination();
				}
				else{ /*new destination in area*/
					//System.out.println("newinarea");
					getNextDestination();
					state = TO_DEST;
					step = 0;
				}
		}
		else if(state == GO_OUTSIDE){ /*leave the current area*/
			//System.out.println("gooutside");

				if(step == 0){ /*go to the entry*/
					//System.out.println("a");
					dest_position = current_area.getClosestEntry(current_position);
					route = getRoute();
					step++;
				}
				else if(step < route.size()){/*continue*/
					//System.out.println("b");
					current_position = route.get(step);
					step++;
				}
				else if(step == route.size()){ /* change area*/
					//System.out.println("c");
					current_position = dest_position;
					step = 0;
					state = OUTSIDE;
				}
		}
		else if(state == OUTSIDE){ /*find aux route from current entry to closest entry of dest area*/
			//System.out.println("outside");

				/*i'm in access area now*/
				if(step == 0){ /*go to the other entry and compute route*/
					//System.out.println("a");
					dest_position = dest_area.getClosestEntry(current_position);
					double this_speed = r.nextDouble()*(max_speed - min_speed) + min_speed;
					route = stepify(this_speed, access.getConnectingRoute(current_position, dest_position));
					step++;
				}
				else if(step < route.size()){
					//System.out.println("b");
					current_position = route.get(step);
					step++;
				}
				else if(step == route.size()){
					//System.out.println("c");
					current_position = dest_position;
					step = 0;
					state = GO_INSIDE;
				}
		}
		else if(state == GO_INSIDE){ /*mark as current area, then regular movements*/
			//System.out.println("goinside");
			current_area = dest_area;
			state = PAUSE;
			timeout = pause -1;
		}
		return current_position;
	}	
	
	public void getNextDestination(){
		if(state == PAUSE){
			//System.out.println("new dest in area");
			current_position = dest_position;
			dest_position = current_area.getRandomPosition();
			step = 0;
			state = TO_DEST;
		}
		else if(state == NEW_AREA){
			//System.out.println("new area");
			dest_area = current_area;
			while(current_area == dest_area){
				dest_area = areas[r.nextInt(areas.length)];
			}
			dest_position = dest_area.getClosestEntry(current_position);
			state = GO_OUTSIDE;
		}
		else{
			System.out.println("shouldn't find new destination while on the way");
			System.exit(0);
		}
		route = getRoute();
	}

	public void print() {
		System.out.println("Node type " + type + " area " + current_area + " start " + start.toString());
	}

	public String movementString() {
		StringBuffer sb = new StringBuffer(140*waypoints.size());
		for (int i = 0; i < waypoints.size(); i++) {
			Waypoint w = waypoints.elementAt(i);
			sb.append("\n");
			sb.append(w.time);
			sb.append("\n");
			sb.append(w.pos.x);
			sb.append("\n");
			sb.append(w.pos.y);
			sb.append("\n");
			sb.append(w.pos.status);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}

}
