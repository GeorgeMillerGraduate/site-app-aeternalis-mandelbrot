package com.jenga.aeternalis.fractal;
import java.util.function.BooleanSupplier;
/** Numerical contract, independent of JavaFX. More precision backends belong in separate evaluators. */
public interface FractalFormula {
    String name();
    Iteration evaluate(double real, double imaginary, int limit, BooleanSupplier cancelled);
}
