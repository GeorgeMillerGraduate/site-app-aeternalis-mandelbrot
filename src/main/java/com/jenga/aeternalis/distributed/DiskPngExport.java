package com.jenga.aeternalis.distributed;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.layers.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.network.*;
import com.jenga.aeternalis.animation.ZoomJobIO;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.zip.*;
/** Disk-backed numerical tiles and ARGB layers; PNG is streamed one composited scanline at a time. */
public final class DiskPngExport {
    private DiskPngExport(){}
    public static void render(StudioDocument doc,int width,int height,Path output,AtomicBoolean cancelled,Consumer<String> progress)throws Exception{
        if(width<1||height<1||width>131072||height>131072)throw new IllegalArgumentException("Export dimensions 1–131072 required");
        Path parent=output.toAbsolutePath().getParent();Files.createDirectories(parent);var visible=doc.layers().stream().filter(l->l.visible()&&l.opacity()>0).toList();
        long pixels=Math.multiplyExact((long)width,height),samples=visible.stream().mapToLong(l->(long)l.calculation().grid()*l.calculation().grid()).max().orElse(1);
        long estimate=Math.addExact(Math.multiplyExact(pixels,4L*visible.size()),Math.multiplyExact(pixels,8*samples));if(Files.getFileStore(parent).getUsableSpace()<estimate+64_000_000)throw new IOException("Insufficient disk space; estimated scratch requirement "+(estimate/1048576)+" MiB");
        Path scratch=Files.createTempDirectory(parent,"aeternalis-tiles-"),temporary=Files.createTempFile(parent,"aeternalis-image-",".png");MasterServer privateServer=null;List<RandomAccessFile> layers=new ArrayList<>();
        try{
            NetworkService service=NetworkService.instance();MasterServer server=service.master();if(server==null||service.mode()==NetworkService.Mode.LOCAL){privateServer=new MasterServer("127.0.0.1",0,"",Math.min(256,Runtime.getRuntime().availableProcessors()));server=privateServer;}
            int layerIndex=0;for(var layer:visible){check(cancelled);var c=layer.calculation();RenderRequest request=NetworkService.explicit(new RenderRequest(layer.viewport(),width,height,c.limit(),c.grid(),c.order(),c.periodicity(),c.bailoutSquared(),false,layer.formula(),c.precision(),c.perturbation()));
                Path tiles=scratch.resolve("samples-"+layerIndex);Files.createDirectory(tiles);AtomicInteger number=new AtomicInteger();int index=++layerIndex;CompletableFuture<Void> finished=new CompletableFuture<>();
                MasterServer selected=server;
                if(service.master()!=null&&!service.use(request)&&service.mode()!=NetworkService.Mode.DISTRIBUTED){if(privateServer==null)privateServer=new MasterServer("127.0.0.1",0,"",Math.min(256,Runtime.getRuntime().availableProcessors()));selected=privateServer;}
                DistributedJob job=selected.render(request,service.tileSize(),service.estimate(request),tile->{try{
                    if(tile.step()<0){try(var files=Files.list(tiles)){for(Path f:files.toList())Files.deleteIfExists(f);}number.set(0);return;}
                    Path file=tiles.resolve(String.format(Locale.ROOT,"%09d.bin",number.getAndIncrement()));try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))){out.writeInt(tile.x());out.writeInt(tile.y());out.writeInt(tile.width());out.writeInt(tile.height());out.writeInt(tile.sampleGrid());for(double v:tile.values())out.writeDouble(v);}
                    progress.accept("Layer "+index+" / "+visible.size()+" · "+String.format(Locale.ROOT,"%.2f%%",100.*tile.completed()/tile.total())+" · "+tile.completed()+" / "+tile.total()+" tiles");if(tile.completed()==tile.total())finished.complete(null);
                }catch(Exception e){throw new UncheckedIOException(new IOException(e));}},finished::completeExceptionally,context->{});
                // The overloaded coordinator callback is deliberately not used; generation resets arrive as step=-1.
                while(!finished.isDone()){if(cancelled.get()){job.cancel();throw new CancellationException();}try{finished.get(100,TimeUnit.MILLISECONDS);}catch(TimeoutException ignored){}}
                finished.get();check(cancelled);List<Path> files;try(var stream=Files.list(tiles)){files=stream.sorted().toList();}
                double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;long count=0;
                if(layer.colours().histogram())for(Path file:files){check(cancelled);for(double value:read(file).values())if(Double.isFinite(value)){min=Math.min(min,value);max=Math.max(max,value);count++;}}
                double[] bins=new double[4096];if(layer.colours().histogram()&&count>0&&min!=max){for(Path file:files){check(cancelled);for(double v:read(file).values())if(Double.isFinite(v))bins[(int)Math.min(4095,(v-min)/(max-min)*4095)]++;}double sum=0;for(int n=0;n<bins.length;n++){sum+=bins[n];bins[n]=sum/count;}}
                ColourSettings colour=layer.colours(),plain=new ColourSettings(colour.gradient(),colour.transfer(),colour.density(),colour.scale(),colour.offset(),colour.power(),colour.repeat(),colour.reverse(),false,colour.interior(),colour.transparentInterior());
                RandomAccessFile argb=new RandomAccessFile(scratch.resolve("layer-"+index+".argb").toFile(),"rw");layers.add(argb);argb.setLength(pixels*4);int coloured=0;
                for(Path file:files){check(cancelled);var tile=read(file);double[] values=tile.values();if(colour.histogram())for(int n=0;n<values.length;n++)if(Double.isFinite(values[n]))values[n]=cdf(values[n],min,max,bins)*colour.scale();
                    var local=new RenderEngine.Tile(0,0,tile.width(),tile.height(),values,0,0,0,0,tile.sampleGrid(),1,new long[6]);int[] colours=ColourRenderer.render(List.of(local),tile.width(),tile.height(),tile.sampleGrid(),plain,cancelled::get);byte[] row=new byte[tile.width()*4];
                    for(int y=0;y<tile.height();y++){for(int x=0;x<tile.width();x++){int v=colours[y*tile.width()+x],at=x*4;row[at]=(byte)(v>>>24);row[at+1]=(byte)(v>>>16);row[at+2]=(byte)(v>>>8);row[at+3]=(byte)v;}argb.seek(((long)(tile.y()+y)*width+tile.x())*4);argb.write(row);}
                    Files.delete(file);if((++coloured&31)==0)progress.accept("Colouring layer "+index+" · "+coloured+" / "+files.size()+" tiles");
                }Files.delete(tiles);
            }
            for(var layer:layers)layer.seek(0);writePng(temporary,width,height,layers,visible,cancelled,progress);check(cancelled);ZoomJobIO.atomicMove(temporary,output.toAbsolutePath());progress.accept("100.00% · Saved "+output);
        }finally{if(privateServer!=null)privateServer.close();for(var layer:layers)try{layer.close();}catch(IOException ignored){}Files.deleteIfExists(temporary);try(var paths=Files.walk(scratch)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
    private static void check(AtomicBoolean c){if(c.get()||Thread.currentThread().isInterrupted())throw new CancellationException();}
    private static RenderEngine.Tile read(Path path)throws IOException{try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))){int x=in.readInt(),y=in.readInt(),w=in.readInt(),h=in.readInt(),grid=in.readInt();double[] values=new double[w*h*grid*grid];for(int n=0;n<values.length;n++)values[n]=in.readDouble();return new RenderEngine.Tile(x,y,w,h,values,0,0,0,0,grid,1,new long[6]);}}
    private static double cdf(double v,double min,double max,double[] bins){if(min==max||!Double.isFinite(min))return .5;if(v>=max)return 1;double p=Math.max(0,Math.min(4095,(v-min)/(max-min)*4095));int i=(int)p;double a=i==0?0:bins[i-1],b=bins[i];return a+(b-a)*(p-i);}
    private static void chunk(DataOutputStream out,String name,byte[] bytes)throws IOException{byte[] type=name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);out.writeInt(bytes.length);out.write(type);out.write(bytes);CRC32 crc=new CRC32();crc.update(type);crc.update(bytes);out.writeInt((int)crc.getValue());}
    private static void writePng(Path file,int width,int height,List<RandomAccessFile> layers,List<LayerState> settings,AtomicBoolean cancelled,Consumer<String> progress)throws IOException{
        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))){
            out.writeLong(0x89504e470d0a1a0aL);ByteArrayOutputStream header=new ByteArrayOutputStream();try(var h=new DataOutputStream(header)){h.writeInt(width);h.writeInt(height);h.write(new byte[]{8,6,0,0,0});}chunk(out,"IHDR",header.toByteArray());
            class Idat extends OutputStream {final byte[] b=new byte[65536];int n;public void write(int value)throws IOException{b[n++]=(byte)value;if(n==b.length)flush();}public void write(byte[] data,int off,int length)throws IOException{while(length>0){int take=Math.min(length,b.length-n);System.arraycopy(data,off,b,n,take);n+=take;off+=take;length-=take;if(n==b.length)flush();}}public void flush()throws IOException{if(n>0){chunk(out,"IDAT",Arrays.copyOf(b,n));n=0;}}}
            Idat idat=new Idat();Deflater deflater=new Deflater(6);try{var zip=new DeflaterOutputStream(idat,deflater,65536);byte[] raw=new byte[width*4+1],argb=new byte[width*4];int[][] rows=new int[layers.size()][width];List<Compositor.Input> inputs=new ArrayList<>();for(int n=0;n<rows.length;n++)inputs.add(new Compositor.Input(rows[n],settings.get(n).opacity(),settings.get(n).blend()));
                for(int y=0;y<height;y++){check(cancelled);for(int l=0;l<layers.size();l++){layers.get(l).readFully(argb);for(int x=0;x<width;x++){int at=x*4;rows[l][x]=(argb[at]&255)<<24|(argb[at+1]&255)<<16|(argb[at+2]&255)<<8|(argb[at+3]&255);}}int[] pixels=Compositor.compose(inputs,width,cancelled::get);raw[0]=0;for(int x=0;x<width;x++){int c=pixels[x],at=1+x*4;raw[at]=(byte)(c>>>16);raw[at+1]=(byte)(c>>>8);raw[at+2]=(byte)c;raw[at+3]=(byte)(c>>>24);}zip.write(raw);if((y&127)==0)progress.accept("Writing PNG · "+String.format(Locale.ROOT,"%.2f%%",100.*y/height));}zip.finish();idat.flush();
            }finally{deflater.end();}chunk(out,"IEND",new byte[0]);
        }
    }
}
