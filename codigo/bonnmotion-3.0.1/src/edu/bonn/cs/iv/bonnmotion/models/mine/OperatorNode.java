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

public class OperatorNode extends MineNode {
	public int pause_dest;
	public int pause_dump;
	public int repetitions;
	public int rep_t;
	
	int START = -1;
	int TO_DEST = 0;
	int PAUSE_DEST = 1;
	int NEW_AREA = 2;
	int GO_OUTSIDE = 3;
	
	public final int area_change_avg;
	public MineArea[] areas;
	public MineArea dest_area;
	public int area_change = 0;
	
	/* * * * * * * * * * *
	 * area:
	 * 0: access 
	 * 1: extraction
	 * 2: maintenance
	 * * * * * * * * * * *
	 * type:
	 * 0: maquina: movimiento circular, solo en area de extraccion
	 * 1: operador: movimiento en areas de mantencion
	 * 2: supervisor: se mueve por todas las areas
	 * * * * * * * * * * */
	
	public OperatorNode(Position start, MineArea[] areas, MineArea area) {
		super(start);
		this.type = 1;
		this.current_area = area;
		this.areas = areas;
		this.min_speed = 8;
		this.max_speed = 12;
		this.area_change_avg = 10;
		this.area_change = area_change_avg + r.nextInt(5) - 2;
		this.timeout_avg = 50;
	}
	
	public void add(Position start) {
		this.start = start;
	}

	public Position getNextStep(){
		
		if(state == START){
			state = PAUSE_DEST;
			timeout = pause_ext -1;
		}
		else if(state == TO_DUMP || state == TO_EXT){
			step++;
			if(step == route.size()){/*i'm here*/
				step = 0;
				rep_t = 0;
				current_position = dest_position;
				if(dest_position == dump) state = PAUSE_DUMP;
				if(dest_position == ext) state = PAUSE_EXT;
			}
			else{
				current_position =  route.get(step);

			}
		}
		else{
			timeout++;
			if((state == PAUSE_DUMP && timeout == pause_dump) || (state == PAUSE_EXT && timeout == pause_ext)){ /*time to go back*/
				getNextDestination();
				timeout = 0;
			}
		}
		return current_position;
	}	
	
	public void getNextDestination(){
		if(state == PAUSE_DEST){
			System.out.println("new dest in area");
			current_position = dest_position;
			dest_position = current_area.getRandomPosition();
			step = 0;
			state = TO_DEST;
		}
		else if(state == NEW_AREA){
			System.out.println("new area");
			MineArea dest_area = current_area;
			while(current_area == dest_area){
				dest_area = areas[r.getInt(areas.length)];
			}
			dest_position = dest_area.getRandomPosition();
			state = OUTSIDE;
		}
		else{
			System.out.println("shouldn't find new destination while on the way");
			System.exit(0);
		}
		route = getRoute();
	}
	
	public void print() {
		System.out.println("OperatorNode: type " + type + " area " + areas.toString() + " start " + start.toString());
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
