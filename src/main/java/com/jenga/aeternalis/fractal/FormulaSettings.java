package com.jenga.aeternalis.fractal;
/** Immutable formula parameters. Julia's constant is independent of viewport position. */
public record FormulaSettings(Family family,int power,double juliaReal,double juliaImaginary) {
    public enum Family {
        MANDELBROT("Mandelbrot"),JULIA("Julia"),NEWTON("Newton · zⁿ − 1");
        private final String label;Family(String s){label=s;}public String toString(){return label;}
    }
    public static final FormulaSettings DEFAULT=new FormulaSettings(Family.MANDELBROT,3,-.745,.186);
    public FormulaSettings {
        if(family==null)throw new IllegalArgumentException("Unsupported formula. This edition supports Mandelbrot, Julia and Newton z^n - 1 only.");
        if(power<2||power>12||!Double.isFinite(juliaReal)||!Double.isFinite(juliaImaginary)||Math.abs(juliaReal)>4||Math.abs(juliaImaginary)>4)
            throw new IllegalArgumentException("Use power 2–12 and finite Julia parameters in −4…4.");
    }
    public FractalFormula evaluator(double squaredBailout,boolean cycles){
        return family==Family.MANDELBROT?new Mandelbrot(squaredBailout,cycles):new FamilyFormula(this,squaredBailout);
    }
}
