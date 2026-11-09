package com.jenga.aeternalis;
import com.jenga.aeternalis.ui.*;
import javafx.application.Application;
import javafx.stage.Stage;
/** Production startup; Studio remains independently launchable for UI verification. */
public final class AeternalisLauncher extends Application {
    private OpeningSplash splash;private Studio studio;
    public void start(Stage primary){splash=new OpeningSplash(()->{studio=new Studio();studio.start(primary);});splash.show();}
    public void stop(){if(splash!=null)splash.close();if(studio!=null)studio.stop();}
}
