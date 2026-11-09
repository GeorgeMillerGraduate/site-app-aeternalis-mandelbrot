package com.jenga.aeternalis.fractal;
import com.jenga.aeternalis.math.precision.*;
import java.math.*;
import java.util.function.BooleanSupplier;
/** Precision-generic finite evaluators. All recurrence operations use the selected backend. */
public final class PreciseFormula<T> {
    private final Arithmetic<T> math;private final FormulaSettings settings;private final T zero,one,two,three,bailout,juliaR,juliaI,tolerance;
    public PreciseFormula(Arithmetic<T> math,FormulaSettings settings,double bailoutSquared){
        this.math=math;this.settings=settings;zero=constant(0);one=constant(1);two=constant(2);three=constant(3);
        double threshold=settings.family()==FormulaSettings.Family.JULIA?Math.max(bailoutSquared,StrictMath.pow(StrictMath.hypot(settings.juliaReal(),settings.juliaImaginary())+1,2)):bailoutSquared;
        bailout=constant(threshold);juliaR=constant(settings.juliaReal());juliaI=constant(settings.juliaImaginary());tolerance=math.value(new BigDecimal("1e-20"));
    }
    private T constant(double v){return math.value(BigDecimal.valueOf(v));}
    private T square(T a){return math.multiply(a,a);}private T norm(T a,T b){return math.add(square(a),square(b));}
    public Iteration evaluate(BigDecimal x,BigDecimal y,int limit,BooleanSupplier cancelled){
        T cr=math.value(x),ci=math.value(y);boolean julia=settings.family()==FormulaSettings.Family.JULIA;
        T a=julia?cr:zero,b=julia?ci:zero;if(julia){cr=juliaR;ci=juliaI;}
        if(settings.family()==FormulaSettings.Family.NEWTON)return newton(math.value(x),math.value(y),limit,cancelled);
        int degree=2,n=0;
        while(math.compare(norm(a,b),bailout)<=0&&n<limit){
            if((n&31)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            T[] next=power(a,b,degree);a=math.add(next[0],cr);b=math.add(next[1],ci);n++;
        }
        boolean escaped=math.compare(norm(a,b),bailout)>0;int colourIteration=n;
        if(escaped)for(int i=0;i<3;i++){T[] next=power(a,b,degree);T ar=math.add(next[0],cr),bi=math.add(next[1],ci);if(!Double.isFinite(math.approximate(ar))||!Double.isFinite(math.approximate(bi)))break;a=ar;b=bi;colourIteration++;}
        double radius=StrictMath.hypot(math.approximate(a),math.approximate(b));
        double logRadius=Double.isFinite(radius)&&radius>0?StrictMath.log(radius):.5*math.logMagnitude(norm(a,b));
        double smooth=escaped&&Double.isFinite(logRadius)&&logRadius>0?colourIteration+1-StrictMath.log(logRadius)/StrictMath.log(degree):n;
        return new Iteration(escaped,smooth,n);
    }
    @SuppressWarnings("unchecked") private T[] power(T a,T b,int degree){
        T r=a,i=b;for(int k=1;k<degree;k++){T next=math.subtract(math.multiply(r,a),math.multiply(i,b));i=math.add(math.multiply(r,b),math.multiply(i,a));r=next;}
        return (T[])new Object[]{r,i};
    }
    private Iteration newton(T a,T b,int limit,BooleanSupplier cancelled){
        for(int n=0;n<limit;n++){
            if((n&31)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            int degree=settings.power();T[] previous=power(a,b,degree-1);
            T fr=math.subtract(math.subtract(math.multiply(previous[0],a),math.multiply(previous[1],b)),one),fi=math.add(math.multiply(previous[0],b),math.multiply(previous[1],a));
            if(math.compare(norm(fr,fi),tolerance)<0){
                int root=FamilyFormula.rootIndex(math.approximate(a),math.approximate(b),degree);return new Iteration(false,root*64./degree+Math.min(12,n*.15),n,Iteration.Reason.CONVERGED);
            }
            T dr=math.multiply(constant(degree),previous[0]),di=math.multiply(constant(degree),previous[1]),den=norm(dr,di);if(math.compare(den,math.value(new BigDecimal("1e-28")))<0)break;
            T nextA=math.subtract(a,math.divide(math.add(math.multiply(fr,dr),math.multiply(fi,di)),den));b=math.subtract(b,math.divide(math.subtract(math.multiply(fi,dr),math.multiply(fr,di)),den));a=nextA;
        }return new Iteration(false,limit,limit,Iteration.Reason.LIMIT);
    }
}
