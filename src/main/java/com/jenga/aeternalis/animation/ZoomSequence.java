package com.jenga.aeternalis.animation;

import com.jenga.aeternalis.rendering.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import java.nio.channels.*;

/** One frame at a time. PNG rename precedes checkpoint; restart reconciles decoded PNGs, never trusts a counter. */
public final class ZoomSequence implements AutoCloseable {
    public record Progress(int completed,int total,double frameProgress,String message){}
    private final DocumentRenderer renderer=new DocumentRenderer();private final AtomicBoolean paused=new AtomicBoolean();
    public void pause(){paused.set(true);renderer.cancel();}
    public void run(Path xml,Consumer<Progress> progress)throws Exception{
        ZoomJob job=ZoomJobIO.load(xml);Path folder=xml.toAbsolutePath().getParent();
        try(FileChannel channel=FileChannel.open(folder.resolve(".zoom.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.tryLock()){
            if(lock==null)throw new IOException("This zoom folder is already rendering in another process.");
            String fingerprint=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(xml)));
            Path identity=folder.resolve("frames-job.sha256");
            if(Files.exists(identity)&&!Files.readString(identity).trim().equals(fingerprint))throw new IOException("XML changed since frames were rendered. Use a new empty folder for the changed job.");
            if(!Files.exists(identity)){
                try(var files=Files.list(folder)){if(files.anyMatch(p->p.getFileName().toString().matches("frame-\\d{6}\\.png")))throw new IOException("Existing frames have no job identity. Use an empty folder.");}
                Files.writeString(identity,fingerprint+"\n",StandardOpenOption.CREATE_NEW);
            }
            int total=job.frames();
            for(int index=0;index<total;index++){
                if(paused.get())throw new CancellationException();Path png=folder.resolve(ZoomJob.filename(index));
                if(validFrame(png,job.width(),job.height())){progress.accept(new Progress(index+1,total,0,"Verified frame "+(index+1)));continue;}
                final int current=index;AtomicReference<DocumentRenderer.Update> complete=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();CountDownLatch finished=new CountDownLatch(1);
                renderer.render(job.frame(index),job.width(),job.height(),u->{progress.accept(new Progress(current,total,u.progress(),"Rendering frame "+(current+1)+" / "+total));if(u.complete()){complete.set(u);finished.countDown();}},e->{failure.set(e);finished.countDown();});
                while(!finished.await(100,TimeUnit.MILLISECONDS))if(paused.get())throw new CancellationException();
                if(paused.get())throw new CancellationException();if(failure.get()!=null)throw new IOException("Frame failed",failure.get());
                var result=complete.get();BufferedImage image=new BufferedImage(job.width(),job.height(),BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,job.width(),job.height(),result.pixels(),0,job.width());
                Path tmp=Files.createTempFile(folder,"frame-",".tmp");try{if(!ImageIO.write(image,"png",tmp.toFile()))throw new IOException("No PNG writer");ZoomJobIO.atomicMove(tmp,png);}finally{Files.deleteIfExists(tmp);}
                // Advisory only; frames are always validated on resume, including a crash between PNG and checkpoint.
                Path checkpoint=folder.resolve("progress.txt"),temp=Files.createTempFile(folder,"progress-",".tmp");try{Files.writeString(temp,"completed="+(index+1)+"\ntotal="+total+"\n");ZoomJobIO.atomicMove(temp,checkpoint);}finally{Files.deleteIfExists(temp);}
                progress.accept(new Progress(index+1,total,0,"Saved frame "+(index+1)+" / "+total));
            }
        }
    }
    public static boolean validFrame(Path file,int width,int height){try{if(!Files.isRegularFile(file))return false;var image=ImageIO.read(file.toFile());return image!=null&&image.getWidth()==width&&image.getHeight()==height;}catch(IOException|RuntimeException ex){return false;}}
    public void close(){pause();renderer.close();}
}
