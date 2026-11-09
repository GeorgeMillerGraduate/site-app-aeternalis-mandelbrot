package com.jenga.aeternalis;
import com.jenga.aeternalis.animation.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.ui.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Field;

/** Explicit end-to-end release smoke; renders real frames and encodes real GIF/MP4. */
public final class AnimationSmoke {
    private static <T>T fx(Callable<T> action)throws Exception{FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(30,TimeUnit.SECONDS);}
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args[0]);Files.createDirectories(folder);var d=StudioDocument.initial();var c=new Calculation(160,1,ScanOrder.RADIAL,true,10,false);
        d=d.replace(d.selected().calculation(c).view(new Viewport(-.7435,.1314,.012)));var job=new ZoomJob(d,Viewport.HOME,320,180,new BigDecimal(".8"),15);ZoomJobIO.save(folder.resolve("zoom.xml"),job);
        try(var s=new ZoomSequence()){s.run(folder.resolve("zoom.xml"),p->{});}SequenceEncoder.gif(folder,folder.resolve("zoom-preview.gif"),15,new AtomicBoolean(),p->{});SequenceEncoder.mp4(folder,folder.resolve("zoom-preview.mp4"),15,"ffmpeg",new AtomicBoolean(),p->{});
        System.out.println("Rendered "+job.frames()+" PNG frames; GIF and H.264 MP4 encoded.");
        Platform.startup(()->{});Platform.setImplicitExit(false);final StudioDocument target=d;ZoomWindow window=fx(()->{var w=new ZoomWindow(new Stage(),target,Viewport.HOME);w.show();return w;});
        try{fx(()->{Field f=ZoomWindow.class.getDeclaredField("window");f.setAccessible(true);Stage stage=(Stage)f.get(window);stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",folder.resolve("zoom-workshop.png").toFile());return null;});}finally{fx(()->{window.close();return null;});Platform.exit();}
        System.out.println("Workshop UI snapshot passed.");
    }
}
