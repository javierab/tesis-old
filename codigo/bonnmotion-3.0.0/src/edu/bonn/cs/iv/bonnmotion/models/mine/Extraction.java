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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Extraction extends MineArea {
	private static final long serialVersionUID = 7750320765927287614L;

	int TRUE = 1;
	int FALSE = 0;
	public Extraction(double[] val, double[] entries) {
		super(val, entries);
		this.type = 2;
		super.obstacles = makeObstacles();

		if (debug) System.out.println ("AreaType: Extraction");
	}
	
	protected void SetDefaultValues() {
		//TODO
	}
	
	protected Obstacle[] makeObstacles(){
		
		/* it's a rectangle:*/
		double height = this.getBounds().height;
		double width = this.getBounds().width;
		List<Obstacle> obstacles = new ArrayList<Obstacle>();
		/*TODO: fixed number? should i scale?*/
		
		double h_step = height/11;
		double w_step = width/15;
		
		for (int i = 0 ; i < 15; i++){
			if((i & 1) == 0){
				; /*even numbers are roads*/
			}
			else{
				new Random();
				int[] breaks = new int[11];
				for(int j=1; j < 10; j++){ /*borders are empty*/
					if((j & 1) == 0){ /*always occupied*/
						breaks[j] = 0;
					}
					else{
						double m = Math.random();
						if(m > 0.5) 
							breaks[j] = 1;
						else 
							breaks[j] = 0;
					}
				}
				breaks[0] = 1; breaks[10] = 1;
				obstacles.addAll(buildObstacles(h_step, w_step, w_step*i, breaks));
			}
		}
		/*return as array*/
		Obstacle[] ret = new Obstacle[obstacles.size()];
		obstacles.toArray(ret);
		return ret;
		
	}
	
	public List<Obstacle> buildObstacles(double h_step, double w_step, double w_start, int[] breaks){
		boolean state = false;
		double h_start = 0;
		List<Obstacle> obs = new ArrayList<Obstacle>();
		
		for(int i = 0; i< breaks.length ; i++){
			//System.out.println("State for i: " + i + " = " + state);
			if(breaks[i] == 0 && state == false){ h_start = i; state = true;}
			else if(breaks[i] == 1 && state == true){
				/*create obstacle*/
				double[] vertices = {h_start*h_step, w_start, i*h_step, w_start, i*h_step, w_start+w_step, h_start*h_step, w_start+w_step};
				Obstacle o = new Obstacle(vertices);
				/*append to return*/
				obs.add(o);
				/*reset*/
				state = false;
			}
		}
		return obs;
	}
	
	public String toString(){
		return super.toString();
	}
	
	
	public static void main(String[] args){
		
		double[] corners = {0.0, 0.0, 0.0, 1150.0, 1150.0, 1150.0, 0.0, 1150.0};
		double[] entries = {0.0, 100.0};
		
		MineArea a = new Extraction(corners, entries);
		
		a.print();
		}
	
}