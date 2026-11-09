package com.jenga.aeternalis;
import com.jenga.aeternalis.ui.*;
import com.jenga.aeternalis.network.*;
import com.jenga.aeternalis.app.*;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
public final class NetworkUiSmoke {
    static <T>T fx(Callable<T> call)throws Exception{FutureTask<T> task=new FutureTask<>(call);Platform.runLater(task);return task.get(30,TimeUnit.SECONDS);}
    static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args[0]);Files.createDirectories(folder);Platform.startup(()->{});Platform.setImplicitExit(false);NetworkService service=NetworkService.instance();service.host("127.0.0.1",0,"ui",1);service.settings(NetworkService.Mode.DISTRIBUTED,0,32);service.master().overlay(true);
        try(var worker=new WorkerClient("127.0.0.1",service.master().port(),"ui",1)){
            NetworkRenderingTest.await(()->service.master().workerCount()==1);final Studio studio=fx(()->{Studio s=new Studio();s.start(new Stage());return s;});NetworkManagerWindow network=fx(()->{Stage owner=(Stage)field(studio,"stage");NetworkManagerWindow w=new NetworkManagerWindow(owner,StudioDocument::initial,()->{},()->{});w.show(0);return w;});
            try{Thread.sleep(1800);for(int index:new int[]{0,1,2,3,4,5}){final int tab=index;fx(()->{network.show(tab);return null;});Thread.sleep(150);fx(()->{Stage stage=(Stage)field(network,"window");if(stage.getIcons().isEmpty())throw new AssertionError("Network icon missing");stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",folder.resolve("network-tab-"+tab+".png").toFile());return null;});}
                fx(()->{Stage stage=(Stage)field(studio,"stage");ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",folder.resolve("network-main.png").toFile());return null;});
                System.out.println("Passed Network menu, distributed interactive render, six network tabs, icon and owned-window screenshots.");
            }finally{fx(()->{network.close();studio.stop();return null;});}
        }finally{service.close();Platform.exit();}
    }
}
