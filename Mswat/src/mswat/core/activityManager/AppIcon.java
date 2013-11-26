package mswat.core.activityManager;

import android.graphics.drawable.Drawable;
/**
 * Class that represents one application icon and its name
*/
public class AppIcon {
	private Drawable icon;
	private String description;
	
	public AppIcon(Drawable ico, String desc){
		icon=ico;
		description=desc;
	}
	
	public Drawable getIcon(){
		return icon;
	}
	
	public String getDescription() {
		return description;
	}
}
