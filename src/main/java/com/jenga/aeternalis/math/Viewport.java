package com.jenga.aeternalis.math;
import java.math.*;
/** Exact decimal coordinates, with legacy double accessors for ordinary previews and old project compatibility. */
public record Viewport(double real,double imaginary,double span,BigDecimal realExact,BigDecimal imaginaryExact,BigDecimal spanExact) {
    public static final Viewport HOME=new Viewport(-.5,0,3.5);
    public Viewport(double real,double imaginary,double span){this(real,imaginary,span,BigDecimal.valueOf(real),BigDecimal.valueOf(imaginary),BigDecimal.valueOf(span));}
    public Viewport(BigDecimal real,BigDecimal imaginary,BigDecimal span){this(real.doubleValue(),imaginary.doubleValue(),span.doubleValue(),real,imaginary,span);}
    public static Viewport parse(String real,String imaginary,String span){return new Viewport(new BigDecimal(real.trim()),new BigDecimal(imaginary.trim()),new BigDecimal(span.trim()));}
    public Viewport {
        // Older schema-1 files contain only the three doubles; Gson invokes this constructor.
        if(realExact==null)realExact=BigDecimal.valueOf(real);if(imaginaryExact==null)imaginaryExact=BigDecimal.valueOf(imaginary);if(spanExact==null)spanExact=BigDecimal.valueOf(span);
        realExact=realExact.stripTrailingZeros();imaginaryExact=imaginaryExact.stripTrailingZeros();spanExact=spanExact.stripTrailingZeros();
        real=realExact.doubleValue();imaginary=imaginaryExact.doubleValue();span=spanExact.doubleValue();
        if(!Double.isFinite(real)||!Double.isFinite(imaginary)||spanExact.signum()<=0||!Double.isFinite(span))throw new IllegalArgumentException("Coordinates must be finite and span must be positive.");
        if(Math.abs((long)spanExact.scale())>10000||realExact.precision()>10000||imaginaryExact.precision()>10000)throw new IllegalArgumentException("This release supports coordinates up to 10,000 decimal places.");
    }
    public MathContext context(){
        int magnitude=Math.max(0,Math.max(realExact.precision()-realExact.scale(),imaginaryExact.precision()-imaginaryExact.scale()));
        int digits=Math.max(50,Math.max(Math.max(realExact.precision(),imaginaryExact.precision()),magnitude+spanExact.scale()-spanExact.precision()+40));
        if(digits>10000)throw new IllegalArgumentException("Requested viewport exceeds the 10,000-digit computation budget.");return new MathContext(digits,RoundingMode.HALF_EVEN);
    }
    public double realAt(double x,int width){return real+(x-width/2.)/width*span;}
    public double imaginaryAt(double y,int width,int height){return imaginary-(y-height/2.)/width*span;}
    public BigDecimal realAtExact(BigDecimal x,int width,MathContext mc){return realExact.add(spanExact.multiply(x.subtract(BigDecimal.valueOf(width).divide(BigDecimal.TWO)),mc).divide(BigDecimal.valueOf(width),mc),mc);}
    public BigDecimal imaginaryAtExact(BigDecimal y,int width,int height,MathContext mc){return imaginaryExact.subtract(spanExact.multiply(y.subtract(BigDecimal.valueOf(height).divide(BigDecimal.TWO)),mc).divide(BigDecimal.valueOf(width),mc),mc);}
    public Viewport zoom(double factor,double x,double y,int width,int height){
        MathContext mc=context();BigDecimal f=BigDecimal.valueOf(factor),oneMinus=BigDecimal.ONE.subtract(f);
        BigDecimal anchorR=realAtExact(BigDecimal.valueOf(x),width,mc),anchorI=imaginaryAtExact(BigDecimal.valueOf(y),width,height,mc);
        return new Viewport(realExact.multiply(f,mc).add(anchorR.multiply(oneMinus,mc),mc),imaginaryExact.multiply(f,mc).add(anchorI.multiply(oneMinus,mc),mc),spanExact.multiply(f,mc));
    }
    public Viewport pan(double dx,double dy,int width){MathContext mc=context();BigDecimal step=spanExact.divide(BigDecimal.valueOf(width),mc);return new Viewport(realExact.subtract(step.multiply(BigDecimal.valueOf(dx),mc),mc),imaginaryExact.add(step.multiply(BigDecimal.valueOf(dy),mc),mc),spanExact);}
    public Viewport selection(double x,double y,int width,int height,double fraction){MathContext mc=context();return new Viewport(realAtExact(BigDecimal.valueOf(x),width,mc),imaginaryAtExact(BigDecimal.valueOf(y),width,height,mc),spanExact.multiply(BigDecimal.valueOf(fraction),mc));}
}
