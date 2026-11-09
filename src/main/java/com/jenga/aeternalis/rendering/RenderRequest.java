package com.jenga.aeternalis.rendering;
import com.jenga.aeternalis.math.Viewport;
/** Immutable numerical settings. Bailout is explicitly the squared modulus threshold. */
public record RenderRequest(Viewport viewport,int width,int height,int limit,int grid,ScanOrder order,boolean periodicity,double bailoutSquared,boolean previews,com.jenga.aeternalis.fractal.FormulaSettings formula,com.jenga.aeternalis.math.precision.PrecisionMode precision,boolean perturbation) {
    public RenderRequest(Viewport v,int w,int h,int n,int g,ScanOrder order,boolean cycles,double bailout,boolean preview,com.jenga.aeternalis.fractal.FormulaSettings formula){this(v,w,h,n,g,order,cycles,bailout,preview,formula,com.jenga.aeternalis.math.precision.PrecisionMode.AUTOMATIC,true);}
    public RenderRequest(Viewport v,int w,int h,int n,int g,ScanOrder order,boolean cycles,double bailout,boolean preview){this(v,w,h,n,g,order,cycles,bailout,preview,com.jenga.aeternalis.fractal.FormulaSettings.DEFAULT);}
    public RenderRequest(Viewport v,int w,int h,int n){this(v,w,h,n,1);}
    public RenderRequest(Viewport v,int w,int h,int n,int g){this(v,w,h,n,g,ScanOrder.RADIAL,false,4,false);}
    public RenderRequest {
        if(viewport==null||order==null||formula==null||precision==null||width<1||height<1||grid<1||grid>3||limit<1||limit>100000||width>131072||height>131072)
            throw new IllegalArgumentException("Dimensions must be 1–131072 and numerical settings must be valid.");
        if(!Double.isFinite(bailoutSquared)||bailoutSquared<4||bailoutSquared>1e12)throw new IllegalArgumentException("Squared bailout must be between 4 and 1e12.");
    }
}
