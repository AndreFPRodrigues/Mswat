package mswat.core.feedback;

import java.util.Locale;

import mswat.core.activityManager.HierarchicalService;

import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class FeedBack implements OnInitListener {

	// Debugging purposes
	private final static String LT = "FeedBack";

	// TTS
	static TextToSpeech mTts;

	// Highlighter
	static Handler hightligherHandler;
	static RelativeLayout overlay;
	static ImageView tv1;
	static RelativeLayout.LayoutParams parms;

	static WindowManager windowManager;
	static LayoutParams params;

	static HierarchicalService hs;

	static boolean firstTime = true;
	static boolean stop = false;

	public FeedBack(HierarchicalService hs, WindowManager windowManager,
			LayoutParams params) {
		// TTS
		FeedBack.hs = hs;
		mTts = new TextToSpeech(hs, this);
		onInit(TextToSpeech.SUCCESS);

		// Highlighter
		hightligherHandler = new Handler();
		FeedBack.windowManager = windowManager;
		FeedBack.params = params;

		overlay = new RelativeLayout(hs);
		parms = new RelativeLayout.LayoutParams(0, 0);
		tv1 = new ImageView(hs);

	}

	/**
	 * Initialise TTS
	 * 
	 * @param status
	 */
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {

			int result = mTts.setLanguage(Locale.ENGLISH);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.d("TTS", "This Language is not supported");
			} else {
				Log.d("TTS", "Ready!");
			}

		} else {
			Log.d("TTS", "Initilization Failed!");
		}

	}

	/**
	 * Text to speech Wait for tts to end before returning
	 * 
	 * @param text
	 */
	public static boolean waitFortextToSpeech(String text) {
		mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);

		boolean isspeakign = mTts.isSpeaking();
		do {
			SystemClock.sleep(200);
			isspeakign = mTts.isSpeaking();
		} while (isspeakign);

		return isspeakign;

	}

	/**
	 * Text to speech
	 * 
	 * @param text
	 */
	public static void textToSpeech(String text) {
		mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);

	}

	/**
	 * Adds a highlight and removes all others
	 * 
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 * @param color
	 */
	public synchronized static void hightlight(final int marginTop,
			final int marginLeft, final float alpha, final int width,
			final int height, final int color) {
		hightligherHandler.post(new Runnable() {
			public synchronized void run() {
				if (!stop) {
					if (overlay != null) {
						overlay.removeAllViews();
					}
					tv1.setBackgroundColor(color);
					tv1.setAlpha(alpha);
					parms.height = height;
					parms.width = width;
					parms.leftMargin = marginLeft;
					parms.topMargin = marginTop;
					tv1.setLayoutParams(parms);

					if (overlay != null) {
						overlay.addView(tv1, parms);
					}

					if (!firstTime) {
						windowManager.updateViewLayout(overlay, params);
					} else {
						windowManager.addView(overlay, params);

					}

					firstTime = false;
				}
			}
		});
	}

	/**
	 * Adds a highlight
	 * 
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 * @param color
	 */
	public static void addHighlight(final int marginTop, final int marginLeft,
			final float alpha, final int width, final int height,
			final int color) {
		Log.d(LT, "ADD OVERLAY");

		hightligherHandler.post(new Runnable() {
			public synchronized void run() {
				ImageView tv2 = new ImageView(hs);
				tv2.setBackgroundColor(color);
				tv2.setAlpha(alpha);
				RelativeLayout.LayoutParams parms1 = new RelativeLayout.LayoutParams(
						0, 0);
				parms1.height = height;
				parms1.width = width;
				parms1.leftMargin = marginLeft;
				parms1.topMargin = marginTop;
				tv2.setLayoutParams(parms1);

				if (overlay != null) {
					overlay.addView(tv2, parms1);
				}

				if (!firstTime) {
					windowManager.updateViewLayout(overlay, params);
				} else {
					windowManager.addView(overlay, params);

				}

				firstTime = false;

			}
		});
	}

	/**
	 * Clear highlights
	 */
	public static void clearHightlights() {

		if (overlay != null) {
			windowManager.removeView(overlay);
			overlay.removeAllViews();
			overlay = null;
			stop = true;
		}
	}

	public static void stop() {
		clearHightlights();
		mTts.shutdown();

	}

}
