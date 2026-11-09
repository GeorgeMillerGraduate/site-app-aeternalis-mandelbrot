package com.jenga.aeternalis.ui;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;

/** Three-second illustrated opening. The bar is a timed introduction, not a fake loading percentage. */
public final class OpeningSplash {
    public static final Duration DISPLAY_TIME=Duration.seconds(3);
    private final Stage window=new Stage(StageStyle.UNDECORATED);
    private final ProgressBar progress=new ProgressBar(0);
    private final Timeline timer;
    public OpeningSplash(Runnable finished){AppIcon.apply(window);
        Image artwork=new Image(java.util.Objects.requireNonNull(getClass().getResourceAsStream("/com/jenga/aeternalis/assets/opening-illustration.png")));
        Rectangle2D screen=Screen.getPrimary().getVisualBounds();double height=Math.min(740,screen.getHeight()*.82-58),width=height*artwork.getWidth()/artwork.getHeight();
        ImageView image=new ImageView(artwork);image.setPreserveRatio(true);image.setFitHeight(height);image.setFitWidth(width);
        progress.setMaxWidth(Double.MAX_VALUE);progress.setPrefHeight(9);progress.getStyleClass().add("opening-progress");
        progress.setPrefHeight(7);progress.setMinHeight(7);progress.setMaxHeight(7);progress.setPrefWidth(width-16);
        StackPane body=new StackPane(image,progress);StackPane.setAlignment(progress,Pos.BOTTOM_CENTER);StackPane.setMargin(progress,new Insets(0,8,height*.009,8));body.getStyleClass().add("opening-splash");
        Scene scene=new Scene(body);Theme.apply(scene);window.setScene(scene);window.setTitle("Aeternalis · Opening the observatory");window.setResizable(false);
        timer=new Timeline(new KeyFrame(Duration.ZERO,new KeyValue(progress.progressProperty(),0)),new KeyFrame(DISPLAY_TIME,new KeyValue(progress.progressProperty(),1,Interpolator.LINEAR)));
        timer.setOnFinished(e->{finished.run();window.close();});
        scene.setOnKeyPressed(e->{if(e.getCode()==javafx.scene.input.KeyCode.ESCAPE){close();javafx.application.Platform.exit();}});
        window.setOnCloseRequest(e->{timer.stop();javafx.application.Platform.exit();});
    }
    public void show(){window.show();window.centerOnScreen();timer.playFromStart();}
    public void close(){timer.stop();window.close();}
    public Stage window(){return window;}
    public double progress(){return progress.getProgress();}
}
