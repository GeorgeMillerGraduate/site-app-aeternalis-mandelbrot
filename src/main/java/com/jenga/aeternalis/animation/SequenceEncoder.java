package com.jenga.aeternalis.animation;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;

/** Streaming built-in GIF writer; optional external FFmpeg for YouTube-compatible H.264 MP4. */
public final class SequenceEncoder {
    private SequenceEncoder(){}
    public static List<Path> frames(Path folder)throws IOException{
        List<Path> frames;try(var stream=Files.list(folder)){frames=stream.filter(p->p.getFileName().toString().matches("frame-\\d{6}\\.png")).sorted().toList();}
        if(frames.isEmpty())throw new IOException("No frame-000000.png sequence found.");
        for(int i=0;i<frames.size();i++)if(!frames.get(i).getFileName().toString().equals(ZoomJob.filename(i)))throw new IOException("Missing frame "+ZoomJob.filename(i)+". Finish/resume rendering first.");
        Path xml=folder.resolve("zoom.xml");if(Files.exists(xml)&&ZoomJobIO.load(xml).frames()!=frames.size())throw new IOException("The zoom job is not complete. Resume rendering before encoding.");
        return frames;
    }
    private static Path target(Path output,String extension)throws IOException{
        Path path=output.toAbsolutePath();if(!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))throw new IOException("Output must have a "+extension+" extension.");return path;
    }
    private static void check(AtomicBoolean cancel){if(cancel.get()||Thread.currentThread().isInterrupted())throw new CancellationException();}
    public static void gif(Path folder,Path output,int fps,AtomicBoolean cancel,Consumer<String> progress)throws Exception{
        if(fps<1||fps>100)throw new IllegalArgumentException("GIF supports 1–100 fps; timing is rounded to hundredths of a second.");
        List<Path> files=frames(folder);Path target=target(output,".gif");Files.createDirectories(target.getParent());Path tmp=Files.createTempFile(target.getParent(),"animation-",".gif");
        ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(ImageOutputStream out=ImageIO.createImageOutputStream(tmp.toFile())){
            writer.setOutput(out);writer.prepareWriteSequence(null);int width=0,height=0;
            for(int i=0;i<files.size();i++){
                check(cancel);BufferedImage source=ImageIO.read(files.get(i).toFile());if(source==null)throw new IOException("Invalid PNG: "+files.get(i));
                if(i==0){width=source.getWidth();height=source.getHeight();}if(source.getWidth()!=width||source.getHeight()!=height)throw new IOException("Frame dimensions differ.");
                BufferedImage rgb=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var g=rgb.createGraphics();g.setColor(java.awt.Color.BLACK);g.fillRect(0,0,width,height);g.drawImage(source,0,0,null);g.dispose();
                var param=writer.getDefaultWriteParam();IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(rgb),param);
                String format=metadata.getNativeMetadataFormatName();IIOMetadataNode root=(IIOMetadataNode)metadata.getAsTree(format),control=node(root,"GraphicControlExtension");
                control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");
                int delay=(int)Math.round((i+1)*100.0/fps)-(int)Math.round(i*100.0/fps);control.setAttribute("delayTime",""+Math.max(1,delay));control.setAttribute("transparentColorIndex","0");
                if(i==0){IIOMetadataNode app=new IIOMetadataNode("ApplicationExtension");app.setAttribute("applicationID","NETSCAPE");app.setAttribute("authenticationCode","2.0");app.setUserObject(new byte[]{1,0,0});node(root,"ApplicationExtensions").appendChild(app);}
                metadata.setFromTree(format,root);writer.writeToSequence(new IIOImage(rgb,null,metadata),param);progress.accept("GIF frame "+(i+1)+" / "+files.size());
            }
            writer.endWriteSequence();out.flush();check(cancel);
        }catch(Exception e){Files.deleteIfExists(tmp);throw e;}finally{writer.dispose();}
        try{ZoomJobIO.atomicMove(tmp,target);}finally{Files.deleteIfExists(tmp);}
    }
    private static IIOMetadataNode node(IIOMetadataNode root,String name){for(int i=0;i<root.getLength();i++)if(root.item(i).getNodeName().equals(name))return (IIOMetadataNode)root.item(i);var n=new IIOMetadataNode(name);root.appendChild(n);return n;}
    public static void mp4(Path folder,Path output,int fps,String executable,AtomicBoolean cancel,Consumer<String> progress)throws Exception{
        if(fps<1||fps>120)throw new IllegalArgumentException("Use 1–120 fps.");List<Path> files=frames(folder);
        // Decode validation happens before external encoding, one image at a time.
        int width=0,height=0;for(var file:files){check(cancel);var image=ImageIO.read(file.toFile());if(image==null)throw new IOException("Invalid PNG "+file);if(width==0){width=image.getWidth();height=image.getHeight();}if(width!=image.getWidth()||height!=image.getHeight())throw new IOException("Frame dimensions differ.");}
        Path target=target(output,".mp4");Files.createDirectories(target.getParent());Path tmp=Files.createTempFile(target.getParent(),"animation-",".mp4"),log=Files.createTempFile("aeternalis-ffmpeg-",".log");Process process=null;
        try{
            String runtime=executable==null||executable.isBlank()||executable.equals("bundled")||executable.equals("ffmpeg")?FfmpegRuntime.executable():executable;
            List<String> command=List.of(runtime,"-hide_banner","-loglevel","error","-nostdin","-y","-framerate",""+fps,"-start_number","0","-i",folder.toAbsolutePath().resolve("frame-%06d.png").toString(),"-frames:v",""+files.size(),"-vf","pad=ceil(iw/2)*2:ceil(ih/2)*2:color=black","-c:v","libx264","-preset","medium","-crf","18","-pix_fmt","yuv420p","-movflags","+faststart",tmp.toString());
            progress.accept("Encoding H.264 MP4 · "+files.size()+" frames");process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            while(!process.waitFor(150,TimeUnit.MILLISECONDS))check(cancel);check(cancel);
            if(process.exitValue()!=0){String error=Files.readString(log);throw new IOException("FFmpeg failed: "+error.substring(0,Math.min(4000,error.length())));}
            ZoomJobIO.atomicMove(tmp,target);progress.accept("MP4 saved");
        }catch(IOException e){throw new IOException("MP4 export could not use the project FFmpeg runtime. Reload Maven dependencies. "+e.getMessage(),e);}
        finally{if(process!=null&&process.isAlive()){process.destroy();if(!process.waitFor(1,TimeUnit.SECONDS))process.destroyForcibly();}Files.deleteIfExists(tmp);Files.deleteIfExists(log);}
    }
}
