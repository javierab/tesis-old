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

public class Access extends MineArea {
	private static final long serialVersionUID = -1509884865779246928L;

	public Access(double[] val, double[] entries) {
		super(val, entries);
		this.type = 0;
		makeObstacles();

		if (debug) System.out.println ("AreaType: Access");
	}
	
	protected void SetDefaultValues() {
		//TODO
	}
	
	protected Obstacle[] makeObstacles(){
		//TODO
		/*acá no hay obstáculos*/
		return new Obstacle[0];
		
	}
	
}