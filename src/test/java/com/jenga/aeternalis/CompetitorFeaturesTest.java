package com.jenga.aeternalis;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.layers.*;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class CompetitorFeaturesTest {
    @TempDir Path dir;
    @Test void juliaConstantZeroHasUnitCircleBoundary(){
        var julia=new FormulaSettings(FormulaSettings.Family.JULIA,2,0,0).evaluator(4,true);
        assertFalse(julia.evaluate(.5,0,200,()->false).escaped());assertFalse(julia.evaluate(1,0,200,()->false).escaped());assertTrue(julia.evaluate(1.1,0,200,()->false).escaped());
    }
    @Test void newtonPowersHaveDistinctRootColours(){
        for(int degree=2;degree<=12;degree++){
            var f=new FormulaSettings(FormulaSettings.Family.NEWTON,degree,0,0).evaluator(4,false);Set<Double> colours=new HashSet<>();
            for(int k=0;k<degree;k++){double angle=2*Math.PI*k/degree;var v=f.evaluate(Math.cos(angle),Math.sin(angle),100,()->false);assertEquals(Iteration.Reason.CONVERGED,v.reason());colours.add(v.smooth());}
            assertEquals(degree,colours.size());
        }
    }
    @Test void newtonConvergesToThreeDifferentRootsAndHandlesSingularity(){
        var f=new FormulaSettings(FormulaSettings.Family.NEWTON,3,0,0).evaluator(4,false);
        Set<Double> values=new HashSet<>();
        for(double[] p:new double[][]{{1,0},{-.5,Math.sqrt(3)/2},{-.5,-Math.sqrt(3)/2}}){var v=f.evaluate(p[0],p[1],100,()->false);assertEquals(Iteration.Reason.CONVERGED,v.reason());assertTrue(v.colourable());assertFalse(v.escaped());values.add(v.smooth());}
        assertEquals(3,values.size());assertFalse(f.evaluate(0,0,100,()->false).colourable());
    }
    @Test void formulaValidationAndCancellation(){
        assertThrows(IllegalArgumentException.class,()->new FormulaSettings(FormulaSettings.Family.JULIA,13,0,0));
        var f=new FormulaSettings(FormulaSettings.Family.JULIA,2,0,0).evaluator(4,false);assertThrows(CancellationException.class,()->f.evaluate(0,0,100,()->true));
    }
    private int compose(int base,int top,double opacity,BlendMode mode){return Compositor.compose(List.of(new Compositor.Input(new int[]{base},1,BlendMode.NORMAL),new Compositor.Input(new int[]{top},opacity,mode)),1,()->false)[0];}
    @Test void normalOpacityUsesLinearLight(){assertEquals(0xffbcbcbc,compose(0xff000000,0xffffffff,.5,BlendMode.NORMAL));}
    @Test void multiplyAndScreenMatchPrimaryColourReferences(){assertEquals(0xff000000,compose(0xffff0000,0xff0000ff,1,BlendMode.MULTIPLY));assertEquals(0xffff00ff,compose(0xffff0000,0xff0000ff,1,BlendMode.SCREEN));}
    @Test void sourceOverPreservesStraightAlpha(){
        assertEquals(0x80ff0000,compose(0,0x80ff0000,1,BlendMode.NORMAL));assertEquals(0xff00ff00,compose(0xff00ff00,0xffff0000,0,BlendMode.NORMAL));
        for(var mode:BlendMode.values()){int c=compose(0xffabcdef,0xff765432,.42,mode);assertEquals(255,c>>>24);}
    }
    @Test void transparentInteriorUsesSubpixelCoverageWithoutDarkFringes(){
        var settings=new ColourSettings(Gradient.of(0xffffff,0xffffff),ColourSettings.Transfer.LINEAR,1,1,0,1,false,false,false,0,true);
        var tile=new RenderEngine.Tile(0,0,1,1,new double[]{Double.NaN,Double.NaN,1,1},1,1,0,0);
        assertEquals(0x80ffffff,ColourRenderer.render(List.of(tile),1,1,2,settings,()->false)[0]);
    }
    @Test void projectRoundTripPreservesIndependentLayers(){
        var base=LayerState.initial();var top=base.duplicate().formula(new FormulaSettings(FormulaSettings.Family.JULIA,5,-.8,.156)).presentation("Julia overlay",true,.4,BlendMode.SCREEN).view(new Viewport(.11,-.22,.03125));
        var project=new StudioDocument(List.of(base,top),top.id());Path path=dir.resolve("art.aeternalis");
        assertDoesNotThrow(()->{ProjectIO.save(path,project);assertEquals(project,ProjectIO.load(path));});
        assertEquals(1,assertDoesNotThrow(()->Files.list(dir).count()));
    }
    @Test void invalidAndFutureProjectsAreRejected()throws Exception{
        Path path=dir.resolve("bad.aeternalis");Files.writeString(path,"{\"schemaVersion\":99,\"document\":null}");assertThrows(java.io.IOException.class,()->ProjectIO.load(path));
        Files.writeString(path,"{not json}");assertThrows(java.io.IOException.class,()->ProjectIO.load(path));
        assertThrows(IllegalArgumentException.class,()->new StudioDocument(List.of(LayerState.initial()),UUID.randomUUID()));
    }
    private DocumentRenderer.Update finish(DocumentRenderer renderer,StudioDocument project)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<DocumentRenderer.Update> output=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
        renderer.render(project,101,67,u->{if(u.complete()){output.set(u);done.countDown();}},e->{failure.set(e);done.countDown();});assertTrue(done.await(20,TimeUnit.SECONDS));assertNull(failure.get());return output.get();
    }
    @Test void layerPaletteAndOpacityChangesReuseOrbitCaches()throws Exception{
        var base=LayerState.initial().calculation(new Calculation(100,1,ScanOrder.SIMPLE,true,10,false));
        var top=base.duplicate().formula(new FormulaSettings(FormulaSettings.Family.JULIA,3,-.8,.156)).presentation("Julia",true,.5,BlendMode.SCREEN);
        var project=new StudioDocument(List.of(base,top),top.id());
        try(var renderer=new DocumentRenderer()){
            var initial=finish(renderer,project);assertEquals(2,renderer.numericalPasses());
            var old=top.colours();var recoloured=new ColourSettings(Palette.EMBER.gradient(),old.transfer(),1,128,.2,.5,true,false,false,0);
            project=project.replace(top.colours(recoloured));var changed=finish(renderer,project);assertEquals(2,renderer.numericalPasses());assertFalse(Arrays.equals(initial.pixels(),changed.pixels()));
            top=project.selected().presentation("Julia",true,.2,BlendMode.MULTIPLY);project=project.replace(top);finish(renderer,project);assertEquals(2,renderer.numericalPasses());
            project=project.replace(top.view(new Viewport(0,0,2)));finish(renderer,project);assertEquals(3,renderer.numericalPasses());
        }
    }
}
