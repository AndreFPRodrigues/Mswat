package mswat.examples.adapt;

import mswat.core.CoreController;
import mswat.interfaces.IOReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TouchAdapter extends BroadcastReceiver implements IOReceiver {
	private final String LT = "VirtualDrive";
	private int deviceIndex;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("mswat_init1")) {

			Log.d(LT, "Virtual touch initialised");

			// register receiver
			registerIOReceiver();

			// starts monitoring touchscreen
			deviceIndex = CoreController.monitorTouch();

			// blocks the touch screen
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					true);

			// create virtual touch drive
			CoreController.commandIO(CoreController.CREATE_VIRTUAL_TOUCH, -1,
					false);

		}
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {

		if (device == deviceIndex) {

			// adapting to the real screen coords
			if (code == 53)
				value = CoreController.xToScreenCoord(value);
			if (code == 54) {
				value = CoreController.yToScreenCoord(value);
			}
			
			//adapt values before injecting
			CoreController.injectToVirtual(type, code, value);
		}
	}

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);

	}

	@Override
	public void onTouchReceived(int type) {
		// TODO Auto-generated method stub
		
	}
}
