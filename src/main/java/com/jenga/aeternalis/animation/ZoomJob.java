package com.jenga.aeternalis.animation;

import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.Viewport;
import java.math.*;
import java.util.*;

/** Immutable reproducible journey. Factor is horizontal-span retention, not centre multiplication. */
public record ZoomJob(StudioDocument destination,Viewport start,int width,int height,BigDecimal factor,int fps) {
    public ZoomJob {
        Objects.requireNonNull(destination);Objects.requireNonNull(start);Objects.requireNonNull(factor);
        if(width<16||height<16||width>7680||height>4320||fps<1||fps>120)throw new IllegalArgumentException("Use dimensions 16–7680 × 16–4320 and 1–120 fps.");
        if(factor.precision()>16)throw new IllegalArgumentException("Use at most 16 significant digits for span retention.");
        if(factor.compareTo(new BigDecimal("0.01"))<0||factor.compareTo(new BigDecimal("0.9999"))>0)throw new IllegalArgumentException("Span retention must be 0.01–0.9999.");
        if(destination.selected().viewport().spanExact().compareTo(start.spanExact())>0)throw new IllegalArgumentException("Destination must be at least as zoomed in as Home.");
        if(steps(start,destination.selected().viewport(),factor)>100000)throw new IllegalArgumentException("Journey exceeds 100,001 frames. Increase the change per frame.");
    }
    // Compact record constructors must use the incoming destination, not unassigned record fields.
    private static int steps(Viewport start,Viewport end,BigDecimal factor){
        if(start.spanExact().compareTo(end.spanExact())==0)return start.equals(end)?0:1;
        double distance=log(start.spanExact())-log(end.spanExact());
        int n=(int)Math.min(100001,Math.max(1,Math.ceil(distance/-Math.log(factor.doubleValue()))));
        if(n>100000)return n;
        MathContext mc=end.context();
        while(n>1&&start.spanExact().multiply(factor.pow(n-1,mc),mc).compareTo(end.spanExact())<=0)n--;
        while(n<=100000&&start.spanExact().multiply(factor.pow(n,mc),mc).compareTo(end.spanExact())>0)n++;
        return n;
    }
    private static double log(BigDecimal v){int exponent=v.precision()-v.scale()-1;return Math.log(v.movePointLeft(exponent).doubleValue())+exponent*Math.log(10);}
    public Viewport target(){return destination.selected().viewport();}
    public int frames(){return steps(start,target(),factor)+1;}
    public Viewport viewport(int index){
        int last=frames()-1;if(index<0||index>last)throw new IndexOutOfBoundsException();if(index==0)return start;if(index==last)return target();
        Viewport end=target();MathContext mc=end.context();BigDecimal span=start.spanExact().multiply(factor.pow(index,mc),mc).max(end.spanExact());
        BigDecimal denominator=start.spanExact().subtract(end.spanExact());
        BigDecimal t=denominator.signum()==0?BigDecimal.valueOf(index).divide(BigDecimal.valueOf(last),mc):start.spanExact().subtract(span).divide(denominator,mc);
        return new Viewport(start.realExact().add(end.realExact().subtract(start.realExact()).multiply(t,mc),mc),start.imaginaryExact().add(end.imaginaryExact().subtract(start.imaginaryExact()).multiply(t,mc),mc),span);
    }
    public StudioDocument frame(int index){return destination.replace(destination.selected().view(viewport(index)));}
    public static String filename(int index){return String.format(Locale.ROOT,"frame-%06d.png",index);}
}
