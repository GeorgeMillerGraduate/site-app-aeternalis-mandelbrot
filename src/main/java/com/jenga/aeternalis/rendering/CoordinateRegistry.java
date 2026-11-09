package com.jenga.aeternalis.rendering;
import com.jenga.aeternalis.math.precision.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
/** Factored HashMaps: O(width*grid + height*grid), rather than one entry per complex sample.
 * Repeated real values in different rows are valid; only distinct positions on the same axis collide. */
final class CoordinateRegistry {
    private final Axis real,imaginary;
    CoordinateRegistry(RenderRequest r,PrecisionPlan plan){real=new Axis(r,plan,true);imaginary=new Axis(r,plan,false);}
    void check(int x,int y,int sx,int sy,int grid){real.check(x*grid+sx);imaginary.check(y*grid+sy);}
    private static final class Axis {
        final RenderRequest r;final PrecisionPlan plan;final boolean real;final AtomicIntegerArray checked;final HashMap<Object,Integer> used=new HashMap<>();final MathContext mc;
        Axis(RenderRequest r,PrecisionPlan plan,boolean real){this.r=r;this.plan=plan;this.real=real;checked=new AtomicIntegerArray((real?r.width():r.height())*r.grid());mc=new MathContext(Math.max(plan.decimalDigits()+16,r.viewport().context().getPrecision()));}
        void check(int index){if(checked.get(index)!=0)return;synchronized(this){if(checked.get(index)!=0)return;
            Object key;
            if(plan.mode()==PrecisionMode.DOUBLE){double pixel=(index/r.grid())+((index%r.grid())+.5)/r.grid();double value=real?r.viewport().realAt(pixel,r.width()):r.viewport().imaginaryAt(pixel,r.width(),r.height());if(!Double.isFinite(value))throw new CoordinateCollision("Nonfinite Double coordinate");key=value==0?0d:value;}
            else{
                long numerator=real?2L*index+1-(long)r.width()*r.grid():(long)r.height()*r.grid()-2L*index-1;
                BigDecimal offset=r.viewport().spanExact().multiply(BigDecimal.valueOf(numerator),mc).divide(BigDecimal.valueOf(2L*r.width()*r.grid()),mc);
                BigDecimal exact=(real?r.viewport().realExact():r.viewport().imaginaryExact()).add(offset,mc);
                try{key=switch(plan.mode()){case DOUBLE_DOUBLE->DoubleDouble.of(exact).decimal().stripTrailingZeros();case QUAD_DOUBLE->QuadDouble.of(exact).decimal().stripTrailingZeros();default->exact.round(new MathContext(plan.decimalDigits())).stripTrailingZeros();};}catch(NumberFormatException ex){throw new CoordinateCollision("Coordinate exceeds the expansion exponent range");}
            }
            Integer previous=used.putIfAbsent(key,index);if(previous!=null&&previous!=index)throw new CoordinateCollision((real?"Real":"Imaginary")+" samples "+previous+" and "+index+" share a "+plan.mode()+" coordinate");checked.set(index,1);
        }}
    }
}
