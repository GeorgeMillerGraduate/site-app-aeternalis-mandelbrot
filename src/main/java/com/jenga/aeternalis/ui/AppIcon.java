package com.jenga.aeternalis.ui;
import javafx.stage.Stage;
import javafx.scene.image.Image;
/** One application identity for every owned stage, including dialogs and colour-picker custom windows. */
public final class AppIcon {
    private static Image icon;private static boolean watching;
    private AppIcon(){}
    public static void apply(Stage stage){if(icon==null)icon=new Image(java.util.Objects.requireNonNull(AppIcon.class.getResourceAsStream("/com/jenga/aeternalis/assets/aeternalis-emblem.png")));stage.getIcons().setAll(icon);}
    public static void watch(){if(watching)return;watching=true;javafx.stage.Window.getWindows().addListener((javafx.collections.ListChangeListener<javafx.stage.Window>)change->{while(change.next())if(change.wasAdded())for(var w:change.getAddedSubList())if(w instanceof Stage s)apply(s);});}
}
