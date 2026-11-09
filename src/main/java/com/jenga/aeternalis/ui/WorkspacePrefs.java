package com.jenga.aeternalis.ui;
import java.util.prefs.Preferences;
/** Versioned local workspace preferences, independent of artwork/project files. */
public final class WorkspacePrefs {
    public record Layout(double width,double height,boolean maximized,double horizontal,double vertical,int properties,int browser,boolean focus) {
        public Layout {if(!Double.isFinite(width)||!Double.isFinite(height)||width<1000||height<650||width>10000||height>10000||!Double.isFinite(horizontal)||!Double.isFinite(vertical)||horizontal<.2||horizontal>.9||vertical<.2||vertical>.85||properties<0||properties>4||browser<0||browser>3)throw new IllegalArgumentException("Invalid workspace dimensions");}
        public static Layout defaults(){return new Layout(1440,900,true,.76,.64,0,0,false);}
    }
    private WorkspacePrefs(){}
    private static boolean enabled(){return !Boolean.getBoolean("aeternalis.disableWorkspacePersistence");}
    public static Layout load(){
        if(!enabled())return Layout.defaults();
        try{var p=Preferences.userNodeForPackage(WorkspacePrefs.class);if(p.getInt("schema",1)!=1)return Layout.defaults();return new Layout(p.getDouble("width",1440),p.getDouble("height",900),p.getBoolean("maximized",true),p.getDouble("horizontal",.76),p.getDouble("vertical",.64),p.getInt("properties",0),p.getInt("browser",0),p.getBoolean("focus",false));}catch(RuntimeException ex){return Layout.defaults();}
    }
    public static void save(Layout value){
        if(!enabled())return;try{var p=Preferences.userNodeForPackage(WorkspacePrefs.class);p.putInt("schema",1);p.putDouble("width",value.width());p.putDouble("height",value.height());p.putBoolean("maximized",value.maximized());p.putDouble("horizontal",value.horizontal());p.putDouble("vertical",value.vertical());p.putInt("properties",value.properties());p.putInt("browser",value.browser());p.putBoolean("focus",value.focus());}catch(RuntimeException ignored){}
    }
}
