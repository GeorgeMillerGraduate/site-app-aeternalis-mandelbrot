package com.jenga.aeternalis.layers;
public enum BlendMode {
    NORMAL("Normal"),MULTIPLY("Multiply"),SCREEN("Screen"),OVERLAY("Overlay"),DIFFERENCE("Difference"),ADD("Add"),LIGHTEN("Lighten"),DARKEN("Darken");
    private final String label;BlendMode(String s){label=s;}public String toString(){return label;}
    public double apply(double background,double source){return switch(this){case NORMAL->source;case MULTIPLY->background*source;case SCREEN->1-(1-background)*(1-source);case OVERLAY->background<=.5?2*background*source:1-2*(1-background)*(1-source);case DIFFERENCE->Math.abs(background-source);case ADD->Math.min(1,background+source);case LIGHTEN->Math.max(background,source);case DARKEN->Math.min(background,source);};}
}
