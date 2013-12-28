package mswat.core.feedback;

import java.util.Locale;

import mswat.core.activityManager.HierarchicalService;

import android.graphics.Color;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class FeedBack implements OnInitListener {
	
	//TTS 
	static TextToSpeech mTts;
	
	//Highlighter
	static Handler hightligherHandler;
	static // Set Focus manually
	RelativeLayout overlay;
	
	static WindowManager windowManager;
	static LayoutParams params;
	
	static HierarchicalService hs;
	
	public FeedBack(HierarchicalService hs, WindowManager windowManager, LayoutParams params){
		//TTS
		FeedBack.hs=hs;
		mTts = new TextToSpeech(hs, this);
		onInit(TextToSpeech.SUCCESS);
		
		//Highlighter
		hightligherHandler= new Handler();
		FeedBack.windowManager= windowManager;
		FeedBack.params=params;
		
	}
	
	/**
	 * Initialise TTS
	 * @param status
	 */
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {

			int result = mTts.setLanguage(Locale.getDefault());

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
	 * Text to speech
	 * @param text
	 */
	public static void textToSpeech(String text){
		mTts.speak(text,TextToSpeech.QUEUE_FLUSH, null);
	}
	
	
	/**
	 *  Adds a highlight and removes all others
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 * @param color 
	 */
	public static void hightlight(final int marginTop, final  int marginLeft,
			 final float alpha,  final int width, final int height, final int color) {
		hightligherHandler.post(new Runnable() {
			public void run() {

				if (overlay != null){
					windowManager.removeView(overlay);
					overlay.removeAllViews();
					overlay=null;
				}

				
					overlay = new RelativeLayout(hs);

					ImageView tv1 = new ImageView(hs);
					tv1.setBackgroundColor(color);

					tv1.setAlpha(alpha);

					RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(
							width, height);
					parms.leftMargin = marginLeft;
					parms.topMargin = marginTop;
					tv1.setLayoutParams(parms);

					overlay.addView(tv1, parms);
					windowManager.addView(overlay, params);
					
					
				

			}
		});
	}
	
	/**
	 * Adds a highlight 
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 */
	public static void addHightlight( int marginTop,  int marginLeft,
			 float alpha,  int width,  int height) {
	
	}
	
	/**
	 * Clear highlights
	 */
	public static void clearHightlights( ) {
	
		if (overlay != null){
			windowManager.removeView(overlay);
			overlay.removeAllViews();
			overlay=null;
		}
	}

	public static void stop() {
		clearHightlights();
		mTts.shutdown();
		
	}
	
	
	
	
	
}
