package mswat.caseStudy.controllers.autonav;

import java.util.ArrayList;

import mswat.core.CoreController;
import mswat.core.activityManager.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AutoNavSetup extends Activity {

	ImageButton imageButton;
	private final static String LT = "Calibration";
	private ArrayList <Integer>selectedListItems = new ArrayList<Integer>();
	private ArrayList<String> filter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		filter = new ArrayList<String>();

		setContentView(R.layout.activity_listviewexampleactivity);
		final ListView listview = (ListView) findViewById(R.id.listview);
		Intent sender = getIntent();
		String[] values = sender.getStringArrayExtra("values");

		final ArrayList<String> list = new ArrayList<String>();

		for (int i = 1; i < values.length; ++i) {
			list.add(values[i]);
		}

		list.add("end filtering");

		final ListAdapter ad = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, list);
		listview.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, list) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView textView = (TextView) super.getView(position,
						convertView, parent);

				if (convertView != null) {
					if (selectedListItems.indexOf(position)!=-1) {
						convertView.setBackgroundColor(Color.LTGRAY);
					} else {
						convertView.setBackgroundColor(Color.TRANSPARENT);

					}
				}

				return textView;
			}
		});
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {

				String selectedFromList = (String) (listview
						.getItemAtPosition(position));
				if (selectedFromList.equals("end filtering")) {
					Intent intent = new Intent();
					intent.setAction("mswat_autoNavStart");
					String[] fi = new String[filter.size()];
					fi = filter.toArray(fi);
					intent.putExtra("filter", fi);
					sendBroadcast(intent);
					finish();
				}
				if (!filter.contains(selectedFromList)) {
					selectedListItems.add(position);
					Log.d(LT, "Painted");
					filter.add(selectedFromList);
					listview.setItemChecked(position, true);
					view.setBackgroundColor(Color.LTGRAY);

				} else {
					filter.remove(selectedFromList);
					selectedListItems.remove((Object) position);
					view.setBackgroundColor(Color.TRANSPARENT);
				}

			}

		});
	}

}
