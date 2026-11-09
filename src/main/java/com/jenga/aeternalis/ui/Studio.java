package com.jenga.aeternalis.ui;

import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.math.precision.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.layers.*;
import com.jenga.aeternalis.persistence.ProjectIO;
import java.nio.file.Path;
import com.jenga.aeternalis.colouring.*;
import java.util.concurrent.atomic.AtomicLong;
import com.jenga.aeternalis.rendering.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** JavaFX presentation layer. All numerical work runs in RenderEngine's worker pool. */
public final class Studio extends Application {
    private NetworkManagerWindow networkWindow;
    private final javafx.scene.canvas.Canvas networkOverlay=new javafx.scene.canvas.Canvas();
    private final java.util.List<ZoomWindow> zoomWindows=new java.util.ArrayList<>();
    private final DocumentRenderer documentRenderer=new DocumentRenderer();
    private StudioDocument document=StudioDocument.initial();
    private Path projectPath;private boolean dirty,loadingControls;
    private final ComboBox<FormulaSettings.Family> formula=new ComboBox<>();
    private VBox powerParameters,juliaParameters;
    private final Spinner<Integer> exponent=new Spinner<>(2,12,3);
    private final NumericField juliaReal=new NumericField(-.745,-4,4,.001,"Julia real constant"),juliaImaginary=new NumericField(.186,-4,4,.001,"Julia imaginary constant");
    private final ListView<LayerState> layers=new ListView<>();
    private final TextField layerName=new TextField();
    private final CheckBox layerVisible=new CheckBox("Visible");
    private final Slider layerOpacity=new Slider(0,1,1);
    private final ComboBox<BlendMode> blend=new ComboBox<>();
    private final Deque<Viewport> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final ImageView image=new ImageView();
    private final StackPane canvas=new StackPane(image);
    private final NumericField real=new NumericField(-.5,-Double.MAX_VALUE,Double.MAX_VALUE,.01,"Real centre"),imaginary=new NumericField(0,-Double.MAX_VALUE,Double.MAX_VALUE,.01,"Imaginary centre"),span=new NumericField(3.5,0,Double.MAX_VALUE,.05,"Horizontal span");
    private final ComboBox<PrecisionMode> precisionChoice=new ComboBox<>();
    private final CheckBox perturbationChoice=new CheckBox("Reference-orbit perturbation with validated fallback"),automaticThreads=new CheckBox("Automatic · one worker per logical processor");
    private final Spinner<Integer> threadCount=new Spinner<>(1,Math.max(256,Runtime.getRuntime().availableProcessors()*2),Runtime.getRuntime().availableProcessors());
    private final ComboBox<ComputeMode> computeChoice=new ComboBox<>();private final Label performance=new Label(),gpuDetails=new Label();
    private final Spinner<Integer> limit=new Spinner<>(32,100000,600,100);
    private ColourPanel colours;
    private final ComboBox<Quality> quality=new ComboBox<>();
    private final ComboBox<ScanOrder> scanOrder=new ComboBox<>();
    private final CheckBox cycles=new CheckBox("Adaptive exact-cycle checks"),previews=new CheckBox("Progressive square previews");
    private final Spinner<Double> bailout=new Spinner<>(4.,1000000.,10.,2.);
    private final Label classification=new Label();
    private ViewportInteraction interaction;
    private final ImageView navigator=new ImageView();
    private final ListView<Viewport> visits=new ListView<>();
    private OverviewNavigator overview;private SplitPane inspectorSplit;private double savedDivider=.76;
    private TabPane browser;private final Label documentTitle=new Label();
    private final javafx.beans.property.BooleanProperty atlasEnabled=new javafx.beans.property.SimpleBooleanProperty(true);
    private VBox editorialSidebar;private ZoomStudyStrip studyStrip;private Stage paletteWindow;private ScrollPane floatingColours;private Tab colourTab;private VBox inspector;
    private final PauseTransition colourDelay=new PauseTransition(Duration.millis(60));
    private boolean colourDirty,exporting,colourReady;
    private int grid=2;
    private double displayWidth,displayHeight;
    
    private final Label state=new Label("Ready"),details=new Label(),coordinates=new Label(),mouse=new Label(""),explanation=new Label();
    private final Label precisionText=new Label("Automatic precision");private PrecisionPlan activePlan;
    private final Label notificationText=new Label(),progressText=new Label("0.00%"),zoomText=new Label(),dimensionsText=new Label(),elapsedText=new Label("0.0 s");
    private final HBox notification=new HBox();private long renderStarted;
    private final Timeline elapsedClock=new Timeline(new KeyFrame(Duration.millis(250),e->updatePerformance()));
    private final ProgressBar progress=new ProgressBar(0);
    private final Button export=new Button("Export PNG"),back=new Button("Undo"),forward=new Button("Redo");
    private final PauseTransition resizeDelay=new PauseTransition(Duration.millis(250));
    private Viewport viewport=Viewport.HOME;
    private volatile long generation;
    private WritableImage bitmap;private int width,height,applied;
    private boolean rendering,focusMode;
    private SplitPane split;private TabPane properties;private Stage stage;
    private long renderTicket;

    @Override public void start(Stage stage) {
        this.stage=stage;
        colours=new ColourPanel(this::paletteChanged);
        quality.getItems().setAll(Quality.values());quality.setValue(Quality.CLEAN);
        quality.setOnAction(e->render());colourDelay.setOnFinished(e->renderDocument(false));
        IterationInput.install(limit);limit.setMaxWidth(Double.MAX_VALUE);limit.getEditor().setOnAction(e->applyFields());
        scanOrder.getItems().setAll(ScanOrder.values());scanOrder.setValue(ScanOrder.RADIAL);cycles.setSelected(true);previews.setSelected(true);
        scanOrder.setOnAction(e->render());cycles.setOnAction(e->render());previews.setOnAction(e->render());bailout.valueProperty().addListener((a,b,c)->render());
        colourTab=new Tab("Colours",scroll(colours));
        properties=new TabPane(new Tab("Location",scroll(buildLeft())),colourTab,new Tab("Render",scroll(buildRender())),new Tab("Formula",scroll(buildFormula())),new Tab("Performance",scroll(buildPerformance())));
        overview=new OverviewNavigator(this::change);
        properties.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Label propertyTitle=new Label("FRACTAL PROPERTIES");propertyTitle.getStyleClass().add("dock-title");
        browser=new TabPane(new Tab("Navigator",overview),new Tab("History",visits),new Tab("Layers",scroll(buildLayers())),new Tab("Composite",scroll(buildRight())));browser.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        inspectorSplit=new SplitPane(properties,browser);inspectorSplit.setOrientation(Orientation.VERTICAL);inspectorSplit.setDividerPositions(.64);VBox.setVgrow(inspectorSplit,Priority.ALWAYS);
        inspector=new VBox(propertyTitle,inspectorSplit);inspector.setMinWidth(305);inspector.setPrefWidth(335);
        visits.setCellFactory(list->new ListCell<>(){protected void updateItem(Viewport v,boolean empty){super.updateItem(v,empty);setText(empty||v==null?null:String.format(Locale.ROOT,"Span %.5g · %.6g %+.6gi",v.span(),v.real(),v.imaginary()));}});
        visits.setOnMouseClicked(e->{if(e.getClickCount()==2&&visits.getSelectionModel().getSelectedItem()!=null)change(visits.getSelectionModel().getSelectedItem());});
        canvas.getStyleClass().add("fractal-viewport");canvas.setMinSize(200,200);
        networkOverlay.setMouseTransparent(true);networkOverlay.widthProperty().bind(canvas.widthProperty().subtract(16));networkOverlay.heightProperty().bind(canvas.heightProperty().subtract(16));canvas.getChildren().add(networkOverlay);
        image.setPreserveRatio(true);image.setSmooth(false);
        image.fitWidthProperty().bind(canvas.widthProperty().subtract(16));image.fitHeightProperty().bind(canvas.heightProperty().subtract(16));
        documentTitle.setText("Mandelbrot study     /     Aeternalis");documentTitle.getStyleClass().add("document-title");
        BorderPane document=new BorderPane(canvas);document.setTop(documentTitle);
        studyStrip=new ZoomStudyStrip(this::selectStudy,url->getHostServices().showDocument(url),stage,()->atlasEnabled.set(false));document.setBottom(studyStrip);
        split=new SplitPane(document,inspector);split.setDividerPositions(.76);studyStrip.visibleProperty().bind(atlasEnabled.and(javafx.beans.binding.Bindings.createBooleanBinding(()->!focusMode,split.getItems())));studyStrip.managedProperty().bind(studyStrip.visibleProperty());
        BorderPane workspace=new BorderPane(split);editorialSidebar=buildEditorialSidebar();workspace.setLeft(editorialSidebar);editorialSidebar.visibleProperty().bind(stage.widthProperty().greaterThanOrEqualTo(1380).and(javafx.beans.binding.Bindings.createBooleanBinding(()->!focusMode,split.getItems())));editorialSidebar.managedProperty().bind(editorialSidebar.visibleProperty());
        BorderPane root=new BorderPane(workspace);notificationText.setWrapText(true);HBox.setHgrow(notificationText,Priority.ALWAYS);notification.getChildren().addAll(new Label("!"),notificationText,button("Dismiss",this::clearNotification));notification.getStyleClass().add("notification");notification.setVisible(false);notification.setManaged(false);
        root.setTop(new VBox(menu(),masthead(),toolbar(),notification));
        state.getStyleClass().add("status-state");state.setPrefWidth(235);state.setMaxWidth(235);mouse.setPrefWidth(235);mouse.setMaxWidth(235);zoomText.setPrefWidth(100);elapsedText.setPrefWidth(58);progressText.setMinWidth(64);progressText.setPrefWidth(64);precisionText.setPrefWidth(125);precisionText.setMaxWidth(125);mouse.visibleProperty().bind(stage.widthProperty().greaterThan(1500));mouse.managedProperty().bind(mouse.visibleProperty());dimensionsText.visibleProperty().bind(stage.widthProperty().greaterThan(1200));dimensionsText.managedProperty().bind(dimensionsText.visibleProperty());
        Region statusSpring=new Region();HBox.setHgrow(statusSpring,Priority.ALWAYS);
        HBox status=new HBox(state,progress,progressText,new Separator(Orientation.VERTICAL),precisionText,elapsedText,statusSpring,dimensionsText,zoomText,mouse);status.setAlignment(Pos.CENTER_LEFT);status.getStyleClass().add("status");
        progress.setPrefWidth(110);root.setBottom(status);
        ContextMenu copyCoordinates=new ContextMenu(item("Copy exact active coordinates",()->{var clipboard=javafx.scene.input.Clipboard.getSystemClipboard();var content=new javafx.scene.input.ClipboardContent();content.putString("Re="+viewport.realExact()+"\nIm="+viewport.imaginaryExact()+"\nSpan="+viewport.spanExact());clipboard.setContent(content);}));coordinates.setContextMenu(copyCoordinates);mouse.setContextMenu(copyCoordinates);
        Scene scene=new Scene(root,1440,900);Theme.apply(scene);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S,KeyCombination.CONTROL_DOWN),()->saveProject(false));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O,KeyCombination.CONTROL_DOWN),this::openProject);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER,KeyCombination.CONTROL_DOWN),this::applyFields);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z,KeyCombination.CONTROL_DOWN),()->history(false));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y,KeyCombination.CONTROL_DOWN),()->history(true));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11),()->stage.setFullScreen(!stage.isFullScreen()));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE),()->{if(interaction==null||!interaction.cancelSelection())cancel();});
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.LEFT,KeyCombination.ALT_DOWN),()->panKeyboard(-.1,0));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.RIGHT,KeyCombination.ALT_DOWN),()->panKeyboard(.1,0));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.UP,KeyCombination.ALT_DOWN),()->panKeyboard(0,-.1));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DOWN,KeyCombination.ALT_DOWN),()->panKeyboard(0,.1));
        elapsedClock.setCycleCount(Animation.INDEFINITE);elapsedClock.play();
        AppIcon.watch();AppIcon.apply(stage);stage.setScene(scene);stage.setTitle("Aeternalis Fractal Studio · George Miller");stage.setMinWidth(1000);stage.setMinHeight(650);
        stage.setOnCloseRequest(e->{if(!confirmDiscard())e.consume();});
        WorkspacePrefs.Layout layout=WorkspacePrefs.load();var bounds=Screen.getPrimary().getVisualBounds();stage.setWidth(Math.min(layout.width(),bounds.getWidth()));stage.setHeight(Math.min(layout.height(),bounds.getHeight()));stage.setMaximized(layout.maximized());stage.show();
        split.setDividerPositions(layout.horizontal());savedDivider=layout.horizontal();inspectorSplit.setDividerPositions(layout.vertical());properties.getSelectionModel().select(layout.properties());browser.getSelectionModel().select(layout.browser());if(layout.focus())focus();
        stage.outputScaleXProperty().addListener((a,b,c)->resizeDelay.playFromStart());
        stage.outputScaleYProperty().addListener((a,b,c)->resizeDelay.playFromStart());
        canvas.widthProperty().addListener((a,b,c)->resizeDelay.playFromStart());canvas.heightProperty().addListener((a,b,c)->resizeDelay.playFromStart());
        resizeDelay.setOnFinished(e->renderDocument(false));navigation();loadActiveControls();recordVisit();syncFields();
        Platform.runLater(()->renderDocument(false));
    }
    private ScrollPane scroll(VBox box){ScrollPane s=new ScrollPane(box);s.setFitToWidth(true);s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);return s;}
    private Label section(String text){Label l=new Label(text);l.getStyleClass().add("section-title");return l;}
    private Label muted(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
    private Button button(String text,Runnable action){Button b=new Button(text);b.setOnAction(e->action.run());String key=Icons.key(text);if(key!=null)b.setGraphic(Icons.of(key));if("plus".equals(key)||"minus".equals(key))b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);b.setAccessibleText(text.equals("+")?"Zoom in":text.equals("−")?"Zoom out":text);String shortcut=switch(text){case "Save"->"Ctrl+S";case "Open"->"Ctrl+O";case "Render"->"Ctrl+Enter";case "Cancel"->"Escape";default->"";};b.setTooltip(new Tooltip(text+(shortcut.isEmpty()?"":" · "+shortcut)));return b;}
    private HBox masthead(){
        ImageView emblem=new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/jenga/aeternalis/assets/aeternalis-emblem.png"))));emblem.setFitWidth(40);emblem.setFitHeight(40);
        Label wordmark=new Label("AETERNALIS");wordmark.getStyleClass().add("wordmark");Label sub=new Label("FRACTAL STUDIO   /   GEORGE MILLER");sub.getStyleClass().add("wordmark-caption");Region spring=new Region();HBox.setHgrow(spring,Priority.ALWAYS);Label edition=new Label("The Mathematical Observatory  ·  "+BuildInfo.VERSION);edition.getStyleClass().add("edition-label");
        HBox box=new HBox(12,emblem,new VBox(0,wordmark,sub),spring,edition);box.setAlignment(Pos.CENTER_LEFT);box.getStyleClass().add("masthead");return box;
    }
    private VBox buildEditorialSidebar(){
        Label eyebrow=new Label("THE MATHEMATICAL OBSERVATORY");eyebrow.getStyleClass().add("atlas-title");eyebrow.setWrapText(true);
        ImageView art=EditorialArt.image("explorer",164,246);
        Label heading=new Label("Explore the\ninfinite.");heading.getStyleClass().add("explorer-title");
        Label caption=muted("A workbench for mathematical discovery.");
        VBox families=new VBox(7);for(var family:FormulaSettings.Family.values()){
            Button choice=button(family.toString(),()->formula.setValue(family));choice.setMaxWidth(Double.MAX_VALUE);families.getChildren().add(choice);
        }
        Label guide=muted("Choose a study below. Drag a rectangle to look closer.");Region gap=new Region();VBox.setVgrow(gap,Priority.ALWAYS);
        Button network=button("Network rendering",()->network(3));network.setMaxWidth(Double.MAX_VALUE);
        VBox box=new VBox(12,eyebrow,art,heading,caption,new Separator(),families,gap,guide,network);box.setPrefWidth(192);box.setMinWidth(192);box.setMaxWidth(192);box.getStyleClass().add("editorial-sidebar");
        art.visibleProperty().bind(stage.heightProperty().greaterThan(770));art.managedProperty().bind(art.visibleProperty());return box;
    }
    private void selectStudy(ZoomStudies.Study study){
        // Replace only the active layer. Preserve layer ID, opacity/blend, other layers and network session.
        cancel();undo.push(viewport);redo.clear();viewport=study.viewport();
        LayerState active=document.selected().view(viewport).formula(FormulaSettings.DEFAULT).colours(ColourSettings.wikipedia());
        var c=active.calculation();active=active.calculation(new Calculation(study.iterations(),c.grid(),c.order(),c.periodicity(),c.bailoutSquared(),c.previews(),c.precision(),c.perturbation()));
        document=document.replace(active);loadActiveControls();recordVisit();studyStrip.selected(study.index());markDirty();renderDocument(false);
    }
    private void panKeyboard(double x,double y){if(width>0)change(viewport.pan(-x*width,-y*height,width));}
    private MenuItem item(String name,Runnable action){MenuItem i=new MenuItem(name);i.setOnAction(e->action.run());return i;}
    private MenuBar menu(){
        Menu file=new Menu("File");file.getItems().addAll(item("New project",this::newProject),item("Open project…",this::openProject),item("Save project",()->saveProject(false)),item("Save project as…",()->saveProject(true)),item("Export composition PNG…",this::export),new SeparatorMenuItem(),item("Exit",()->stage.close()));
        Menu edit=new Menu("Edit");edit.getItems().addAll(item("Undo viewport",()->history(false)),item("Redo viewport",()->history(true)));
        Menu view=new Menu("View");view.getItems().addAll(item("Focus viewport",this::focus),item("Performance & precision",()->properties.getSelectionModel().select(4)),item("Detach gradient editor",this::detachPalette),item("Dock gradient editor",this::dockPalette),item("Full screen · F11",()->stage.setFullScreen(!stage.isFullScreen())));
        CheckMenuItem atlasItem=new CheckMenuItem("Mandelbrot atlas");atlasItem.setId("atlas-toggle");atlasItem.selectedProperty().bindBidirectional(atlasEnabled);view.getItems().add(atlasItem);
        Menu fractal=new Menu("Fractal");for(var family:FormulaSettings.Family.values())fractal.getItems().add(item(family.toString(),()->formula.setValue(family)));
        Menu render=new Menu("Render");render.getItems().addAll(item("Render · Ctrl+Enter",this::applyFields),item("Cancel · Escape",this::cancel));
        Menu help=new Menu("Help");help.getItems().addAll(item("About Aeternalis",this::showAbout),item("Wikipedia atlas · image credits",()->ZoomStudyStrip.credits(stage,url->getHostServices().showDocument(url))),item("Navigation & shortcuts",()->{Alert a=new Alert(Alert.AlertType.INFORMATION,"Drag: selection zoom\nShift-drag / middle-drag: pan\nWheel / double-click: zoom at pointer\nAlt+arrows: nudge view\nCtrl+Enter: apply coordinates\nCtrl+Z/Y: viewport undo/redo\nCtrl+S/O: save/open project\nF11: full screen · Escape: cancel selection or render",ButtonType.OK);a.initOwner(stage);a.setHeaderText("Observatory field guide");Theme.apply(a);a.showAndWait();}));
        Menu gallery=new Menu("Gallery");for(var entry:ExampleDocuments.gallery().entrySet())gallery.getItems().add(item(entry.getKey(),()->{if(confirmDiscard()){installDocument(entry.getValue(),null);markDirty();browser.getSelectionModel().select(2);}}));
        Menu layerMenu=new Menu("Layers");layerMenu.getItems().addAll(item("Show layer stack",()->browser.getSelectionModel().select(2)),item("Add layer",()->addLayer(false)),item("Duplicate selected",()->addLayer(true)),item("Delete selected",this::deleteLayer),item("Raise",()->moveLayer(1)),item("Lower",()->moveLayer(-1)));
        Menu network=new Menu("Network");network.getItems().addAll(item("Start Master Server…",()->network(0)),item("Connect as Worker…",()->network(1)),item("Disconnect",()->{cancel();com.jenga.aeternalis.network.NetworkService.instance().close();}),item("Network Rendering Settings…",()->network(2)),item("Connected Computers…",()->network(3)),item("Network Rendering Statistics…",()->network(4)),item("Render high-resolution PNG…",()->network(5)));
        MenuBar bar=new MenuBar(file,edit,view,fractal,layerMenu,gallery,render,network,help);
        Menu animation=new Menu("Animation");animation.getItems().add(item("Render zoom frames…",()->{cancel();ZoomWindow window=new ZoomWindow(stage,document,home(document.selected().formula().family()));zoomWindows.add(window);window.show();}));animation.getItems().add(item("Convert frame folder to video / GIF…",()->{ZoomWindow window=ZoomWindow.converter(stage);zoomWindows.add(window);window.show();}));bar.getMenus().add(bar.getMenus().size()-1,animation);
        for(String name:new String[]{"Formula language"}){Menu m=new Menu(name);m.setDisable(true);bar.getMenus().add(bar.getMenus().size()-1,m);}return bar;
    }
    private void network(int tab){if(networkWindow==null)networkWindow=new NetworkManagerWindow(stage,()->document,this::cancel,()->{documentRenderer.invalidate();renderDocument(false);});networkWindow.show(tab);}
    private ToolBar toolbar(){
        back.setOnAction(e->history(false));forward.setOnAction(e->history(true));export.setOnAction(e->export());
        Button render=button("Render",this::applyFields);render.getStyleClass().add("primary");
        ToggleButton select=new ToggleButton("Select zoom"),pan=new ToggleButton("Pan");ToggleGroup modes=new ToggleGroup();select.setToggleGroup(modes);pan.setToggleGroup(modes);select.setSelected(true);select.setGraphic(Icons.of("select"));pan.setGraphic(Icons.of("pan"));select.setTooltip(new Tooltip("Drag an aspect-locked rectangle to zoom"));pan.setTooltip(new Tooltip("Drag to pan · Shift-drag or middle-drag also works"));
        modes.selectedToggleProperty().addListener((o,a,v)->{if(v==null){a.setSelected(true);return;}if(interaction!=null)interaction.setMode(v==pan?ViewportInteraction.Mode.PAN:ViewportInteraction.Mode.SELECT);});
        back.setGraphic(Icons.of("undo"));forward.setGraphic(Icons.of("redo"));export.setGraphic(Icons.of("export"));back.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);forward.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);back.setTooltip(new Tooltip("Undo viewport · Ctrl+Z"));forward.setTooltip(new Tooltip("Redo viewport · Ctrl+Y"));export.setTooltip(new Tooltip("Export the completed visible composition as PNG"));
        Button stop=button("Cancel",this::cancel);stop.getStyleClass().add("cancel-button");
        ToolBar b=new ToolBar(button("New",this::newProject),button("Open",this::openProject),button("Save",()->saveProject(false)),new Separator(Orientation.VERTICAL),back,forward,new Separator(Orientation.VERTICAL),select,pan,button("+",()->zoom(.5,displayWidth/2,displayHeight/2)),button("−",()->zoom(2,displayWidth/2,displayHeight/2)),button("Home",()->change(home(document.selected().formula().family()))),new Separator(Orientation.VERTICAL),render,stop,new Separator(Orientation.VERTICAL),button("Gradient",this::detachPalette),button("Layers",()->browser.getSelectionModel().select(2)),export,button("Focus",this::focus));b.getStyleClass().add("toolbar");return b;
    }
    private VBox buildLeft(){
        VBox b=new VBox(9);b.getStyleClass().add("panel");formula.getItems().setAll(FormulaSettings.Family.values());formula.setValue(FormulaSettings.Family.MANDELBROT);formula.setMaxWidth(Double.MAX_VALUE);
        formula.setOnAction(e->formulaChanged());exponent.valueProperty().addListener((a,old,value)->formulaChanged());
        juliaReal.setOnAction(e->formulaChanged());juliaImaginary.setOnAction(e->formulaChanged());
        Button apply=button("Apply & render",this::applyFields);apply.getStyleClass().add("primary");apply.setMaxWidth(Double.MAX_VALUE);
        for(TextField field:new TextField[]{real,imaginary,span})field.setOnAction(e->applyFields());
        real.setAccessibleText("Real centre coordinate");imaginary.setAccessibleText("Imaginary centre coordinate");span.setAccessibleText("Horizontal complex-plane span");
        quality.setAccessibleText("Render quality and anti-aliasing");
        quality.setMaxWidth(Double.MAX_VALUE);
        powerParameters=new VBox(5,new Label("Newton exponent n · zⁿ − 1"),exponent);juliaParameters=new VBox(5,new Label("Julia constant · real / imaginary"),new HBox(5,juliaReal,juliaImaginary),button("Apply Julia constant",this::formulaChanged));
        b.getChildren().addAll(section("Position & scale"),Sections.group("Complex-plane coordinates",true,new Label("Real centre"),real,new Label("Imaginary centre"),imaginary,new Label("Horizontal span"),span,apply),Sections.group("Observation record",false,coordinates,muted("Right-click to copy exact coordinate values.")),button("Open gradient editor",this::detachPalette),muted("Drag a rectangle to approach a detail. Shift-drag moves the selected layer."));return b;
    }
    private VBox buildFormula(){VBox box=new VBox(9,section("Fractal formula"),formula,powerParameters,juliaParameters,muted("Changing family resets only the selected layer to its starting view. Other layers keep their own settings."));box.getStyleClass().add("panel");return box;}
    private VBox buildRender(){
        VBox b=new VBox(9);b.getStyleClass().add("panel");scanOrder.setMaxWidth(Double.MAX_VALUE);bailout.setEditable(false);bailout.setMaxWidth(Double.MAX_VALUE);quality.setMaxWidth(Double.MAX_VALUE);classification.setWrapText(true);
        b.getChildren().addAll(section("Render instrument"),Sections.group("Calculation",true,new Label("Iteration limit"),limit,new Label("Squared bailout · |z|²"),bailout,muted("Escape is tested against the squared modulus, not the radius.")),Sections.group("Sampling & performance",true,new Label("Sample quality"),quality,new Label("Scan order"),scanOrder,cycles,previews),button("Recalculate",this::applyFields),Sections.group("Diagnostics",false,details,classification,explanation));return b;
    }
    private VBox buildPerformance(){
        precisionChoice.getItems().setAll(PrecisionMode.values());precisionChoice.setValue(PrecisionMode.AUTOMATIC);perturbationChoice.setSelected(true);
        precisionChoice.setOnAction(e->render());perturbationChoice.setOnAction(e->render());automaticThreads.setSelected(true);threadCount.setEditable(false);threadCount.setDisable(true);
        computeChoice.getItems().setAll(ComputeMode.values());computeChoice.setValue(ComputeMode.AUTOMATIC);
        automaticThreads.setOnAction(e->{threadCount.setDisable(automaticThreads.isSelected());configureHardware();});threadCount.valueProperty().addListener((a,b,c)->{if(!automaticThreads.isSelected())configureHardware();});computeChoice.setOnAction(e->{documentRenderer.computeMode(computeChoice.getValue());renderDocument(false);});
        performance.setWrapText(true);gpuDetails.setWrapText(true);performance.getStyleClass().add("numeric-label");gpuDetails.getStyleClass().add("muted");
        VBox box=new VBox(9,section("Compute observatory"),Sections.group("Numerical precision",true,precisionChoice,perturbationChoice,muted("Precision is a minimum request. Collapsing grids escalate automatically; pixels are never dropped.")),Sections.group("CPU scheduling",true,automaticThreads,threadCount,muted("More threads can be slower. Representative benchmark results are included in docs.")),Sections.group("GPU compute",true,computeChoice,gpuDetails),Sections.group("Live diagnostics",true,performance));box.getStyleClass().add("panel");GpuCompute.instance();return box;
    }
    private void drawNetworkOverlay(){
        var g=networkOverlay.getGraphicsContext2D();g.clearRect(0,0,networkOverlay.getWidth(),networkOverlay.getHeight());var server=com.jenga.aeternalis.network.NetworkService.instance().master();if(server==null||!server.overlay())return;var job=server.job();if(job==null||!job.request().viewport().equals(viewport)||job.request().width()!=width||job.request().height()!=height)return;
        double sx=networkOverlay.getWidth()/width,sy=networkOverlay.getHeight()/height;for(var entry:job.owners().entrySet()){var tile=job.geometry(entry.getKey());g.setStroke(javafx.scene.paint.Color.hsb(Math.floorMod(server.label(entry.getValue()).hashCode(),360),.8,.9,.75));g.strokeRect(tile.x()*sx,tile.y()*sy,tile.width()*sx,tile.height()*sy);if(tile.width()*sx>48&&tile.height()*sy>16){g.setFill(g.getStroke());g.setFont(javafx.scene.text.Font.font(9));String owner=server.label(entry.getValue());g.fillText(owner.substring(0,Math.min(10,owner.length())),tile.x()*sx+3,tile.y()*sy+11);}}
    }
    private void configureHardware(){if(loadingControls)return;documentRenderer.configureWorkers(automaticThreads.isSelected(),threadCount.getValue());renderDocument(false);}
    private void updatePerformance(){
        drawNetworkOverlay();
        double seconds=rendering?(System.nanoTime()-renderStarted)/1e9:0;if(rendering)elapsedText.setText(String.format(Locale.ROOT,"%.1f s",seconds));if(properties==null||properties.getSelectionModel().getSelectedIndex()!=4)return;
        performance.setText("Logical processors: "+Runtime.getRuntime().availableProcessors()+"\nConfigured workers: "+documentRenderer.workers()+"\n"+documentRenderer.backendSummary()+"\nActive precision: "+(activePlan==null?"Automatic":activePlan.toString())+"\n"+(activePlan==null?"":activePlan.reason())+"\nLayer-cache hits: "+documentRenderer.cacheHits()+" ("+String.format(Locale.ROOT,"%.2f%%",100.*documentRenderer.cacheHits()/Math.max(1,documentRenderer.cacheHits()+documentRenderer.numericalPasses()))+")"+"\nPrecision escalations: "+documentRenderer.escalationEvents()+"\nPerturbation samples: "+documentRenderer.perturbationPixels()+"\nAccurate fallbacks: "+documentRenderer.perturbationFallbacks()+"\nOpenCL tiles: "+GpuCompute.instance().tiles()+" · failures: "+GpuCompute.instance().failures()+"\nNumerical pixels: "+documentRenderer.processedPixels()+"\nPixels/s: "+String.format(Locale.ROOT,"%.0f",documentRenderer.processedPixels()/Math.max(.001,documentRenderer.durationSeconds()))+"\nRetained-sample budget: "+(documentRenderer.sampleBudget()*8/1048576)+" MiB\nHeap used: "+((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576)+" MiB");gpuDetails.setText(GpuCompute.instance().report().summary());
    }
    private VBox buildRight(){
        VBox b=new VBox(7);b.getStyleClass().add("panel");coordinates.setWrapText(true);coordinates.getStyleClass().add("numeric-label");details.setWrapText(true);explanation.setWrapText(true);
        navigator.setPreserveRatio(true);navigator.setFitWidth(260);navigator.setFitHeight(150);navigator.setSmooth(true);
        HBox locations=new HBox(5,button("Home",()->change(home(document.selected().formula().family()))),button("Seahorse",()->change(new Viewport(-.7435,.1314,.012))),button("Spiral",()->change(new Viewport(-.74543,.11301,.003))));
        b.getChildren().addAll(navigator,locations,muted("Current view · double-click a History entry to revisit it."));return b;
    }
    private void detachPalette(){
        if(paletteWindow!=null){paletteWindow.toFront();return;}
        ((ScrollPane)colourTab.getContent()).setContent(null);
        colourTab.setContent(new VBox(10,muted("The gradient editor is open in its own window."),button("Dock gradient editor",this::dockPalette)));
        paletteWindow=new Stage(StageStyle.UTILITY);paletteWindow.initOwner(stage);paletteWindow.setTitle("Aeternalis · Gradient editor");
        GradientCurves curves=new GradientCurves(colours.gradient());colours.observeGradient(curves::setGradient);floatingColours=scroll(colours);
        SplitPane editor=new SplitPane(curves,floatingColours);editor.setDividerPositions(.57);BorderPane pane=new BorderPane(editor);pane.setTop(button("Dock to properties",this::dockPalette));
        Scene scene=new Scene(pane,Math.min(820,Screen.getPrimary().getVisualBounds().getWidth()-60),Math.max(480,Math.min(680,Screen.getPrimary().getVisualBounds().getHeight()-90)));Theme.apply(scene);paletteWindow.setScene(scene);WindowBounds.install(paletteWindow,stage);paletteWindow.setOnCloseRequest(e->{e.consume();dockPalette();});paletteWindow.show();
    }
    private void dockPalette(){
        if(paletteWindow==null){properties.getSelectionModel().select(colourTab);return;}
        Stage window=paletteWindow;paletteWindow=null;BorderPane root=(BorderPane)window.getScene().getRoot();floatingColours.setContent(null);floatingColours=null;colours.observeGradient(g->{});root.setCenter(null);window.setOnCloseRequest(null);window.close();colourTab.setContent(scroll(colours));properties.getSelectionModel().select(colourTab);
    }
    private void navigation(){
        interaction=new ViewportInteraction(canvas,()->displayWidth,()->displayHeight,new ViewportInteraction.Actions(){
            public void zoom(double factor,double x,double y){Studio.this.zoom(factor,x,y);}
            public void pan(double dx,double dy){if(width>0)change(viewport.pan(dx*width/displayWidth,dy*height/displayHeight,width));}
            public void select(SelectionBox box){if(width>0)change(box.zoom(viewport,width,height,displayWidth,displayHeight));}
            public void pointer(double x,double y){if(width>0)mouse.setText(String.format(Locale.ROOT,"c = %.9g %+.9gi",viewport.realAt(x*width/displayWidth,width),viewport.imaginaryAt(y*height/displayHeight,width,height)));}
        });
    }
    private void zoom(double factor,double x,double y){if(width>0)change(viewport.zoom(factor,x*width/displayWidth,y*height/displayHeight,width,height));}
    private void focus(){focusMode=!focusMode;if(focusMode){savedDivider=split.getDividerPositions()[0];split.getItems().remove(inspector);}else{split.getItems().add(inspector);split.setDividerPositions(savedDivider);}}
    private void applyFields(){
        clearNotification();try{Viewport next=new Viewport(real.decimalValue(),imaginary.decimalValue(),span.decimalValue());document=document.replace(document.selected().calculation(currentCalculation()));if(next.equals(viewport))render();else change(next);}
        catch(IllegalArgumentException e){error("Invalid coordinates",e.getMessage());}
    }
    private void change(Viewport next){
        if(next.equals(viewport))return;
        undo.push(viewport);redo.clear();viewport=next;document=document.replace(document.selected().view(next));markDirty();recordVisit();syncFields();renderDocument(false);
    }
    private void history(boolean forwards){Deque<Viewport> from=forwards?redo:undo,to=forwards?undo:redo;if(from.isEmpty())return;to.push(viewport);viewport=from.pop();document=document.replace(document.selected().view(viewport));markDirty();recordVisit();syncFields();renderDocument(false);}
    private void recordVisit(){if(visits.getItems().isEmpty()||!visits.getItems().getFirst().equals(viewport)){visits.getItems().add(0,viewport);if(visits.getItems().size()>100)visits.getItems().removeLast();}}
    private void syncFields(){real.step(viewport.spanExact().divide(java.math.BigDecimal.valueOf(100)));imaginary.step(viewport.spanExact().divide(java.math.BigDecimal.valueOf(100)));span.step(viewport.spanExact().divide(java.math.BigDecimal.valueOf(20)));zoomText.setText(java.math.BigDecimal.valueOf(3.5).divide(viewport.spanExact(),new java.math.MathContext(5)).toEngineeringString()+"×");real.setText(viewport.realExact().toString());imaginary.setText(viewport.imaginaryExact().toString());span.setText(viewport.spanExact().toString());back.setDisable(undo.isEmpty());forward.setDisable(redo.isEmpty());coordinates.setText("Re  "+viewport.realExact().toEngineeringString()+"\nIm  "+viewport.imaginaryExact().toEngineeringString()+"\nSpan "+viewport.spanExact().toEngineeringString());}
    private Calculation currentCalculation(){return new Calculation(IterationInput.commit(limit),quality.getValue().grid(),scanOrder.getValue(),cycles.isSelected(),bailout.getValue(),previews.isSelected(),precisionChoice.getValue(),perturbationChoice.isSelected());}
    private void markDirty(){dirty=true;stage.setTitle("Aeternalis Fractal Studio · "+(projectPath==null?"Untitled":projectPath.getFileName())+" *");}
    private void paletteChanged(){
        if(loadingControls)return;document=document.replace(document.selected().colours(colours.settings()));markDirty();
        renderTicket++;documentRenderer.cancel();colourDirty=true;colourReady=false;export.setDisable(true);colourDelay.playFromStart();
    }
    private void cancel(){
        generation++;renderTicket++;documentRenderer.cancel();colourDelay.stop();colourDirty=false;colourReady=false;
        if(rendering){rendering=false;state.setText("Cancelled · partial composition");}export.setDisable(true);
    }
    private void render(){
        if(loadingControls)return;
        try{
            var c=currentCalculation();
            document=document.replace(document.selected().calculation(c));markDirty();renderDocument(true);
        }catch(IllegalArgumentException e){error("Invalid render settings",e.getMessage());}
    }
    private void renderDocument(boolean changedNumericalSettings){
        if(loadingControls||stage==null)return;
        documentTitle.setText(document.selected().name()+"     /     "+document.selected().formula().family());
        if(interaction!=null)interaction.cancelSelection();colourDelay.stop();colourDirty=false;colourReady=false;
        if(changedNumericalSettings)generation++;
        displayWidth=Math.max(1,canvas.getWidth()-16);displayHeight=Math.max(1,canvas.getHeight()-16);
        width=Math.max(1,(int)Math.ceil(displayWidth*stage.getOutputScaleX()));height=Math.max(1,(int)Math.ceil(displayHeight*stage.getOutputScaleY()));grid=document.selected().calculation().grid();try{activePlan=PrecisionController.analyse(viewport,width,height,grid,document.selected().calculation().limit(),document.selected().calculation().precision());precisionText.setText(activePlan.toString());precisionText.setTooltip(new Tooltip(activePlan.reason()));}catch(IllegalArgumentException ex){error("Precision budget",ex.getMessage());return;}overview.update(document.selected(),(double)height/width);
        // Validate dimensions before allocating a GUI image, even for a completely hidden stack.
        if((long)width*height>16_000_000L){error("Render size limit","The output exceeds 16 million pixels.");return;}
        rendering=true;renderStarted=System.nanoTime();export.setDisable(true);progress.setProgress(0);progressText.setText("0.00%");state.setText("Preparing composition…");long ticket=++renderTicket;
        documentRenderer.render(document,width,height,update->Platform.runLater(()->{
            if(ticket!=renderTicket)return;
            if(update.pixels()!=null){
                if(bitmap==null||bitmap.getWidth()!=update.width()||bitmap.getHeight()!=update.height()){bitmap=new WritableImage(update.width(),update.height());image.setImage(bitmap);navigator.setImage(bitmap);}
                bitmap.getPixelWriter().setPixels(0,0,update.width(),update.height(),PixelFormat.getIntArgbInstance(),update.pixels(),0,update.width());
            }
            var liveContext=documentRenderer.currentContext();if(liveContext!=null&&liveContext.request.viewport().equals(viewport)){activePlan=liveContext.plan;precisionText.setText(activePlan.toString());}
            progress.setProgress(update.progress());progressText.setText(String.format(Locale.ROOT,"%.2f%%",100*update.progress()));dimensionsText.setText(width+" × "+height);state.setText(update.status());state.setTooltip(new Tooltip(update.status()));
            details.setText(String.format(Locale.ROOT,"%d × %d output pixels\n%d workers · %d numerical layer passes\n%d total layers",width,height,documentRenderer.workers(),update.numericalPasses(),document.layers().size()));
            long[] r=update.classifications();classification.setText(String.format(Locale.ROOT,"Cardioid %,d · Bulb %,d\nExact cycles %,d · Limit %,d\nNewton convergence %,d",r[1],r[2],r[3],r[4],r[5]));
            explanation.setText("Active: "+document.selected().formula().family()+" · "+grid+" × "+grid+" sampling");
            if(update.complete()){elapsedText.setText(String.format(Locale.ROOT,"%.1f s",(System.nanoTime()-renderStarted)/1e9));rendering=false;colourReady=true;export.setDisable(exporting);smokeCapture();}
        }),failure->Platform.runLater(()->{if(ticket==renderTicket){rendering=false;state.setText("Render failed");error("Render failed",failure.getMessage()==null?failure.toString():failure.getMessage());}}));
    }
    private void formulaChanged(){
        if(loadingControls)return;
        clearNotification();try{
            FormulaSettings next=new FormulaSettings(formula.getValue(),exponent.getValue(),juliaReal.value(),juliaImaginary.value());
            boolean switched=next.family()!=document.selected().formula().family();
            LayerState layer=document.selected().formula(next);
            if(switched){viewport=home(next.family());layer=layer.view(viewport);undo.clear();redo.clear();recordVisit();syncFields();}
            document=document.replace(layer);updateFormulaAvailability();markDirty();renderDocument(true);
        }catch(IllegalArgumentException e){error("Invalid formula",e.getMessage());}
    }
    private static Viewport home(FormulaSettings.Family family){return switch(family){case JULIA,NEWTON->new Viewport(0,0,3.5);default->Viewport.HOME;};}
    private void updateFormulaAvailability(){boolean multibrot=formula.getValue()==FormulaSettings.Family.NEWTON;powerParameters.setVisible(multibrot);powerParameters.setManaged(multibrot);boolean julia=formula.getValue()==FormulaSettings.Family.JULIA;juliaParameters.setVisible(julia);juliaParameters.setManaged(julia);exponent.setDisable(!multibrot);juliaReal.setDisable(!julia);juliaImaginary.setDisable(!julia);cycles.setDisable(formula.getValue()!=FormulaSettings.Family.MANDELBROT);}
    private VBox buildLayers(){
        layers.setPrefHeight(130);layers.setCellFactory(list->new ListCell<>(){protected void updateItem(LayerState layer,boolean empty){super.updateItem(layer,empty);setText(empty||layer==null?null:layer.toString());}});
        layers.getSelectionModel().selectedItemProperty().addListener((a,old,value)->{if(!loadingControls&&value!=null&&!value.id().equals(document.active())){document=document.select(value.id());undo.clear();redo.clear();loadActiveControls();renderDocument(false);}});
        blend.getItems().setAll(BlendMode.values());blend.setMaxWidth(Double.MAX_VALUE);layerOpacity.setAccessibleText("Active layer opacity");
        layerVisible.setOnAction(e->layerPresentation());blend.setOnAction(e->layerPresentation());layerOpacity.valueProperty().addListener((a,old,value)->layerPresentation());layerName.setOnAction(e->layerPresentation());
        layers.setPrefHeight(83);layers.setMinHeight(65);layers.setMaxHeight(95);layerOpacity.setPrefWidth(140);
        HBox tools=new HBox(4,button("Add",()->addLayer(false)),button("Duplicate",()->addLayer(true)),button("Delete",this::deleteLayer),button("↑",()->moveLayer(1)),button("↓",()->moveLayer(-1)));
        HBox opacityRow=new HBox(7,layerVisible,new Label("Opacity"),layerOpacity);opacityRow.setAlignment(Pos.CENTER_LEFT);
        HBox blendRow=new HBox(7,new Label("Blend"),blend);HBox.setHgrow(blend,Priority.ALWAYS);blend.setMaxWidth(Double.MAX_VALUE);
        HBox nameRow=new HBox(7,new Label("Name"),layerName);HBox.setHgrow(layerName,Priority.ALWAYS);
        VBox box=new VBox(6,new Label("TOP LAYER FIRST"),layers,tools,opacityRow,blendRow,nameRow,muted("Name: Enter to apply. Transparent interior is in Colours."));box.getStyleClass().add("panel");return box;
    }
    private void refreshLayers(){
        boolean before=loadingControls;loadingControls=true;List<LayerState> top=new ArrayList<>(document.layers());Collections.reverse(top);layers.getItems().setAll(top);layers.getSelectionModel().select(document.selected());loadingControls=before;
    }
    private void loadActiveControls(){
        loadingControls=true;LayerState l=document.selected();viewport=l.viewport();formula.setValue(l.formula().family());exponent.getValueFactory().setValue(l.formula().power());juliaReal.setText(Double.toString(l.formula().juliaReal()));juliaImaginary.setText(Double.toString(l.formula().juliaImaginary()));
        var c=l.calculation();limit.getValueFactory().setValue(c.limit());quality.setValue(Arrays.stream(Quality.values()).filter(q->q.grid()==c.grid()).findFirst().orElseThrow());scanOrder.setValue(c.order());cycles.setSelected(c.periodicity());bailout.getValueFactory().setValue(c.bailoutSquared());previews.setSelected(c.previews());precisionChoice.setValue(c.precision());perturbationChoice.setSelected(c.perturbation());colours.apply(l.colours());
        layerName.setText(l.name());layerVisible.setSelected(l.visible());layerOpacity.setValue(l.opacity());blend.setValue(l.blend());grid=c.grid();loadingControls=false;updateFormulaAvailability();refreshLayers();syncFields();
    }
    private void layerPresentation(){
        if(loadingControls)return;
        try{document=document.replace(document.selected().presentation(layerName.getText().trim(),layerVisible.isSelected(),layerOpacity.getValue(),blend.getValue()));markDirty();refreshLayers();renderDocument(false);}catch(IllegalArgumentException ex){error("Invalid layer",ex.getMessage());}
    }
    private void addLayer(boolean duplicate){
        if(document.layers().size()>=8){error("Layer limit","This release supports up to eight layers.");return;}
        LayerState layer=duplicate?document.selected().duplicate():LayerState.initial().presentation("Layer "+(document.layers().size()+1),true,1,BlendMode.NORMAL);
        List<LayerState> list=new ArrayList<>(document.layers());list.add(layer);document=new StudioDocument(list,layer.id());undo.clear();redo.clear();loadActiveControls();browser.getSelectionModel().select(2);markDirty();renderDocument(false);
    }
    private void deleteLayer(){if(document.layers().size()==1)return;List<LayerState> list=new ArrayList<>(document.layers());list.remove(document.selected());document=new StudioDocument(list,list.getLast().id());undo.clear();redo.clear();loadActiveControls();browser.getSelectionModel().select(2);markDirty();renderDocument(false);}
    private void moveLayer(int delta){List<LayerState> list=new ArrayList<>(document.layers());int i=list.indexOf(document.selected()),to=i+delta;if(to<0||to>=list.size())return;Collections.swap(list,i,to);document=new StudioDocument(list,document.active());markDirty();refreshLayers();renderDocument(false);}
    private FileChooser projectChooser(String title){FileChooser chooser=new FileChooser();chooser.setTitle(title);chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Aeternalis project","*.aeternalis"));chooser.setInitialFileName(projectPath==null?"Artwork.aeternalis":projectPath.getFileName().toString());return chooser;}
    private boolean saveProject(boolean choose){
        Path target=projectPath;if(target==null||choose){File f=projectChooser("Save project").showSaveDialog(stage);if(f==null)return false;target=f.toPath();}
        try{ProjectIO.save(target,document);projectPath=target;dirty=false;stage.setTitle("Aeternalis Fractal Studio · "+target.getFileName());state.setText("Saved project");return true;}catch(IOException e){error("Save failed",e.getMessage());return false;}
    }
    private boolean confirmDiscard(){
        if(!dirty)return true;ButtonType save=new ButtonType("Save"),discard=new ButtonType("Discard"),cancel=new ButtonType("Cancel",ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,"Save changes to this project?",save,discard,cancel);alert.initOwner(stage);alert.setHeaderText("Keep your observations");Theme.apply(alert);var result=alert.showAndWait();return result.isPresent()&&(result.get()==discard||(result.get()==save&&saveProject(false)));
    }
    private void openProject(){File file=projectChooser("Open project").showOpenDialog(stage);if(file==null)return;try{StudioDocument loaded=ProjectIO.load(file.toPath());if(!confirmDiscard())return;installDocument(loaded,file.toPath());}catch(IOException e){error("Open failed",e.getMessage());}}
    private void installDocument(StudioDocument loaded,Path path){cancel();document=loaded;projectPath=path;dirty=false;undo.clear();redo.clear();visits.getItems().clear();loadActiveControls();recordVisit();stage.setTitle("Aeternalis Fractal Studio · "+(path==null?"Untitled":path.getFileName()));renderDocument(false);}
    private void newProject(){if(confirmDiscard())installDocument(StudioDocument.initial(),null);}
    private void export(){
        if(rendering||colourDirty||bitmap==null||export.isDisabled()||progress.getProgress()<1){error("Export unavailable","Finish rendering before exporting the viewport.");return;}
        FileChooser chooser=new FileChooser();chooser.setTitle("Export rendered viewport");chooser.setInitialFileName("Aeternalis.png");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image","*.png"));File file=chooser.showSaveDialog(stage);if(file==null)return;
        // Snapshot conversion occurs on the FX thread; disk encoding is asynchronous.
        var buffered=SwingFXUtils.fromFXImage(bitmap,null);exporting=true;export.setDisable(true);
        Thread writer=new Thread(()->{try{if(!ImageIO.write(buffered,"png",file))throw new IOException("PNG writer unavailable");Platform.runLater(()->{exporting=false;state.setText("Saved "+file.getName());if(colourReady&&!rendering&&!colourDirty)export.setDisable(false);});}catch(IOException e){Platform.runLater(()->{exporting=false;export.setDisable(!colourReady);error("Export failed",e.getMessage());});}},"aeternalis-export");writer.setDaemon(true);writer.start();
    }
    private void error(String title,String message){notificationText.setText(title+" · "+message);notification.setVisible(true);notification.setManaged(true);}
    private void clearNotification(){notification.setVisible(false);notification.setManaged(false);notificationText.setText("");}
    private void showAbout(){
        Dialog<Void> dialog=new Dialog<>();dialog.initOwner(stage);dialog.setTitle("About Aeternalis Fractal Studio");dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);Theme.apply(dialog);
        VBox hero=new VBox(6);hero.getStyleClass().add("about-hero");hero.setAlignment(Pos.CENTER_LEFT);
        Label title=new Label("Aeternalis");title.getStyleClass().add("about-title");Label sub=new Label("FRACTAL STUDIO\nAn Infinite Canvas for Mathematical Discovery");sub.getStyleClass().add("about-subtitle");sub.setWrapText(true);sub.setMaxWidth(260);hero.getChildren().addAll(title,sub);
        VBox body=new VBox(9,hero,section("The Mathematical Observatory"),new Label("Version "+BuildInfo.VERSION+"  ·  Created by George Miller"),muted("Mandelbrot · Julia · Newton zⁿ − 1 · independent fractal layers · custom cubic gradients"),muted("Java 21 / JavaFX · CPU / optional OpenCL · automatic numerical precision\nSource: MIT · George Miller, 2025–2026\nWikipedia atlas images: Wolfgang Beyer · CC BY-SA 3.0\nOpenJFX: GPL v2 with Classpath Exception · Gson: Apache 2.0 · JOCL: MIT"));body.setPrefWidth(600);dialog.getDialogPane().setContent(body);dialog.showAndWait();
    }
    private void smokeCapture(){String path=System.getProperty("aeternalis.capture");if(path!=null){Platform.runLater(()->{try{ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null),null),"png",new File(path));Platform.exit();}catch(IOException e){throw new UncheckedIOException(e);}});}}
    @Override public void stop(){
        double divider=split.getItems().size()>1?split.getDividerPositions()[0]:savedDivider;
        try{WorkspacePrefs.save(new WorkspacePrefs.Layout(Math.max(1000,stage.getWidth()),Math.max(650,stage.getHeight()),stage.isMaximized(),Math.max(.2,Math.min(.9,divider)),Math.max(.2,Math.min(.85,inspectorSplit.getDividerPositions()[0])),properties.getSelectionModel().getSelectedIndex(),browser.getSelectionModel().getSelectedIndex(),focusMode));}catch(RuntimeException ignored){}
        if(networkWindow!=null)networkWindow.close();com.jenga.aeternalis.network.NetworkService.instance().close();zoomWindows.forEach(ZoomWindow::close);overview.close();elapsedClock.stop();resizeDelay.stop();colourDelay.stop();documentRenderer.close();if(paletteWindow!=null){paletteWindow.setOnCloseRequest(null);paletteWindow.close();}}
}
