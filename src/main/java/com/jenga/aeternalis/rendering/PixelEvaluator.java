package com.jenga.aeternalis.rendering;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.precision.*;
import java.math.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;
/** Worker-local evaluator with exact subpixel mapping and precision-safe fallback. */
public final class PixelEvaluator {
    public static final class Context {
        public final RenderRequest request;public final PrecisionPlan plan;public final LongAdder perturbationPixels=new LongAdder(),fallbackPixels=new LongAdder();
        final CoordinateRegistry coordinates;
        private volatile PerturbationOrbit.Reference reference;
        public Context(RenderRequest request){this.request=request;plan=PrecisionController.analyse(request.viewport(),request.width(),request.height(),request.grid(),request.limit(),request.precision());coordinates=new CoordinateRegistry(request,plan);}
        public void validateTile(int x,int y,int width,int height){
            for(int i=0;i<width;i++)for(int sx=0;sx<request.grid();sx++)coordinates.check(x+i,y,sx,0,request.grid());
            for(int j=0;j<height;j++)for(int sy=0;sy<request.grid();sy++)coordinates.check(x,y+j,0,sy,request.grid());
        }
        PerturbationOrbit.Reference reference(BooleanSupplier cancelled){if(reference==null)synchronized(this){if(reference==null)reference=PerturbationOrbit.reference(request.viewport(),Math.max(80,plan.decimalDigits()+16),request.limit(),request.bailoutSquared(),cancelled);}return reference;}
    }
    private final Context context;private final RenderRequest request;private final MathContext mapping;
    private final FractalFormula standard;private final PreciseFormula<?> precise,fallback;
    private final BooleanSupplier cancelled;
    public PixelEvaluator(Context context,BooleanSupplier cancelled){
        this.context=context;this.request=context.request;this.cancelled=cancelled;mapping=new MathContext(Math.max(context.plan.decimalDigits()+16,request.viewport().context().getPrecision()));
        standard=request.formula().evaluator(request.bailoutSquared(),request.periodicity());
        precise=new PreciseFormula<>(Arithmetic.forPlan(context.plan),request.formula(),request.bailoutSquared());
        var stronger=new PrecisionPlan(PrecisionMode.ARBITRARY,Math.min(10000,Math.max(80,context.plan.decimalDigits()+24)),context.plan.requiredBits(),context.plan.doubleCollapse(),"Validated fallback");
        fallback=new PreciseFormula<>(Arithmetic.forPlan(stronger),request.formula(),request.bailoutSquared());
    }
    public Iteration sample(int x,int y,int sx,int sy,int grid){
        context.coordinates.check(x,y,sx,sy,grid);
        if(context.plan.mode()==PrecisionMode.DOUBLE){double px=x+(sx+.5)/grid,py=y+(sy+.5)/grid;return standard.evaluate(request.viewport().realAt(px,request.width()),request.viewport().imaginaryAt(py,request.width(),request.height()),request.limit(),cancelled);}
        BigDecimal denominator=BigDecimal.valueOf(2L*request.width()*grid);
        BigDecimal dx=request.viewport().spanExact().multiply(BigDecimal.valueOf(2L*x*grid+2L*sx+1L-(long)request.width()*grid),mapping).divide(denominator,mapping);
        BigDecimal dy=request.viewport().spanExact().multiply(BigDecimal.valueOf((long)request.height()*grid-2L*y*grid-2L*sy-1),mapping).divide(denominator,mapping);
        return evaluate(request.viewport().realExact().add(dx,mapping),request.viewport().imaginaryExact().add(dy,mapping),dx,dy);
    }
    public Iteration preview(double x,double y){
        if(context.plan.mode()==PrecisionMode.DOUBLE)return standard.evaluate(request.viewport().realAt(x,request.width()),request.viewport().imaginaryAt(y,request.width(),request.height()),request.limit(),cancelled);
        BigDecimal r=request.viewport().realAtExact(BigDecimal.valueOf(x),request.width(),mapping),i=request.viewport().imaginaryAtExact(BigDecimal.valueOf(y),request.width(),request.height(),mapping);
        return evaluate(r,i,r.subtract(request.viewport().realExact()),i.subtract(request.viewport().imaginaryExact()));
    }
    private Iteration evaluate(BigDecimal r,BigDecimal i,BigDecimal deltaR,BigDecimal deltaI){
        if(request.formula().family()==FormulaSettings.Family.MANDELBROT&&request.viewport().spanExact().compareTo(new BigDecimal("1e-280"))>=0&&request.perturbation()){
            Iteration value=perturb(deltaR.doubleValue(),deltaI.doubleValue());if(value!=null){context.perturbationPixels.increment();return value;}
            context.fallbackPixels.increment();return fallback.evaluate(r,i,request.limit(),cancelled);
        }
        return precise.evaluate(r,i,request.limit(),cancelled);
    }
    /** Conservative propagated delta/reference error; ambiguous escape tests and cancellation glitches fall back. */
    private Iteration perturb(double cr,double ci){
        var ref=context.reference(cancelled);double a=0,b=0,error=0;double radius=Math.sqrt(request.bailoutSquared());
        for(int n=0;n<request.limit();n++){
            if((n&31)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();if(n+1>=ref.length())return null;
            double ar=ref.real()[n],bi=ref.imaginary()[n],previousDelta=StrictMath.hypot(a,b);
            double next=2*(ar*a-bi*b)+a*a-b*b+cr;double nextB=2*(ar*b+bi*a)+2*a*b+ci;
            double rounding=64*Math.ulp(Math.max(Math.abs(next),Math.abs(nextB)))+64*Double.MIN_VALUE;
            error=Math.nextUp(2*(StrictMath.hypot(ar,bi)+previousDelta+error)*error+2*previousDelta*(ref.error()[n]+2*Math.ulp(Math.max(Math.abs(ar),Math.abs(bi))))+2*Math.ulp(Math.max(Math.abs(cr),Math.abs(ci)))+rounding);
            a=next;b=nextB;double zr=ref.real()[n+1]+a,zi=ref.imaginary()[n+1]+b,norm=StrictMath.hypot(zr,zi);
            double total=error+ref.error()[n+1]+8*Math.ulp(norm);
            if(!Double.isFinite(total)||total>1e-9*(norm+1)||Math.abs(norm-radius)<=total)return null;
            if(norm>radius){
                int count=n+1,refined=count;
                for(int k=0;k<3;k++){double real=zr*zr-zi*zi+request.viewport().real()+cr,imaginary=2*zr*zi+request.viewport().imaginary()+ci;total=Math.nextUp(2*(StrictMath.hypot(zr,zi)+total)*total+16*Math.ulp(Math.max(Math.abs(real),Math.abs(imaginary))));zr=real;zi=imaginary;refined++;}
                double magnitude=StrictMath.hypot(zr,zi);if(!Double.isFinite(magnitude)||total>1e-8*magnitude)return null;
                return new Iteration(true,refined+1-StrictMath.log(StrictMath.log(magnitude))/StrictMath.log(2),count);
            }
        }return new Iteration(false,request.limit(),request.limit());
    }
}
