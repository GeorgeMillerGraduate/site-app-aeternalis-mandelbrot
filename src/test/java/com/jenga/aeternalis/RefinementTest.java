package com.jenga.aeternalis;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.ui.WorkspacePrefs;
import com.jenga.aeternalis.app.BuildInfo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;

class RefinementTest {
    @Test void gradientReverseIsAnInvolution(){var gradient=Palette.HERITAGE.gradient();assertEquals(gradient,GradientOperations.reverse(GradientOperations.reverse(gradient)));}
    @Test void stopOperationsRejectDuplicatesAndKeepEndpoints(){var gradient=Gradient.of(0,0xffffff);var changed=GradientOperations.insert(gradient,.4,0x378bda);assertEquals(3,changed.stops().size());assertEquals(gradient,GradientOperations.remove(changed,1));assertThrows(IllegalArgumentException.class,()->GradientOperations.insert(changed,.4,0));assertThrows(IllegalArgumentException.class,()->GradientOperations.remove(changed,0));}
    @Test void paletteClipboardTextRoundTripsCubicMode()throws Exception{var gradient=Palette.EMBER.gradient().withMode(Gradient.Mode.CATMULL_ROM);assertEquals(gradient,PaletteIO.decode(PaletteIO.encode(gradient)));assertThrows(java.io.IOException.class,()->PaletteIO.decode("invalid"));}
    @Test void workspaceRejectsNonfiniteAndInvalidDimensions(){assertEquals(1440,WorkspacePrefs.Layout.defaults().width());assertThrows(IllegalArgumentException.class,()->new WorkspacePrefs.Layout(Double.NaN,900,true,.7,.6,0,0,false));}
    @Test void aboutVersionMatchesMavenMetadata()throws Exception{String pom=Files.readString(Path.of("pom.xml"));assertTrue(pom.contains("<version>"+BuildInfo.VERSION+"</version>"));}
    @Test void oldProjectsRecoverExactCoordinates()throws Exception{
        for(int i=1;i<=4;i++){
            var document=com.jenga.aeternalis.persistence.ProjectIO.load(Path.of("examples/Example-"+i+".aeternalis"));
            assertFalse(document.layers().isEmpty());
            for(var layer:document.layers())assertNotNull(layer.viewport().spanExact());
        }
    }
}
