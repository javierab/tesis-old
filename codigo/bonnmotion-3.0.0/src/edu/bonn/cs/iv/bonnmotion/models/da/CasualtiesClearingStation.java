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

package edu.bonn.cs.iv.bonnmotion.models.da;

public class CasualtiesClearingStation extends CatastropheArea {
	private static final long serialVersionUID = 632345523282177640L;

	protected CasualtiesClearingStation(double[] Positions) {
		super(Positions);
		if (debug) System.out.println ("AreaType: CasualtiesClearingStation");
	}
	
	protected void SetDefaultValues() {
		this.groupsize[0] = 1;
		this.groupsize[1] = 1;
		this.minspeed[0] = 1.0;
		this.maxspeed[0] = 2.0;
		this.minspeed[1] = 1.0;
		this.maxspeed[1] = 2.0;

	}
}