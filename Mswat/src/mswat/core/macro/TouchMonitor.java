package mswat.core.macro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import mswat.core.CoreController;

/**
 * @author Andre Rodrigues
 * 
 */
public class TouchMonitor implements mswat.interfaces.IOReceiver {
	private final String LT = "Macro";
	private int monitorIndex;
	private int macroMode;
	private StringBuilder touches = new StringBuilder();

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		if (macroMode == MacroManagment.TOUCH_MACRO) {
			// Log.d(LT, "Device:" + device + " type:" + type + " code" + code +
			 //" value:" + value + " timestamp:" + timestamp);
			//
			//CoreController.addMacroStep(type +"," + code +"," +value +"," +timestamp );
			touches.append(type +"," + code +"," +value +"," +timestamp +",");
		}
	}

	@Override
	public void onTouchReceived(int type) {
		// TODO Auto-generated method stub

	}
	
	public String getTouches(){
		String result = ","+touches.substring(0, touches.length()-2);
		touches = new StringBuilder();
		return result;
	}

	public TouchMonitor() {
		CoreController.monitorTouch();
		monitorIndex = registerIOReceiver();
		macroMode = MacroManagment.NAV_MACRO;
	}

	public void setMode(int mode) {
		Log.d(LT, "setMode" + mode);
		macroMode = mode;
	}

	public void finish() {
		Log.d(LT, "finished");
		macroMode= MacroManagment.NAV_MACRO;;
		CoreController.unregisterIOReceiver(monitorIndex);
	}

}
