package mswat.virtualDrive;

import mswat.core.CoreController;
import android.content.BroadcastReceiver;

public  abstract class VirtualTouchControllerA   {
	final int EV_ABS = 0x03,ABS_MT_POSITION_X =53, ABS_MT_POSITION_Y=54, SYN_MT_REPORT=2, SYN_REPORT=0;// 330
	
	
	public void injectTouch(int x , int y){
		CoreController.injectToTouch(EV_ABS, ABS_MT_POSITION_X, x);
		CoreController.injectToTouch(EV_ABS, ABS_MT_POSITION_Y, y);
		CoreController.injectToTouch(EV_ABS, 58, 15);
		CoreController.injectToTouch(EV_ABS, 48, 3);
		CoreController.injectToTouch(EV_ABS, 57, 0);

		CoreController.injectToTouch(0, SYN_MT_REPORT, 0);
		CoreController.injectToTouch(0, SYN_REPORT, 0);
		CoreController.injectToTouch(0, SYN_MT_REPORT, 0);
		CoreController.injectToTouch(0, SYN_REPORT, 0);

	} 
	
	public void injectSlide(int x , int y, int x1 , int y1){
		int addX=(x1-x)/5;
		int addY=(y1-y1)/5;
		
		for(int i =0;i<5;i++){
			CoreController.injectToVirtual(EV_ABS, ABS_MT_POSITION_X, x);
			CoreController.injectToVirtual(EV_ABS, ABS_MT_POSITION_Y, y);
			CoreController.injectToVirtual(0, SYN_REPORT, 0);
			x+=addX;
			y+=addY;
		}
	}
	
	

	public void sinc(){
		CoreController.injectToVirtual(0, SYN_REPORT, 0);
		CoreController.injectToVirtual(0, SYN_MT_REPORT, 0);
		CoreController.injectToVirtual(0, SYN_REPORT, 0);

	}
}
