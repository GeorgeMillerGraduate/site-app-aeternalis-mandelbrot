package com.jenga.aeternalis;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.Viewport;
import com.jenga.aeternalis.persistence.ProjectIO;
import com.jenga.aeternalis.ui.IterationInput;
import com.jenga.aeternalis.colouring.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
public class EditorialGalleryTest {
    @TempDir Path temp;
    @Test void completeOfflineSequenceRunsWidestToDeepest()throws Exception{
        var all=ZoomStudies.all();assertEquals(15,all.size());BigDecimal previous=new BigDecimal("4");
        for(var s:all){assertTrue(previous.compareTo(s.viewport().spanExact())>0);previous=s.viewport().spanExact();assertTrue(s.source().startsWith("https://commons.wikimedia.org/wiki/File:Mandel_zoom_"));try(var in=getClass().getResourceAsStream(s.image())){var image=ImageIO.read(in);assertNotNull(image);assertEquals(120,image.getWidth());assertEquals(90,image.getHeight());}}
        assertEquals(new BigDecimal("3.0769"),all.getFirst().viewport().spanExact());
    }
    @Test void finalIslandPublishedValuesAreExact(){var s=ZoomStudies.all().getLast();assertEquals("-0.743643887037151",s.real());assertEquals("0.131825904205330",s.imaginary());assertEquals("0.000000000051299",s.span());assertEquals(0,new BigDecimal(s.imaginary()).compareTo(s.viewport().imaginaryExact()));}
    @Test void everyPublishedDestinationSurvivesProjectRoundTrip()throws Exception{for(var s:ZoomStudies.all()){var d=StudioDocument.initial();d=d.replace(d.selected().view(s.viewport()));Path path=temp.resolve(s.index()+".aeternalis");ProjectIO.save(path,d);assertEquals(s.viewport(),ProjectIO.load(path).selected().viewport());}}
    @Test void longTypedCoordinateNeverRoundTripsThroughDouble()throws Exception{
        var v=Viewport.parse("-0.743643887037151123456789012345678901234567890123456789","0.131825904205330987654321098765432109876543210987654321","1.234567890123456789012345678901234567890123456789E-60");
        var d=StudioDocument.initial();d=d.replace(d.selected().view(v));Path path=temp.resolve("exact.aeternalis");ProjectIO.save(path,d);var loaded=ProjectIO.load(path).selected().viewport();assertEquals(v,loaded);assertNotEquals(BigDecimal.valueOf(v.real()),loaded.realExact());assertEquals(new BigDecimal("1.234567890123456789012345678901234567890123456789E-60"),loaded.spanExact());
    }
    @Test void typedIterationCountsValidateWithoutClamping(){assertEquals(32768,IterationInput.parse("32768"));assertEquals(100000,IterationInput.parse(" 100000 "));for(String invalid:new String[]{"31","100001","2.5","abc","","2147483648","-1"})assertThrows(IllegalArgumentException.class,()->IterationInput.parse(invalid));}
    @Test void defaultPaletteIsCubicBlueGoldWithBlackInterior(){var c=StudioDocument.initial().selected().colours();assertEquals(Palette.WIKIPEDIA.gradient().stops(),c.gradient().stops());assertEquals(Gradient.Mode.CATMULL_ROM,c.gradient().mode());assertEquals(0,c.interior());}
}
