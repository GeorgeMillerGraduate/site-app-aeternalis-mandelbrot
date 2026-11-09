package com.jenga.aeternalis.math.precision;
/** A distinct grid position was rounded onto an already used axis coordinate. */
public final class CoordinateCollision extends RuntimeException {
    public CoordinateCollision(String message){super(message);}
}
