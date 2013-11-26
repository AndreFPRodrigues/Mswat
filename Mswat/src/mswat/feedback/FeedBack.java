package mswat.feedback;

import java.util.Locale;

import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class FeedBack {
	
	//TTS 
	TextToSpeech mTts;
	
	//Highlighter
	Handler hightligherHandler;
	
	public FeedBack(TextToSpeech mTts){
		this.mTts =mTts;
		onInit(TextToSpeech.SUCCESS);
		
		//TO DO//
	}
	
	/**
	 * Initialise TTS
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
	 * Text to speech
	 * @param text
	 */
	public void textToSpeech(String text){
		
	}
	
	
	/**
	 *  Adds a highlight and removes all others
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 */
	public void hightlight( int marginTop,  int marginLeft,
			 float alpha,  int width,  int height) {
	
	}
	
	/**
	 * Adds a highlight 
	 * @param marginTop
	 * @param marginLeft
	 * @param alpha
	 * @param width
	 * @param height
	 */
	public void addHightlight( int marginTop,  int marginLeft,
			 float alpha,  int width,  int height) {
	
	}
	
	/**
	 * Clear highlights
	 */
	public void clearHightlights( ) {
	
	}
	
	
	
	
	
}
