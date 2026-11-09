package com.jenga.aeternalis.ui;
import com.jenga.aeternalis.app.LayerState;
import com.jenga.aeternalis.colouring.ColourRenderer;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.Viewport;
import com.jenga.aeternalis.rendering.RenderEngine;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Selected-layer reference map. Preview calculation is independent of the main render and never blocks FX. */
public final class OverviewNavigator extends VBox implements AutoCloseable {
    private final Canvas canvas=new Canvas(250,150);private final Label caption=new Label("Selected-layer reference view");
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"aeternalis-navigator");t.setDaemon(true);return t;});
    private final AtomicLong generation=new AtomicLong();private final Consumer<Viewport> navigate;
    private LayerState active;private Viewport reference;private double aspect=1;private WritableImage image;private Key key;
    private record Key(FormulaSettings formula,com.jenga.aeternalis.colouring.ColourSettings colours,double bailout,int limit,int width){}
    public OverviewNavigator(Consumer<Viewport> navigate){
        this.navigate=navigate;getStyleClass().add("panel");setSpacing(8);caption.getStyleClass().add("muted");caption.setWrapText(true);getChildren().addAll(canvas,caption);
        canvas.widthProperty().bind(widthProperty().subtract(24));canvas.widthProperty().addListener((a,b,c)->{if(active!=null)update(active,aspect);});
        canvas.setOnMouseClicked(e->{if(active==null||reference==null)return;int w=Math.max(1,(int)canvas.getWidth());navigate.accept(new Viewport(java.math.BigDecimal.valueOf(reference.realAt(e.getX(),w)),java.math.BigDecimal.valueOf(reference.imaginaryAt(e.getY(),w,150)),active.viewport().spanExact()));});
    }
    public void update(LayerState active,double aspect){
        this.active=active;this.aspect=aspect;int w=Math.max(1,(int)canvas.getWidth());
        reference=switch(active.formula().family()){case JULIA,NEWTON->new Viewport(0,0,3.5);default->Viewport.HOME;};
        draw();Key next=new Key(active.formula(),active.colours(),active.calculation().bailoutSquared(),Math.min(300,active.calculation().limit()),w);
        if(next.equals(key))return;key=next;long id=generation.incrementAndGet();Viewport home=reference;
        worker.submit(()->{
            try{
                var evaluator=active.formula().evaluator(next.bailout(),active.calculation().periodicity());double[] values=new double[w*150];
                for(int y=0;y<150;y++){
                    if(generation.get()!=id)return;
                    for(int x=0;x<w;x++){var v=evaluator.evaluate(home.realAt(x+.5,w),home.imaginaryAt(y+.5,w,150),next.limit(),()->generation.get()!=id);values[y*w+x]=v.colourable()?v.smooth():Double.NaN;}
                }
                var tile=new RenderEngine.Tile(0,0,w,150,values,1,1,0,0);
                int[] pixels=ColourRenderer.render(List.of(tile),w,150,1,active.colours(),()->generation.get()!=id);
                Platform.runLater(()->{if(generation.get()!=id)return;image=new WritableImage(w,150);image.getPixelWriter().setPixels(0,0,w,150,PixelFormat.getIntArgbInstance(),pixels,0,w);draw();});
            }catch(CancellationException ignored){}
        });
    }
    private void draw(){
        var g=canvas.getGraphicsContext2D();double w=canvas.getWidth();g.setFill(Color.web("#102026"));g.fillRect(0,0,w,150);if(image!=null)g.drawImage(image,0,0,w,150);
        if(active==null||reference==null)return;
        Viewport v=active.viewport();double bw=v.span()/reference.span()*w,bh=bw*aspect,cx=(v.real()-reference.real())/reference.span()*w+w/2,cy=75-(v.imaginary()-reference.imaginary())/reference.span()*w;
        g.setStroke(Color.web("#f2d69a"));g.setLineWidth(1.3);g.strokeRect(cx-Math.max(2,bw)/2,cy-Math.max(2,bh)/2,Math.max(2,bw),Math.max(2,bh));
        g.strokeLine(cx-5,cy,cx+5,cy);g.strokeLine(cx,cy-5,cx,cy+5);caption.setText("Selected layer · click to centre the view.\n"+(bw<2?"Deep zoom indicated by the crosshair.":"Gold frame marks the current viewport."));
    }
    public void close(){generation.incrementAndGet();worker.shutdownNow();}
}
