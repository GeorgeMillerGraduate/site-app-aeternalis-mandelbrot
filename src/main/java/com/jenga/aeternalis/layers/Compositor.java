package com.jenga.aeternalis.layers;
import com.jenga.aeternalis.colouring.Gradient;
import java.util.*;
import java.util.function.BooleanSupplier;
/** Source-over blend composition in linear light, with straight-alpha ARGB input/output. */
public final class Compositor {
    private Compositor(){}
    public record Input(int[] pixels,double opacity,BlendMode mode){}
    private static double channel(double cb,double cs,double ab,double as,double ao,BlendMode mode){return (as*(1-ab)*cs+as*ab*mode.apply(cb,cs)+(1-as)*ab*cb)/ao;}
    public static int[] compose(List<Input> bottomToTop,int size,BooleanSupplier cancelled){
        int[] out=new int[size];
        for(int at=0;at<size;at++){
            if((at&1023)==0&&cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            double alpha=0,r=0,g=0,b=0;
            for(Input input:bottomToTop){
                if(input.pixels().length!=size)throw new IllegalArgumentException("Layer dimensions differ.");
                int c=input.pixels()[at];double as=(c>>>24)/255.*input.opacity();if(as==0)continue;
                double next=as+alpha*(1-as);
                r=channel(r,Gradient.linear((c>>16)&255),alpha,as,next,input.mode());
                g=channel(g,Gradient.linear((c>>8)&255),alpha,as,next,input.mode());
                b=channel(b,Gradient.linear(c&255),alpha,as,next,input.mode());alpha=next;
            }
            out[at]=(int)Math.round(alpha*255)<<24|Gradient.srgb(r)<<16|Gradient.srgb(g)<<8|Gradient.srgb(b);
        }return out;
    }
}
