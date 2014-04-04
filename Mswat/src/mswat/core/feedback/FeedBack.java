package mswat.core.feedback;

import java.util.Locale;

import mswat.core.CoreController;
import mswat.core.activityManager.HierarchicalService;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

	// macro commands
	static LinearLayout macroLayout;

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
		// Log.d(LT, "ADD OVERLAY");

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
		if (macroLayout != null) {
			// windowManager.removeView(macroLayout);
			macroLayout.removeAllViews();
		}else
		if (overlay != null && overlay.getHeight() != 0) {
			windowManager.removeView(overlay);
			overlay.removeAllViews();
			// overlay = null;
			stop = true;
			firstTime = true;
		} 
	}

	public static void stop() {
		clearHightlights();
		Log.d(LT, "Feed back stoped");
		mTts.shutdown();

	}

	public static void enableHightlights() {
		stop = false;

	}

	public static void macroCommands() {

		LinearLayout.LayoutParams buttonLayout = new LinearLayout.LayoutParams(
				60, 60);
		macroLayout = new LinearLayout(hs);
		Button home = new Button(hs);
		home.setLayoutParams(buttonLayout);
		home.append("H");

		Button back = new Button(hs);
		back.setLayoutParams(buttonLayout);
		back.append("B");

		Button mode = new Button(hs);
		mode.setLayoutParams(buttonLayout);
		mode.append("M");

		Button end = new Button(hs);
		end.setLayoutParams(buttonLayout);
		end.append("E");

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				PixelFormat.OPAQUE);
		params.gravity = Gravity.RIGHT | Gravity.TOP;

		home.setOnTouchListener(new android.view.View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, android.view.MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					CoreController.addMacroStep("Home");
					CoreController.home();
				}
				return false;
			}
		});
		back.setOnTouchListener(new android.view.View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, android.view.MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					CoreController.addMacroStep("Back");
					CoreController.back();
				}
				return false;
			}
		});
		mode.setOnTouchListener(new android.view.View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, android.view.MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					CoreController.changeModeMacro();
				}
				return false;
			}
		});
		end.setOnTouchListener(new android.view.View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, android.view.MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {

					CoreController.finishMacro();
				}
				return false;
			}
		});
		macroLayout.addView(home);
		macroLayout.addView(back);
		macroLayout.addView(mode);
		macroLayout.addView(end);

		windowManager.addView(macroLayout, params);

	}

}
