package mswat.keyboard;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import mswat.core.CoreController;

/**
 *
 * @author Andre Rodrigues
 *
 */
public abstract class SwatKeyboard extends  BroadcastReceiver {
	
	protected ArrayList<ArrayList<String>> keyboardLayout;
	protected  int [] index= new int[2];
	
	/**
	 * Give the keyboard structure and registers has active keyboard
	 * @param keys
	 */
	public void setKeyboard(ArrayList<ArrayList<String>> keys){
		keyboardLayout=keys;
		registerKeyboard();
	}
	
	/**
	 * Register keyboard
	 */
	public void registerKeyboard(){
		CoreController.registerActivateKeyboard(this);
	}
	
	/**
	 * Write char to current focus
	 * @param c
	 */
	public  void writeChar(char c){
		int code=c;
		CoreController.callKeyboardWriteChar(code);
	}
	
	/**
	 * Backspace in current focused editBox
	 */
	public  void backSpace(){
		CoreController.callKeyboardWriteChar(-5);
	}
	
	/**
	 * Backspace in current focused editBox
	 */
	public  void del(){
		CoreController.callKeyboardWriteDirect(112);
	}
	
	/**
	 * Write string to the current focus
	 * @param s
	 */
	public  void writeString(String s){
		int size= s.length();
		int [] array = new int [size];
		
		for(int i=0 ;i<size;i++){
			array[i]=s.charAt(i);
		}
		
		CoreController.callKeyboardWriteString(array);
		
	}
	
	/**
	 * Navigate rigth on the keyboard layout
	 * @return true when the index goes from the last column to the first
	 */
	public boolean navRight(){
		index[1]++;
		
		boolean cycled=false;
		
		if(index[1]>=keyboardLayout.get(index[0]).size())
			cycled=true;
		
		index[1] = index[1] % keyboardLayout.get(index[0]).size();
		
		return cycled;
	}
	
	/**
	 * Navigate left on the keyboard layout
	 * @return
	 */
	public boolean navLeft(){
		index[1]--;
		
		boolean cycled=false;
		if(index[1]<0){
			cycled=true;
			index[1] =keyboardLayout.get(index[0]).size() -1;
		}
		return cycled;
	}
	
	/**
	 * Navigate down on the keyboard layout
	 * @return
	 */
	public boolean navDown(){
		index[0]++;
		
		boolean cycled=false;
		if(index[0]>=keyboardLayout.size())
			cycled=true;
		
		index[0] = index[0] % keyboardLayout.size();
		
		return cycled;
	}
	
	/**
	 * Navigate down on the keyboard layout
	 * @return
	 */
	public boolean navUp(){
		index[0]--;
		
		boolean cycled=false;
		if(index[0]<0){
			cycled=true;
			index[0] = keyboardLayout.size() -1;
		}
		return cycled;
	}
	
	public void resetSearchDown(){
		index[1]=-1;
 
	}
	public void resetSearchUp(){
		index[1]=-1;
		

	}
	public void resetSearch(){
		index[1]=-1;
		index[0]=-1;

	}
	
	
	public void resetColumnSearch(){
		index[1]=-1;

	}
	
	/**
	 * Return current focused key
	 * @return
	 */
	public String getCurrent(){
		if(index[1]<0 )
			return  keyboardLayout.get(index[0]).get(0);
		return keyboardLayout.get(index[0]).get(index[1]);
	}
	
	
	/**
	 * Show keyboard layout
	 * @param c
	 */
	public abstract void show(Context c);
	
	/**
	 * Hide/close keyboard
	 */
	public abstract void hide();

	/**
	 * Start keyboard
	 */
	public abstract void start();

	/**
	 * Update UI
	 */
	public abstract void update() ;
		
	
}
