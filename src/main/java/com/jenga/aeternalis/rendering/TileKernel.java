package com.jenga.aeternalis.rendering;
import com.jenga.aeternalis.fractal.Iteration;
import java.util.function.*;
/** The same evaluator and global pixel mapping are used by local and network CPU tiles. */
public final class TileKernel {
    private TileKernel(){}
    public static RenderEngine.Tile calculate(PixelEvaluator.Context context,int x,int y,int width,int height,BooleanSupplier cancelled,DoubleConsumer progress){
        var request=context.request;
        if(x<0||y<0||width<1||height<1||width>256||height>256||(long)x+width>request.width()||(long)y+height>request.height())throw new IllegalArgumentException("Invalid tile bounds");
        context.validateTile(x,y,width,height);PixelEvaluator evaluator=new PixelEvaluator(context,cancelled);int grid=request.grid(),spp=grid*grid;
        double[] values=new double[width*height*spp];long[] reasons=new long[Iteration.Reason.values().length];long iterations=0,start=System.nanoTime();
        for(int j=0;j<height;j++){
            if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            for(int i=0;i<width;i++)for(int sy=0;sy<grid;sy++)for(int sx=0;sx<grid;sx++){
                var value=evaluator.sample(x+i,y+j,sx,sy,grid);values[(j*width+i)*spp+sy*grid+sx]=value.colourable()?value.smooth():Double.NaN;iterations+=value.iterations();reasons[value.reason().ordinal()]++;
            }
            progress.accept((j+1.)/height);
        }
        return new RenderEngine.Tile(x,y,width,height,values,0,0,iterations,System.nanoTime()-start,grid,1,reasons);
    }
}
