package mswat.core.macro;

import mswat.core.CoreController;
import mswat.core.activityManager.R;
import mswat.core.activityManager.R.layout;
import mswat.core.activityManager.R.menu;
import mswat.core.feedback.FeedBack;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class RunMacro extends Activity {
	private final static String LT = "Macro";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run_macro);
		
		String macro = getIntent().getStringExtra("macro");
		if(macro!=null){
			Log.d(LT, "Run:" + macro );
			CoreController.runMacro(macro);
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.run_macro, menu);

		return true;
	}

	public void newMacro(View v) {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Create Macro");
		alert.setMessage("Name");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  Editable value = input.getText();
		  CoreController.newMacro(value.toString());
			finish();
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		  }
		});

		alert.show();
	}

}
