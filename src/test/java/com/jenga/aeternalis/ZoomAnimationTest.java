package com.jenga.aeternalis;

import com.jenga.aeternalis.animation.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.Viewport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;

class ZoomAnimationTest {
    @TempDir Path dir;
    private ZoomJob job(){var d=StudioDocument.initial();return new ZoomJob(d.replace(d.selected().view(new Viewport(-.7,.1,.875))),Viewport.HOME,40,24,new BigDecimal("0.5"),25);}
    @Test void exactGeometricWidthsAndEndpoint(){
        var d=StudioDocument.initial();var target=Viewport.parse("-.743643887037151",".13182590420533","0.0000000000001");
        var j=new ZoomJob(d.replace(d.selected().view(target)),Viewport.parse("-.5","0","4"),40,24,new BigDecimal("0.9"),30);
        assertEquals(299,j.frames());assertEquals(new BigDecimal("3.6"),j.viewport(1).spanExact());assertEquals(new BigDecimal("3.24"),j.viewport(2).spanExact());assertEquals(target,j.viewport(j.frames()-1));assertEquals(j.start(),j.viewport(0));
        for(int i=1;i<j.frames();i++)assertTrue(j.viewport(i).spanExact().compareTo(j.viewport(i-1).spanExact())<0);
    }
    @Test void xmlRoundTripPreservesDeepCoordinatesAndAllLayers()throws Exception{
        var d=StudioDocument.initial();var v=Viewport.parse("-.7436438870371511234567890123456789",".1318259042053300000000000001","1e-80");var j=new ZoomJob(d.replace(d.selected().view(v)),Viewport.HOME,1920,1080,new BigDecimal(".9"),60);Path xml=dir.resolve("zoom.xml");ZoomJobIO.save(xml,j);assertEquals(j,ZoomJobIO.load(xml));assertTrue(Files.readString(xml).contains("destination"));
    }
    @Test void rejectsUnsafeXmlAndInvalidSettings()throws Exception{
        Path bad=dir.resolve("bad.xml");Files.writeString(bad,"<!DOCTYPE x [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><aeternalis-zoom>&x;</aeternalis-zoom>");assertThrows(java.io.IOException.class,()->ZoomJobIO.load(bad));
        assertThrows(IllegalArgumentException.class,()->new ZoomJob(StudioDocument.initial(),Viewport.HOME,40,24,BigDecimal.ONE,30));
    }
    @Test void pauseResumeRepairsCorruptionAndKeepsCompletedFrames()throws Exception{
        Path xml=dir.resolve("zoom.xml");ZoomJobIO.save(xml,job());
        try(var s=new ZoomSequence()){assertThrows(CancellationException.class,()->s.run(xml,p->{if(p.message().startsWith("Saved frame 1"))s.pause();}));}
        Path first=dir.resolve(ZoomJob.filename(0));byte[] original=Files.readAllBytes(first);assertFalse(Files.exists(dir.resolve(ZoomJob.filename(1))));
        Files.writeString(dir.resolve(ZoomJob.filename(1)),"crash partial");
        try(var s=new ZoomSequence()){s.run(xml,p->{});}assertArrayEquals(original,Files.readAllBytes(first));assertEquals(3,SequenceEncoder.frames(dir).size());
        var result=new CompletableFuture<com.jenga.aeternalis.rendering.DocumentRenderer.Update>();
        try(var reference=new com.jenga.aeternalis.rendering.DocumentRenderer()){
            reference.render(job().frame(2),40,24,u->{if(u.complete())result.complete(u);},result::completeExceptionally);
            var image=ImageIO.read(dir.resolve(ZoomJob.filename(2)).toFile());assertArrayEquals(result.get(10,TimeUnit.SECONDS).pixels(),image.getRGB(0,0,40,24,null,0,40));
        }
        for(int i=0;i<3;i++)assertTrue(ZoomSequence.validFrame(dir.resolve(ZoomJob.filename(i)),40,24));
    }
    @Test void changedJobDoesNotReuseOldFrames()throws Exception{
        Path xml=dir.resolve("zoom.xml");ZoomJobIO.save(xml,job());try(var s=new ZoomSequence()){s.run(xml,p->{});}Files.writeString(xml,Files.readString(xml)+"\n");try(var s=new ZoomSequence()){assertThrows(java.io.IOException.class,()->s.run(xml,p->{}));}
    }
    @Test void gifContainsAllFramesAndLoopMetadata()throws Exception{
        Path xml=dir.resolve("zoom.xml");ZoomJobIO.save(xml,job());try(var s=new ZoomSequence()){s.run(xml,p->{});}Path gif=dir.resolve("movie.gif");SequenceEncoder.gif(dir,gif,25,new AtomicBoolean(),p->{});
        var reader=ImageIO.getImageReadersByFormatName("gif").next();try(var in=ImageIO.createImageInputStream(gif.toFile())){reader.setInput(in);assertEquals(3,reader.getNumImages(true));assertEquals(40,reader.getWidth(0));String tree=reader.getImageMetadata(0).getAsTree("javax_imageio_gif_image_1.0").getNodeName();assertNotNull(tree);}finally{reader.dispose();}
    }
    @Test void incompleteAndCancelledEncodingCannotPublishOutput()throws Exception{
        Path xml=dir.resolve("zoom.xml");ZoomJobIO.save(xml,job());assertThrows(java.io.IOException.class,()->SequenceEncoder.frames(dir));try(var s=new ZoomSequence()){s.run(xml,p->{});}Path gif=dir.resolve("cancelled.gif");assertThrows(CancellationException.class,()->SequenceEncoder.gif(dir,gif,25,new AtomicBoolean(true),p->{}));assertFalse(Files.exists(gif));Files.delete(dir.resolve(ZoomJob.filename(1)));assertThrows(java.io.IOException.class,()->SequenceEncoder.frames(dir));
    }
    @Test void actualMp4WithProjectFfmpegWithoutPath()throws Exception{
        Path xml=dir.resolve("zoom.xml");ZoomJobIO.save(xml,job());try(var s=new ZoomSequence()){s.run(xml,p->{});}Path mp4=dir.resolve("movie.mp4");SequenceEncoder.mp4(dir,mp4,25,"bundled",new AtomicBoolean(),p->{});assertTrue(Files.size(mp4)>100);byte[] bytes=Files.readAllBytes(mp4);assertEquals("ftyp",new String(bytes,4,4,java.nio.charset.StandardCharsets.US_ASCII));
    }
}
