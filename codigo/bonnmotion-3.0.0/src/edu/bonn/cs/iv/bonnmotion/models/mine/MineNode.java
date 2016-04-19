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

import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

/** Mine node */

public class MineNode extends MobileNode {
	public int belongsto;
	public int type;
	public Position start;
	//public Group group;
	
	public MineNode(int belongsto, int type, Position start) {
		super();
		this.belongsto = belongsto;
		this.type = type;
		this.start = start;
	}
	
	public MineNode(MineNode group) {
		super();
		this.belongsto = group.belongsto;
		this.type = group.type;
		this.start = group.start;
		//this.group = group;
	}
	
	public void add(Position start) {
		this.start = start;
	}
	
	/*public MineNode group() {
		return this.group;
	}*/
	
	public void print() {
		System.out.println("Node type " + type + " area " + belongsto + " start " + start.toString());
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
