package com.jenga.aeternalis.ui;
import com.jenga.aeternalis.colouring.Gradient;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/** Read-only channel curves driven by the actual active cubic gradient. */
public final class GradientCurves extends VBox {
    private final Canvas plot=new Canvas(400,360);private Gradient gradient;
    public GradientCurves(Gradient gradient){
        this.gradient=gradient;setSpacing(10);getStyleClass().add("panel");
        Label title=new Label("Gradient channel curves");title.getStyleClass().add("section-title");
        Label note=new Label("The red, green and blue curves show the actual cubic interpolation. Add colours and move stops in the controls to the right.");note.setWrapText(true);
        getChildren().addAll(title,plot,note);plot.widthProperty().bind(widthProperty().subtract(24));plot.widthProperty().addListener((a,b,c)->draw());draw();
    }
    public void setGradient(Gradient gradient){this.gradient=gradient;draw();}
    private void draw(){
        var g=plot.getGraphicsContext2D();double w=plot.getWidth();if(w<1)return;
        Color[] channels={Color.web("#f19c83"),Color.web("#9fcaa6"),Color.web("#91b8e1")};
        for(int channel=0;channel<3;channel++){
            double top=channel*120;g.setFill(Color.web("#253039"));g.fillRect(0,top,w,116);
            g.setStroke(Color.web("#475258"));g.setLineWidth(.5);
            for(int j=1;j<4;j++)g.strokeLine(0,top+j*28,w,top+j*28);
            for(var stop:gradient.stops()){double x=stop.position()*(w-1);g.strokeLine(x,top,x,top+115);}
            g.setStroke(channels[channel]);g.setLineWidth(1.7);g.beginPath();
            for(int x=0;x<(int)w;x++){double y=top+109-Gradient.srgb(gradient.sample(x/Math.max(1,w-1))[channel])/255.*100;if(x==0)g.moveTo(x,y);else g.lineTo(x,y);}g.stroke();
            g.setFill(channels[channel]);g.fillText(new String[]{"RED","GREEN","BLUE"}[channel],8,top+16);
        }
    }
}
