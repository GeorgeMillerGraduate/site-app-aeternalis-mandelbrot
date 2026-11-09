package com.jenga.aeternalis.math.precision;
public enum PrecisionMode {
    AUTOMATIC("Automatic"),DOUBLE("Double"),DOUBLE_DOUBLE("Double-double"),QUAD_DOUBLE("Quad-double"),ARBITRARY("Arbitrary precision");
    private final String title;PrecisionMode(String title){this.title=title;}public String toString(){return title;}
}
