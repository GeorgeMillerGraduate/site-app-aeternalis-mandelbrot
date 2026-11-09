package com.jenga.aeternalis.colouring;
import java.util.*;
/** Immutable stops with smoothstep or gamut-clamped Catmull–Rom interpolation in linear light. */
public record Gradient(List<Stop> stops,Mode mode) {
    public enum Mode { SMOOTHSTEP("Cubic smoothstep"),CATMULL_ROM("Catmull–Rom cubic");private final String label;Mode(String s){label=s;}public String toString(){return label;} }
    public Gradient(List<Stop> stops){this(stops,Mode.SMOOTHSTEP);}
    public Gradient withMode(Mode mode){return new Gradient(stops,mode);}
    public record Stop(double position,int rgb) {
        public Stop { if(!Double.isFinite(position)||position<0||position>1)throw new IllegalArgumentException("Stop position must be 0…1.");rgb &= 0xffffff; }
    }
    public Gradient {
        Objects.requireNonNull(mode);stops=List.copyOf(stops);
        if(stops.size()<2||stops.size()>32||stops.getFirst().position()!=0||stops.getLast().position()!=1)
            throw new IllegalArgumentException("Use 2–32 stops, starting at 0 and ending at 1.");
        for(int i=1;i<stops.size();i++)if(stops.get(i).position()-stops.get(i-1).position()<.0001)
            throw new IllegalArgumentException("Colour stops must have distinct increasing positions.");
    }
    public static Gradient of(int... colours){
        List<Stop> s=new ArrayList<>();for(int i=0;i<colours.length;i++)s.add(new Stop((double)i/(colours.length-1),colours[i]));return new Gradient(s);
    }
    public static double cubic(double t){return t*t*(3-2*t);}
    private static final double[] LINEAR=new double[256];
    static {for(int i=0;i<256;i++){double s=i/255.;LINEAR[i]=s<=.04045?s/12.92:Math.pow((s+.055)/1.055,2.4);}}
    public static double linear(int channel){return LINEAR[channel];}
    public static int srgb(double linear){double c=Math.max(0,Math.min(1,linear));return (int)Math.round(255*(c<=.0031308?12.92*c:1.055*Math.pow(c,1/2.4)-.055));}
    public double[] sample(double t){
        t=Math.max(0,Math.min(1,t));int i=0;while(i<stops.size()-2&&t>stops.get(i+1).position())i++;
        Stop a=stops.get(i),b=stops.get(i+1),before=stops.get(Math.max(0,i-1)),after=stops.get(Math.min(stops.size()-1,i+2));
        double span=b.position()-a.position(),u=(t-a.position())/span;double[] out=new double[3];
        for(int c=0;c<3;c++){
            int shift=16-8*c;double p1=linear((a.rgb()>>shift)&255),p2=linear((b.rgb()>>shift)&255);
            if(mode==Mode.SMOOTHSTEP){double f=cubic(u);out[c]=p1*(1-f)+p2*f;}
            else{
                // Catmull–Rom's Hermite form, with position-aware tangents for uneven stops.
                double p0=linear((before.rgb()>>shift)&255),p3=linear((after.rgb()>>shift)&255);
                double m1=(p2-p0)*span/(i==0?2*span:b.position()-before.position());
                double m2=(p3-p1)*span/(i==stops.size()-2?2*span:after.position()-a.position());
                double u2=u*u,u3=u2*u;
                out[c]=Math.max(0,Math.min(1,(2*u3-3*u2+1)*p1+(u3-2*u2+u)*m1+(-2*u3+3*u2)*p2+(u3-u2)*m2));
            }
        }
        return out;
    }
    public int argb(double t){double[] c=sample(t);return 0xff000000|srgb(c[0])<<16|srgb(c[1])<<8|srgb(c[2]);}
}
