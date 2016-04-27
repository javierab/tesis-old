
package edu.bonn.cs.iv.bonnmotion.models.mine;

import java.util.Random;

import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

/** Mine node */

public class MineNode extends MobileNode {
	public int type;
	public MineArea current_area;
	public Position start;
	public Position current_position;
	public Position dest_position;
	public int timeout = 0;
	public int timeout_avg;
	double min_speed;
	double max_speed;
	int avg_pause;
	int std_pause;
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

	public MineNode(Position start) {
		super();
		this.type = -1;
		this.start = start;
		this.dest_position = current_position = start;
		/*each node define its pause*/
		this.avg_pause = 4;
		this.std_pause = 2;
	}
	
	public void add(Position start) {
		this.start = start;
	}
	
	public void print() {
		System.out.println("Node type " + type + " start " + start.toString());
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

	/*we'll assume it's between 2 and 6s*/
	public int getPause(){
		Random r = new Random();
		return r.nextInt(2*std_pause+1)+avg_pause/2;
		
	}
	
	
}
