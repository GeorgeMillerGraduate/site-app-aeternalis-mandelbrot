package com.jenga.aeternalis.app;
import com.jenga.aeternalis.rendering.ScanOrder;
/** Layer-local numerical controls. */
public record Calculation(int limit,int grid,ScanOrder order,boolean periodicity,double bailoutSquared,boolean previews,com.jenga.aeternalis.math.precision.PrecisionMode precision,boolean perturbation){
    public Calculation(int limit,int grid,ScanOrder order,boolean periodicity,double bailout,boolean previews){this(limit,grid,order,periodicity,bailout,previews,com.jenga.aeternalis.math.precision.PrecisionMode.AUTOMATIC,true);}
    public static final Calculation DEFAULT=new Calculation(600,2,ScanOrder.RADIAL,true,10,true);
    public Calculation {if(precision==null)precision=com.jenga.aeternalis.math.precision.PrecisionMode.AUTOMATIC;if(limit<32||limit>100000||grid<1||grid>3||order==null||!Double.isFinite(bailoutSquared)||bailoutSquared<4||bailoutSquared>1e6)throw new IllegalArgumentException("Invalid calculation settings.");}
}
