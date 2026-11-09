package com.jenga.aeternalis.math.precision;
import java.math.*;
/** Small typed arithmetic boundary; each worker owns its evaluator and constants. */
public interface Arithmetic<T> {
    T value(BigDecimal v);T add(T a,T b);T multiply(T a,T b);T divide(T a,T b);T negate(T a);T abs(T a);int compare(T a,T b);double approximate(T a);
    default double logMagnitude(T value){return StrictMath.log(Math.abs(approximate(value)));}
    default T subtract(T a,T b){return add(a,negate(b));}
    static Arithmetic<?> forPlan(PrecisionPlan plan){
        return switch(plan.mode()){
            case DOUBLE_DOUBLE->new Arithmetic<DoubleDouble>(){
                public DoubleDouble value(BigDecimal v){return DoubleDouble.of(v);}public DoubleDouble add(DoubleDouble a,DoubleDouble b){return a.add(b);}public DoubleDouble multiply(DoubleDouble a,DoubleDouble b){return a.multiply(b);}public DoubleDouble divide(DoubleDouble a,DoubleDouble b){return a.divide(b);}public DoubleDouble negate(DoubleDouble a){return a.negate();}public DoubleDouble abs(DoubleDouble a){return a.abs();}public int compare(DoubleDouble a,DoubleDouble b){return a.compareTo(b);}public double approximate(DoubleDouble a){return a.doubleValue();}
            };
            case QUAD_DOUBLE->new Arithmetic<QuadDouble>(){
                public QuadDouble value(BigDecimal v){return QuadDouble.of(v);}public QuadDouble add(QuadDouble a,QuadDouble b){return a.add(b);}public QuadDouble multiply(QuadDouble a,QuadDouble b){return a.multiply(b);}public QuadDouble divide(QuadDouble a,QuadDouble b){return a.divide(b);}public QuadDouble negate(QuadDouble a){return a.negate();}public QuadDouble abs(QuadDouble a){return a.abs();}public int compare(QuadDouble a,QuadDouble b){return a.compareTo(b);}public double approximate(QuadDouble a){return a.doubleValue();}
            };
            default->new Arithmetic<BigDecimal>(){
                private final MathContext mc=new MathContext(plan.decimalDigits(),RoundingMode.HALF_EVEN);
                public double logMagnitude(BigDecimal value){int exponent=value.precision()-value.scale()-1;return StrictMath.log(value.abs().movePointLeft(exponent).doubleValue())+exponent*StrictMath.log(10);}
                public BigDecimal value(BigDecimal v){return v.round(mc);}public BigDecimal add(BigDecimal a,BigDecimal b){return a.add(b,mc);}public BigDecimal multiply(BigDecimal a,BigDecimal b){return a.multiply(b,mc);}public BigDecimal divide(BigDecimal a,BigDecimal b){return a.divide(b,mc);}public BigDecimal negate(BigDecimal a){return a.negate();}public BigDecimal abs(BigDecimal a){return a.abs();}public int compare(BigDecimal a,BigDecimal b){return a.compareTo(b);}public double approximate(BigDecimal a){return a.doubleValue();}
            };
        };
    }
}
