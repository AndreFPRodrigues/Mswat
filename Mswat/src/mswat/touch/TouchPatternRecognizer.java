package mswat.touch;

import java.util.ArrayList;

import android.util.Log;

/**
 * Process touch event received, identify type of touch
 * 
 * Feed all the touch positions via store(), when it returns true a type of
 * touch has finished. Use identifyTouch() to get the type of touch iden
 * 
 * @author Andre Rodrigues
 * 
 */
public class TouchPatternRecognizer {

	private final String LT = "TouchRec";

	// Touch types on release
	public final static int TOUCHED = 0;
	public final static int SLIDE = 1;
	public final static int LONGPRESS = 2;
	
	// Touch types on change
	public final static int DOWN = 0;
	public final static int MOVE = 1;
	public final static int UP = 2;
	
	//input types
	private final int POSITION_X= 53;
	private final int POSITION_Y= 54;
	private final int PRESSURE= 58;
	private final int TOUCH_MAJOR= 48;
	private final int TRACKING_ID= 57;
	private final int SYN_MT_REPORT=2;
	private final int SYN_REPORT=0;
	
	// identifier variables
	private int lastX;
	private int lastY;
	private int slideXorigin;
	private int slideYorigin;
	private int lastEventCode = -1;
	private int pressure;
	private int touchSize;
	private int identifier;

	// Array to store all touch events
	private ArrayList<TouchEvent> touches = new ArrayList<TouchEvent>();
	
	//number of fingers used
	private int numberTouches = 0;
	private ArrayList <Integer > id_touches=new ArrayList<Integer>();
	private int idFingerUp;
	private int biggestIdentifier=0;
	
	//used to prevent double tap
	private int lastTouch=0;
	private int doubleTapThreshold=1000;
	/**
	 * Identify type of touch (slide/touch/long press)
	 * 
	 * @return
	 */
	private int identifyTouch() {
		double distance = 0;
		if (touches.size() < 5) {
			touches.clear();
			return TOUCHED;
		} else {
			for (int i = 1; i < touches.size(); i++) {
				distance = Math.sqrt((Math.pow((touches.get(i).getY() - touches
						.get(i - 1).getY()), 2) + Math.pow((touches.get(i)
						.getX() - touches.get(i - 1).getX()), 2)));
				if (distance > 50) {
					slideXorigin = touches.get(0).getX();
					slideYorigin = touches.get(0).getY();
					touches.clear();
					return SLIDE;
				}
			}
			touches.clear();
			return LONGPRESS;
		}
	}

	/**
	 * Register in array all touch positions until finger release
	 * 
	 * @return (-1) -> no touch identified yet (0) -> touch identified (1) ->
	 *         slide identified (2) -> LongPress identified
	 */
	public int identifyOnRelease(int type, int code, int value, int timestamp) {
		 Log.d(LT,"t:"+ type+ " c:" + code + " v:"+ value);
		if(code==TRACKING_ID){
			identifier=value;
		}else
		if (code == PRESSURE)
			pressure = value;
		else if (code == TOUCH_MAJOR)
			touchSize = value;
		if (code == POSITION_X)
			lastX = value;
		else if (code == POSITION_Y)
			lastY = value;
		else if (code == SYN_MT_REPORT && value == 0) {
			TouchEvent p = new TouchEvent(lastX, lastY, timestamp, pressure, touchSize,identifier );
			touches.add(p);
		}
		if (code == SYN_MT_REPORT && lastEventCode == SYN_REPORT && touches.size() > 0) {
			lastEventCode = -1;
			//prevents double tap
			if((lastTouch-timestamp)<-doubleTapThreshold){
				Log.d(LT, "last time:"+lastTouch+" timestamp:" +timestamp + " diference" + (lastTouch-timestamp));
				lastTouch=timestamp;
				return identifyTouch();
			}
			else
				return -1;
		}
		lastEventCode = code;
		return -1;
	}
	
	/**
	 * Register in array all touch positions until finger release
	 * 
	 * @return (-1) -> no touch identified yet (0) -> down identified (1) ->
	 *         move identified (2) -> up identified
	 */
	public int identifyOnChange(int type, int code, int value , int timestamp){
		
		switch(code){
			case POSITION_X:
				lastX=value;
				break;
			case POSITION_Y:
				lastY=value;
				break;
			case PRESSURE:
				pressure=value;
				break;
			case TOUCH_MAJOR:
				touchSize=value;
				break;
			case TRACKING_ID:
				identifier=value;			
				break;
			case SYN_MT_REPORT:
				TouchEvent p = new TouchEvent(lastX, lastY, timestamp, pressure, touchSize,identifier);
				touches.add(p);
				
				//register touch id (alive), to detect ups on multi touch
				id_touches.add(identifier);
				
				//all fingers up
				if(lastEventCode== SYN_REPORT){
					lastEventCode = -1;
					touches.clear();
					numberTouches=0;
					biggestIdentifier=0;
					return UP;
				}
				//first down touch
				if(touches.size()<2){
					lastEventCode = -1;
					return DOWN;
				}
				//down if another finger is used
				if(identifier> biggestIdentifier) {
					biggestIdentifier=identifier;
					lastEventCode = -1;
					numberTouches++;
					return DOWN;
				}
				
				//check if the touch event moved
				if(checkIfMoved(p)){
					lastEventCode=-1;
					return MOVE;
				}
				break;
			case SYN_REPORT:
				if(id_touches.size()<numberTouches+1){
					Log.d(LT, " Syn report up");

					boolean fingerSet=false;
					for(int i =0;i<id_touches.size();i++){
						if(id_touches.get(i)!=i){
							idFingerUp=i;
							fingerSet=true;
						}
					}
					if(!fingerSet)
						idFingerUp=id_touches.size();
					id_touches.clear();
					numberTouches--;
					return UP;
				}
				id_touches.clear();
				break;
		}
		
		lastEventCode = code;
		
		//Log.d(LT, "t:" + type + " c:" + code + " v:" + value);
		return -1;
	}
	
	/**
	 * Checks if the touch event moved more than 5px
	 * if not it returns false
	 * @param p
	 * @return
	 */
	private boolean checkIfMoved(TouchEvent p) {
		TouchEvent te;
		for(int i=touches.size()-2;i>0;i--){
			if((te=touches.get(i)).getIdentifier()==p.getIdentifier())
				return checkDistance(p,te);
		}
		return false;
	}

	/**
	 * Check if distance between touch events is greater than 5px in any axis
	 * @param p
	 * @param te
	 * @return
	 */
	private boolean checkDistance(TouchEvent p, TouchEvent te) {

		if(Math.pow((p.getX()-te.getX()),2)>25)
			return true;
		if(Math.pow((p.getY()-te.getY()),2)>25)
			return true;		
		return false;
	}

	public int getLastX() {
		return lastX;
	}

	public int getLastY() {
		return lastY;
	}

	public int getOriginX() {
		return slideXorigin;
	}

	public int getOriginY() {
		return slideXorigin;
	}

	public int getPressure() {
		return pressure;
	}
	public int getIdentifier() {
		return identifier;
	}
	public int getTouchSize() {
		return touchSize;
	}

	public int getIdFingerUp() {
		return idFingerUp;
	}

}
