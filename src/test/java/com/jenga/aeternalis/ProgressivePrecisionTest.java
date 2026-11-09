package com.jenga.aeternalis;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.math.precision.*;
import com.jenga.aeternalis.colouring.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
class ProgressivePrecisionTest {
    @Test void widePrecisionUsesSmallTilesAndRetainsAllFinalSamples()throws Exception{
        var request=new RenderRequest(Viewport.parse(".5",".5","1e-18"),48,32,64,2,ScanOrder.RADIAL,false,10,false,FormulaSettings.DEFAULT,PrecisionMode.DOUBLE_DOUBLE,false);
        assertEquals(16,RenderEngine.tileSize(request));CountDownLatch done=new CountDownLatch(6);AtomicInteger samples=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<>();
        try(var engine=new RenderEngine(2)){engine.render(request,t->{assertEquals(1,t.step());samples.addAndGet(t.values().length);done.countDown();},e->{error.set(e);while(done.getCount()>0)done.countDown();});assertTrue(done.await(10,TimeUnit.SECONDS));assertNull(error.get());assertEquals(48*32*4,samples.get());}
    }
    @Test void firstWidePreviewIsCoarseAndArrivesBeforeFinalTiles()throws Exception{
        var request=new RenderRequest(Viewport.parse(".1",".1","1e-18"),128,64,4096,1,ScanOrder.RADIAL,false,10,true,new FormulaSettings(FormulaSettings.Family.JULIA,3,0,0),PrecisionMode.DOUBLE_DOUBLE,false);
        CompletableFuture<RenderEngine.Tile> first=new CompletableFuture<>();
        try(var engine=new RenderEngine(1)){var job=engine.render(request,first::complete,first::completeExceptionally);var tile=first.get(10,TimeUnit.SECONDS);job.cancel();assertEquals(64,tile.step());assertEquals(2,tile.values().length);}
    }
    @Test void unfinishedPreviewDoesNotEraseEarlierComputedPixels(){
        var settings=new ColourSettings(Gradient.of(0xffffff,0xffffff),ColourSettings.Transfer.LINEAR,1,1,0,1,false,false,false,0);
        var old=new RenderEngine.Tile(0,0,4,1,new double[]{1},0,1,0,0,1,4,new long[6]);
        var partial=new RenderEngine.Tile(0,0,4,1,new double[]{Double.NaN,Double.POSITIVE_INFINITY},0,1,0,0,1,2,new long[6]);
        int[] pixels=ColourRenderer.render(List.of(old,partial),4,1,1,settings,()->false);assertEquals(0xff000000,pixels[0]);assertEquals(0xffffffff,pixels[3]);
    }
}
