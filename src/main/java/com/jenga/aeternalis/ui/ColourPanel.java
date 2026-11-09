package com.jenga.aeternalis.ui;

import com.jenga.aeternalis.colouring.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import java.io.*;
import java.util.*;

/** Cubic palette laboratory with exact stop editing, bounded history and shared live state. */
public final class ColourPanel extends VBox {
    private final ComboBox<Palette> presets=new ComboBox<>();
    private Gradient gradient=Palette.WIKIPEDIA.gradient().withMode(Gradient.Mode.CATMULL_ROM);
    private final ComboBox<Gradient.Mode> interpolation=new ComboBox<>();
    private final Canvas strip=new Canvas(270,75);
    private final ColorPicker colour=new ColorPicker(),interior=new ColorPicker(Color.web("#090e16"));
    private final TextField hex=new TextField();
    private final NumericField exactPosition=new NumericField(0,0,1,.001,"Colour stop position");
    private final Slider position=new Slider(0,1,0),density=new Slider(.05,4,1),scale=new Slider(8,512,128),offset=new Slider(0,1,0),power=new Slider(.1,3,.5);
    private final ComboBox<ColourSettings.Transfer> transfer=new ComboBox<>();
    private final CheckBox transparent=new CheckBox("Transparent interior"),repeat=new CheckBox("Repeat gradient"),reverse=new CheckBox("Reverse direction"),histogram=new CheckBox("Histogram distribution");
    private final Label selected=new Label(),values=new Label(),sourceNote=new Label("Observatory · customisable preset"),problem=new Label();
    private final Button undo=new Button("Undo"),redo=new Button("Redo");
    private final Deque<Gradient> past=new ArrayDeque<>(),future=new ArrayDeque<>();
    private int selection;private boolean updating,dragging,remembered;
    private final Runnable changed;
    private java.util.function.Consumer<Gradient> observer=g->{};
    public void observeGradient(java.util.function.Consumer<Gradient> observer){this.observer=observer;observer.accept(gradient);}
    public ColourPanel(Runnable changed){
        this.changed=changed;getStyleClass().add("panel");setSpacing(9);
        Label title=new Label("Gradient laboratory");title.getStyleClass().add("section-title");sourceNote.getStyleClass().add("palette-meta");problem.getStyleClass().add("field-error");problem.setWrapText(true);
        presets.getItems().setAll(Palette.values());presets.setValue(Palette.WIKIPEDIA);presets.setMaxWidth(Double.MAX_VALUE);
        presets.setOnAction(e->{if(!updating&&presets.getValue()!=null)edit(presets.getValue().gradient().withMode(interpolation.getValue()),"Preset · "+presets.getValue());});
        interpolation.getItems().setAll(Gradient.Mode.values());interpolation.setValue(Gradient.Mode.CATMULL_ROM);interpolation.setMaxWidth(Double.MAX_VALUE);
        interpolation.setOnAction(e->{if(!updating)edit(gradient.withMode(interpolation.getValue()),"Custom cubic palette");});
        transfer.getItems().setAll(ColourSettings.Transfer.values());transfer.setValue(ColourSettings.Transfer.SQRT);transfer.setMaxWidth(Double.MAX_VALUE);
        repeat.setSelected(true);colour.setMaxWidth(Double.MAX_VALUE);interior.setMaxWidth(Double.MAX_VALUE);
        Label guide=new Label("Double-click the strip to add a colour. Drag a marker to move it; double-click a marker to edit.");guide.setWrapText(true);guide.getStyleClass().add("muted");
        strip.getStyleClass().add("palette-strip");strip.widthProperty().bind(widthProperty().subtract(24));strip.widthProperty().addListener((a,b,c)->draw());
        strip.setOnMousePressed(e->{selection=nearest(e.getX()/Math.max(1,strip.getWidth()));dragging=true;remembered=false;sync();});
        strip.setOnMouseDragged(e->{if(selection>0&&selection<gradient.stops().size()-1)position.setValue(e.getX()/Math.max(1,strip.getWidth()));});
        strip.setOnMouseReleased(e->{dragging=false;remembered=false;});
        strip.setOnMouseClicked(e->{if(e.getClickCount()==2){if(e.getY()<48)insert(Math.max(.001,Math.min(.999,e.getX()/Math.max(1,strip.getWidth()))),null);else colour.show();}});
        colour.setOnAction(e->{if(!updating)replace(gradient.stops().get(selection).position(),rgb(colour.getValue()));});
        hex.setPromptText("#RRGGBB");hex.getStyleClass().add("numeric-field");hex.setAccessibleText("Selected colour as hexadecimal RGB");
        hex.setOnAction(e->{String text=hex.getText().trim().replaceFirst("^#","");if(!text.matches("[0-9a-fA-F]{6}")){problem.setText("Use six hexadecimal digits, for example #C5A46D.");return;}replace(gradient.stops().get(selection).position(),Integer.parseInt(text,16));});
        exactPosition.setOnAction(e->{try{replace(exactPosition.value(),gradient.stops().get(selection).rgb());}catch(IllegalArgumentException ex){problem.setText(ex.getMessage());}});
        position.valueChangingProperty().addListener((a,b,c)->{dragging=c;remembered=false;});
        position.valueProperty().addListener((a,b,c)->{if(!updating&&selection>0&&selection<gradient.stops().size()-1){double low=gradient.stops().get(selection-1).position()+.001,high=gradient.stops().get(selection+1).position()-.001;replace(Math.max(low,Math.min(high,c.doubleValue())),gradient.stops().get(selection).rgb());}});
        undo.setGraphic(Icons.of("undo"));redo.setGraphic(Icons.of("redo"));undo.setOnAction(e->history(false));redo.setOnAction(e->history(true));
        HBox editing=new HBox(5,button("Add colour",()->insert(GradientOperations.largestGap(gradient),null)),undo,redo);
        HBox stopTools=new HBox(5,button("Duplicate",()->insert(GradientOperations.largestGap(gradient),gradient.stops().get(selection).rgb())),button("Remove",this::remove));
        HBox paletteTools=new HBox(5,button("Reverse",()->edit(GradientOperations.reverse(gradient),"Reversed palette")),button("Close loop",()->edit(GradientOperations.replace(gradient,gradient.stops().size()-1,1,gradient.stops().getFirst().rgb()),"Cyclic palette")),button("Reset",()->edit(Palette.OBSERVATORY.gradient().withMode(Gradient.Mode.CATMULL_ROM),"Observatory preset")));
        HBox files=new HBox(5,button("Save…",()->file(false)),button("Load…",()->file(true)),button("Copy",this::copy),button("Paste",this::paste));
        getChildren().addAll(EditorialArt.image("prism",280,90),title,presets,sourceNote,new Label("Cubic interpolation"),interpolation,strip,guide,editing,problem,
            Sections.group("Selected colour",true,selected,colour,new Label("Hexadecimal RGB"),hex,new Label("Exact stop position · 0…1"),exactPosition,position,stopTools),paletteTools,
            Sections.group("Colour distribution",false,new Label("Transfer mapping"),transfer,new Label("Frequency / density"),density,new Label("Escape scale"),scale,new Label("Palette offset"),offset,new Label("Power exponent"),power,values,repeat,reverse,histogram),
            Sections.group("Interior & transparency",false,transparent,interior),files);
        for(Slider slider:new Slider[]{density,scale,offset,power})slider.valueProperty().addListener((a,b,c)->{labels();notifyChange();});
        transfer.setOnAction(e->{labels();notifyChange();});for(CheckBox box:new CheckBox[]{repeat,reverse,histogram,transparent})box.setOnAction(e->notifyChange());interior.setOnAction(e->notifyChange());
        colour.setAccessibleText("Selected gradient colour · custom dialog includes RGB and HSB controls");position.setAccessibleText("Colour stop position slider");strip.setAccessibleText("Cubic gradient strip with draggable stops");
        sync();labels();
    }
    private Button button(String text,Runnable action){Button b=new Button(text);b.setOnAction(e->action.run());b.setTooltip(new Tooltip(text));return b;}
    private void remember(){if(dragging&&remembered)return;past.push(gradient);while(past.size()>64)past.removeLast();future.clear();remembered=true;}
    private void edit(Gradient next,String note){if(next.equals(gradient))return;remember();gradient=next;selection=Math.min(selection,gradient.stops().size()-1);sourceNote.setText(note);problem.setText("");sync();notifyChange();}
    private void history(boolean forward){Deque<Gradient> from=forward?future:past,to=forward?past:future;if(from.isEmpty())return;to.push(gradient);gradient=from.pop();selection=Math.min(selection,gradient.stops().size()-1);sync();notifyChange();}
    private void replace(double p,int rgb){
        try{if(selection==0)p=0;if(selection==gradient.stops().size()-1)p=1;edit(GradientOperations.replace(gradient,selection,p,rgb),"Custom palette");}
        catch(IllegalArgumentException e){problem.setText("Keep stops in increasing positions, at least 0.0001 apart.");sync();}
    }
    private void insert(double p,Integer rgb){try{edit(GradientOperations.insert(gradient,p,rgb==null?gradient.argb(p):rgb),"Custom palette");selection=nearest(p);sync();colour.show();}catch(IllegalArgumentException e){problem.setText("Use at most 32 colours and keep stops at distinct positions.");}}
    private void remove(){try{edit(GradientOperations.remove(gradient,selection),"Custom palette");}catch(IllegalArgumentException e){problem.setText(e.getMessage());}}
    private void copy(){try{ClipboardContent content=new ClipboardContent();content.putString(PaletteIO.encode(gradient));Clipboard.getSystemClipboard().setContent(content);sourceNote.setText("Palette copied");}catch(IOException e){problem.setText(e.getMessage());}}
    private void paste(){try{String text=Clipboard.getSystemClipboard().getString();if(text==null)throw new IOException("Clipboard has no palette text.");edit(PaletteIO.decode(text),"Pasted palette");}catch(IOException e){problem.setText(e.getMessage());}}
    public ColourSettings settings(){return new ColourSettings(gradient,transfer.getValue(),density.getValue(),scale.getValue(),offset.getValue(),power.getValue(),repeat.isSelected(),reverse.isSelected(),histogram.isSelected(),rgb(interior.getValue()),transparent.isSelected());}
    public void apply(ColourSettings s){
        updating=true;range(density,.05,4,s.density());range(scale,8,512,s.scale());range(offset,0,1,s.offset());range(power,.1,3,s.power());gradient=s.gradient();selection=0;past.clear();future.clear();
        transfer.setValue(s.transfer());density.setValue(s.density());scale.setValue(s.scale());offset.setValue(s.offset());power.setValue(s.power());repeat.setSelected(s.repeat());reverse.setSelected(s.reverse());histogram.setSelected(s.histogram());transparent.setSelected(s.transparentInterior());interior.setValue(fx(s.interior()));presets.setValue(java.util.Arrays.stream(Palette.values()).filter(p->p.gradient().stops().equals(gradient.stops())).findFirst().orElse(null));sourceNote.setText(presets.getValue()==null?"Selected layer · custom palette":"Preset · "+presets.getValue());problem.setText("");updating=false;sync();labels();observer.accept(gradient);
    }
    private static void range(Slider s,double min,double max,double value){s.setMin(Math.min(min,value));s.setMax(Math.max(max,value));}
    public Gradient gradient(){return gradient;}
    private void notifyChange(){if(updating)return;draw();observer.accept(gradient);changed.run();}
    private void labels(){values.setText(String.format(Locale.ROOT,"Density %.2f · Scale %.0f\nOffset %.2f · Power %.2f",density.getValue(),scale.getValue(),offset.getValue(),power.getValue()));power.setDisable(transfer.getValue()!=ColourSettings.Transfer.POWER);}
    private static int rgb(Color c){return (int)Math.round(c.getRed()*255)<<16|(int)Math.round(c.getGreen()*255)<<8|(int)Math.round(c.getBlue()*255);}
    private static Color fx(int c){return Color.rgb((c>>16)&255,(c>>8)&255,c&255);}
    private int nearest(double p){int best=0;for(int i=1;i<gradient.stops().size();i++)if(Math.abs(gradient.stops().get(i).position()-p)<Math.abs(gradient.stops().get(best).position()-p))best=i;return best;}
    private void sync(){updating=true;interpolation.setValue(gradient.mode());var stop=gradient.stops().get(selection);colour.setValue(fx(stop.rgb()));hex.setText(String.format(Locale.ROOT,"#%06X",stop.rgb()));exactPosition.setText(Double.toString(stop.position()));position.setValue(stop.position());boolean endpoint=selection==0||selection==gradient.stops().size()-1;position.setDisable(endpoint);exactPosition.setDisable(endpoint);selected.setText("Colour "+(selection+1)+" of "+gradient.stops().size());undo.setDisable(past.isEmpty());redo.setDisable(future.isEmpty());updating=false;draw();}
    private void draw(){
        var g=strip.getGraphicsContext2D();double w=strip.getWidth();g.clearRect(0,0,w,75);if(w<=1)return;
        for(int x=0;x<(int)w;x++){g.setFill(fx(gradient.argb(x/(w-1))));g.fillRect(x,0,1,48);}
        g.setStroke(Color.web("#b8a16e"));g.setLineWidth(1);g.strokeRect(.5,.5,w-1,47);
        for(int i=0;i<gradient.stops().size();i++){var s=gradient.stops().get(i);double x=Math.max(6,Math.min(w-6,s.position()*w));g.setFill(fx(s.rgb()));g.fillPolygon(new double[]{x,x-5,x+5},new double[]{53,66,66},3);g.setStroke(i==selection?Color.web("#efd6a0"):Color.web("#8ba09a"));g.setLineWidth(i==selection?2:1);g.strokePolygon(new double[]{x,x-5,x+5},new double[]{53,66,66},3);}
    }
    private void file(boolean loading){FileChooser chooser=new FileChooser();chooser.setTitle(loading?"Load palette":"Save custom palette");chooser.setInitialFileName("Custom.aetpalette");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Aeternalis palette","*.aetpalette"));File f=loading?chooser.showOpenDialog(getScene().getWindow()):chooser.showSaveDialog(getScene().getWindow());if(f==null)return;try{if(loading)edit(PaletteIO.load(f.toPath()),"Loaded · "+f.getName());else{PaletteIO.save(f.toPath(),gradient);sourceNote.setText("Saved · "+f.getName());}}catch(IOException e){problem.setText(e.getMessage());}}
}
