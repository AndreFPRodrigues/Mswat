package mswat.core.calibration;

import java.util.ArrayList;


import mswat.core.CoreController;
import mswat.core.activityManager.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;

public class CalibrationActivity extends Activity {

	ImageButton imageButton;
	private final static String LT = "Calibration";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_calibration);
		// addListenerOnButton();

		setContentView(R.layout.activity_listviewexampleactivity);
		final ListView listview = (ListView) findViewById(R.id.listview);
		String[] values = CoreController.getDevices();

		final ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < values.length; ++i) {
			list.add(values[i]);
		}
		final ListAdapter ad = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, list);
		listview.setAdapter(ad);

		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {


				CoreController.commandIO(CoreController.SETUP_TOUCH, position, false);
				Intent i = new Intent(getBaseContext(), CalibrationScreen.class);
				startActivity(i);
				finish(); 
			}

		});
	}


}
