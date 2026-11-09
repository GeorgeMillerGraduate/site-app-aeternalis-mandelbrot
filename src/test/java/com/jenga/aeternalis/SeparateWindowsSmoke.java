package com.jenga.aeternalis;
import com.jenga.aeternalis.ui.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.*;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
public final class SeparateWindowsSmoke {
    static <T>T fx(Callable<T> c)throws Exception{FutureTask<T> f=new FutureTask<>(c);Platform.runLater(f);return f.get(20,TimeUnit.SECONDS);}
    static Stage stage(ZoomWindow w)throws Exception{Field f=ZoomWindow.class.getDeclaredField("window");f.setAccessible(true);return (Stage)f.get(w);}
    static void bounds(Stage c,Stage p){if(c.getIcons().isEmpty())throw new AssertionError("Missing application icon");var s=Screen.getPrimary().getVisualBounds();if(c.getWidth()>s.getWidth()*.9+1||c.getHeight()>s.getHeight()*.9+1||c.getX()<p.getX()-1||c.getY()<p.getY()-1||c.getX()+c.getWidth()>p.getX()+p.getWidth()+1||c.getY()+c.getHeight()>p.getY()+p.getHeight()+1)throw new AssertionError("Child escaped owner/screen: "+c.getWidth()+"x"+c.getHeight());}
    static boolean button(Stage s,String text){return s.getScene().getRoot().lookupAll(".button").stream().anyMatch(n->n instanceof Button b&&b.getText().contains(text));}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);Files.createDirectories(out);Platform.startup(()->{});Platform.setImplicitExit(false);
        Stage owner=fx(()->{Stage s=new Stage();s.setScene(new Scene(new StackPane(),1000,740));s.setX(80);s.setY(40);s.show();return s;});
        ZoomWindow render=fx(()->{var w=new ZoomWindow(owner,StudioDocument.initial(),Viewport.HOME);w.show();return w;}),convert=fx(()->{var w=ZoomWindow.converter(owner);w.show();return w;});
        try{
            fx(()->{Stage r=stage(render),c=stage(convert);r.getScene().getRoot().applyCss();c.getScene().getRoot().applyCss();r.getScene().getRoot().layout();c.getScene().getRoot().layout();bounds(r,owner);bounds(c,owner);if(button(r,"GIF")||button(r,"MP4")||!button(r,"Create zoom"))throw new AssertionError("Frame prompt still mixes encoding");if(button(c,"Create zoom")||button(c,"Render / resume")||!button(c,"Choose PNG")||!button(c,"MP4"))throw new AssertionError("Converter mixes rendering");ImageIO.write(SwingFXUtils.fromFXImage(r.getScene().snapshot(null),null),"png",out.resolve("render-frames.png").toFile());ImageIO.write(SwingFXUtils.fromFXImage(c.getScene().snapshot(null),null),"png",out.resolve("convert-folder.png").toFile());owner.setWidth(650);owner.setHeight(450);bounds(r,owner);bounds(c,owner);r.setWidth(3000);r.setHeight(3000);r.setX(-999);r.setY(-999);bounds(r,owner);return null;});
            fx(()->{Alert a=new Alert(Alert.AlertType.INFORMATION,"A large settings body\n".repeat(60));a.initOwner(owner);Theme.apply(a);a.show();bounds((Stage)a.getDialogPane().getScene().getWindow(),owner);a.close();return null;});
            System.out.println("Passed separate prompts; owner containment; 90% screen cap; oversized resize/drag; parent shrink; themed dialog containment.");
        }finally{fx(()->{render.close();convert.close();owner.close();return null;});Platform.exit();}
    }
}
