package com.jenga.aeternalis.rendering;
import java.util.*;
/** Adapted from George Miller's 2025 MIT scan-order algorithms; priorities now apply to tiles. */
public enum ScanOrder {
    RADIAL("Radial"),SIMPLE("Rows"),SPIRAL("Spiral"),SQUARE_SPIRAL("Square spiral"),DIAMOND("Diamond"),FLOWER("Flower"),ANGULAR("Angular"),TRIANGLE("Triangle"),XOR("XOR"),PYTHAGORAS("Pythagoras / AND");
    private final String title;ScanOrder(String t){title=t;}public String toString(){return title;}
    public List<int[]> tiles(int width,int height,int size){
        List<int[]> list=new ArrayList<>();for(int y=0;y<height;y+=size)for(int x=0;x<width;x+=size)list.add(new int[]{x,y});
        int cols=(width+size-1)/size,rows=(height+size-1)/size;
        list.sort(Comparator.comparingDouble((int[] p)->priority(p[0]/size,p[1]/size,cols,rows)).thenComparingInt(p->p[1]*width+p[0]));return list;
    }
    private double priority(int x,int y,int width,int height){
        double dx=x-width/2,dy=y-height/2,r=Math.hypot(dx,dy),a=Math.atan2(dy,-dx)/(2*Math.PI);
        int ix=(int)dx,iy=(int)dy,ax=Math.abs(ix),ay=Math.abs(iy);
        return switch(this){
            case SIMPLE->y*width+x;
            case RADIAL->dx*dx+dy*dy;
            case SPIRAL->(int)(a+r/3)*3-a;
            case FLOWER->r*(Math.cos(a*2*Math.PI*5)+2);
            case ANGULAR->Math.abs(((a+.5)*8)%1-.5);
            case DIAMOND->ax+ay+(dx>0?(dy<=0?.1:.2):(dy>0?.3:.4));
            case XOR->ax^ay;
            case PYTHAGORAS->x&y;
            case TRIANGLE->(ax^ay)+(dx>dy&&dx< -dy?100000:dx>dy&&dx>=-dy?200000:dx< -dy?400000:300000);
            case SQUARE_SPIRAL->dx>dy&&dx< -dy?4*dy*dy+7*dy+3+dx:dx>dy&&dx>=-dy?4*dx*dx+3*dx+1+dy:dx< -dy?4*dx*dx+dx-1-dy:4*dy*dy+5*dy+1-dx;
        };
    }
}
