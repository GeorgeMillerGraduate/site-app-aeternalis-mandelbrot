package com.jenga.aeternalis;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.math.precision.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.math.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class PrecisionEngineTest {
    private static final MathContext ORACLE=new MathContext(120,RoundingMode.HALF_EVEN);
    @TempDir Path directory;
    @Test void twoComponentArithmeticRetainsLostDoubleIncrement(){
        var a=DoubleDouble.of(new BigDecimal("10000000000000000"));var b=DoubleDouble.of(BigDecimal.ONE);assertEquals(0,a.add(b).decimal().compareTo(new BigDecimal("10000000000000001")));
    }
    @Test void doubleDoubleOperationsMatchDecimalOracle(){
        Random random=new Random(793);
        for(int n=0;n<100;n++){
            BigDecimal a=new BigDecimal("0."+Math.abs(random.nextLong())),b=new BigDecimal("1."+Math.abs(random.nextLong()));var x=DoubleDouble.of(a);var y=DoubleDouble.of(b);
            assertTrue(x.multiply(y).decimal().subtract(a.multiply(b,ORACLE)).abs().compareTo(new BigDecimal("1e-30"))<0);
            assertTrue(x.divide(y).decimal().subtract(a.divide(b,ORACLE)).abs().compareTo(new BigDecimal("1e-30"))<0);
        }
    }
    @Test void quadExpansionRetainsFourComponentInformation(){
        BigDecimal a=new BigDecimal("1.000000000000000000000000000000000000000000000000001"),b=new BigDecimal("1.2345678901234567890123456789012345678901234567890123456789");var x=QuadDouble.of(a);var y=QuadDouble.of(b);
        assertTrue(x.decimal().subtract(a).abs().compareTo(new BigDecimal("1e-62"))<0);
        assertTrue(x.multiply(y).decimal().subtract(a.multiply(b,ORACLE)).abs().compareTo(new BigDecimal("1e-60"))<0);
        assertTrue(x.divide(y).decimal().subtract(a.divide(b,ORACLE)).abs().compareTo(new BigDecimal("1e-60"))<0);
    }
    @Test void automaticPrecisionStartsDoubleWithoutMargins(){
        for(String span:new String[]{"1e-5","1e-15","1e-40","1e-90","1e-1000"})assertEquals(PrecisionMode.DOUBLE,PrecisionController.analyse(Viewport.parse("-.75",".1",span),100,80,3,100000,PrecisionMode.AUTOMATIC).mode());
    }
    @Test void explicitArbitraryCoordinatesRemainDistinct(){
        var view=Viewport.parse("-.75",".1","1e-1000");var plan=PrecisionController.analyse(view,100,80,3,1000,PrecisionMode.ARBITRARY);MathContext mc=new MathContext(plan.decimalDigits());
        assertNotEquals(view.realAtExact(BigDecimal.ZERO,100,mc),view.realAtExact(BigDecimal.ONE,100,mc));assertNotEquals(view.imaginaryAtExact(BigDecimal.ZERO,100,80,mc),view.imaginaryAtExact(BigDecimal.ONE,100,80,mc));
    }
    @Test void subnormalSpacingStartsDoubleAndIsCheckedAtRuntime(){assertEquals(PrecisionMode.DOUBLE,PrecisionController.analyse(Viewport.parse("0","0","1e-320"),1000,800,3,1000,PrecisionMode.AUTOMATIC).mode());}
    @Test void zeroRoundedSpanAndNavigationPreserveExactCoordinates(){
        var view=Viewport.parse("-.75",".1","1e-400");assertEquals(0,view.span());var next=view.zoom(.5,50,40,100,80);assertEquals(view.realExact(),next.realExact());assertEquals(new BigDecimal("5e-401"),next.spanExact());
        assertEquals(PrecisionMode.DOUBLE,PrecisionController.analyse(next,100,80,1,1000,PrecisionMode.DOUBLE).mode());
    }
    @Test void preciseKernelsMatchArbitraryReferenceForAllFamilies(){
        for(var family:FormulaSettings.Family.values()){
            var settings=new FormulaSettings(family,5,-.8,.156);
            var reference=new PreciseFormula<>(Arithmetic.forPlan(new PrecisionPlan(PrecisionMode.ARBITRARY,100,300,false,"oracle")),settings,10);
            for(var mode:new PrecisionMode[]{PrecisionMode.DOUBLE_DOUBLE,PrecisionMode.QUAD_DOUBLE}){
                var evaluator=new PreciseFormula<>(Arithmetic.forPlan(new PrecisionPlan(mode,mode==PrecisionMode.DOUBLE_DOUBLE?34:66,100,false,"test")),settings,10);
                var expected=reference.evaluate(new BigDecimal(".6"),new BigDecimal(".7"),200,()->false);var actual=evaluator.evaluate(new BigDecimal(".6"),new BigDecimal(".7"),200,()->false);
                assertEquals(expected.reason(),actual.reason());if(expected.colourable())assertEquals(expected.smooth(),actual.smooth(),1e-8);
            }
        }
    }
    @Test void perturbationMatchesAccurateFallbackAndDetectsInvalidReferenceRegions(){
        var view=Viewport.parse("2.5","0","4");var request=new RenderRequest(view,8,4,100,1,ScanOrder.RADIAL,false,4,false,FormulaSettings.DEFAULT,PrecisionMode.DOUBLE_DOUBLE,true);var context=new PixelEvaluator.Context(request);var accelerated=new PixelEvaluator(context,()->false);
        var directRequest=new RenderRequest(view,8,4,100,1,ScanOrder.RADIAL,false,4,false,FormulaSettings.DEFAULT,PrecisionMode.ARBITRARY,false);var direct=new PixelEvaluator(new PixelEvaluator.Context(directRequest),()->false);
        for(int y=0;y<4;y++)for(int x=0;x<8;x++){var a=accelerated.sample(x,y,0,0,1);var b=direct.sample(x,y,0,0,1);assertEquals(b.colourable(),a.colourable());if(b.colourable())assertEquals(b.smooth(),a.smooth(),1e-7);}
        assertTrue(context.fallbackPixels.sum()>0);
    }
    @Test void deepPerturbationRetainsClassificationAndSmoothValues(){
        var view=Viewport.parse("-.75",".1","1e-30");var req=new RenderRequest(view,9,7,200,1,ScanOrder.RADIAL,false,4,false,FormulaSettings.DEFAULT,PrecisionMode.DOUBLE_DOUBLE,true);var context=new PixelEvaluator.Context(req);var fast=new PixelEvaluator(context,()->false);
        var accurate=new PixelEvaluator(new PixelEvaluator.Context(new RenderRequest(view,9,7,200,1,ScanOrder.RADIAL,false,4,false,FormulaSettings.DEFAULT,PrecisionMode.ARBITRARY,false)),()->false);
        for(int y=0;y<7;y++)for(int x=0;x<9;x++){var a=fast.sample(x,y,0,0,1);var b=accurate.sample(x,y,0,0,1);assertEquals(b.colourable(),a.colourable());if(b.colourable())assertEquals(b.smooth(),a.smooth(),1e-7);}
        assertTrue(context.perturbationPixels.sum()>0);
    }
    @Test void everyIndependentSubpixelIsCalculatedAfterEscalation()throws Exception{
        var view=Viewport.parse(".5",".5","1e-80");CountDownLatch done=new CountDownLatch(1);AtomicReference<RenderEngine.Tile> result=new AtomicReference<>();AtomicReference<Throwable> error=new AtomicReference<>();
        try(var engine=new RenderEngine(2)){
            engine.render(new RenderRequest(view,7,5,100,3,ScanOrder.RADIAL,false,4,false,FormulaSettings.DEFAULT,PrecisionMode.AUTOMATIC,false),t->{if(t.step()==1&&t.completed()==t.total()){result.set(t);done.countDown();}},e->{error.set(e);done.countDown();});assertTrue(done.await(20,TimeUnit.SECONDS));assertNull(error.get());assertEquals(7*5*9,result.get().values().length);assertEquals(7*5*9,Arrays.stream(result.get().classifications()).sum());
        }
    }
    @Test void precisionCancellationAndWorkerReconfigurationRemainCooperative(){
        var formula=new PreciseFormula<>(Arithmetic.forPlan(new PrecisionPlan(PrecisionMode.ARBITRARY,100,300,false,"test")),FormulaSettings.DEFAULT,10);
        assertThrows(CancellationException.class,()->formula.evaluate(BigDecimal.ZERO,BigDecimal.ZERO,100000,()->true));try(var engine=new RenderEngine(1)){engine.configureWorkers(2);assertEquals(2,engine.workers());}
    }
    @Test void exactProjectCoordinatesRoundTripBeyondDoubleRange()throws Exception{
        var layer=LayerState.initial().view(Viewport.parse("-.7436438870371510000000000000000000001",".1318259042053300000000000000000000002","1e-400"));var document=new StudioDocument(List.of(layer),layer.id());Path p=directory.resolve("deep.aeternalis");ProjectIO.save(p,document);assertEquals(document,ProjectIO.load(p));
    }
    @Test void gpuFallbackDoesNotDowngradeRequiredPrecision()throws Exception{
        var gpu=GpuCompute.instance();var report=gpu.detection().get(30,TimeUnit.SECONDS);var request=new RenderRequest(Viewport.parse("-.75",".1","1e-100"),100,80,100,1);var plan=PrecisionController.analyse(request.viewport(),100,80,1,100,PrecisionMode.ARBITRARY);assertFalse(gpu.eligible(request,plan,ComputeMode.GPU));assertNotNull(report.detail());
    }
    @Test void optionalGpuOutputMatchesCpuOnValidatedHardware()throws Exception{
        var gpu=GpuCompute.instance();Assumptions.assumeTrue(gpu.detection().get(30,TimeUnit.SECONDS).validated(),"No validated OpenCL GPU is exposed in this environment.");var request=new RenderRequest(Viewport.HOME,32,24,300,1);var plan=PrecisionController.analyse(Viewport.HOME,32,24,1,300,PrecisionMode.AUTOMATIC);var result=gpu.tryTile(request,plan,ComputeMode.GPU,0,0,32,24,()->false);assertNotNull(result);var cpu=new Mandelbrot(4,false);
        for(int y=0;y<24;y++)for(int x=0;x<32;x++){var v=cpu.evaluate(Viewport.HOME.realAt(x+.5,32),Viewport.HOME.imaginaryAt(y+.5,32,24),300,()->false);double actual=result.values()[y*32+x];assertEquals(v.colourable(),Double.isFinite(actual));if(v.colourable())assertEquals(v.smooth(),actual,2e-7);}
    }
}
