package com.jenga.aeternalis.math;
/** Conservative coordinate-spacing guard; this is not a complete orbit-error bound. */
public final class PrecisionGuard {
    private PrecisionGuard() {}
    public static boolean supports(Viewport v, int width, int height) {
        double step = v.span()/width;
        double magnitude = Math.max(Math.abs(v.real())+v.span()/2,
                Math.abs(v.imaginary())+v.span()*height/width/2);
        return Double.isFinite(magnitude) && step > 32 * Math.ulp(Math.max(1, magnitude));
    }
}
