package mswat.core.calibration;

import mswat.core.CoreController;
import mswat.core.activityManager.R;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;

import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.ImageButton;

public class CalibrationScreen extends Activity {

	ImageButton imageButton;
	private final static String LT = "Calibration";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibration);
		addListenerOnButton();

	

	}

	public void addListenerOnButton() {

		imageButton = (ImageButton) findViewById(R.id.imageButton1);

		imageButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				CoreController.stopCalibration();
				close();

			}

		});

	}

	public void close() {
		this.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.calibration, menu);
		return true;
	}

}
