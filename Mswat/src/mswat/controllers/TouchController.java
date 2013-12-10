package mswat.controllers;

import mswat.core.CoreController;
import mswat.touch.TouchPatternRecognizer;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Touch interfacing scheme
 * 
 * @author Andre Rodrigues
 * 
 */
public class TouchController extends ControlInterface {
	private final String LT = "TouchController";
	// if the control interface is active or not
	private static boolean isEnable = false;

	@Override
	public void onReceive(Context context, Intent intent) {

		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")
				&& intent.getExtras().get("controller")
						.equals("touchController")) {
			Log.d(LT, "MSWAT INIT");

			// enable controller
			isEnable = true;

			// starts monitoring touchscreen
			int deviceIndex = CoreController.monitorTouch();
			// blocks the touch screen
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					true);

			// sets automatic highlighting
			//CoreController.setAutoHighlight(true);

		} else if (isEnable) {
			// Triggered with every IO monitor message
			if (intent.getAction().equals("monitor")) {
				Log.d(LT, intent.getExtras().getString("message"));
				int type = intent.getExtras().getInt("type");
				switch (type) {
				case TouchPatternRecognizer.SLIDE:
					navNext();
					break;
				case TouchPatternRecognizer.TOUCHED:
					selectCurrent();
					break;
				case TouchPatternRecognizer.LONGPRESS:
					// Stops service
					CoreController.stopService();
					break;
				}

			}
		}

	}

}
