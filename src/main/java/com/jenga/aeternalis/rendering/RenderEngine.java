package com.jenga.aeternalis.rendering;
import com.jenga.aeternalis.fractal.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
/** Bounded worker pool. Each worker pulls centre-prioritised tiles; only workers, not tiles, are queued. */
public final class RenderEngine implements AutoCloseable {
    public record Tile(int x,int y,int width,int height,double[] values,int completed,int total,long iterations,long nanos,int sampleGrid,int step,long[] classifications) {
        public Tile(int x,int y,int width,int height,double[] values,int completed,int total,long iterations,long nanos){this(x,y,width,height,values,completed,total,iterations,nanos,0,1,new long[Iteration.Reason.values().length]);}
    }
    public final class Job {
        private Job parent;private int attempt;private com.jenga.aeternalis.distributed.DistributedJob remote;
        private final AtomicBoolean cancelled=new AtomicBoolean();
        public void cancel(){cancelled.set(true);if(remote!=null)remote.cancel();}
        public boolean cancelled(){return cancelled.get() || closed.get() || parent!=null&&parent.cancelled();}
    }
    private int workers;
    private ExecutorService pool;
    private volatile PixelEvaluator.Context numericalContext;
    private volatile ComputeMode computeMode=ComputeMode.AUTOMATIC;
    private final GpuCompute gpu=GpuCompute.instance();
    public void computeMode(ComputeMode mode){computeMode=mode;}
    public String backendSummary(){var c=numericalContext;if(c==null)return "CPU";if(active!=null&&active.remote!=null)return "LAN distributed CPU · "+active.remote.done()+" / "+active.remote.total+" tiles";return gpu.eligible(c.request,c.plan,computeMode)?(computeMode==ComputeMode.GPU?"OpenCL FP64 · CPU safety fallback":"Hybrid CPU/OpenCL · benchmark validated"):"CPU · "+(computeMode==ComputeMode.GPU?"GPU request fell back safely":"precision-aware");}
    public RenderEngine(){this(Runtime.getRuntime().availableProcessors());}
    /** Explicit pool size supports reproducible numerical and scheduling checks. */
    public RenderEngine(int workers){
        if(workers<1||workers>Math.max(256,Runtime.getRuntime().availableProcessors()*2))throw new IllegalArgumentException("Worker count is outside the supported CPU range.");this.workers=workers;
        pool=Executors.newFixedThreadPool(workers,r->{Thread t=new Thread(r,"aeternalis-render");t.setDaemon(true);return t;});
    }
    private final AtomicBoolean closed=new AtomicBoolean();
    private Job active;
    public int workers(){return workers;}
    public synchronized void configureWorkers(int count){if(count==workers)return;if(count<1||count>Math.max(256,Runtime.getRuntime().availableProcessors()*2))throw new IllegalArgumentException("Invalid worker count");if(active!=null)active.cancel();pool.shutdownNow();workers=count;pool=Executors.newFixedThreadPool(workers,r->{Thread t=new Thread(r,"aeternalis-render");t.setDaemon(true);return t;});}
    public PixelEvaluator.Context numericalContext(){return numericalContext;}
    public static int tileSize(RenderRequest request){return com.jenga.aeternalis.math.precision.PrecisionController.analyse(request.viewport(),request.width(),request.height(),request.grid(),request.limit(),request.precision()).mode()==com.jenga.aeternalis.math.precision.PrecisionMode.DOUBLE?64:16;}
    public synchronized Job render(RenderRequest request, Consumer<Tile> listener, Consumer<Throwable> failure) {
        if(closed.get())throw new IllegalStateException("Renderer is closed.");if(active!=null)active.cancel();Job outer=new Job();active=outer;
        var network=com.jenga.aeternalis.network.NetworkService.instance();
        if(network.use(request)){outer.remote=network.render(request,t->{if(!outer.cancelled())listener.accept(t);},e->{if(!outer.cancelled())failure.accept(e);},c->numericalContext=c);if(outer.cancelled())outer.remote.cancel();}
        else attempt(request,outer,listener,failure);return outer;
    }
    private void attempt(RenderRequest request,Job outer,Consumer<Tile> listener,Consumer<Throwable> failure){
        Job pass=new Job();pass.parent=outer;int id=++outer.attempt;
        Consumer<Tile> publish=t->{synchronized(outer){if(!outer.cancelled()&&outer.attempt==id)listener.accept(t);}};
        Consumer<Throwable> failed=error->{synchronized(outer){
            if(outer.cancelled()||outer.attempt!=id)return;
            if(error instanceof com.jenga.aeternalis.math.precision.CoordinateCollision){
                var current=numericalContext.plan.mode();var next=com.jenga.aeternalis.math.precision.PrecisionController.next(current);
                if(current==com.jenga.aeternalis.math.precision.PrecisionMode.ARBITRARY){outer.cancel();failure.accept(error);return;}
                pass.cancel();publish.accept(new Tile(0,0,0,0,new double[0],0,0,0,0,1,-1,new long[Iteration.Reason.values().length]));
                RenderRequest retry=new RenderRequest(request.viewport(),request.width(),request.height(),request.limit(),request.grid(),request.order(),request.periodicity(),request.bailoutSquared(),request.previews(),request.formula(),next,request.perturbation());attempt(retry,outer,listener,failure);
            }else{outer.cancel();failure.accept(error);}
        }};
        renderPass(request,pass,publish,failed);
    }
    private void renderPass(RenderRequest request,Job job,Consumer<Tile> listener,Consumer<Throwable> failure){
        PixelEvaluator.Context context=new PixelEvaluator.Context(request);numericalContext=context;
        int tileSize=tileSize(request);List<int[]> tiles=request.order().tiles(request.width(),request.height(),tileSize);
        AtomicInteger next=new AtomicInteger(),done=new AtomicInteger();AtomicLong iterations=new AtomicLong();long start=System.nanoTime();
        if(request.previews())pool.submit(()->{
            try{
                PixelEvaluator evaluator=new PixelEvaluator(context,job::cancelled);
                for(int step:new int[]{64,32,16,8,4,2}){
                    int cw=(request.width()+step-1)/step,ch=(request.height()+step-1)/step;double[] values=new double[cw*ch];Arrays.fill(values,Double.POSITIVE_INFINITY);long published=System.nanoTime();
                    for(int y=0;y<ch;y++){
                        if(job.cancelled()||done.get()==tiles.size())return;
                        for(int x=0;x<cw;x++){
                            // The centre of the clipped square, including partial edge blocks.
                            double px=(x*step+Math.min(request.width(),(x+1)*step))/2.,py=(y*step+Math.min(request.height(),(y+1)*step))/2.;
                            var v=evaluator.preview(px,py);
                            values[y*cw+x]=v.colourable()?v.smooth():Double.NaN;
                            if(System.nanoTime()-published>500_000_000L&&!job.cancelled()){
                                listener.accept(new Tile(0,0,request.width(),request.height(),values.clone(),0,tiles.size(),0,System.nanoTime()-start,1,step,new long[Iteration.Reason.values().length]));published=System.nanoTime();
                            }
                        }
                    }
                    if(!job.cancelled()&&done.get()<tiles.size())listener.accept(new Tile(0,0,request.width(),request.height(),values,0,tiles.size(),0,System.nanoTime()-start,1,step,new long[Iteration.Reason.values().length]));
                }
            }catch(CancellationException ignored){}catch(Throwable error){if(!job.cancelled()){job.cancel();failure.accept(error);}}
        });
        for(int w=0;w<Math.min(computeMode==ComputeMode.GPU&&gpu.eligible(request,context.plan,computeMode)?1:workers,tiles.size());w++) pool.submit(()->{
            try {
                PixelEvaluator evaluator=new PixelEvaluator(context,job::cancelled);
                while(!job.cancelled()) {
                    int index=next.getAndIncrement();if(index>=tiles.size())return;
                    int x=tiles.get(index)[0],y=tiles.get(index)[1],tw=Math.min(tileSize,request.width()-x),th=Math.min(tileSize,request.height()-y);
                    // Check the tile's factored coordinate axes before dispatching a GPU kernel too.
                    for(int i=0;i<tw;i++)for(int sx=0;sx<request.grid();sx++)context.coordinates.check(x+i,y,sx,0,request.grid());
                    for(int j=0;j<th;j++)for(int sy=0;sy<request.grid();sy++)context.coordinates.check(x,y+j,0,sy,request.grid());
                    GpuCompute.Result accelerated=gpu.tryTile(request,context.plan,computeMode,x,y,tw,th,job::cancelled);
                    if(accelerated!=null){long total=iterations.addAndGet(accelerated.iterations());int completed=done.incrementAndGet();if(!job.cancelled())listener.accept(new Tile(x,y,tw,th,accelerated.values(),completed,tiles.size(),total,System.nanoTime()-start,request.grid(),1,accelerated.classifications()));continue;}
                    var result=TileKernel.calculate(context,x,y,tw,th,job::cancelled,p->{});
                    long totalIterations=iterations.addAndGet(result.iterations());int completed=done.incrementAndGet();
                    if(!job.cancelled())listener.accept(new Tile(x,y,tw,th,result.values(),completed,tiles.size(),totalIterations,System.nanoTime()-start,request.grid(),1,result.classifications()));
                }
            } catch(CancellationException ignored) { /* Expected when a newer viewport supersedes this one. */ }
            catch(Throwable error) {if(!job.cancelled()){job.cancel();failure.accept(error);}}
        });
    }
    public synchronized void close(){closed.set(true);if(active!=null)active.cancel();pool.shutdownNow();}
}
