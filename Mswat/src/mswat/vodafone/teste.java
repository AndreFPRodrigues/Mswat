package mswat.vodafone;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import mswat.controllers.ControlInterface;
import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import mswat.interfaces.ContentReceiver;

public class teste extends ControlInterface implements ContentReceiver {
	
	final String LT= "vodafone";
	@Override
	public int registerContentReceiver() {
		CoreController.registerContentReceiver(this);
		return 0;
	}

	@Override
	public void onUpdateContent(ArrayList<Node> content) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReceive(Context arg0, Intent intent) {
		if (intent.getAction().equals("mswat_init1")) {
			registerContentReceiver();
			Log.d(LT, "init vodafone");
		}
		
	}

}
