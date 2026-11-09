package com.jenga.aeternalis.distributed;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.math.precision.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
/** Single authoritative generation, collision registry and lease table for every local/remote participant. */
public final class DistributedJob {
    public record Contribution(String computer,long tiles,long samples,long iterations,long nanos){}
    public final UUID id=UUID.randomUUID();public final long started=System.nanoTime();public volatile int tileSize,total;private final int baseTileSize;public final double estimatedLocalSeconds;
    private final Consumer<RenderEngine.Tile> output;private final Consumer<Throwable> failure;private final Consumer<PixelEvaluator.Context> precision;private final Runnable changed;
    private final ArrayDeque<Integer> pending=new ArrayDeque<>();private final Map<UUID,Lease> leased=new HashMap<>();private final BitSet finished=new BitSet();
    private final Map<String,long[]> contributions=new LinkedHashMap<>();private final Map<Integer,String> owners=new HashMap<>();
    private record Lease(RenderTask task,String owner,long start){}
    private volatile RenderRequest request;private volatile PixelEvaluator.Context context;private volatile long generation=1,ended;private volatile boolean cancelled,complete;private int done;private long iterations;private int escalations;public final CompletableFuture<Void> completion=new CompletableFuture<>();
    public DistributedJob(RenderRequest request,int tileSize,double estimatedLocalSeconds,Consumer<RenderEngine.Tile> output,Consumer<Throwable> failure,Consumer<PixelEvaluator.Context> precision,Runnable changed){
        if(tileSize<16||tileSize>256)throw new IllegalArgumentException("Tile size 16–256 required");this.request=request;this.baseTileSize=tileSize;this.tileSize=tileSize;this.total=Math.multiplyExact((request.width()+tileSize-1)/tileSize,(request.height()+tileSize-1)/tileSize);if(total>1_000_000)throw new IllegalArgumentException("More than one million tiles; increase tile size");this.estimatedLocalSeconds=estimatedLocalSeconds;this.output=output;this.failure=failure;this.precision=precision;this.changed=changed;reset();
    }
    private void reset(){context=new PixelEvaluator.Context(request);
        int divisor=switch(context.plan.mode()){case DOUBLE_DOUBLE->2;case QUAD_DOUBLE,ARBITRARY->4;default->1;};tileSize=Math.max(16,baseTileSize/divisor);
        while((long)((request.width()+tileSize-1)/tileSize)*((request.height()+tileSize-1)/tileSize)>1_000_000&&tileSize<256)tileSize=Math.min(256,tileSize*2);
        total=Math.multiplyExact((request.width()+tileSize-1)/tileSize,(request.height()+tileSize-1)/tileSize);
pending.clear();leased.clear();finished.clear();owners.clear();contributions.clear();done=0;iterations=0;
        int columns=(request.width()+tileSize-1)/tileSize;
        for(int[] xy:request.order().tiles(request.width(),request.height(),tileSize))pending.add((xy[1]/tileSize)*columns+xy[0]/tileSize);precision.accept(context);
    }
    public RenderRequest request(){return request;}public PixelEvaluator.Context context(){return context;}public long generation(){return generation;}public boolean stopped(){return cancelled||complete;}public boolean cancelled(){return cancelled;}public boolean complete(){return complete;}
    public synchronized int done(){return done;}public int escalations(){return escalations;}public double seconds(){return ((ended==0?System.nanoTime():ended)-started)/1e9;}
    public synchronized List<Contribution> contributions(){return contributions.entrySet().stream().map(e->new Contribution(e.getKey(),e.getValue()[0],e.getValue()[1],e.getValue()[2],e.getValue()[3])).toList();}
    public synchronized Map<Integer,String> owners(){return Map.copyOf(owners);}public synchronized int inFlight(){return leased.size();}
    public RenderTask geometry(int tile){int columns=(request.width()+tileSize-1)/tileSize,x=(tile%columns)*tileSize,y=(tile/columns)*tileSize;return new RenderTask(id,generation,tile,UUID.randomUUID(),x,y,Math.min(tileSize,request.width()-x),Math.min(tileSize,request.height()-y));}
    public synchronized RenderTask claim(String owner){
        while(!stopped()&&!pending.isEmpty()){
            int tile=pending.removeFirst();RenderTask task=geometry(tile);
            try{context.validateTile(task.x(),task.y(),task.width(),task.height());}
            catch(CoordinateCollision ex){escalate();continue;}
            leased.put(task.assignment(),new Lease(task,owner,System.nanoTime()));return task;
        }return null;
    }
    private void escalate(){
        var old=context.plan.mode();if(old==PrecisionMode.ARBITRARY){fail(new IllegalArgumentException("Arbitrary coordinate precision budget exhausted"));return;}
        var next=PrecisionController.next(old);var r=request;request=new RenderRequest(r.viewport(),r.width(),r.height(),r.limit(),r.grid(),r.order(),r.periodicity(),r.bailoutSquared(),r.previews(),r.formula(),next,r.perturbation());generation++;escalations++;
        output.accept(new RenderEngine.Tile(0,0,0,0,new double[0],0,0,0,0,1,-1,new long[6]));reset();changed.run();
    }
    public synchronized boolean owns(RenderTask task,String owner){Lease lease=leased.get(task.assignment());return !stopped()&&task.job().equals(id)&&task.generation()==generation&&lease!=null&&lease.owner().equals(owner)&&lease.task().equals(task);}
    public synchronized boolean accept(RenderTask task,String owner,RenderEngine.Tile tile,boolean overlay){
        if(!owns(task,owner)||finished.get(task.tile()))return false;
        leased.remove(task.assignment());finished.set(task.tile());done++;iterations+=tile.iterations();long[] counts=contributions.computeIfAbsent(owner,k->new long[4]);counts[0]++;counts[1]+=(long)task.width()*task.height()*request.grid()*request.grid();counts[2]+=tile.iterations();counts[3]+=tile.nanos();if(overlay)owners.put(task.tile(),owner);
        try{output.accept(new RenderEngine.Tile(task.x(),task.y(),task.width(),task.height(),tile.values(),done,total,iterations,System.nanoTime()-started,request.grid(),1,tile.classifications()));}
        catch(Throwable error){fail(error);return false;}
        if(done==total){complete=true;ended=System.nanoTime();completion.complete(null);changed.run();}return true;
    }
    public synchronized void collision(RenderTask task,String owner){if(owns(task,owner))escalate();}
    public synchronized void release(String owner){Iterator<Lease> it=leased.values().iterator();while(it.hasNext()){Lease lease=it.next();if(lease.owner().equals(owner)){pending.addFirst(lease.task().tile());it.remove();}}}
    public synchronized boolean expired(RenderTask task,long maxMillis){Lease lease=leased.get(task.assignment());return lease!=null&&(System.nanoTime()-lease.start())/1_000_000>maxMillis;}
    public synchronized void cancel(){if(stopped())return;cancelled=true;ended=System.nanoTime();pending.clear();leased.clear();completion.completeExceptionally(new CancellationException());failure.accept(new CancellationException());changed.run();}
    public synchronized void fail(Throwable error){if(stopped())return;cancelled=true;ended=System.nanoTime();pending.clear();leased.clear();completion.completeExceptionally(error);failure.accept(error);changed.run();}
}
