package com.jenga.aeternalis;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.math.precision.*;
import com.jenga.aeternalis.rendering.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
class RuntimeCollisionTest {
    private RenderRequest request(String span,PrecisionMode mode){return new RenderRequest(Viewport.parse("1.2345678901234567890123456789",".23456789123456789123456789",span),12,8,100,3,ScanOrder.RADIAL,false,4,false,FormulaSettings.DEFAULT,mode,false);}
    @Test void runtimeRestartsOnlyAtObservedCollisionsAndMatchesReference()throws Exception{
        String[] spans={"1e-13","1e-18","1e-40","1e-90","1e-400"};PrecisionMode[] expected={PrecisionMode.DOUBLE,PrecisionMode.DOUBLE_DOUBLE,PrecisionMode.QUAD_DOUBLE,PrecisionMode.ARBITRARY,PrecisionMode.ARBITRARY};
        for(int i=0;i<spans.length;i++){
            var r=request(spans[i],PrecisionMode.AUTOMATIC);CompletableFuture<RenderEngine.Tile> result=new CompletableFuture<>();AtomicInteger resets=new AtomicInteger();
            try(var engine=new RenderEngine(4)){
                engine.render(r,t->{if(t.step()<0)resets.incrementAndGet();else if(t.step()==1&&t.completed()==t.total())result.complete(t);},result::completeExceptionally);
                var tile=result.get(10,TimeUnit.SECONDS);assertEquals(expected[i],engine.numericalContext().plan.mode(),spans[i]);assertEquals(i==4?3:i,resets.get(),spans[i]);assertEquals(12*8*9,tile.values().length);
                var oracle=new PixelEvaluator(new PixelEvaluator.Context(request(spans[i],PrecisionMode.ARBITRARY)),()->false);
                for(int y=0;y<8;y++)for(int x=0;x<12;x++)for(int sy=0;sy<3;sy++)for(int sx=0;sx<3;sx++)assertEquals(oracle.sample(x,y,sx,sy,3).smooth(),tile.values()[(y*12+x)*9+sy*3+sx],1e-7);
            }
        }
    }
    @Test void repeatedAxesAcrossRowsDoNotCauseFalseEscalation()throws Exception{
        var r=request("1e-13",PrecisionMode.AUTOMATIC);CompletableFuture<RenderEngine.Tile> done=new CompletableFuture<>();try(var engine=new RenderEngine(3)){engine.render(r,t->{assertTrue(t.step()>0);if(t.completed()==t.total())done.complete(t);},done::completeExceptionally);done.get(10,TimeUnit.SECONDS);assertEquals(PrecisionMode.DOUBLE,engine.numericalContext().plan.mode());}
    }
    @Test void documentRestartsDiscardOldTilesAndCompleteAtNewTileSize()throws Exception{
        var layer=LayerState.initial().view(Viewport.parse("1.234567890123456789",".23456789123456789","1e-40")).calculation(new Calculation(100,1,ScanOrder.RADIAL,false,4,false,PrecisionMode.AUTOMATIC,false));var doc=new StudioDocument(List.of(layer),layer.id());CompletableFuture<DocumentRenderer.Update> done=new CompletableFuture<>();
        try(var renderer=new DocumentRenderer()){renderer.render(doc,48,32,u->{if(u.complete())done.complete(u);},done::completeExceptionally);var u=done.get(10,TimeUnit.SECONDS);assertEquals(48*32,u.pixels().length);assertEquals(48*32,Arrays.stream(u.classifications()).sum());assertEquals(2,renderer.escalationEvents());assertEquals(PrecisionMode.QUAD_DOUBLE,renderer.currentContext().plan.mode());}
    }
}
