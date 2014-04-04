package tutorials.receivers;

import java.util.ArrayList;

import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This tutorial shows how you can use use the NotificationReceiver interface
 * 
 * Check out Receivers tutorial
 * 
 * @author Andre Rodrigues
 * 
 */
public class ContentTutorial extends BroadcastReceiver implements
		mswat.interfaces.ContentReceiver {

	@Override
	public int registerContentReceiver() {
		return CoreController.registerContentReceiver(this);
	}

	/**
	 * Receives updates of the current describable content on the screen
	 */
	@Override
	public void onUpdateContent(ArrayList<Node> content) {
		Log.d("ContentTutorial", "-------------------------------------------------");

		for (int i = 0; i < content.size(); i++)
			Log.d("ContentTutorial", content.get(i).getName());

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")) {

			// Uncomment the line bellow to test content receiver
			//registerContentReceiver();
			
			

		}

	}

	@Override
	public int getType() {
		return DESCRIBABLE;
	}

}
