package com.jenga.aeternalis.math.precision;
import java.math.*;
/** Two-component expansion, using TwoSum and an FMA product residual. Finite operands only. */
public record DoubleDouble(double hi,double lo) {
    public static DoubleDouble of(BigDecimal value){double hi=value.doubleValue();double lo=value.subtract(new BigDecimal(hi)).doubleValue();return normal(hi,lo);}
    private static DoubleDouble normal(double a,double b){double s=a+b,v=s-a;return new DoubleDouble(s,(a-(s-v))+(b-v));}
    public DoubleDouble add(DoubleDouble b){double s=hi+b.hi,v=s-hi,t=((b.hi-v)+(hi-(s-v)))+lo+b.lo;return normal(s,t);}
    public DoubleDouble negate(){return new DoubleDouble(-hi,-lo);}
    public DoubleDouble subtract(DoubleDouble b){return add(b.negate());}
    public DoubleDouble multiply(DoubleDouble b){double p=hi*b.hi,e=Math.fma(hi,b.hi,-p)+hi*b.lo+lo*b.hi+lo*b.lo;return normal(p,e);}
    public DoubleDouble divide(DoubleDouble b){double q=hi/b.hi;DoubleDouble first=new DoubleDouble(q,0);double correction=subtract(b.multiply(first)).doubleValue()/b.hi;return first.add(new DoubleDouble(correction,0));}
    public double doubleValue(){return hi+lo;}
    public BigDecimal decimal(){return new BigDecimal(hi).add(new BigDecimal(lo));}
    public int compareTo(DoubleDouble b){DoubleDouble d=subtract(b);return d.hi<0?-1:d.hi>0?1:Double.compare(d.lo,0);}
    public DoubleDouble abs(){return hi<0||hi==0&&lo<0?negate():this;}
}
