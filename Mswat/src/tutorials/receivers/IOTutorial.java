package tutorials.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import mswat.core.CoreController;

/**
 * This tutorial shows how you can use use the IOReceiver interface
 * 
 * Check out IOReceiver Interface tutorial
 * @author Andre Rodrigues
 * 
 */
public class IOTutorial extends BroadcastReceiver implements
		mswat.interfaces.IOReceiver {

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		Log.d("tutorialIO", "Device:" + device + " type:" + type + " code" + code + " value:" + value + " timestamp:" + timestamp);
	}

	@Override
	public void onTouchReceived(int type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReceive(Context arg0, Intent intent) {
		
		//remove the false boolean from the if statement to try the tutorial 
		if (intent.getAction().equals("mswat_init") && false ) {

			// register receiver
			registerIOReceiver();

			//starts monitoring touchscreen (option 1)
			 CoreController.monitorTouch(); 
		
			//uncomment to monitor the keypad (option 2)
			 /*
			String[] devices = CoreController.getDevices();
			for (int i = 0; i < devices.length; i++) {
				if (devices[i].contains("keypad")) {
					CoreController.commandIO(CoreController.MONITOR_DEV, i,
							true);
					break;
				}
			}*/

		}

	}

}
