package com.jenga.aeternalis.math;
/** Aspect-locked selection in display coordinates, clamped to the visible image. */
public record SelectionBox(double x,double y,double width,double height) {
    public static SelectionBox fit(double ax,double ay,double bx,double by,double width,double height){
        if(width<=0||height<=0)throw new IllegalArgumentException("Invalid display size");
        ax=clamp(ax,width);bx=clamp(bx,width);ay=clamp(ay,height);by=clamp(by,height);
        double w=Math.abs(bx-ax),h=Math.abs(by-ay),ratio=width/height;
        if(w==0&&h==0)return new SelectionBox(ax,ay,0,0);
        w=Math.max(w,h*ratio);h=w/ratio;
        double scale=Math.min(1,Math.min(width/w,height/h));w*=scale;h*=scale;
        double x=Math.max(0,Math.min(width-w,(ax+bx-w)/2)),y=Math.max(0,Math.min(height-h,(ay+by-h)/2));
        return new SelectionBox(x,y,w,h);
    }
    private static double clamp(double v,double max){return Math.max(0,Math.min(max,v));}
    public Viewport zoom(Viewport current,int pixelWidth,int pixelHeight,double displayWidth,double displayHeight){
        if(width<3||height<3)throw new IllegalArgumentException("Selection is too small");
        double cx=(x+width/2)*pixelWidth/displayWidth,cy=(y+height/2)*pixelHeight/displayHeight;
        return current.selection(cx,cy,pixelWidth,pixelHeight,width/displayWidth);
    }
}
