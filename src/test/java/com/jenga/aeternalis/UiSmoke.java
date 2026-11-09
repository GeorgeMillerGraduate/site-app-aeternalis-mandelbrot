package com.jenga.aeternalis;

import com.jenga.aeternalis.ui.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.layers.*;
import com.jenga.aeternalis.persistence.*;
import com.jenga.aeternalis.rendering.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.input.*;
import javafx.event.EventType;
import javafx.scene.layout.StackPane;
import com.jenga.aeternalis.math.Viewport;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Optional headless integration check, launched explicitly rather than by the unit-test suite. */
public final class UiSmoke {
    private static Studio studio;
    private static <T> T fx(Callable<T> task)throws Exception{FutureTask<T> future=new FutureTask<>(task);Platform.runLater(future);return future.get(30,TimeUnit.SECONDS);}
    private static Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static void call(Object owner,String name)throws Exception{Method m=owner.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(owner);}
    private static void ready()throws Exception{
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);
        while(System.nanoTime()<until){if(fx(()->((Label)field(studio,"state")).getText().equals("Render complete")))return;Thread.sleep(40);}
        throw new AssertionError("Render did not finish");
    }
    private static long hash()throws Exception{return fx(()->{WritableImage b=(WritableImage)field(studio,"bitmap");long h=0;for(int y=0;y<b.getHeight();y+=3)for(int x=0;x<b.getWidth();x+=3)h=h*31+b.getPixelReader().getArgb(x,y);return h;});}
    private static void mouse(StackPane canvas,EventType<MouseEvent> type,double x,double y,boolean shift,boolean down){
        canvas.fireEvent(new MouseEvent(type,x,y,x,y,MouseButton.PRIMARY,1,shift,false,false,false,down,false,false,false,false,false,new PickResult(canvas,x,y)));
    }
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);Files.createDirectories(out);Platform.startup(()->{});
        try{
            fx(()->{studio=new Studio();studio.start(new Stage());return null;});ready();
            fx(()->{((TextField)field(studio,"real")).setText("-0.705401618929016");((TextField)field(studio,"imaginary")).setText("0.24844333748443317");((TextField)field(studio,"span")).setText("0.00341796875");((Spinner<Integer>)field(studio,"limit")).getValueFactory().setValue(1300);call(studio,"applyFields");return null;});ready();
            long before=hash(),generation=fx(()->(Long)field(studio,"generation"));
            fx(()->{
                ColourPanel panel=(ColourPanel)field(studio,"colours");
                for(var node:panel.getChildren())if(node instanceof javafx.scene.layout.HBox row)for(var child:row.getChildren())if(child instanceof Button button&&button.getText().equals("Add colour"))button.fire();
                ColorPicker picker=(ColorPicker)field(panel,"colour");picker.hide();picker.setValue(Color.web("#fa9643"));picker.fireEvent(new ActionEvent());
                ((TabPane)field(studio,"properties")).getSelectionModel().select(1);return null;
            });ready();
            if(fx(()->(Long)field(studio,"generation"))!=generation)throw new AssertionError("Palette edit recalculated orbits");
            // ready() can observe the old status while a palette pass is queued; wait for export readiness too.
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
            while(fx(()->((Button)field(studio,"export")).isDisabled())&&System.nanoTime()<until)Thread.sleep(40);
            if(hash()==before)throw new AssertionError("Custom colour did not change image");
            fx(()->{Stage stage=(Stage)field(studio,"stage");ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",out.resolve("custom-palette-preview.png").toFile());return null;});
            fx(()->{((ComboBox<Quality>)field(studio,"quality")).setValue(Quality.FINE);return null;});ready();
            if(fx(()->(Integer)field(studio,"grid"))!=3)throw new AssertionError("Fine grid not selected");
            fx(()->{Stage stage=(Stage)field(studio,"stage");int w=(Integer)field(studio,"width");double logical=(Double)field(studio,"displayWidth");if(w!=(int)Math.ceil(logical*stage.getOutputScaleX()))throw new AssertionError("Output scale mismatch");ImageIO.write(SwingFXUtils.fromFXImage((WritableImage)field(studio,"bitmap"),null),"png",out.resolve("clean-fractal.png").toFile());return null;});
            double oldSpan=fx(()->((Viewport)field(studio,"viewport")).span());
            fx(()->{
                StackPane canvas=(StackPane)field(studio,"canvas");double w=(Double)field(studio,"displayWidth"),h=(Double)field(studio,"displayHeight");
                mouse(canvas,MouseEvent.MOUSE_PRESSED,8+w*.25,8+h*.3,false,true);
                mouse(canvas,MouseEvent.MOUSE_DRAGGED,8+w*.7,8+h*.65,false,true);
                if(!((ViewportInteraction)field(studio,"interaction")).selectionVisible())throw new AssertionError("Selection overlay missing");
                mouse(canvas,MouseEvent.MOUSE_RELEASED,8+w*.7,8+h*.65,false,false);return null;
            });ready();
            double selectedSpan=fx(()->((Viewport)field(studio,"viewport")).span());if(!(selectedSpan<oldSpan*.6&&selectedSpan>oldSpan*.3))throw new AssertionError("Rectangle zoom span wrong");
            fx(()->{
                StackPane canvas=(StackPane)field(studio,"canvas");ViewportInteraction interaction=(ViewportInteraction)field(studio,"interaction");
                mouse(canvas,MouseEvent.MOUSE_PRESSED,100,100,false,true);mouse(canvas,MouseEvent.MOUSE_DRAGGED,200,200,false,true);
                if(!interaction.cancelSelection())throw new AssertionError("Escape cancellation missing");mouse(canvas,MouseEvent.MOUSE_RELEASED,200,200,false,false);return null;
            });
            if(fx(()->((Viewport)field(studio,"viewport")).span())!=selectedSpan)throw new AssertionError("Cancelled selection changed view");
            fx(()->{call(studio,"detachPalette");return null;});
            fx(()->{
                Stage floating=(Stage)field(studio,"paletteWindow");if(floating==null||!floating.isShowing())throw new AssertionError("Floating palette missing");
                ImageIO.write(SwingFXUtils.fromFXImage(floating.getScene().snapshot(null),null),"png",out.resolve("floating-gradient.png").toFile());call(studio,"dockPalette");
                if(field(studio,"paletteWindow")!=null)throw new AssertionError("Palette did not dock");return null;
            });
            fx(()->{Method m=Studio.class.getDeclaredMethod("addLayer",boolean.class);m.setAccessible(true);m.invoke(studio,true);return null;});ready();
            fx(()->{((ComboBox<FormulaSettings.Family>)field(studio,"formula")).setValue(FormulaSettings.Family.JULIA);((ComboBox<BlendMode>)field(studio,"blend")).setValue(BlendMode.SCREEN);((Slider)field(studio,"layerOpacity")).setValue(.62);return null;});ready();
            fx(()->{
                StudioDocument d=(StudioDocument)field(studio,"document");if(d.layers().size()!=2||d.selected().formula().family()!=FormulaSettings.Family.JULIA)throw new AssertionError("Independent Julia layer missing");
                if(d.layers().getFirst().viewport().equals(d.selected().viewport()))throw new AssertionError("Layer viewports are coupled");
                ((TabPane)field(studio,"properties")).getSelectionModel().select(0);((TabPane)field(studio,"browser")).getSelectionModel().select(2);
                ImageIO.write(SwingFXUtils.fromFXImage(((Stage)field(studio,"stage")).getScene().snapshot(null),null),"png",out.resolve("layers-workspace.png").toFile());return null;
            });
            Path projectFile=out.resolve("Roundtrip.aeternalis");
            fx(()->{ProjectIO.save(projectFile,(StudioDocument)field(studio,"document"));return null;});long savedHash=hash();
            fx(()->{Method install=Studio.class.getDeclaredMethod("installDocument",StudioDocument.class,Path.class);install.setAccessible(true);install.invoke(studio,ProjectIO.load(projectFile),projectFile);return null;});ready();
            if(savedHash!=hash())throw new AssertionError("Project reload changed the composition");
            fx(()->{((ComboBox<FormulaSettings.Family>)field(studio,"formula")).setValue(FormulaSettings.Family.NEWTON);((Spinner<Integer>)field(studio,"exponent")).getValueFactory().setValue(8);return null;});ready();
            fx(()->{
                ImageIO.write(SwingFXUtils.fromFXImage(((Stage)field(studio,"stage")).getScene().snapshot(null),null),"png",out.resolve("newton-workspace.png").toFile());return null;
            });
            System.out.println("UI integration passed: custom palette; Fine sampling; physical size; rectangle zoom/cancel; floating editor/docking; independent Julia layer; blend/opacity controls; project save/reload with identical image; Newton family.");
        }finally{fx(()->{if(studio!=null)studio.stop();return null;});Platform.exit();}
    }
}
