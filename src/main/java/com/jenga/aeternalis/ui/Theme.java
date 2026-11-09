package com.jenga.aeternalis.ui;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import java.util.*;
/** Shared styles for the main studio, palettes and secondary dialogs. */
public final class Theme {
    private Theme(){}
    public static void apply(Scene scene){for(String file:List.of("tokens.css","base.css","controls.css","workspace.css","dialogs.css"))scene.getStylesheets().add(Objects.requireNonNull(Theme.class.getResource("styles/"+file)).toExternalForm());}
    public static void apply(Dialog<?> dialog){WindowBounds.install(dialog);for(String file:List.of("tokens.css","base.css","controls.css","workspace.css","dialogs.css"))dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(Theme.class.getResource("styles/"+file)).toExternalForm());dialog.getDialogPane().getStyleClass().add("aeternalis-dialog");}
}
