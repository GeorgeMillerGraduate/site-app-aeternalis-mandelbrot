package com.jenga.aeternalis.math.precision;
import java.math.*;
import java.util.*;
/** Four-component floating expansion. Error-free grow-expansion sums are renormalised before truncation. */
public final class QuadDouble {
    private final double[] limbs; // low to high, non-overlapping after expansion summation
    private QuadDouble(double[] limbs){this.limbs=limbs;}
    public static QuadDouble of(BigDecimal value){double[] terms=new double[4];BigDecimal remainder=value;for(int i=0;i<4;i++){double v=remainder.doubleValue();terms[i]=v;remainder=remainder.subtract(new BigDecimal(v));}return sum(terms);}
    private static QuadDouble sum(double[] terms){
        Double[] sorted=Arrays.stream(terms).boxed().toArray(Double[]::new);Arrays.sort(sorted,Comparator.comparingDouble(Math::abs));double[] expansion=new double[0];
        for(double term:sorted){double q=term;double[] next=new double[expansion.length+1];int n=0;for(double e:expansion){double s=q+e,v=s-q,error=(q-(s-v))+(e-v);if(error!=0)next[n++]=error;q=s;}if(q!=0||n==0)next[n++]=q;expansion=Arrays.copyOf(next,n);}
        expansion=compress(expansion);return new QuadDouble(Arrays.copyOfRange(expansion,Math.max(0,expansion.length-4),expansion.length));
    }
    private static double[] compress(double[] expansion){
        if(expansion.length<2)return expansion;double[] h=new double[expansion.length];double q=expansion[expansion.length-1];int bottom=expansion.length;
        for(int i=expansion.length-2;i>=0;i--){double sum=q+expansion[i],v=sum-q,error=(q-(sum-v))+(expansion[i]-v);if(error!=0){h[--bottom]=sum;q=error;}else q=sum;}
        h[--bottom]=q;int top=0;q=h[bottom];
        for(int i=bottom+1;i<h.length;i++){double a=h[i],sum=a+q,v=sum-a,error=(a-(sum-v))+(q-v);if(error!=0)h[top++]=error;q=sum;}
        if(q!=0||top==0)h[top++]=q;return Arrays.copyOf(h,top);
    }
    public QuadDouble add(QuadDouble b){double[] terms=Arrays.copyOf(limbs,limbs.length+b.limbs.length);System.arraycopy(b.limbs,0,terms,limbs.length,b.limbs.length);return sum(terms);}
    public QuadDouble negate(){double[] terms=limbs.clone();for(int i=0;i<terms.length;i++)terms[i]=-terms[i];return new QuadDouble(terms);}
    public QuadDouble subtract(QuadDouble b){return add(b.negate());}
    public QuadDouble multiply(QuadDouble b){double[] terms=new double[limbs.length*b.limbs.length*2];int at=0;for(double a:limbs)for(double v:b.limbs){double p=a*v;terms[at++]=p;terms[at++]=Math.fma(a,v,-p);}return sum(terms);}
    public QuadDouble divide(QuadDouble b){QuadDouble result=of(BigDecimal.ZERO),remainder=this;for(int i=0;i<5;i++){QuadDouble q=of(BigDecimal.valueOf(remainder.doubleValue()/b.doubleValue()));result=result.add(q);remainder=remainder.subtract(b.multiply(q));}return result;}
    public double doubleValue(){double v=0;for(double limb:limbs)v+=limb;return v;}
    public BigDecimal decimal(){BigDecimal value=BigDecimal.ZERO;for(double limb:limbs)value=value.add(new BigDecimal(limb));return value;}
    public int compareTo(QuadDouble b){QuadDouble d=subtract(b);for(int i=d.limbs.length-1;i>=0;i--)if(d.limbs[i]!=0)return d.limbs[i]<0?-1:1;return 0;}
    public QuadDouble abs(){return compareTo(of(BigDecimal.ZERO))<0?negate():this;}
    public boolean equals(Object other){return other instanceof QuadDouble q&&compareTo(q)==0;}
    public int hashCode(){return decimal().stripTrailingZeros().hashCode();}
}
