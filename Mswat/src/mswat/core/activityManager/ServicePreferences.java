package mswat.core.activityManager;


import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class ServicePreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public final String LOG = "log";
	public final String CONTROLLER = "controller";
	public final String KEYBOARD = "keyboard";
	public final String AUDIO = "audio";
	public final String VISUAL = "visual";
	public final String CALIBRATION = "calibration";
	public final String BROADCAST_IO = "broadcast_io";
	public final String BROADCAST_CONTENT = "broadcast_content";

	public final String TOUCH_INDEX = "touch_device";

	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	}


}
