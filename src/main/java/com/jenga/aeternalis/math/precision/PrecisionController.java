package com.jenga.aeternalis.math.precision;
import com.jenga.aeternalis.math.Viewport;
import java.math.*;
/** Starts at the requested minimum. Only an observed coordinate collision escalates a render. */
public final class PrecisionController {
    private PrecisionController(){}
    public static PrecisionPlan analyse(Viewport v,int width,int height,int grid,int iterations,PrecisionMode requested){
        PrecisionMode mode=requested==null||requested==PrecisionMode.AUTOMATIC?PrecisionMode.DOUBLE:requested;
        BigDecimal step=v.spanExact().divide(BigDecimal.valueOf((long)width*grid),v.context());
        int magnitude=Math.max(0,Math.max(v.realExact().precision()-v.realExact().scale(),v.imaginaryExact().precision()-v.imaginaryExact().scale()));
        int digits=switch(mode){case DOUBLE->17;case DOUBLE_DOUBLE->34;case QUAD_DOUBLE->66;default->Math.max(80,magnitude+step.scale()-step.precision()+24);};
        if(digits>10000)throw new IllegalArgumentException("Requested coordinates exceed the 10,000-digit budget.");
        return new PrecisionPlan(mode,digits,0,false,"Runtime HashMap collision detection; no ULP or iteration safety threshold. Pixel spacing "+step.toEngineeringString());
    }
    public static PrecisionMode next(PrecisionMode mode){return switch(mode){case AUTOMATIC,DOUBLE->PrecisionMode.DOUBLE_DOUBLE;case DOUBLE_DOUBLE->PrecisionMode.QUAD_DOUBLE;default->PrecisionMode.ARBITRARY;};}
}
