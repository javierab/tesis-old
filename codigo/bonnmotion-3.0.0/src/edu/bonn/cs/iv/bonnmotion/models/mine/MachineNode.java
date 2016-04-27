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

/** Mine node */

public class MachineNode extends MineNode {
	public Position dump;
	
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
		try{
			this.current_area = (Extraction) area;
		}
		catch(Exception e){
			System.out.println("Machines can only be located in Extraction areas");
			System.exit(1);
		}
		this.dump = ((Extraction)this.current_area).getDump();
		/*Specific values for machines*/
		this.min_speed = 5;
		this.max_speed = 8;
		this.timeout_avg = 30;
	}
	
	public void add(Position start) {
		this.start = start;
	}
	
	public void print() {
		System.out.println("MachineNode: \n --position:" + dest_position.toString() + "\n --dump:" + dump.toString());
	}
	
	
	public Position getNextDestination(boolean toDump){
		
		timeout--;
		if(timeout > 0){
			return dest_position;
		}
		if(toDump){
			System.out.println("Going to dump: " + dump.toString());
			start = dest_position;
			dest_position = dump;
			return dump;
			
		}
		else{
			/*find a point to go*/
			Position p = current_area.getRandomPosition();
			System.out.println("Going to random point: " + p.toString());
			start = dest_position;
			dest_position = p;
			return p;
		}
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
