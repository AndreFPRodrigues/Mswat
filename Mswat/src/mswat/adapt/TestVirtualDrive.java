package mswat.adapt;

import mswat.core.CoreController;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TestVirtualDrive extends BroadcastReceiver {
	private final String LT = "VirtualDrive";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("mswat_init1")) {
			Log.d(LT, "MSWaT INIT");
			// starts monitoring touchscreen
			int deviceIndex = CoreController.monitorTouch();

			// blocks the touch screen
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					true);

			// monitoring touch returns touches in raw data
			CoreController.commandIO(CoreController.SET_TOUCH_RAW, -1, true);

			// create virtual touch drive
			CoreController.commandIO(CoreController.CREATE_VIRTUAL_TOUCH, -1,
					false);
			
			//start forwarding events to the virtual touch drive 
			CoreController.commandIO(CoreController.FOWARD_TO_VIRTUAL, 0, true);
			
		} else {
			
			if (intent.getAction().equals("monitor")) {
			

			}
		}
	}
}
