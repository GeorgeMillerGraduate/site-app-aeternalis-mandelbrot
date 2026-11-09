package com.jenga.aeternalis;
import com.jenga.aeternalis.animation.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.ui.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.math.BigDecimal;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Regression for empty-folder export and numbered create/render/export UI gating. */
public final class ZoomWorkflowSmoke {
    private static <T>T fx(Callable<T> c)throws Exception{FutureTask<T> t=new FutureTask<>(c);Platform.runLater(t);return t.get(20,TimeUnit.SECONDS);}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void call(Object o,String name,Class<?>[] types,Object... args)throws Exception{Method m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(o,args);}
    private static void idle(ZoomWindow w)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(System.nanoTime()<end){if(fx(()->!(boolean)field(w,"busy")))return;Thread.sleep(20);}throw new AssertionError("Workflow did not become idle");}
    private static void gated(ZoomWindow w,boolean disabled)throws Exception{fx(()->{if(((Button)field(w,"gif")).isDisabled()!=disabled||((Button)field(w,"mp4")).isDisabled()!=disabled)throw new AssertionError("Wrong export button state");return null;});}
    public static void main(String[] args)throws Exception{
        Path dir=Path.of(args[0]);Files.createDirectories(dir);Platform.startup(()->{});Platform.setImplicitExit(false);
        var doc=StudioDocument.initial();doc=doc.replace(doc.selected().view(new Viewport(-.7,.1,.875)));var job=new ZoomJob(doc,Viewport.HOME,40,24,new BigDecimal(".5"),25);Path xml=dir.resolve("zoom.xml");ZoomJobIO.save(xml,job);final StudioDocument destination=doc;
        ZoomWindow w=fx(()->{var v=new ZoomWindow(new Stage(),destination,Viewport.HOME);v.show();return v;});
        try{
            gated(w,true);fx(()->{call(w,"checkFolder",new Class[]{Path.class,boolean.class},dir,true);return null;});idle(w);gated(w,true);
            fx(()->{if(!((Label)field(w,"status")).getText().contains("render its PNGs first"))throw new AssertionError("Missing helpful empty-folder guidance");call(w,"install",new Class[]{Path.class,ZoomJob.class},xml,job);return null;});gated(w,true);
            fx(()->{call(w,"render",new Class[]{});return null;});idle(w);gated(w,false);
            fx(()->{Stage stage=(Stage)field(w,"window");stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",dir.resolve("workflow-ready.png").toFile());return null;});
            Files.delete(dir.resolve(ZoomJob.filename(1)));
            fx(()->{call(w,"encode",new Class[]{boolean.class},true);return null;});idle(w);gated(w,true);
            fx(()->{if(!((Label)field(w,"status")).getText().contains("Render/resume it first"))throw new AssertionError("Pre-export gap was not caught");return null;});
            System.out.println("Workflow passed: fresh/empty exports gated; helpful guidance; installed job gated; actual render unlocks exports; deleted frame caught before save dialog.");
        }finally{fx(()->{w.close();return null;});Platform.exit();}
    }
}
