package com.jenga.aeternalis.rendering;
import com.jenga.aeternalis.math.Viewport;
import java.math.*;
import java.util.*;
import java.util.function.BooleanSupplier;
/** Bounded high-precision reference-orbit cache for quadratic Mandelbrot delta evaluation. */
public final class PerturbationOrbit {
    public record Reference(double[] real,double[] imaginary,double[] error,int length){}
    private record Key(BigDecimal real,BigDecimal imaginary,int digits,int limit,double bailout){}
    private static final LinkedHashMap<Key,Reference> CACHE=new LinkedHashMap<>(8,.75f,true);
    private PerturbationOrbit(){}
    public static synchronized Reference reference(Viewport v,int digits,int limit,double bailout,BooleanSupplier cancelled){
        Key key=new Key(v.realExact(),v.imaginaryExact(),digits,limit,bailout);Reference cached=CACHE.get(key);if(cached!=null)return cached;
        MathContext mc=new MathContext(digits,RoundingMode.HALF_EVEN);BigDecimal cr=v.realExact().round(mc),ci=v.imaginaryExact().round(mc),a=BigDecimal.ZERO,b=BigDecimal.ZERO;
        double constantError=v.realExact().subtract(cr).abs().add(v.imaginaryExact().subtract(ci).abs()).doubleValue();double[] zr=new double[limit+1],zi=new double[limit+1],errors=new double[limit+1];int length=1;
        for(int n=0;n<limit;n++){
            if((n&31)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            double previousNorm=StrictMath.hypot(a.doubleValue(),b.doubleValue());BigDecimal next=a.multiply(a,mc).subtract(b.multiply(b,mc),mc).add(cr,mc);b=a.multiply(b,mc).multiply(BigDecimal.valueOf(2),mc).add(ci,mc);a=next;
            zr[n+1]=a.doubleValue();zi[n+1]=b.doubleValue();
            double unit=Math.max(Double.MIN_VALUE,StrictMath.pow(10,Math.max(0,Math.max(a.precision()-a.scale(),b.precision()-b.scale()))-digits+3));
            errors[n+1]=Math.nextUp(2*previousNorm*errors[n]+errors[n]*errors[n]+constantError+unit);length=n+2;
            if(zr[n+1]*zr[n+1]+zi[n+1]*zi[n+1]>bailout)break;
        }
        Reference ref=new Reference(zr,zi,errors,length);CACHE.put(key,ref);
        long bytes=CACHE.values().stream().mapToLong(r->24L*r.real().length).sum();var it=CACHE.entrySet().iterator();while(bytes>32_000_000&&it.hasNext()){var entry=it.next();bytes-=24L*entry.getValue().real().length;it.remove();}return ref;
    }
}
