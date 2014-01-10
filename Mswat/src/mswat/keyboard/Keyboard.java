package mswat.keyboard;

import mswat.core.CoreController;

/**
 * TODO
 * @author Andre Rodrigues
 *
 */
public class Keyboard {
	

	
	public static void writeChar(char c){
		int code=c;
		CoreController.callKeyboardWriteChar(code);
	}
	
	public static void backSpace(){
		CoreController.callKeyboardWriteChar(-5);
	}
	
	public static void writeString(String s){
		int size= s.length();
		int [] array = new int [size];
		
		for(int i=0 ;i<size;i++){
			array[i]=s.charAt(i);
		}
		
		CoreController.callKeyboardWriteString(array);
		
	}
	
}
