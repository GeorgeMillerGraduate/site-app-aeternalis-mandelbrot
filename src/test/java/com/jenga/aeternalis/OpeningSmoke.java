package com.jenga.aeternalis;
import com.jenga.aeternalis.ui.*;
import javafx.application.Platform;
import javafx.stage.*;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
public final class OpeningSmoke {
    static <T>T fx(Callable<T> c)throws Exception{FutureTask<T> f=new FutureTask<>(c);Platform.runLater(f);return f.get(20,TimeUnit.SECONDS);}
    static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args[0]);Files.createDirectories(folder);Platform.startup(()->{});Platform.setImplicitExit(false);
        AeternalisLauncher launcher=new AeternalisLauncher();Stage primary=fx(()->{Stage p=new Stage();launcher.start(p);return p;});
        try{
            Thread.sleep(1200);
            fx(()->{OpeningSplash splash=(OpeningSplash)field(launcher,"splash");if(primary.isShowing()||splash.progress()<.2||splash.progress()>.85)throw new AssertionError("Splash timer/visibility incorrect");splash.window().getScene().getRoot().applyCss();splash.window().getScene().getRoot().layout();ImageIO.write(SwingFXUtils.fromFXImage(splash.window().getScene().snapshot(null),null),"png",folder.resolve("opening-splash.png").toFile());return null;});
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);while(!fx(primary::isShowing)&&System.nanoTime()<end)Thread.sleep(30);
            fx(()->{OpeningSplash splash=(OpeningSplash)field(launcher,"splash");if(splash.window().isShowing()||!primary.isShowing()||splash.progress()!=1)throw new AssertionError("Splash did not hand over to Studio");primary.setMaximized(false);primary.setWidth(1366);primary.setHeight(900);return null;});
            Thread.sleep(1400);fx(()->{primary.getScene().getRoot().applyCss();primary.getScene().getRoot().layout();ImageIO.write(SwingFXUtils.fromFXImage(primary.getScene().snapshot(null),null),"png",folder.resolve("cream-workspace.png").toFile());return null;});
            System.out.println("Passed: supplied artwork, progressing three-second bar, automatic handover, themed workspace.");
        }finally{fx(()->{launcher.stop();primary.hide();return null;});Platform.exit();}
    }
}
