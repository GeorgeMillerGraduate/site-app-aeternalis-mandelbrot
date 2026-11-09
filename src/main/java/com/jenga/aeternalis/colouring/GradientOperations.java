package com.jenga.aeternalis.colouring;
import java.util.*;
/** Pure stop operations shared by mouse and keyboard palette editing. */
public final class GradientOperations {
    private GradientOperations(){}
    public static Gradient reverse(Gradient gradient){List<Gradient.Stop>s=new ArrayList<>();for(var stop:gradient.stops())s.add(new Gradient.Stop(1-stop.position(),stop.rgb()));Collections.reverse(s);return new Gradient(s,gradient.mode());}
    public static Gradient insert(Gradient gradient,double position,int rgb){List<Gradient.Stop>s=new ArrayList<>(gradient.stops());s.add(new Gradient.Stop(position,rgb));s.sort(Comparator.comparingDouble(Gradient.Stop::position));return new Gradient(s,gradient.mode());}
    public static Gradient replace(Gradient gradient,int at,double position,int rgb){List<Gradient.Stop>s=new ArrayList<>(gradient.stops());s.set(at,new Gradient.Stop(position,rgb));return new Gradient(s,gradient.mode());}
    public static Gradient remove(Gradient gradient,int at){if(at<=0||at>=gradient.stops().size()-1)throw new IllegalArgumentException("The two endpoints must remain.");List<Gradient.Stop>s=new ArrayList<>(gradient.stops());s.remove(at);return new Gradient(s,gradient.mode());}
    public static double largestGap(Gradient gradient){int gap=0;var stops=gradient.stops();for(int i=1;i<stops.size()-1;i++)if(stops.get(i+1).position()-stops.get(i).position()>stops.get(gap+1).position()-stops.get(gap).position())gap=i;return (stops.get(gap).position()+stops.get(gap+1).position())/2;}
}
