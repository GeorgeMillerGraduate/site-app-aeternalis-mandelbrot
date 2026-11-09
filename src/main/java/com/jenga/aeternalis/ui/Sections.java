package com.jenga.aeternalis.ui;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
/** Shared compact inspector groups. */
public final class Sections {
    private Sections(){}
    public static TitledPane group(String title,boolean expanded,Node... controls){VBox body=new VBox(8,controls);body.getStyleClass().add("parameter-group");TitledPane pane=new TitledPane(title,body);pane.setExpanded(expanded);pane.setAnimated(false);return pane;}
}
