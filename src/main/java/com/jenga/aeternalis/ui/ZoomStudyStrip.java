package com.jenga.aeternalis.ui;
import com.jenga.aeternalis.app.ZoomStudies;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.geometry.Pos;
import javafx.stage.Window;
import java.util.*;
import java.util.function.Consumer;
/** Offline image destinations, with exact coordinates and license information attached to every card. */
public final class ZoomStudyStrip extends VBox {
    private final Map<Integer,ToggleButton> cards=new HashMap<>();
    public ZoomStudyStrip(Consumer<ZoomStudies.Study> select,Consumer<String> open,Window owner,Runnable hide){
        getStyleClass().add("study-gallery");setSpacing(5);
        Label heading=new Label("THE MANDELBROT ATLAS");heading.getStyleClass().add("atlas-title");
        Label guide=new Label("Widest → deepest · select a study to render");guide.getStyleClass().add("muted");
        Region spring=new Region();HBox.setHgrow(spring,Priority.ALWAYS);
        Hyperlink credits=new Hyperlink("Wolfgang Beyer · CC BY-SA 3.0");credits.setOnAction(e->credits(owner,open));
        Button close=new Button("×");close.setId("atlas-close");close.setAccessibleText("Hide Mandelbrot atlas");close.setTooltip(new Tooltip("Hide atlas · restore with View → Mandelbrot atlas"));close.setOnAction(e->hide.run());close.getStyleClass().add("atlas-close");
        HBox header=new HBox(12,heading,guide,spring,credits,close);header.setAlignment(Pos.CENTER_LEFT);
        HBox thumbnails=new HBox(8);ToggleGroup group=new ToggleGroup();
        for(var study:ZoomStudies.all()){
            ImageView thumbnail=new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(study.image()))));thumbnail.setFitWidth(108);thumbnail.setFitHeight(81);thumbnail.setPreserveRatio(true);
            Label label=new Label(String.format(Locale.ROOT,"%02d · %s",study.index(),study.title()));label.setMaxWidth(110);label.getStyleClass().add("study-caption");
            VBox content=new VBox(4,thumbnail,label);content.setAlignment(Pos.CENTER_LEFT);
            ToggleButton card=new ToggleButton();card.setGraphic(content);card.setToggleGroup(group);card.setId("wiki-study-"+study.index());card.getStyleClass().add("study-card");card.setAccessibleText("Render Wikipedia zoom "+study.index()+": "+study.title());
            card.setTooltip(new Tooltip(study.title()+"\nRe: "+study.real()+"\nIm: "+study.imaginary()+"\nHorizontal span: "+study.span()+"\nPublished 4:3 view; current canvas aspect ratio is retained.\n"+ZoomStudies.AUTHOR+" · "+ZoomStudies.LICENSE+"\nRecommended iterations: "+study.iterations()+" (Aeternalis preset)"));
            card.setOnAction(e->{card.setSelected(true);select.accept(study);});
            MenuItem source=new MenuItem("Image details & original coordinates");source.setOnAction(e->open.accept(study.source()));
            MenuItem license=new MenuItem("Image license · CC BY-SA 3.0");license.setOnAction(e->open.accept(ZoomStudies.LICENSE_URL));card.setContextMenu(new ContextMenu(source,license));
            cards.put(study.index(),card);thumbnails.getChildren().add(card);
        }
        ScrollPane scroll=new ScrollPane(thumbnails);scroll.setFitToHeight(true);scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);scroll.setPrefHeight(125);scroll.setMinHeight(125);scroll.setMaxHeight(125);
        getChildren().addAll(header,scroll);
    }
    public void selected(int index){ToggleButton b=cards.get(index);if(b!=null)b.setSelected(true);}
    public static void credits(Window owner,Consumer<String> open){
        Alert dialog=new Alert(Alert.AlertType.INFORMATION);dialog.initOwner(owner);dialog.setTitle("Aeternalis · Image credits");dialog.setHeaderText("The Mandelbrot atlas · Wolfgang Beyer");
        VBox body=new VBox(10,new Label("Images created by Wolfgang Beyer using Ultra Fractal 3."));
        Label note=new Label("Reused under Creative Commons Attribution–ShareAlike 3.0. Bundled images are Wikimedia-generated thumbnails; no other image changes. No endorsement is implied. Coordinates are preserved as published decimal text. Aeternalis uses its own render engine and cubic colour interpolation, so rendered colours may differ from the reference images.");note.setWrapText(true);body.getChildren().add(note);
        Hyperlink license=new Hyperlink("Read CC BY-SA 3.0");license.setOnAction(e->open.accept(ZoomStudies.LICENSE_URL));body.getChildren().add(license);
        for(var s:ZoomStudies.all()){Hyperlink link=new Hyperlink(String.format(Locale.ROOT,"%02d · %s — source & coordinates",s.index(),s.title()));link.setOnAction(e->open.accept(s.source()));body.getChildren().add(link);}
        ScrollPane scroll=new ScrollPane(body);scroll.setFitToWidth(true);scroll.setPrefSize(620,430);dialog.getDialogPane().setContent(scroll);Theme.apply(dialog);dialog.showAndWait();
    }
}
