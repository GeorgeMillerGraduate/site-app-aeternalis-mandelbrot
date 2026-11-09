package com.jenga.aeternalis.colouring;
/** Pure recolouring settings; changing them never invalidates orbit data. */
public record ColourSettings(Gradient gradient,Transfer transfer,double density,double scale,double offset,double power,boolean repeat,boolean reverse,boolean histogram,int interior,boolean transparentInterior) {
    public ColourSettings(Gradient g,Transfer t,double d,double scale,double offset,double power,boolean repeat,boolean reverse,boolean histogram,int interior){this(g,t,d,scale,offset,power,repeat,reverse,histogram,interior,false);}
    public enum Transfer {
        LINEAR("Linear"),SQRT("Square root"),CBRT("Cube root"),LOG("Logarithmic"),POWER("Adjustable power");
        private final String title;Transfer(String t){title=t;}public String toString(){return title;}
    }
    public ColourSettings {
        if(gradient==null||transfer==null||!Double.isFinite(density)||density<=0||!Double.isFinite(scale)||scale<=0||!Double.isFinite(offset)||!Double.isFinite(power)||power<=0)
            throw new IllegalArgumentException("Invalid colour settings.");
    }
    /** Blue/cyan/ivory/amber scheme based on Beyer's published UPR palette, using Aeternalis cubic interpolation. */
    public static ColourSettings wikipedia(){return new ColourSettings(Palette.WIKIPEDIA.gradient().withMode(Gradient.Mode.CATMULL_ROM),Transfer.SQRT,1,256,0,.5,true,false,false,0x000000);}
    public double position(double value){
        double u=Math.max(0,value/scale);
        double mapped=switch(transfer){case LINEAR->u;case SQRT->Math.sqrt(u);case CBRT->Math.cbrt(u);case LOG->Math.log1p(u);case POWER->Math.pow(u,power);};
        double t=density*mapped+offset;
        t=repeat?t-Math.floor(t):Math.max(0,Math.min(1,t));return reverse?1-t:t;
    }
}
