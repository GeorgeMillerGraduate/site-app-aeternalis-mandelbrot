package com.jenga.aeternalis.app;
import com.jenga.aeternalis.math.Viewport;
import com.jenga.aeternalis.fractal.FormulaSettings;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.layers.BlendMode;
import java.util.*;
/** Immutable independent fractal layer; render caches live outside the saved model. */
public record LayerState(UUID id,String name,boolean visible,double opacity,BlendMode blend,Viewport viewport,FormulaSettings formula,Calculation calculation,ColourSettings colours){
    public LayerState {
        Objects.requireNonNull(id);Objects.requireNonNull(blend);Objects.requireNonNull(viewport);Objects.requireNonNull(formula);Objects.requireNonNull(calculation);Objects.requireNonNull(colours);
        if(name==null||name.isBlank()||name.length()>80||!Double.isFinite(opacity)||opacity<0||opacity>1)throw new IllegalArgumentException("Invalid layer name or opacity.");
    }
    public static LayerState initial(){return new LayerState(UUID.randomUUID(),"Mandelbrot study",true,1,BlendMode.NORMAL,Viewport.HOME,FormulaSettings.DEFAULT,Calculation.DEFAULT,ColourSettings.wikipedia());}
    public LayerState view(Viewport v){return new LayerState(id,name,visible,opacity,blend,v,formula,calculation,colours);}
    public LayerState formula(FormulaSettings f){return new LayerState(id,name,visible,opacity,blend,viewport,f,calculation,colours);}
    public LayerState calculation(Calculation c){return new LayerState(id,name,visible,opacity,blend,viewport,formula,c,colours);}
    public LayerState colours(ColourSettings c){return new LayerState(id,name,visible,opacity,blend,viewport,formula,calculation,c);}
    public LayerState presentation(String n,boolean v,double opacity,BlendMode b){return new LayerState(id,n,v,opacity,b,viewport,formula,calculation,colours);}
    public LayerState duplicate(){return new LayerState(UUID.randomUUID(),name.length()>73?name.substring(0,73)+" copy":name+" copy",visible,opacity,blend,viewport,formula,calculation,colours);}
    public String toString(){return (visible?"● ":"○ ")+name;}
}
