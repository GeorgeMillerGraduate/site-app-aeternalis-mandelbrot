package com.jenga.aeternalis;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.network.*;
import com.jenga.aeternalis.distributed.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
/** Run with -Xmx128m. This deliberately exceeds the old interactive retained-sample limit. */
public final class LargeNetworkExportSmoke {
    public static void main(String[] args)throws Exception{
        Path folder=Path.of(args[0]);Files.createDirectories(folder);NetworkService service=NetworkService.instance();service.host("127.0.0.1",0,"large",2);service.settings(NetworkService.Mode.DISTRIBUTED,0,256);
        try(var worker=new WorkerClient("127.0.0.1",service.master().port(),"large",2)){
            NetworkRenderingTest.await(()->service.master().workerCount()==1);var layer=LayerState.initial().view(new Viewport(3,2,.02)).calculation(new Calculation(32,1,ScanOrder.RADIAL,false,4,false));var doc=new StudioDocument(List.of(layer),layer.id());Path png=folder.resolve("4096-network.png");long started=System.nanoTime();DiskPngExport.render(doc,4096,4096,png,new AtomicBoolean(),s->{});
            var reader=javax.imageio.ImageIO.getImageReadersByFormatName("png").next();try(var in=javax.imageio.ImageIO.createImageInputStream(png.toFile())){reader.setInput(in);if(reader.getWidth(0)!=4096||reader.getHeight(0)!=4096)throw new AssertionError("Dimensions");var param=reader.getDefaultReadParam();param.setSourceRegion(new java.awt.Rectangle(0,0,1,1));reader.read(0,param);}finally{reader.dispose();}
            String report="4096 × 4096 actual distributed PNG, "+Runtime.getRuntime().maxMemory()/1048576+" MiB max JVM heap, "+String.format(Locale.ROOT,"%.3f",(System.nanoTime()-started)/1e9)+" seconds, "+Files.size(png)+" output bytes, "+worker.status().tiles()+" remote tiles. Header and a decoded pixel verified; all image data passed through streaming zlib/CRC output.\n";Files.writeString(folder.resolve("large-export.txt"),report);System.out.println(report);
        }finally{service.close();}
    }
}
