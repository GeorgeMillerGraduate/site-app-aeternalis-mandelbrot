package com.jenga.aeternalis.fractal;
import java.util.function.BooleanSupplier;
/** Independent CPU kernels. Mandelbrot-specific interior/cycle shortcuts are never applied here. */
public final class FamilyFormula implements FractalFormula {
    private final FormulaSettings settings;private final double bailout;
    public FamilyFormula(FormulaSettings settings,double bailout){this.settings=settings;this.bailout=bailout;}
    public String name(){return settings.family().toString();}
    public Iteration evaluate(double x,double y,int limit,BooleanSupplier cancelled){
        if(settings.family()==FormulaSettings.Family.NEWTON)return newton(x,y,limit,cancelled);
        boolean julia=settings.family()==FormulaSettings.Family.JULIA;
        double a=julia?x:0,b=julia?y:0,cr=julia?settings.juliaReal():x,ci=julia?settings.juliaImaginary():y;
        int power=2;
        // For Julia constants outside the radius-two disk, increase the escape bound conservatively.
        double threshold=julia?Math.max(bailout,StrictMath.pow(StrictMath.hypot(cr,ci)+1,2)):bailout;
        int n=0;
        while(a*a+b*b<=threshold&&n<limit){
            if((n&63)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            double r=a,i=b;
            for(int k=1;k<power;k++){double next=r*a-i*b;i=r*b+i*a;r=next;}
            a=r+cr;b=i+ci;n++;
        }
        boolean escaped=a*a+b*b>threshold;
        int refined=n;
        if(escaped)for(int k=0;k<3;k++){
            double ar=a,bi=b;
            double rr=ar,ii=bi;for(int j=1;j<power;j++){double next=rr*ar-ii*bi;ii=rr*bi+ii*ar;rr=next;}
            rr+=cr;ii+=ci;if(!Double.isFinite(rr)||!Double.isFinite(ii))break;a=rr;b=ii;refined++;
        }
        double smooth=escaped?refined+1-StrictMath.log(StrictMath.log(StrictMath.hypot(a,b)))/StrictMath.log(power):limit;
        return new Iteration(escaped,smooth,n);
    }
    public static int rootIndex(double real,double imaginary,int degree){return Math.floorMod((int)Math.round(StrictMath.atan2(imaginary,real)*degree/(2*Math.PI)),degree);}
    private Iteration newton(double a,double b,int limit,BooleanSupplier cancelled){
        int degree=settings.power();
        for(int n=0;n<limit;n++){
            if((n&31)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            double r=1,i=0;for(int k=0;k<degree-1;k++){double next=r*a-i*b;i=r*b+i*a;r=next;}
            double fr=r*a-i*b-1,fi=r*b+i*a;
            if(StrictMath.hypot(fr,fi)<1e-10){
                int root=rootIndex(a,b,degree);
                return new Iteration(false,root*64./degree+Math.min(12,n*.15),n,Iteration.Reason.CONVERGED);
            }
            double dr=degree*r,di=degree*i,den=dr*dr+di*di;if(den<1e-28||!Double.isFinite(den))break;
            a-=(fr*dr+fi*di)/den;b-=(fi*dr-fr*di)/den;
        }
        return new Iteration(false,limit,limit,Iteration.Reason.LIMIT);
    }
}
