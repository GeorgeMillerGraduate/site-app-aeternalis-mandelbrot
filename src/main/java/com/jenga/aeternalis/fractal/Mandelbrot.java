package com.jenga.aeternalis.fractal;
import java.util.function.BooleanSupplier;
/** Adapted from George Miller's 2025 evaluator: cached squares, analytic tests and adaptive cycle checkpoints. */
public final class Mandelbrot implements FractalFormula {
    private final double bailoutSquared;private final boolean periodicity;
    public Mandelbrot(){this(4,false);}
    public Mandelbrot(double bailoutSquared,boolean periodicity){
        if(!Double.isFinite(bailoutSquared)||bailoutSquared<4||bailoutSquared>1e12)throw new IllegalArgumentException("Squared bailout must be between 4 and 1e12.");
        this.bailoutSquared=bailoutSquared;this.periodicity=periodicity;
    }
    public String name(){return "Mandelbrot · z² + c";}
    public static boolean analyticInterior(double x,double y){return analyticReason(x,y)!=null;}
    private static Iteration.Reason analyticReason(double x,double y){
        double yy=y*y,q=(x-.25)*(x-.25)+yy;
        if(q*(q+x-.25)<.25*yy-1e-14)return Iteration.Reason.CARDIOID;
        if((x+1)*(x+1)+yy<.0625-1e-14)return Iteration.Reason.BULB;return null;
    }
    public Iteration evaluate(double x,double y,int limit,BooleanSupplier cancelled){
        var reason=analyticReason(x,y);if(reason!=null)return new Iteration(false,limit,0,reason);
        double a=0,b=0,aa=0,bb=0,hx=0,hy=0,tx=0,ty=0;
        int n=0,interval=3,counter=0,updates=0;
        while(aa+bb<=bailoutSquared&&n<limit){
            if((n&63)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            double next=aa-bb+x;b=2*a*b+y;a=next;aa=a*a;bb=b*b;n++;
            if(aa+bb>bailoutSquared)break;
            // Exact double equality only: near-repeats do not establish a deterministic cycle.
            if(periodicity){
                if((a==hx&&b==hy)||(n>10&&n%10==0&&a==tx&&b==ty))return new Iteration(false,limit,n,Iteration.Reason.CYCLE);
                if(n==10){tx=a;ty=b;}
                if(counter++>=interval){counter=0;hx=a;hy=b;if(++updates>=10){interval=Math.min(interval*2,1<<20);updates=0;}}
            }
        }
        boolean escaped=aa+bb>bailoutSquared;int refined=n;
        if(escaped)for(int k=0;k<3;k++){
            double next=a*a-b*b+x,bi=2*a*b+y;if(!Double.isFinite(next)||!Double.isFinite(bi))break;a=next;b=bi;refined++;
        }
        double smooth=escaped?refined+1-StrictMath.log(StrictMath.log(StrictMath.hypot(a,b)))/StrictMath.log(2):limit;
        return new Iteration(escaped,smooth,n,escaped?Iteration.Reason.ESCAPED:Iteration.Reason.LIMIT);
    }
}
