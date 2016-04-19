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

import java.awt.Polygon;


import edu.bonn.cs.iv.bonnmotion.Position;


public abstract class MineArea extends Polygon {
	
	int POSITIONS = 0;
	int ENTRIES = 1;

	private static final long serialVersionUID = -5890360972338115487L;
	
	protected static boolean debug = false;
	Position[] corners = null;
	Position[] entries = null;
	Obstacle[] obstacles = null;
	public int type;
	
	public String getArea(int area){
		switch(area){
		case 0: return "Acceso";
		case 1: return "Extraccion";
		case 2: return "Mantencion";
		default: return "???";
		}
	}

	/**
	 * Returns a MineArea instance of the correct type for Positions
	 */
	public MineArea getInstance(double[] Positions, double[] entries, int type) {
		switch (type) {
			case 0:
				return new Access(Positions, entries);
			case 1:
				return new Extraction(Positions, entries);
			case 2:
				return new Maintenance(Positions, entries);
			default:
				throw new IllegalArgumentException("Tipo desconocido");
		}
	}


	/* positions: list of x,y values */
	protected void InitializeSpecificValues(int type, double[] values) {

		Position[] temp_p = new Position[values.length/2];
		Position[] temp_e = new Position[values.length/2];

		if(type == POSITIONS){ 
			
			for(int i = 0; i < values.length; i = i+2){
				/*polygon*/
				this.addPoint((int)values[i], (int)values[i+1]);
				temp_p[i/2] = new Position(values[i], values[i+1]);
			}
			corners = temp_p;
		}
		else if(type == ENTRIES){ 
			for(int i = 0; i < values.length; i = i+2){
				temp_e[i/2] = new Position(values[i], values[i+1]);
			}		
		}
			entries = temp_e;
	}
	
	/**
	 */
	protected abstract void SetDefaultValues();
	protected abstract Obstacle[] makeObstacles();

	protected MineArea(double[] Positions, double[] entries) {
		super();
		this.InitializeSpecificValues(POSITIONS, Positions);
		this.InitializeSpecificValues(ENTRIES, entries);		
		obstacles = makeObstacles();
		this.SetDefaultValues();
	}

	public void print(){
		System.out.print("\nCoordinates of MineArea: " + getArea(this.type) + '\n');
		System.out.print("Corners: [");
		for(Position corner:corners) System.out.print("(" + corner.x + ", " + corner.y + ")");
		System.out.print("]\n");
		System.out.print("Entries: [");
		for(Position entry:entries)	System.out.print("(" + entry.x + ", " + entry.y + ")");
		System.out.println("]");
		System.out.print("Obstacles: [");
		for(Obstacle obs:obstacles){ System.out.print("("); obs.print(); System.out.println(")");}
		System.out.println("]");		

	}

	public boolean equals(MineArea other) {
		if(type != other.type) {
			return false;
		}

		for(int i = 0; i < corners.length; i++) {
			if(corners[i] != other.corners[i]){
				return false;
			}
		}
		return true;
	}
}
