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
	private teste1 t1;
	private int aux = 0;
	
	@Override
	public int registerContentReceiver() {
		CoreController.registerContentReceiver(this);
		return 0;
	}

	@Override
	public void onUpdateContent(ArrayList<Node> content) {
		aux++;
		if(aux%3==0){
			Log.d(LT, "Inject");
			t1.injectTouch(200, 200);
			//t1.sinc();
		}
		
	}

	@Override
	public void onReceive(Context arg0, Intent intent) {
		if (intent.getAction().equals("mswat_init1")) {
			t1= new teste1();
			CoreController.monitorTouch();
			registerContentReceiver();
			Log.d(LT, "init vodafone");
		}
		
	}

}
