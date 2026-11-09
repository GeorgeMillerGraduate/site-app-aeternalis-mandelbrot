package com.jenga.aeternalis.ui;

import javafx.geometry.Rectangle2D;
import javafx.stage.*;
import javafx.scene.control.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Secondary windows stay within their owner and 90% of that owner's current screen. */
public final class WindowBounds {
    private WindowBounds(){}
    public static Rectangle2D fit(Rectangle2D owner,Rectangle2D screen,double width,double height){
        double left=Math.max(owner.getMinX(),screen.getMinX()),top=Math.max(owner.getMinY(),screen.getMinY());
        double right=Math.min(owner.getMaxX(),screen.getMaxX()),bottom=Math.min(owner.getMaxY(),screen.getMaxY());
        if(right<=left||bottom<=top){left=screen.getMinX();top=screen.getMinY();right=screen.getMaxX();bottom=screen.getMaxY();}
        double w=Math.max(1,Math.min(width,Math.min(right-left,screen.getWidth()*.9))),h=Math.max(1,Math.min(height,Math.min(bottom-top,screen.getHeight()*.9)));
        return new Rectangle2D(left+(right-left-w)/2,top+(bottom-top-h)/2,w,h);
    }
    public static void install(Stage child,Window owner){AppIcon.apply(child);
        if(child.getProperties().putIfAbsent("aeternalis.windowBounds",Boolean.TRUE)!=null)return;
        AtomicBoolean applying=new AtomicBoolean();
        Runnable adjust=()->{
            if(!child.isShowing()||!applying.compareAndSet(false,true))return;
            try{
                Rectangle2D parent=owner!=null&&owner.isShowing()?new Rectangle2D(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight()):Screen.getPrimary().getVisualBounds();
                var screens=Screen.getScreensForRectangle(parent);var screen=(screens.isEmpty()?Screen.getPrimary():screens.getFirst()).getVisualBounds();
                if(!parent.intersects(screen))parent=screen;
                Rectangle2D box=fit(parent,screen,Double.isFinite(child.getWidth())?child.getWidth():800,Double.isFinite(child.getHeight())?child.getHeight():600);
                child.setMinWidth(0);child.setMinHeight(0);child.setMaxWidth(Math.min(parent.getWidth(),screen.getWidth()*.9));child.setMaxHeight(Math.min(parent.getHeight(),screen.getHeight()*.9));
                child.setWidth(box.getWidth());child.setHeight(box.getHeight());
                double left=Math.max(parent.getMinX(),screen.getMinX()),top=Math.max(parent.getMinY(),screen.getMinY());
                double right=Math.min(parent.getMaxX(),screen.getMaxX()),bottom=Math.min(parent.getMaxY(),screen.getMaxY());
                child.setX(Math.max(left,Math.min(child.getX(),right-child.getWidth())));child.setY(Math.max(top,Math.min(child.getY(),bottom-child.getHeight())));
            }finally{applying.set(false);}
        };
        javafx.beans.value.ChangeListener<Number> listener=(a,b,c)->adjust.run();
        child.widthProperty().addListener(listener);child.heightProperty().addListener(listener);child.xProperty().addListener(listener);child.yProperty().addListener(listener);
        if(owner!=null){owner.widthProperty().addListener(listener);owner.heightProperty().addListener(listener);owner.xProperty().addListener(listener);owner.yProperty().addListener(listener);child.addEventHandler(WindowEvent.WINDOW_HIDDEN,e->{owner.widthProperty().removeListener(listener);owner.heightProperty().removeListener(listener);owner.xProperty().removeListener(listener);owner.yProperty().removeListener(listener);});}
        child.addEventHandler(WindowEvent.WINDOW_SHOWN,e->{adjust.run();Rectangle2D parent=owner!=null&&owner.isShowing()?new Rectangle2D(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight()):Screen.getPrimary().getVisualBounds();child.setX(parent.getMinX()+(parent.getWidth()-child.getWidth())/2);child.setY(parent.getMinY()+(parent.getHeight()-child.getHeight())/2);adjust.run();});
        adjust.run();
    }
    public static void install(Dialog<?> dialog){
        dialog.addEventHandler(DialogEvent.DIALOG_SHOWN,e->{
            var pane=dialog.getDialogPane();var content=pane.getContent();if(content!=null&&!(content instanceof ScrollPane)){ScrollPane scroll=new ScrollPane(content);scroll.setFitToWidth(true);scroll.setMinSize(0,0);pane.setContent(scroll);}
            pane.setMinSize(0,0);if(pane.getScene().getWindow() instanceof Stage stage)install(stage,dialog.getOwner());
        });
    }
}
