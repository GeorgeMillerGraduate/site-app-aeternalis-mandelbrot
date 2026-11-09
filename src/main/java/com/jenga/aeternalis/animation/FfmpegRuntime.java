package com.jenga.aeternalis.animation;
import java.io.IOException;
import java.nio.file.*;
/** Maven-resolved native executable, extracted by JavaCPP to its versioned cache. No PATH dependency. */
public final class FfmpegRuntime {
    private FfmpegRuntime(){}
    private static String executable;
    public static synchronized String executable()throws IOException{
        if(executable!=null&&Files.isExecutable(Path.of(executable)))return executable;
        try{
            System.setProperty("org.bytedeco.javacpp.platform.extension","-gpl");
            String path=org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            if(path==null||!Files.isRegularFile(Path.of(path)))throw new IOException("Packaged FFmpeg executable could not be extracted.");
            executable=path;return executable;
        }catch(LinkageError|RuntimeException ex){throw new IOException("Cannot load the Maven FFmpeg runtime for this platform. Reload Maven dependencies and use a supported 64-bit JDK. "+ex.getMessage(),ex);}
    }
}
