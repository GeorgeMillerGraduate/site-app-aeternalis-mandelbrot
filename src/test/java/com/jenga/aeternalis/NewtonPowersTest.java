package com.jenga.aeternalis;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.precision.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
class NewtonPowersTest {
    @Test void supportedFamiliesAreExactlyTheRequestedThree(){assertEquals(3,FormulaSettings.Family.values().length);assertArrayEquals(new String[]{"MANDELBROT","JULIA","NEWTON"},java.util.Arrays.stream(FormulaSettings.Family.values()).map(Enum::name).toArray(String[]::new));}
    @Test void everyNewtonPowerConvergesNearEveryRootAcrossPrecisions(){
        for(int n=2;n<=12;n++){
            var formula=new FormulaSettings(FormulaSettings.Family.NEWTON,n,0,0);
            for(int k=0;k<n;k++){
                double angle=2*Math.PI*k/n;BigDecimal x=BigDecimal.valueOf(1.1*Math.cos(angle)),y=BigDecimal.valueOf(1.1*Math.sin(angle));
                var normal=formula.evaluator(4,false).evaluate(x.doubleValue(),y.doubleValue(),100,()->false);assertEquals(Iteration.Reason.CONVERGED,normal.reason());
                for(var mode:new PrecisionMode[]{PrecisionMode.DOUBLE_DOUBLE,PrecisionMode.QUAD_DOUBLE,PrecisionMode.ARBITRARY}){
                    var wide=new PreciseFormula<>(Arithmetic.forPlan(new PrecisionPlan(mode,90,300,false,"test")),formula,4).evaluate(x,y,100,()->false);
                    assertEquals(Iteration.Reason.CONVERGED,wide.reason());assertEquals(normal.smooth(),wide.smooth(),1e-8,"n="+n+" root="+k+" mode="+mode);
                }
            }
        }
    }
}
