package com.jenga.aeternalis.network;
import com.jenga.aeternalis.distributed.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.math.precision.*;
import java.io.*;
import java.util.function.*;
/** Application-wide session used by interactive, zoom-sequence and high-resolution renderers. */
public final class NetworkService implements AutoCloseable {
    public enum Mode {LOCAL,DISTRIBUTED,AUTOMATIC}
    private static final NetworkService INSTANCE=new NetworkService();public static NetworkService instance(){return INSTANCE;}
    private volatile MasterServer master;private volatile WorkerClient worker;private volatile Mode mode=Mode.AUTOMATIC;private volatile double threshold=60;private volatile int tileSize=64;private volatile String notice="Disconnected";
    private NetworkService(){}
    public MasterServer master(){return master;}public WorkerClient worker(){return worker;}public Mode mode(){return mode;}public double threshold(){return threshold;}public int tileSize(){return tileSize;}public String notice(){return notice;}
    public void settings(Mode mode,double threshold,int tileSize){if(!Double.isFinite(threshold)||threshold<0||threshold>86400||tileSize<16||tileSize>256)throw new IllegalArgumentException("Threshold 0–86400 seconds, tile size 16–256");this.mode=mode;this.threshold=threshold;this.tileSize=tileSize;}
    public synchronized void host(String bind,int port,String token,int threads)throws IOException{close();master=new MasterServer(bind,port,token,threads);notice="Master listening on "+bind+":"+master.port();}
    public synchronized void connect(String host,int port,String token,int threads)throws IOException{close();worker=new WorkerClient(host,port,token,threads);notice="Worker connecting to "+host+":"+port;}
    public double estimate(RenderRequest r){
        double rate=master==null?20_000_000.*Runtime.getRuntime().availableProcessors():master.iterationsPerSecond();double factor=switch(r.precision()){case DOUBLE_DOUBLE->12;case QUAD_DOUBLE->80;case ARBITRARY->150;default->1;};
        // A bounded 4x4 probe estimates work; it does not make a precision decision.
        var evaluator=r.formula().evaluator(r.bailoutSquared(),r.periodicity());int cap=Math.min(512,r.limit());double work=0;
        for(int y=0;y<4;y++)for(int x=0;x<4;x++){var value=evaluator.evaluate(r.viewport().realAt((x+.5)*r.width()/4,r.width()),r.viewport().imaginaryAt((y+.5)*r.height()/4,r.width(),r.height()),cap,()->false);work+=value.reason()==com.jenga.aeternalis.fractal.Iteration.Reason.LIMIT?r.limit():Math.max(1,value.iterations());}
        return (double)r.width()*r.height()*r.grid()*r.grid()*(work/16)*factor/Math.max(1,rate);
    }
    public boolean use(RenderRequest request){MasterServer server=master;return server!=null&&mode!=Mode.LOCAL&&(mode==Mode.DISTRIBUTED||server.workerCount()>0&&estimate(request)>=threshold);}
    public DistributedJob render(RenderRequest request,java.util.function.Consumer<RenderEngine.Tile> output,java.util.function.Consumer<Throwable> failure,java.util.function.Consumer<PixelEvaluator.Context> precision){MasterServer server=master;if(server==null)throw new IllegalStateException("Start a Master server first");return server.render(explicit(request),tileSize,estimate(request),output,failure,precision);}
    public static RenderRequest explicit(RenderRequest r){var precision=PrecisionController.analyse(r.viewport(),r.width(),r.height(),r.grid(),r.limit(),r.precision()).mode();return new RenderRequest(r.viewport(),r.width(),r.height(),r.limit(),r.grid(),r.order(),r.periodicity(),r.bailoutSquared(),r.previews(),r.formula(),precision,r.perturbation());}
    public synchronized void close(){if(worker!=null){worker.close();worker=null;}if(master!=null){master.close();master=null;}notice="Disconnected";}
}
