package com.jenga.aeternalis;
import com.jenga.aeternalis.ui.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.*;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.*;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Explicit UI layout/interaction matrix; not an OS-DPI certification. */
public final class UiVisualSmoke {
    private static Studio studio;
    private static <T>T fx(Callable<T> code)throws Exception{FutureTask<T> t=new FutureTask<>(code);Platform.runLater(t);return t.get(40,TimeUnit.SECONDS);}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void call(String name)throws Exception{Method m=Studio.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(studio);}
    private static void ready()throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(70);while(System.nanoTime()<end){if(fx(()->!((Boolean)field(studio,"rendering"))&&((Button)field(studio,"export")).isDisabled()==false&&((Animation)field(studio,"resizeDelay")).getStatus()==Animation.Status.STOPPED))return;Thread.sleep(40);}throw new AssertionError("UI did not finish rendering");}
    private static void save(Path path)throws Exception{fx(()->{Stage stage=(Stage)field(studio,"stage");ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",path.toFile());return null;});}
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectories(output);Platform.startup(()->{});
        try{
            fx(()->{studio=new Studio();studio.start(new Stage());Stage stage=(Stage)field(studio,"stage");stage.setMaximized(false);((ComboBox<Quality>)field(studio,"quality")).setValue(Quality.DRAFT);return null;});ready();
            for(int[] size:new int[][]{{1280,720},{1366,768},{1600,900},{1920,1080},{2560,1440}}){
                fx(()->{Stage stage=(Stage)field(studio,"stage");stage.setWidth(size[0]);stage.setHeight(size[1]);return null;});Thread.sleep(350);ready();save(output.resolve("workspace-"+size[0]+"x"+size[1]+".png"));
            }
            fx(()->{Stage stage=(Stage)field(studio,"stage");stage.setWidth(1366);stage.setHeight(768);((TabPane)field(studio,"properties")).getSelectionModel().select(4);return null;});Thread.sleep(400);ready();save(output.resolve("performance-panel.png"));
            fx(()->{NumericField span=(NumericField)field(studio,"span");span.setText("-");call("applyFields");if(!((javafx.scene.layout.HBox)field(studio,"notification")).isVisible())throw new AssertionError("Inline validation missing");span.setText("3.5");call("clearNotification");call("applyFields");return null;});Thread.sleep(350);ready();
            fx(()->{call("focus");return null;});Thread.sleep(350);ready();save(output.resolve("focus-mode.png"));fx(()->{call("focus");return null;});Thread.sleep(350);ready();
            fx(()->{((Stage)field(studio,"stage")).setFullScreen(true);return null;});Thread.sleep(350);ready();save(output.resolve("fullscreen-mode.png"));fx(()->{((Stage)field(studio,"stage")).setFullScreen(false);return null;});Thread.sleep(350);ready();
            // Test an exact deep viewport in the actual controls without creating a large, expensive arbitrary-precision frame.
            fx(()->{Stage stage=(Stage)field(studio,"stage");stage.setWidth(1280);stage.setHeight(720);return null;});Thread.sleep(350);ready();
            fx(()->{
                NumericField real=(NumericField)field(studio,"real"),imaginary=(NumericField)field(studio,"imaginary"),span=(NumericField)field(studio,"span");
                real.setText("0.5");imaginary.setText("0.5");span.setText("1e-18");((Spinner<Integer>)field(studio,"limit")).getValueFactory().setValue(32);call("applyFields");return null;
            });ready();
            fx(()->{Viewport view=(Viewport)field(studio,"viewport");if(view.spanExact().compareTo(new java.math.BigDecimal("1e-18"))!=0)throw new AssertionError("Deep coordinate was rounded");return null;});save(output.resolve("precision-escalation.png"));
            Platform.runLater(()->{try{call("showAbout");}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(250);
            fx(()->{Stage about=null;for(Window w:Window.getWindows())if(w instanceof Stage s&&s.getTitle()!=null&&s.getTitle().startsWith("About Aeternalis"))about=s;if(about==null)throw new AssertionError("About window missing");ImageIO.write(SwingFXUtils.fromFXImage(about.getScene().snapshot(null),null),"png",output.resolve("about-window.png").toFile());about.hide();return null;});
            System.out.println("Visual matrix passed: 5 window sizes; Performance; inline validation; Focus; exact deep-coordinate input; About artwork.");
        }finally{fx(()->{if(studio!=null)studio.stop();return null;});Platform.exit();}
    }
}
