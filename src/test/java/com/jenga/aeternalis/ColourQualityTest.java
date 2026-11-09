package com.jenga.aeternalis;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.fractal.Mandelbrot;
import com.jenga.aeternalis.math.Viewport;
import com.jenga.aeternalis.rendering.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class ColourQualityTest {
    @TempDir Path dir;
    private ColourSettings settings(Gradient g){return new ColourSettings(g,ColourSettings.Transfer.LINEAR,1,1,0,.5,false,false,false,0);}
    @Test void cubicIsNotLinearAndHasFlatEndpointSlopes(){
        assertEquals(.15625,Gradient.cubic(.25),1e-14);
        assertEquals(0,Gradient.cubic(0));assertEquals(1,Gradient.cubic(1));
        double eps=1e-6;assertTrue(Gradient.cubic(eps)/eps<.00001);
        assertTrue((1-Gradient.cubic(1-eps))/eps<.00001);
    }
    @Test void cubicStaysInGamutAndHonoursStops(){
        Gradient g=new Gradient(List.of(new Gradient.Stop(0,0xff0000),new Gradient.Stop(.3,0x00ff00),new Gradient.Stop(1,0x0000ff)));
        assertEquals(0xffff0000,g.argb(0));assertEquals(0xff00ff00,g.argb(.3));assertEquals(0xff0000ff,g.argb(1));
        for(int i=0;i<=1000;i++)for(double c:g.sample(i/1000.))assertTrue(c>=0&&c<=1);
    }
    @Test void subpixelColoursAreAveragedInLinearLight(){
        var tile=new RenderEngine.Tile(0,0,1,1,new double[]{0,0,1,1},1,1,0,0);
        int pixel=ColourRenderer.render(List.of(tile),1,1,2,settings(Gradient.of(0,0xffffff)),()->false)[0];
        int expected=Gradient.srgb(.5);assertEquals(188,expected);assertEquals(0xff000000|expected<<16|expected<<8|expected,pixel);
        // Averaging dwell values and then applying cubic would be a different operation.
        assertNotEquals(128,pixel&255);
    }
    @Test void interiorContributesToBoundaryCoverage(){
        var tile=new RenderEngine.Tile(0,0,1,1,new double[]{Double.NaN,Double.NaN,1,1},1,1,0,0);
        int pixel=ColourRenderer.render(List.of(tile),1,1,2,settings(Gradient.of(0xffffff,0xffffff)),()->false)[0];
        assertEquals(188,pixel&255);
    }
    @Test void transferFunctionsReverseAndWrap(){
        Gradient g=Gradient.of(0,0xffffff);
        for(var transfer:ColourSettings.Transfer.values()){
            var s=new ColourSettings(g,transfer,1,64,0,.5,false,false,false,0);
            assertTrue(Double.isFinite(s.position(1)));assertTrue(Double.isFinite(s.position(100000)));
        }
        var sqrt=new ColourSettings(g,ColourSettings.Transfer.SQRT,1,64,0,.5,false,false,false,0);
        assertEquals(.5,sqrt.position(16),1e-15);
        var reverse=new ColourSettings(g,ColourSettings.Transfer.LINEAR,1,64,0,.5,false,true,false,0);
        assertEquals(.75,reverse.position(16),1e-15);
    }
    @Test void paletteRoundTripPreservesPositionsAndColours()throws Exception{
        Gradient g=new Gradient(List.of(new Gradient.Stop(0,0x123456),new Gradient.Stop(.238,0xfedcba),new Gradient.Stop(1,0x8f00ff)));
        Path f=dir.resolve("mine.aetpalette");PaletteIO.save(f,g);assertEquals(g,PaletteIO.load(f));
        Files.writeString(f,"format=aeternalis-palette-1\ncount=2\nstop.0=0,GGGGGG\nstop.1=1,FFFFFF\n");assertThrows(java.io.IOException.class,()->PaletteIO.load(f));
    }
    @Test void sampleBudgetRejectsOversizedQuality()throws Exception{
        assertDoesNotThrow(()->new RenderRequest(Viewport.HOME,1920,1080,600,2));
        // Large task geometry is valid for disk-backed export; the interactive document enforces its own budget.
        assertDoesNotThrow(()->new RenderRequest(Viewport.HOME,16384,16384,600,3));
        var layer=com.jenga.aeternalis.app.LayerState.initial().calculation(new com.jenga.aeternalis.app.Calculation(600,3,ScanOrder.RADIAL,false,4,false));
        var document=new com.jenga.aeternalis.app.StudioDocument(java.util.List.of(layer),layer.id());var error=new java.util.concurrent.CompletableFuture<Throwable>();
        try(var renderer=new DocumentRenderer()){renderer.render(document,3840,2160,u->{},error::complete);assertInstanceOf(IllegalArgumentException.class,error.get(5,TimeUnit.SECONDS));}
    }
    @Test void rendererComputesDistinctSubpixelsAtCorrectLocations()throws Exception{
        int w=5,h=3,grid=3;var view=new Viewport(-.7,.25,.1);AtomicReference<RenderEngine.Tile> output=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();CountDownLatch done=new CountDownLatch(1);
        try(var engine=new RenderEngine()){
            engine.render(new RenderRequest(view,w,h,100,grid),t->{output.set(t);done.countDown();},e->{failure.set(e);done.countDown();});
            assertTrue(done.await(10,TimeUnit.SECONDS));assertNull(failure.get());assertEquals(w*h*9,output.get().values().length);
            var formula=new Mandelbrot();for(int y=0;y<h;y++)for(int x=0;x<w;x++)for(int sy=0;sy<grid;sy++)for(int sx=0;sx<grid;sx++){
                double px=x+(sx+.5)/grid,py=y+(sy+.5)/grid;
                var expected=formula.evaluate(view.realAt(px,w),view.imaginaryAt(py,w,h),100,()->false);
                double actual=output.get().values()[(y*w+x)*9+sy*3+sx];if(expected.escaped())assertEquals(expected.smooth(),actual);else assertTrue(Double.isNaN(actual));
            }
        }
    }
    @Test void colourPassHonoursCancellation(){
        var tile=new RenderEngine.Tile(0,0,1,1,new double[]{1},1,1,0,0);
        assertThrows(CancellationException.class,()->ColourRenderer.render(List.of(tile),1,1,1,settings(Gradient.of(0,0xffffff)),()->true));
    }
    @Test void histogramHandlesUniformAndInteriorOnlyTiles(){
        Gradient g=Gradient.of(0,0xffffff);var s=new ColourSettings(g,ColourSettings.Transfer.LINEAR,1,64,0,.5,false,false,true,0);
        var tile=new RenderEngine.Tile(0,0,2,1,new double[]{100,Double.NaN},1,1,0,0);
        int[] a=ColourRenderer.render(List.of(tile),2,1,1,s,()->false);assertNotEquals(a[0],a[1]);assertEquals(0xff000000,a[1]);
    }
}
