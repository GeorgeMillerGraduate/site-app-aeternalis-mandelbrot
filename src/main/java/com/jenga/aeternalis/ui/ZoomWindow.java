package com.jenga.aeternalis.ui;

import com.jenga.aeternalis.animation.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.Viewport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.stage.*;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Non-modal sequence workstation. Job captures the applied document; editing the main canvas never changes it. */
public final class ZoomWindow implements AutoCloseable {
    private final Stage window=new Stage();private final StudioDocument destination;private final Viewport home;
    private final TextField startSpan=new TextField(),width=new TextField("1920"),height=new TextField("1080"),retention=new TextField("0.9"),fps=new TextField("30"),ffmpeg=new TextField("bundled");
    private final Label destinationSpan=new Label(),exportSource=new Label("No PNG sequence ready · render frames before exporting.");
    private final Label status=new Label("Capture the applied destination, create a new folder, then render or reopen an XML job."),summary=new Label(),path=new Label("Step 1 · Create a job, or open an existing XML job.");
    private final ProgressBar progress=new ProgressBar(0);private final Button create=new Button("1. Create zoom job…"),open=new Button("Open / resume XML…"),start=new Button("2. Render / resume PNGs"),pause=new Button("Pause"),gif=new Button("3. Save animated GIF…"),mp4=new Button("3. Save YouTube MP4…");
    private final AtomicBoolean cancelled=new AtomicBoolean();private volatile ZoomSequence sequence;private volatile boolean busy;private Path xml,frameFolder;
    private final Button useFolder=new Button("Use an existing PNG folder…");
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"aeternalis-animation");t.setDaemon(true);return t;});
    public ZoomWindow(Stage owner,StudioDocument destination,Viewport home){this(owner,destination,home,false);}
    public static ZoomWindow converter(Stage owner){return new ZoomWindow(owner,StudioDocument.initial(),Viewport.HOME,true);}
    private ZoomWindow(Stage owner,StudioDocument destination,Viewport home,boolean converter){
        this.destination=destination;this.home=home;startSpan.setText(home.spanExact().toPlainString());destinationSpan.setText(destination.selected().viewport().spanExact().toString());window.initOwner(owner);window.setTitle(converter?"Aeternalis · Frame folder to video / GIF":"Aeternalis · Render zoom frames");exportSource.setWrapText(true);status.setWrapText(true);path.setWrapText(true);summary.setWrapText(true);progress.setMaxWidth(Double.MAX_VALUE);
        GridPane fields=new GridPane();fields.setHgap(12);fields.setVgap(9);fields.addRow(0,new Label("Output width"),width,new Label("Output height"),height);fields.addRow(1,new Label("Span retained / frame"),retention,new Label("Frames / second"),fps);fields.addRow(2,new Label("Starting span"),startSpan,new Label("Destination span"),destinationSpan);
        Label note=new Label("0.9 = 10% less width per frame (19% less area). Home → captured destination; final frame is exact. Only the selected layer moves; other layers retain their views. GIF uses 256-colour frames; MP4 uses the included FFmpeg runtime.");note.setWrapText(true);note.getStyleClass().add("muted");
        HBox executable=new HBox(8,new Label("FFmpeg is included through the project’s Maven dependencies. No separate install is needed."));
        VBox body;
        if(converter){
            gif.setText("Save animated GIF…");mp4.setText("Save YouTube MP4…");useFolder.setText("1. Choose PNG frame folder…");
            Label instructions=new Label("Choose an existing completed PNG sequence, then save a video or looping GIF. This dialog does not render fractal frames.");instructions.setWrapText(true);
            body=new VBox(14,EditorialArt.heading("Frame folder → video / GIF","The animation workshop"),instructions,useFolder,exportSource,new HBox(10,new Label("Playback frames / second"),fps),new HBox(8,gif,mp4),pause,progress,status,executable);
            status.setText("Choose your PNG folder to begin. MP4 uses the included FFmpeg runtime; GIF export is built in.");
        }else{
            create.setText("1. Create zoom job…");start.setText("2. Render / resume PNGs");
            Label instructions=new Label("Render numbered PNGs from Home to your captured destination. To encode them later, use Animation → Convert frame folder to video / GIF.");instructions.setWrapText(true);
            body=new VBox(14,EditorialArt.heading("Render zoom frames","The animation workshop"),instructions,fields,note,new HBox(8,create,open),path,summary,new HBox(8,start,pause),progress,status);
        }
        body.setPadding(new Insets(20));ScrollPane scroll=new ScrollPane(body);scroll.setFitToWidth(true);scroll.setMinSize(0,0);Scene scene=new Scene(scroll,900,converter?500:680);Theme.apply(scene);window.setScene(scene);WindowBounds.install(window,owner);
        create.setOnAction(e->create());open.setOnAction(e->open());start.setOnAction(e->render());pause.setOnAction(e->{cancelled.set(true);var s=sequence;if(s!=null)s.pause();status.setText("Pausing · completed PNGs are retained");});gif.setOnAction(e->encode(false));mp4.setOnAction(e->encode(true));useFolder.setOnAction(e->chooseFolder());
        for(TextField field:new TextField[]{width,height,retention,fps,startSpan})field.textProperty().addListener((a,b,c)->estimate());
        window.setOnCloseRequest(e->close());refresh();estimate();
    }
    private ZoomJob proposed(){return new ZoomJob(destination,new Viewport(home.realExact(),home.imaginaryExact(),new BigDecimal(startSpan.getText().trim())),Integer.parseInt(width.getText().trim()),Integer.parseInt(height.getText().trim()),new BigDecimal(retention.getText().trim()),Integer.parseInt(fps.getText().trim()));}
    private void estimate(){if(xml!=null)return;try{ZoomJob j=proposed();summary.setText(j.frames()+" frames · "+String.format(java.util.Locale.ROOT,"%.2f",j.frames()/(double)j.fps())+" seconds · target span "+j.target().spanExact());}catch(Exception e){summary.setText("Check settings: "+e.getMessage());}}
    private void create(){try{
        ZoomJob job=proposed();DirectoryChooser chooser=new DirectoryChooser();chooser.setTitle("Select an empty output folder (create one in this dialog)");var chosen=chooser.showDialog(window);if(chosen==null)return;Path folder=chosen.toPath();try(var files=Files.list(folder)){if(files.findAny().isPresent())throw new IllegalArgumentException("Choose an empty folder to protect existing frames and jobs.");}
        Path next=folder.resolve("zoom.xml");ZoomJobIO.save(next,job);install(next,job);status.setText("Step 2 · Click Render / resume PNGs. When complete, open the separate frame-folder converter from Animation.");
    }catch(Exception e){status.setText(e.getMessage());}}
    private void open(){FileChooser chooser=new FileChooser();chooser.setTitle("Resume zoom XML");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Aeternalis zoom XML","*.xml"));var f=chooser.showOpenDialog(window);if(f==null)return;try{install(f.toPath(),ZoomJobIO.load(f.toPath()));status.setText("Checking saved frames…");checkFolder(xml.getParent(),false);}catch(Exception e){status.setText(e.getMessage());}}
    private void install(Path file,ZoomJob job){frameFolder=null;xml=file.toAbsolutePath();destinationSpan.setText(job.target().spanExact().toString());path.setText(xml.toString());width.setText(""+job.width());height.setText(""+job.height());retention.setText(job.factor().toPlainString());startSpan.setText(job.start().spanExact().toPlainString());fps.setText(""+job.fps());summary.setText(job.frames()+" frames · "+String.format(java.util.Locale.ROOT,"%.2f",job.frames()/(double)job.fps())+" seconds · destination "+job.target().realExact()+", "+job.target().imaginaryExact()+" · span "+job.target().spanExact());refresh();}
    private void render(){if(xml==null||busy)return;Path file=xml;begin();worker.submit(()->{try(ZoomSequence s=new ZoomSequence()){sequence=s;if(cancelled.get())throw new CancellationException();s.run(file,p->Platform.runLater(()->{progress.setProgress((p.completed()+ (p.completed()<p.total()?p.frameProgress():0))/p.total());status.setText(p.message());}));Platform.runLater(()->frameFolder=file.toAbsolutePath().getParent());finish("PNG sequence complete. Use Animation → Convert frame folder to video / GIF.");}catch(CancellationException e){finish("Paused. Reopen this XML to continue; completed PNGs are retained.");}catch(Exception e){finish("Render stopped: "+e.getMessage());}finally{sequence=null;}});}
    private void chooseFolder(){
        if(busy)return;DirectoryChooser chooser=new DirectoryChooser();chooser.setTitle("Select an already rendered PNG sequence");var selected=chooser.showDialog(window);if(selected!=null)checkFolder(selected.toPath(),true);
    }
    private void checkFolder(Path folder,boolean imported){
        begin();status.setText("Checking PNG sequence…");worker.submit(()->{
            try{var frames=SequenceEncoder.frames(folder);Platform.runLater(()->frameFolder=folder.toAbsolutePath());finish("Step 3 · "+frames.size()+" PNGs found. Choose Save animated GIF or Save YouTube MP4.");}
            catch(Exception ex){Platform.runLater(()->frameFolder=null);finish(imported?"This folder has no complete numbered PNG sequence. Create/open a zoom job, then render its PNGs first. "+ex.getMessage():"Step 2 · Click Render / resume PNGs to complete this job. Existing frames will be reused.");}
        });
    }
    private void encode(boolean video){
        if(busy)return;if(frameFolder==null){status.setText("Render the PNG frames first, or use an existing PNG folder.");return;}
        final Path folder=frameFolder;final int rate;try{rate=Integer.parseInt(fps.getText().trim());if(rate<1||rate>(video?120:100))throw new IllegalArgumentException("Use 1–"+(video?120:100)+" fps.");}catch(Exception ex){status.setText("Check frames per second: "+ex.getMessage());return;}
        // Recheck before asking for an output file: a folder may have been moved or frames removed.
        begin();status.setText("Checking completed frames before export…");worker.submit(()->{
            try{SequenceEncoder.frames(folder);Platform.runLater(()->chooseOutput(video,folder,rate));}
            catch(Exception ex){Platform.runLater(()->frameFolder=null);finish("PNG sequence is incomplete or unavailable. Render/resume it first. "+ex.getMessage());}
        });
    }
    private void chooseOutput(boolean video,Path folder,int rate){
        if(!window.isShowing()){finish("Export cancelled.");return;}
        FileChooser output=new FileChooser();output.setTitle(video?"Save YouTube MP4":"Save looping GIF");output.getExtensionFilters().add(new FileChooser.ExtensionFilter(video?"H.264 MP4":"Animated GIF",video?"*.mp4":"*.gif"));output.setInitialFileName(video?"aeternalis-zoom.mp4":"aeternalis-zoom.gif");var file=output.showSaveDialog(window);if(file==null){finish("Export cancelled. PNGs are ready whenever you want to encode.");return;}
        String command=ffmpeg.getText().trim();worker.submit(()->{try{if(video)SequenceEncoder.mp4(folder,file.toPath(),rate,command,cancelled,this::message);else SequenceEncoder.gif(folder,file.toPath(),rate,cancelled,this::message);finish("Saved "+file.getAbsolutePath());}catch(CancellationException ex){finish("Encoding cancelled; PNGs retained.");}catch(Exception ex){finish("Export failed: "+ex.getMessage());}});
    }
    private void message(String text){Platform.runLater(()->status.setText(text));}
    private void begin(){busy=true;cancelled.set(false);progress.setProgress(0);refresh();}
    private void finish(String text){Platform.runLater(()->{busy=false;status.setText(text);progress.setProgress(0);refresh();});}
    private void refresh(){exportSource.setText(frameFolder==null?"No PNG sequence ready · render frames before exporting.":"Export source: "+frameFolder);create.setDisable(busy||xml!=null);open.setDisable(busy);start.setDisable(busy||xml==null);pause.setDisable(!busy);gif.setDisable(busy||frameFolder==null);mp4.setDisable(busy||frameFolder==null);useFolder.setDisable(busy);for(TextField f:new TextField[]{width,height,retention,startSpan})f.setDisable(busy||xml!=null);fps.setDisable(busy);ffmpeg.setDisable(busy);}
    public void show(){window.show();window.toFront();}
    public boolean showing(){return window.isShowing();}
    public void close(){cancelled.set(true);var s=sequence;if(s!=null)s.pause();worker.shutdown();window.close();}
}
