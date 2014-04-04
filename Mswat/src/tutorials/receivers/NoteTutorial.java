package tutorials.receivers;
/**
 * This tutorial shows how you can use use the NotificationReceiver interface
 * 
 * Check out Receivers tutorial
 * @author Andre Rodrigues
 * 
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import mswat.core.CoreController;
import mswat.interfaces.NotificationReceiver;

public class NoteTutorial extends BroadcastReceiver implements
		NotificationReceiver {
	private final String LT = "TestNotification";

	@Override
	public int registerNotificationReceiver() {
		return CoreController.registerNotificationReceiver(this);

	}

	@Override
	public void onNotification(String notification) {
		Log.d(LT, notification);

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")) {
			
			//Uncomment the line bellow to test notification receiver
			//registerNotificationReceiver();
			

		}

	}
}
