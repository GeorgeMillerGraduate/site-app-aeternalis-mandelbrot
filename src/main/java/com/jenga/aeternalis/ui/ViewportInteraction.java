package com.jenga.aeternalis.ui;
import com.jenga.aeternalis.math.SelectionBox;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.Locale;
import java.util.function.DoubleSupplier;

/** Image-space mouse interaction with an aspect-locked, non-intercepting selection overlay. */
public final class ViewportInteraction {
    public enum Mode { SELECT,PAN }
    public interface Actions {
        void zoom(double factor,double x,double y);
        void pan(double dx,double dy);
        void select(SelectionBox box);
        void pointer(double x,double y);
    }
    private final StackPane canvas;private final DoubleSupplier width,height;private final Actions actions;
    private final Rectangle rectangle=new Rectangle();private final Label label=new Label();
    private Mode mode=Mode.SELECT;private double startX,startY;private boolean active,panning,dragged;
    private SelectionBox box;
    public ViewportInteraction(StackPane canvas,DoubleSupplier width,DoubleSupplier height,Actions actions){
        this.canvas=canvas;this.width=width;this.height=height;this.actions=actions;
        Pane overlay=new Pane(rectangle,label);overlay.setMouseTransparent(true);rectangle.setManaged(false);label.setManaged(false);
        rectangle.setFill(Color.web("#9cd8e5",.16));rectangle.setStroke(Color.web("#f5d995"));rectangle.setStrokeWidth(1.5);rectangle.getStrokeDashArray().setAll(7.,4.);
        label.getStyleClass().add("selection-label");
        canvas.getChildren().add(overlay);hide();setMode(Mode.SELECT);
        canvas.setOnMousePressed(e->{
            if(e.getButton()!=MouseButton.PRIMARY&&e.getButton()!=MouseButton.MIDDLE)return;
            startX=clamp(e.getX()-8,width.getAsDouble());startY=clamp(e.getY()-8,height.getAsDouble());active=true;dragged=false;
            panning=mode==Mode.PAN||e.getButton()==MouseButton.MIDDLE||e.isShiftDown();hide();e.consume();
        });
        canvas.setOnMouseDragged(e->{
            if(!active)return;double x=e.getX()-8,y=e.getY()-8;dragged=Math.hypot(x-startX,y-startY)>=5;
            if(!panning&&dragged){box=SelectionBox.fit(startX,startY,x,y,width.getAsDouble(),height.getAsDouble());
                rectangle.setX(box.x());rectangle.setY(box.y());rectangle.setWidth(box.width());rectangle.setHeight(box.height());rectangle.setVisible(true);
                label.setText(String.format(Locale.ROOT,"Zoom %.2f×",width.getAsDouble()/Math.max(1,box.width())));label.relocate(box.x()+5,box.y()+5);label.autosize();label.setVisible(true);
            }e.consume();
        });
        canvas.setOnMouseReleased(e->{
            if(!active)return;active=false;
            double dx=e.getX()-8-startX,dy=e.getY()-8-startY;
            if(Math.hypot(dx,dy)>=5){
                if(panning)actions.pan(dx,dy);
                else if(box!=null&&box.width()>=6&&box.height()>=6)actions.select(box);
            }hide();box=null;e.consume();
        });
        canvas.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()==2&&!dragged)actions.zoom(.5,e.getX()-8,e.getY()-8);});
        canvas.setOnScroll(e->{cancelSelection();actions.zoom(e.getDeltaY()>0?.8:1.25,e.getX()-8,e.getY()-8);e.consume();});
        canvas.setOnMouseMoved(e->actions.pointer(e.getX()-8,e.getY()-8));
    }
    private static double clamp(double v,double max){return Math.max(0,Math.min(max,v));}
    private void hide(){rectangle.setVisible(false);label.setVisible(false);}
    public void setMode(Mode mode){cancelSelection();this.mode=mode;canvas.setCursor(mode==Mode.SELECT?Cursor.CROSSHAIR:Cursor.HAND);}
    public Mode mode(){return mode;}
    public boolean selectionVisible(){return rectangle.isVisible();}
    public boolean cancelSelection(){boolean existed=active||rectangle.isVisible();active=false;box=null;hide();return existed;}
}
