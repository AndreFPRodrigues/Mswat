package mswat.core.activityManager;

import mswat.controllers.TouchController;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;

public class ServicePreferences extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	
	public static final String LOG = "log";
	public static final String CONTROLLER = "controller";
	public static final String AUDIO = "audio";
	public static final String VISUAL = "visual";
	public static final String CALIBRATION = "calibration";
	public static final String BROADCAST_IO = "broadcast_io";
	public static final String BROADCAST_CONTENT = "broadcast_content";

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
