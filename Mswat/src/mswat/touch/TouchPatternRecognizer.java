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

	// Touch types
	public final static int TOUCHED = 0;
	public final static int SLIDE = 1;
	public final static int LONGPRESS = 2;

	// Store variables
	private int lastX;
	private int lastY;
	private int slideXorigin;
	private int slideYorigin;
	private int lastEventCode = -1;
	private int pressure;
	private int touchSize;

	// Array to store all touch events
	private ArrayList<TouchEvent> touches = new ArrayList<TouchEvent>();

	/**
	 * Identify type of touch
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
	public int store(int type, int code, int value, int timestamp) {
		// Log.d(LT," "+ touches.size());
		if (code == 58)
			pressure = value;
		else if (code == 48)
			touchSize = value;
		if (code == 53)
			lastX = value;
		else if (code == 54)
			lastY = value;
		else if (code == 2 && value == 0) {
			TouchEvent p = new TouchEvent(lastX, lastY, timestamp);
			touches.add(p);
		}
		if (code == 2 && lastEventCode == 0 && touches.size() > 0) {
			lastEventCode = -1;
			return identifyTouch();
		}
		lastEventCode = code;
		return -1;
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

	public int getTouchSize() {
		return touchSize;
	}
	
	
}
