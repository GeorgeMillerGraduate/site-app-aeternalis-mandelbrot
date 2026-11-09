package com.jenga.aeternalis.math.precision;
public record PrecisionPlan(PrecisionMode mode,int decimalDigits,int requiredBits,boolean doubleCollapse,String reason) {
    public String toString(){return mode+(mode==PrecisionMode.ARBITRARY?" · "+decimalDigits+" digits":"")+(doubleCollapse?" · collapse prevented":"");}
}
