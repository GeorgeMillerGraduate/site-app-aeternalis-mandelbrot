package com.jenga.aeternalis;
import com.jenga.aeternalis.ui.WindowBounds;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WindowBoundsTest {
    @Test void capsAtNinetyPercentOfScreen(){var b=WindowBounds.fit(new Rectangle2D(0,0,1920,1080),new Rectangle2D(0,0,1920,1040),4000,3000);assertEquals(1728,b.getWidth());assertEquals(936,b.getHeight());}
    @Test void smallerOwnerKeepsChildInsideMainWindow(){var owner=new Rectangle2D(150,100,700,450);var b=WindowBounds.fit(owner,new Rectangle2D(0,0,1920,1040),900,720);assertEquals(700,b.getWidth());assertEquals(450,b.getHeight());assertTrue(owner.contains(b));}
    @Test void negativeMonitorCoordinatesAndOffscreenOwner(){var screen=new Rectangle2D(-1600,0,1600,860);var owner=new Rectangle2D(-1500,40,1300,800);assertTrue(screen.contains(WindowBounds.fit(owner,screen,2000,2000)));var fallback=WindowBounds.fit(new Rectangle2D(9000,0,500,500),screen,900,800);assertTrue(screen.contains(fallback));}
}
