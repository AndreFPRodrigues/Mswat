package mswat.core.activityManager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class ServicePreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public final String LOG_IO = "log1";
	public final String LOG_AT_TOUCH = "log2";
	public final String LOG_NAV = "log3";

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
		
		boolean isEnabled =getPreferenceScreen().getSharedPreferences().getBoolean(LOG_IO, false);

		getPreferenceScreen().findPreference(LOG_AT_TOUCH).setEnabled(
				isEnabled);

	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(LOG_IO)) {

			boolean isEnabled = sharedPreferences.getBoolean(key, false);

			getPreferenceScreen().findPreference(LOG_AT_TOUCH).setEnabled(
					isEnabled);
		}
	}

}
