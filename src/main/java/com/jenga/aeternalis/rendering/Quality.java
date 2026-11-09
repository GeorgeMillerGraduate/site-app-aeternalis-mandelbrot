package com.jenga.aeternalis.rendering;
/** Samples are per physical output pixel, after display-scale correction. */
public enum Quality {
    DRAFT("Draft · 1 sample",1),CLEAN("Clean · 2 × 2",2),FINE("Fine · 3 × 3",3);
    private final String title;private final int grid;
    Quality(String t,int g){title=t;grid=g;}public int grid(){return grid;}public String toString(){return title;}
}
