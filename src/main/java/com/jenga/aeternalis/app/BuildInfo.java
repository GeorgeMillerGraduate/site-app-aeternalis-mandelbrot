package com.jenga.aeternalis.app;
import java.util.Properties;
/** Release metadata also checked against the Maven project during verification. */
public final class BuildInfo {
    public static final String VERSION;
    static {Properties p=new Properties();try(var in=BuildInfo.class.getResourceAsStream("/com/jenga/aeternalis/application.properties")){if(in!=null)p.load(in);}catch(java.io.IOException ignored){}VERSION=p.getProperty("version","development");}
    private BuildInfo(){}
}
