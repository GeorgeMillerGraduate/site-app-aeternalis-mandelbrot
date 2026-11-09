package com.jenga.aeternalis.fractal;
/** Classification describes the finite computation, not a proof for non-analytic interiors. */
public record Iteration(boolean escaped,double smooth,int iterations,Reason reason) {
    public enum Reason { ESCAPED,CARDIOID,BULB,CYCLE,LIMIT,CONVERGED }
    public boolean colourable(){return escaped||reason==Reason.CONVERGED;}
    public Iteration(boolean escaped,double smooth,int iterations){this(escaped,smooth,iterations,escaped?Reason.ESCAPED:Reason.LIMIT);}
}
