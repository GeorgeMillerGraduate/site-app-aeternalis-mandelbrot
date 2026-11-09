package com.jenga.aeternalis.ui;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import java.util.*;
/** Original 18-unit outline icons. SVG paths remain sharp under JavaFX output scaling. */
public final class Icons {
    private Icons(){}
    private static final Map<String,String> PATHS=Map.ofEntries(
        Map.entry("new","M4 2 H11 L15 6 V16 H4 Z M11 2 V6 H15 M7 10 H12 M9.5 7.5 V12.5"),
        Map.entry("open","M2 5 H7 L9 7 H16 L14 15 H2 Z M2 7 V3 H7 L9 5 H15 V7"),
        Map.entry("save","M3 2 H14 L16 4 V16 H3 Z M6 2 V7 H12 V2 M6 16 V10 H13 V16"),
        Map.entry("undo","M6 4 L2 8 L6 12 M3 8 H10 Q16 8 15 14"),
        Map.entry("redo","M12 4 L16 8 L12 12 M15 8 H8 Q2 8 3 14"),
        Map.entry("select","M2 7 V2 H7 M11 2 H16 V7 M16 11 V16 H11 M7 16 H2 V11 M6 6 H12 V12 H6 Z"),
        Map.entry("pan","M4 8 V5 Q4 3 6 4 V8 M6 8 V3 Q7 1 8 3 V8 M8 8 V2 Q9 1 10 3 V8 M10 8 V4 Q12 3 12 5 V10 L14 8 Q16 7 16 9 L13 14 Q12 17 8 16 L4 13 L2 9 Q2 7 4 8"),
        Map.entry("plus","M3 9 H15 M9 3 V15"),Map.entry("minus","M3 9 H15"),
        Map.entry("home","M2 8 L9 2 L16 8 M4 7 V16 H7 V11 H11 V16 H14 V7"),
        Map.entry("render","M5 3 L15 9 L5 15 Z"),Map.entry("cancel","M4 4 L14 14 M14 4 L4 14"),
        Map.entry("export","M9 11 V2 M5 6 L9 2 L13 6 M3 10 V16 H15 V10"),
        Map.entry("gradient","M3 3 H15 V15 H3 Z M3 13 L13 3 M7 15 L15 7 M3 8 L8 3"),
        Map.entry("layers","M2 6 L9 2 L16 6 L9 10 Z M2 10 L9 14 L16 10 M2 14 L9 18 L16 14"),
        Map.entry("focus","M2 7 V2 H7 M11 2 H16 V7 M16 11 V16 H11 M7 16 H2 V11"),
        Map.entry("copy","M6 5 H15 V16 H6 Z M3 12 V2 H12"),
        Map.entry("about","M9 7 V13 M9 4 V4.5 M9 1 A8 8 0 1 1 8.9 1"));
    public static StackPane of(String key){
        SVGPath path=new SVGPath();path.setContent(PATHS.getOrDefault(key,PATHS.get("gradient")));path.getStyleClass().add("tool-icon");
        StackPane box=new StackPane(new Group(path));box.setMinSize(18,18);box.setPrefSize(18,18);box.setMaxSize(18,18);box.setMouseTransparent(true);return box;
    }
    public static String key(String label){return switch(label){case "New"->"new";case "Open"->"open";case "Save"->"save";case "Undo"->"undo";case "Redo"->"redo";case "+"->"plus";case "−"->"minus";case "Home"->"home";case "Render","Recalculate"->"render";case "Cancel"->"cancel";case "Export PNG"->"export";case "Gradient"->"gradient";case "Layers"->"layers";case "Focus"->"focus";default->null;};}
}
