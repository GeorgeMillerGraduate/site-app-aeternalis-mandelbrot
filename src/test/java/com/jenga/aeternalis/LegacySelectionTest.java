package com.jenga.aeternalis;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.colouring.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class LegacySelectionTest {
    @TempDir Path dir;
    @Test void selectionWorksInAllDragDirectionsAndPreservesAspect(){
        var a=SelectionBox.fit(100,100,500,300,1000,600);
        var b=SelectionBox.fit(500,300,100,100,1000,600);assertEquals(a,b);
        assertEquals(1000./600,a.width()/a.height(),1e-12);
        var v=a.zoom(Viewport.HOME,1500,900,1000,600);
        assertEquals(Viewport.HOME.realAt((a.x()+a.width()/2)*1.5,1500),v.real(),1e-15);
        assertEquals(Viewport.HOME.span()*a.width()/1000,v.span(),1e-15);
    }
    @Test void selectionClampsToImageAndRejectsClicks(){
        var box=SelectionBox.fit(-20,-30,1200,900,1000,600);assertEquals(new SelectionBox(0,0,1000,600),box);
        assertThrows(IllegalArgumentException.class,()->SelectionBox.fit(5,5,5,5,1000,600).zoom(Viewport.HOME,1000,600,1000,600));
    }
    @Test void exactCyclesAgreeWithUnacceleratedEvaluator(){
        var fast=new Mandelbrot(4,true);var reference=new Mandelbrot(4,false);
        Random r=new Random(7654);
        for(int i=0;i<12000;i++){
            double x=-2+r.nextDouble()*3,y=-1.5+r.nextDouble()*3;
            var a=fast.evaluate(x,y,1200,()->false);var b=reference.evaluate(x,y,1200,()->false);
            assertEquals(b.escaped(),a.escaped(),x+" + "+y+"i");if(a.escaped())assertEquals(b.smooth(),a.smooth());
        }
        var cycle=fast.evaluate(-2,0,10000,()->false);assertEquals(Iteration.Reason.CYCLE,cycle.reason());assertTrue(cycle.iterations()<100);
    }
    @Test void configurableSquaredBailoutRetainsClassification(){
        for(double bailout:new double[]{4,10,128,1000}){
            var f=new Mandelbrot(bailout,true);assertTrue(f.evaluate(.5,.5,1000,()->false).escaped());assertFalse(f.evaluate(-1,0,1000,()->false).escaped());
        }
        assertThrows(IllegalArgumentException.class,()->new Mandelbrot(2,true));
    }
    @Test void everyLegacyOrderIsDeterministicAndCoversAllEdgeTiles(){
        Set<String> firsts=new HashSet<>();
        for(var order:ScanOrder.values()){
            var tiles=order.tiles(137,79,64);assertEquals(6,tiles.size());Set<String> keys=new HashSet<>();
            for(int[] t:tiles)keys.add(Arrays.toString(t));assertEquals(6,keys.size());firsts.add(Arrays.toString(tiles.getFirst()));
            assertEquals(tiles.stream().map(Arrays::toString).toList(),order.tiles(137,79,64).stream().map(Arrays::toString).toList());
        }
        assertTrue(firsts.size()>1);
    }
    private double[] render(ScanOrder order)throws Exception{
        double[] result=new double[137*79];CountDownLatch done=new CountDownLatch(6);AtomicReference<Throwable> failure=new AtomicReference<>();
        try(var engine=new RenderEngine(2)){
            engine.render(new RenderRequest(Viewport.HOME,137,79,200,1,order,true,10,false),t->{for(int y=0;y<t.height();y++)System.arraycopy(t.values(),y*t.width(),result,(t.y()+y)*137+t.x(),t.width());done.countDown();},failure::set);
            assertTrue(done.await(10,TimeUnit.SECONDS));assertNull(failure.get());return result;
        }
    }
    @Test void scanOrdersDoNotChangeFinalNumericalImage()throws Exception{
        double[] reference=render(ScanOrder.SIMPLE);for(var order:ScanOrder.values())assertArrayEquals(reference,render(order));
    }
    @Test void progressiveSquaresRefineBeforeFullDataOnOneWorker()throws Exception{
        List<RenderEngine.Tile> updates=new CopyOnWriteArrayList<>();CountDownLatch done=new CountDownLatch(6);AtomicReference<Throwable> failure=new AtomicReference<>();
        try(var engine=new RenderEngine(1)){
            engine.render(new RenderRequest(Viewport.HOME,137,79,200,1,ScanOrder.RADIAL,true,10,true),t->{updates.add(t);if(t.step()==1)done.countDown();},failure::set);
            assertTrue(done.await(10,TimeUnit.SECONDS));assertNull(failure.get());assertEquals(List.of(64,32,16,8,4,2),updates.stream().filter(t->t.step()>1).map(RenderEngine.Tile::step).toList());
            for(var tile:updates)assertEquals(((tile.width()+tile.step()-1)/tile.step())*((tile.height()+tile.step()-1)/tile.step()),tile.values().length);
            var settings=new ColourSettings(Palette.OBSERVATORY.gradient(),ColourSettings.Transfer.SQRT,1,128,0,.5,true,false,false,0);
            int[] withPreview=ColourRenderer.render(updates,137,79,1,settings,()->false);
            int[] finals=ColourRenderer.render(updates.stream().filter(t->t.step()==1).toList(),137,79,1,settings,()->false);
            assertArrayEquals(finals,withPreview);
        }
    }
    @Test void catmullRomModePreservesStopsAndSurvivesPaletteSave()throws Exception{
        var g=new Gradient(List.of(new Gradient.Stop(0,0x002255),new Gradient.Stop(.21,0xffac55),new Gradient.Stop(.7,0xf0fffa),new Gradient.Stop(1,0x002255)),Gradient.Mode.CATMULL_ROM);
        for(var stop:g.stops())assertEquals(0xff000000|stop.rgb(),g.argb(stop.position()));
        for(int i=0;i<=1000;i++)for(double c:g.sample(i/1000.))assertTrue(c>=0&&c<=1);
        Path p=dir.resolve("cubic.aetpalette");PaletteIO.save(p,g);assertEquals(g,PaletteIO.load(p));
        assertNotEquals(g.argb(.05),g.withMode(Gradient.Mode.SMOOTHSTEP).argb(.05));
    }
}
