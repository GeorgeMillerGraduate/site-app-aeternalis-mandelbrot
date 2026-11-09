package com.jenga.aeternalis.network;
import com.jenga.aeternalis.distributed.RenderTask;
import com.jenga.aeternalis.rendering.*;
import java.io.*;
public final class TaskCodec {
    public static void task(DataOutputStream out,RenderTask t)throws IOException{NetworkProtocol.uuid(out,t.job());out.writeLong(t.generation());out.writeInt(t.tile());NetworkProtocol.uuid(out,t.assignment());out.writeInt(t.x());out.writeInt(t.y());out.writeInt(t.width());out.writeInt(t.height());}
    public static RenderTask task(DataInputStream in)throws IOException{return new RenderTask(NetworkProtocol.uuid(in),in.readLong(),in.readInt(),NetworkProtocol.uuid(in),in.readInt(),in.readInt(),in.readInt(),in.readInt());}
    public static void result(DataOutputStream out,RenderTask task,RenderEngine.Tile tile)throws IOException{task(out,task);out.writeLong(tile.iterations());out.writeLong(tile.nanos());out.writeInt(tile.values().length);for(double value:tile.values())out.writeDouble(value);for(long count:tile.classifications())out.writeLong(count);}
    public static RenderEngine.Tile result(DataInputStream in,RenderTask task,RenderRequest request)throws IOException{
        long iterations=in.readLong(),nanos=in.readLong();int n=in.readInt(),expected=task.width()*task.height()*request.grid()*request.grid();if(n!=expected||n<1||n>256*256*9||in.available()!=n*8+48||iterations<0||iterations>(long)n*request.limit()||nanos<0)throw new IOException("Invalid result dimensions/counters");
        double[] values=new double[n];for(int i=0;i<n;i++){values[i]=in.readDouble();if(Double.isInfinite(values[i]))throw new IOException("Infinite numerical result");}long[] reasons=new long[6];long sum=0;for(int i=0;i<6;i++){reasons[i]=in.readLong();if(reasons[i]<0||reasons[i]>n)throw new IOException("Invalid classification");sum+=reasons[i];}if(sum!=n)throw new IOException("Incorrect sample count");NetworkProtocol.end(in);
        return new RenderEngine.Tile(task.x(),task.y(),task.width(),task.height(),values,0,0,iterations,nanos,request.grid(),1,reasons);
    }
    private TaskCodec(){}
}
