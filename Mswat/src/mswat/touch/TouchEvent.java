package mswat.touch;

import android.graphics.Point;

/**
 * Class that represents a touch event
 * contains coords and timestamp
 * @author unzi
 *
 */
public class TouchEvent {
	private int x;
	private int y;
	private int msec;
	private int pressure;
	private int touchWidth;
	private int identifier;
	
	public TouchEvent(int x, int y, int msec){
		this.x=x;
		this.y=y;
		this.msec=msec;
	}
	
	public TouchEvent(int x, int y, int msec, int pressure, int touchWidth, int identifier){
		this.x=x;
		this.y=y;
		this.msec=msec;
		this.pressure=pressure;
		this.touchWidth = touchWidth;
		this.identifier = identifier;
	}
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public int getTime(){
		return msec;
	}

	public int getTouchWidth() {
		return touchWidth;
	}


	public int getIdentifier() {
		return identifier;
	}

	public int getPressure() {
		return pressure;
	}

	
}
