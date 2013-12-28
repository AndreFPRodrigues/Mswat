Mswat
=====
SWAT is a framework developed for android to provide developers with the tools to 
monitor, block and inject system wide input events.
Through the accessibility service SWAT is able to determine the current content of the screen system wide.
It empowers developers by giving then options and info that are usually only available at a system level.

##Requirements
Android 4.1.2 or above for full support  
Rooted phone - necessary to monitor/inject/block input events system wide

##How does it work?
The framework is composed by a core module that handles all the work.

###Core

#####Activity Manager Package
Handles all the content info, going through the Accessibility Service and propagates the update 
to the CoreController.
It starts all the framework components.
Checks the Service Preferences.

#####IO Manager Package
Controls all devices, it is able to monitor, block, inject input events into the phone 
devices (touch screen, keypad). It is also able to create a virtual touch drive.
Propagates info to the CoreController

#####Feedback
Provides the TTS and the highlighter.

#####Calibration
Activity responsible for the first use calibration of the framework.
(Calibrating screen size and identifying touch screen driver)

#####CoreController 
Is responsible for all communications. The different core packages communicate 
with the CoreController	which is responsible to propagate the message to the 
registered receivers.
When using the framework always go through the CoreController to access the desired function.

###Interfaces
#####IOReceiver  
Receives IO updates  

#####ContentReceiver  
Receives content update  

###Logger
One possible implementation of a system wide logger    
Current log message (e.g. UP at: Apps x:400 y:400 pressure:3 touchSize:4 id:0)

###Touch Package
#####TouchEvent
Touch event representation
#####TouchPatternRecognizer
Processes the raw input data gathered.
Currently provides two different types of touch recognition   
identifyOnChange() - Up/Move/Down  
identifyOnRelease() - Touch/Slide/LongPress  

###Controllers Package
#####ControlInterface
Parent of all interfaces schemes 
#####TouchController
Example of a interfacing scheme - slide to navigate, touch to select current selected target,
longpress to stop service
#####AutoNavTouch
Example of a interfacing scheme - auto scanning, touch to select current select target

###TouchAdapter
Example of blocking and monitoring the touch driver. 
Creates a virtual touch driver to forward the input events to.



##Getting started
First thing is to understand how to use the framework.
Two of the core functionalities are accessed through implementing the Interfaces mentioned above.

The IOReceiver allows you to get the raw input updates.  
The Content receiver allows you to get updates about the screen contents every time the screen changes

####TouchController
Is an example of an IOReceiver. All you have to do to receive input events is implement 
the interface and register. Registration is done by calling  

    CoreController.registerIOReceiver(this);
After that the onUpdateIO will receive the raw input data.

####AutoNav
Is an example of an IOReceiver and a ContentReceiver. The ContentReceiver works like the IOReceiver
the difference is onUpdateContent it receives a list of the current screen "describable" nodes
(nodes that have some kind of description or are clickable)

####Initialising new components
To start a new component when the framework is initialised all you have to do is go to the 
AndroidManisfest and add a receiver like this:

    <receiver
        android:name="mswat.core.logger.Logger"
        android:enabled="true" >
        <intent-filter>
            <action android:name="mswat_init" />
            <action android:name="mswat_stop" />
        </intent-filter>
    </receiver>
When the service starts a mswat_init intent will be sent to all register broadcasts, you ill
have to extend the broadcastReceiver and check for it:  

    if (intent.getAction().equals("mswat_init")

####Using the framework
To communicate with the core of the framework always go through the CoreController.
All the functions are available through it. 
In TouchController, AutoNavTouch, TouchAdapter and Logger there are several examples of how 
to do it. For more info on the available functions check https://github.com/AndreFPRodrigues/Mswat/blob/master/CoreControllerDoc.md

####Recognizing touch
The touch package is responsible for touch recognition. Currently it provides two types of 
touch classification, in order to learn how to use it please check Logger and TouchController.

####Logger
To use the logger you simply go to the configuration of the service and enable it.
Another possibility is to send and intent to start the logging (from any activity).
All you need to do is:  

    Intent intent = new Intent();
    intent.setAction("mswat_init");
    intent.putExtra("controller", "");
    intent.putExtra("logging", true);
    this.sendBroadcast(intent);

Note: Currently the file is stored in data/data/hierarchical.service/files

####Adapting touch
TouchAdapter is an example on how to block the touch device, create a virtual touch drive
and forward the events from the blocked touch device to the virtual one.





