package com.jenga.aeternalis.colouring;
import com.jenga.aeternalis.rendering.RenderEngine.Tile;
import java.util.*;
import java.util.function.BooleanSupplier;
/** Background colour pass: retain samples, colour separately, then average in linear light. */
public final class ColourRenderer {
    private ColourRenderer(){}
    public static int[] render(List<Tile> tiles,int width,int height,int grid,ColourSettings settings,BooleanSupplier cancelled){
        int[] pixels=new int[width*height];Arrays.fill(pixels,settings.transparentInterior()?0:settings.interior()|0xff000000);
        Histogram histogram=settings.histogram()?Histogram.of(tiles.stream().filter(t->t.step()==1).toList(),cancelled):null;
        // Cubic gradient is evaluated into a lookup table once per pass, never per orbit.
        double[][] table=new double[8192][];for(int i=0;i<table.length;i++)table[i]=settings.gradient().sample(i/(double)(table.length-1));
        double[] inside={Gradient.linear((settings.interior()>>16)&255),Gradient.linear((settings.interior()>>8)&255),Gradient.linear(settings.interior()&255)};
        for(Tile tile:tiles){
            int actualGrid=tile.sampleGrid()==0?grid:tile.sampleGrid(),spp=actualGrid*actualGrid;
            int columns=(tile.width()+tile.step()-1)/tile.step();
            for(int y=0;y<tile.height();y++){
                if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
                for(int x=0;x<tile.width();x++){
                    double r=0,g=0,b=0,coverage=0;int at=((y/tile.step())*columns+x/tile.step())*spp;
                    if(tile.step()>1&&tile.values()[at]==Double.POSITIVE_INFINITY)continue;
                    for(int n=0;n<spp;n++){
                        double value=tile.values()[at+n];double[] c;
                        if(Double.isNaN(value)){if(settings.transparentInterior())continue;c=inside;}
                        else {double input=histogram==null?value:histogram.map(value)*settings.scale();double t=settings.position(input);c=table[(int)Math.round(t*(table.length-1))];}
                        r+=c[0];g+=c[1];b+=c[2];coverage++;
                    }
                    pixels[(tile.y()+y)*width+tile.x()+x]=coverage==0?0:((int)Math.round(255*coverage/spp)<<24)|Gradient.srgb(r/coverage)<<16|Gradient.srgb(g/coverage)<<8|Gradient.srgb(b/coverage);
                }
            }
        }
        return pixels;
    }
    /** Binned CDF, rebuilt from available samples; excludes unescaped points. */
    private record Histogram(double min,double max,double[] cumulative){
        static Histogram of(List<Tile> tiles,BooleanSupplier cancelled){
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;long count=0;
            for(Tile t:tiles){if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();for(double v:t.values())if(Double.isFinite(v)){min=Math.min(min,v);max=Math.max(max,v);count++;}}
            double[] bins=new double[4096];
            if(count==0||min==max)return new Histogram(0,0,bins);
            for(Tile t:tiles){if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();for(double v:t.values())if(Double.isFinite(v))bins[(int)Math.min(4095,(v-min)/(max-min)*4095)]++;}
            double sum=0;for(int i=0;i<bins.length;i++){sum+=bins[i];bins[i]=sum/count;}return new Histogram(min,max,bins);
        }
        double map(double v){if(min==max)return .5;if(v>=max)return 1;double p=Math.max(0,Math.min(4095,(v-min)/(max-min)*4095));int i=(int)p;double a=i==0?0:cumulative[i-1],b=cumulative[i];return a+(b-a)*(p-i);}
    }
}
