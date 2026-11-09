package com.jenga.aeternalis;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.rendering.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class CoreTest {
    private final Mandelbrot formula=new Mandelbrot();
    @Test void knownEscapePointsAndInteriors(){
        assertFalse(formula.evaluate(0,0,1000,()->false).escaped());
        assertFalse(formula.evaluate(-1,0,1000,()->false).escaped());
        assertFalse(formula.evaluate(-2,0,1000,()->false).escaped());
        assertEquals(2,formula.evaluate(2,0,1000,()->false).iterations());
        assertEquals(3,formula.evaluate(1,0,1000,()->false).iterations());
        assertTrue(formula.evaluate(.5,.5,1000,()->false).escaped());
    }
    @Test void analyticTestsAgreeWithIndependentBaselineOnGrid(){
        for(int y=-40;y<=40;y++)for(int x=-100;x<=50;x++){
            double cr=x/50.,ci=y/50.;
            boolean expected=reference(cr,ci,600);
            assertEquals(expected,formula.evaluate(cr,ci,600,()->false).escaped(),cr+" + "+ci+"i");
        }
    }
    private boolean reference(double cr,double ci,int limit){
        double a=0,b=0;for(int n=0;n<limit;n++){double t=a*a-b*b+cr;b=2*a*b+ci;a=t;if(a*a+b*b>4)return true;}return false;
    }
    @Test void cursorAnchorAndAspectRatioArePreserved(){
        Viewport v=Viewport.HOME;Viewport zoom=v.zoom(.2,123,456,1000,700);
        assertEquals(v.realAt(123,1000),zoom.realAt(123,1000),1e-15);
        assertEquals(v.imaginaryAt(456,1000,700),zoom.imaginaryAt(456,1000,700),1e-15);
        assertEquals(v.realAt(1,1000)-v.realAt(0,1000),v.imaginaryAt(0,1000,700)-v.imaginaryAt(1,1000,700),1e-15);
        assertEquals(v,v.pan(80,-100,1000).pan(-80,100,1000));
    }
    @Test void spacingGuardRejectsCoordinateCollapse(){
        assertTrue(PrecisionGuard.supports(Viewport.HOME,1920,1080));
        assertFalse(PrecisionGuard.supports(new Viewport(-.743, .131, 1e-18),1920,1080));
        assertThrows(IllegalArgumentException.class,()->new Viewport(0,0,Double.NaN));
    }
    @Test void paletteIsCyclicAndOpaque(){
        for(Palette p:Palette.values()){
            assertEquals(p.colour(0,0),p.colour(64,0));
            assertEquals(255,p.colour(2.4,.25)>>>24);
            assertEquals(p.colour(0,.5),p.colour(32,0));
        }
    }
    @Test void tiledRenderCoversOddDimensionsExactlyOnce() throws Exception {
        try(RenderEngine engine=new RenderEngine()){
            int w=137,h=79;int[] coverage=new int[w*h];CountDownLatch finished=new CountDownLatch(6);AtomicReference<Throwable> failure=new AtomicReference<>();
            engine.render(new RenderRequest(Viewport.HOME,w,h,100),tile->{
                synchronized(coverage){for(int y=0;y<tile.height();y++)for(int x=0;x<tile.width();x++)coverage[(tile.y()+y)*w+tile.x()+x]++;}
                finished.countDown();
            },failure::set);
            assertTrue(finished.await(10,TimeUnit.SECONDS));assertNull(failure.get());for(int n:coverage)assertEquals(1,n);
        }
    }
    @Test void cancellationInterruptsLongOrbit(){
        assertThrows(CancellationException.class,()->formula.evaluate(-2,0,100000,()->true));
    }
    @Test void supersededJobIsMarkedCancelled() throws Exception {
        try(RenderEngine engine=new RenderEngine()){
            RenderEngine.Job old=engine.render(new RenderRequest(new Viewport(-.75,.1,.00001),500,500,100000),t->{},e->{});
            CountDownLatch done=new CountDownLatch(1);
            engine.render(new RenderRequest(Viewport.HOME,1,1,100),t->done.countDown(),e->{});
            assertTrue(old.cancelled());assertTrue(done.await(10,TimeUnit.SECONDS));
        }
    }
}
