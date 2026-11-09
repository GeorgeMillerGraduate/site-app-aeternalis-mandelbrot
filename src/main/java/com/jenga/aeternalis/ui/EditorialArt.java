package com.jenga.aeternalis.ui;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import java.util.*;
/** Small reusable raster illustrations. Controls and text remain native accessible JavaFX nodes. */
public final class EditorialArt {
    private static final Map<String,Image> IMAGES=new HashMap<>();
    public static ImageView image(String asset,double width,double height){
        Image bitmap=IMAGES.computeIfAbsent(asset,n->new Image(Objects.requireNonNull(EditorialArt.class.getResourceAsStream("/com/jenga/aeternalis/assets/editorial-"+n+".png"))));
        ImageView view=new ImageView(bitmap);view.setFitWidth(width);view.setFitHeight(height);view.setPreserveRatio(true);view.setSmooth(true);view.setMouseTransparent(true);return view;
    }
    public static HBox heading(String title,String subtitle){
        Label name=new Label(title);name.getStyleClass().add("section-title");Label sub=new Label(subtitle);sub.getStyleClass().add("muted");sub.setWrapText(true);
        VBox words=new VBox(3,name,sub);HBox.setHgrow(words,Priority.ALWAYS);
        HBox row=new HBox(14,words,image("masthead",210,70));row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().add("editorial-heading");return row;
    }
    private EditorialArt(){}
}
