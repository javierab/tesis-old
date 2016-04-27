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

/** Mine node */

public class SupervisorNode extends MineNode {
	public final int area_change_avg;
	public MineArea start_area;
	public MineArea[] areas;
	public int timeout;
	public int area_change;
	
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
	
	
	public SupervisorNode(Position start, MineArea[] areas, MineArea start_area) {
		super(start);
		this.type = 2;
		this.areas = areas;
		this.current_area = this.start_area = start_area;
		this.timeout_avg = 20;
		this.area_change_avg = 5;
		this.min_speed = 10;
		this.max_speed = 15;
	}

	public void add(Position start) {
		this.start = start;
	}
	
	public Position getNextDestination(boolean start){

	Random r = new Random();
	timeout--;
	if(timeout < 0){
		//check if i should change the area
		area_change--;
		if(area_change<0){
			//pick new area
			MineArea a = current_area;
			while(a == current_area){
				current_area = areas[r.nextInt(areas.length)];
			}
			//set the timers
			timeout = timeout_avg + r.nextInt(11) - 5;
			area_change = area_change_avg + r.nextInt(3) - 1;
			System.out.println("Changing Operator to new area " + a + " -> "+ current_area + "; TO:" + timeout + "; AC:" + area_change);
			return current_area.getRandomPosition();
		}
		else{
			//just find a new position in my area
			return current_area.getRandomPosition();
		}
	}
	else{
		//stay in my spot
		return this.dest_position;
	}		
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
