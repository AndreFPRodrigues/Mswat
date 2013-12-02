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
	
	public TouchEvent(int x, int y, int msec){
		this.x=x;
		this.y=y;
		this.msec=msec;
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
	
}
