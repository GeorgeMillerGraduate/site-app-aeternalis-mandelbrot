package com.jenga.aeternalis;
import com.jenga.aeternalis.ui.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.Viewport;
import com.jenga.aeternalis.rendering.Quality;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
public final class EditorialUiSmoke {
    static Studio studio;
    static <T>T fx(Callable<T> c)throws Exception{FutureTask<T> task=new FutureTask<>(c);Platform.runLater(task);return task.get(40,TimeUnit.SECONDS);}
    static Object field(String name)throws Exception{Field f=Studio.class.getDeclaredField(name);f.setAccessible(true);return f.get(studio);}
    static void call(String name)throws Exception{Method m=Studio.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(studio);}
    static void ready()throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);while(System.nanoTime()<end){if(fx(()->!((Boolean)field("rendering"))&&!((Button)field("export")).isDisabled()))return;Thread.sleep(50);}throw new AssertionError("Render did not finish");}
    static void capture(Path out,String name)throws Exception{fx(()->{Stage s=(Stage)field("stage");s.getScene().getRoot().applyCss();s.getScene().getRoot().layout();ImageIO.write(SwingFXUtils.fromFXImage(s.getScene().snapshot(null),null),"png",out.resolve(name+".png").toFile());return null;});}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);Files.createDirectories(out);Platform.startup(()->{});
        try{
            fx(()->{studio=new Studio();studio.start(new Stage());((ComboBox<Quality>)field("quality")).setValue(Quality.DRAFT);return null;});ready();
            fx(()->{Stage s=(Stage)field("stage");((Button)s.getScene().lookup("#atlas-close")).fire();var atlas=(ZoomStudyStrip)field("studyStrip");if(atlas.isManaged()||atlas.isVisible())throw new AssertionError("Atlas close did not reclaim layout");call("focus");call("focus");if(atlas.isManaged())throw new AssertionError("Focus reopened a closed atlas");var enabled=Studio.class.getDeclaredField("atlasEnabled");enabled.setAccessible(true);((javafx.beans.property.BooleanProperty)enabled.get(studio)).set(true);if(!atlas.isManaged())throw new AssertionError("View setting did not restore atlas");return null;});
            for(var study:ZoomStudies.all())fx(()->{var before=(StudioDocument)field("document");ButtonBase card=(ButtonBase)((Stage)field("stage")).getScene().lookup("#wiki-study-"+study.index());if(card==null)throw new AssertionError("Missing card "+study.index());card.fire();call("cancel");Viewport v=(Viewport)field("viewport");if(!v.equals(study.viewport()))throw new AssertionError("Gallery rounded coordinates "+study.index());if(((StudioDocument)field("document")).layers().size()!=before.layers().size())throw new AssertionError("Gallery removed layers");if(!((TextField)field("real")).getText().equals(v.realExact().toString()))throw new AssertionError("Coordinate editor mismatch");return null;});
            fx(()->{((TextField)field("real")).setText("0.500000000000000000000000000000000000001");((TextField)field("imaginary")).setText("0.500000000000000000000000000000000000003");((TextField)field("span")).setText("1e-40");var input=(Spinner<Integer>)field("limit");if(!input.isEditable())throw new AssertionError("Iteration field is not editable");input.getEditor().setText("32768");call("applyFields");if(((StudioDocument)field("document")).selected().calculation().limit()!=32768)throw new AssertionError("Typed iteration ignored");if(((Viewport)field("viewport")).realExact().compareTo(new BigDecimal("0.500000000000000000000000000000000000001"))!=0)throw new AssertionError("Typed decimal rounded");return null;});ready();
            fx(()->{var input=(Spinner<Integer>)field("limit");input.getEditor().setText("100001");call("applyFields");if(!((javafx.scene.layout.HBox)field("notification")).isVisible())throw new AssertionError("Invalid iteration was silently accepted");input.getEditor().setText("1200");((TextField)field("real")).setText("-0.743030");((TextField)field("imaginary")).setText("0.126433");((TextField)field("span")).setText("0.016110");call("applyFields");return null;});ready();
            fx(()->{((TabPane)field("properties")).getSelectionModel().select(1);return null;});capture(out,"editorial-workspace");
            fx(()->{Stage s=(Stage)field("stage");s.setMaximized(false);s.setWidth(1280);s.setHeight(720);return null;});Thread.sleep(400);ready();capture(out,"editorial-1280");
            fx(()->{var sidebar=(javafx.scene.layout.VBox)field("editorialSidebar");if(sidebar.isVisible()||sidebar.isManaged())throw new AssertionError("Small screen sidebar did not collapse");call("focus");if(((ZoomStudyStrip)field("studyStrip")).isManaged())throw new AssertionError("Focus gallery not hidden");call("focus");return null;});
            System.out.println("Passed: 15 real thumbnail clicks; exact coordinates; layer count preservation; typed iterations; invalid input; high precision text; small-screen sidebar; Focus gallery; atlas close/reopen with Focus preservation.");
        }finally{fx(()->{if(studio!=null)studio.stop();return null;});Platform.exit();}
    }
}
