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

import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Waypoint;
import java.util.LinkedList;


/** Mine node */

public class MachineNode extends MineNode {
	public Position dump;
	public Position ext;
	public int pause_ext;
	public int pause_dump;
	public int repetitions;
	public int rep_t;
	
	int START = -1;
	int TO_DUMP = 0;
	int PAUSE_DUMP = 1;
	int TO_EXT = 2;
	int PAUSE_EXT = 3;
	
	/* * * * * * * * * * *
	 * area:
	 * 0: access 
	 * 1: extraction
	 * 2: maintenance
	 * * * * * * * * * * *
	 * type:
	 * 0: maquina: movimiento circular, solo en area de extraccion
	 * 1: mantenimiento: movimiento entre areas de mantencion
	 * 2: supervisor: se mueve por todas las areas
	 * * * * * * * * * * */
	
	public MachineNode(Position start, MineArea area) {
		super(start);
		this.type = 0;
		if(area.type == 1){
			this.current_area = area;
		}
		else{
			System.out.println("Machines can only be located in Extraction areas");
			System.exit(1);
		}
		/*values for machines*/
		this.min_speed = 5;
		this.max_speed = 8;
		this.pause_dump = 10;
		this.pause_ext = 12;
		
		/*set points and repetitions for extraction*/
		this.dump = ((Extraction)this.current_area).getDump();
		this.ext = current_area.getRandomPosition();
		this.repetitions = 5 + r.nextInt(3) -1;
		
		/*set values for route for time */
		this.route = getRoute();
		this.state = START;
		this.step = 0;

	}
	
	public void add(Position start) {
		this.start = start;
	}
	
	public void print() {
		System.out.println("MachineNode: \n --position:" + dest_position.toString() + "\n --dump:" + dump.toString());
	}
	
	public Position getNextStep(){
		
		if(state == START){
			state = PAUSE_EXT;
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
		if(state == PAUSE_EXT){
			current_position = dest_position;
			dest_position = dump;
			step = 0;
			state = TO_DUMP;
		}
		else if(state == PAUSE_DUMP){
			rep_t++;
			if(rep_t == repetitions){ /*find new ext point to go*/
				ext = current_area.getRandomPosition();
				rep_t = 0;
			}
			current_position = dest_position;
			dest_position = ext;
			step = 0;
			state = TO_EXT;
		}
		else{
			System.out.println("shouldn't find new destination while on the way");
			System.exit(0);
		}
		route = getRoute();
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
