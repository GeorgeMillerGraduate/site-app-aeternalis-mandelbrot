package com.jenga.aeternalis.rendering;

import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.layers.*;
import com.jenga.aeternalis.math.precision.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/** Sequential layer orchestration over parallel tile workers, bounded cached sample data and background composition. */
public final class DocumentRenderer implements AutoCloseable {
    public record Update(int[] pixels,int width,int height,double progress,boolean complete,String status,long numericalPasses,long[] classifications){}
    private static final class Cache {
        final RenderRequest key;final List<RenderEngine.Tile> tiles;final long[] reasons;
        ColourSettings colours;int[] pixels;
        Cache(RenderRequest key,List<RenderEngine.Tile> tiles,long[] reasons){this.key=key;this.tiles=tiles;this.reasons=reasons;}
        long samples(){return (long)key.width()*key.height()*key.grid()*key.grid();}
    }
    private final RenderEngine engine=new RenderEngine();
    private final ThreadPoolExecutor coordinator=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>(),r->{Thread t=new Thread(r,"aeternalis-document");t.setDaemon(true);return t;});
    private final LinkedHashMap<UUID,Cache> caches=new LinkedHashMap<>(16,.75f,true);
    private final AtomicLong generation=new AtomicLong(),numericalPasses=new AtomicLong(),cacheHits=new AtomicLong(),escalations=new AtomicLong();
    private volatile boolean invalidateCaches;
    private final AtomicLong processedPixels=new AtomicLong();private volatile long beganNanos,endedNanos;
    public void invalidate(){invalidateCaches=true;cancel();}
    public long processedPixels(){return processedPixels.get();}public double durationSeconds(){return ((endedNanos==0?System.nanoTime():endedNanos)-beganNanos)/1e9;}
    private volatile RenderEngine.Job activeJob;private Future<?> task;
    private volatile boolean automaticWorkers=true;private volatile int manualWorkers=Runtime.getRuntime().availableProcessors();
    public void configureWorkers(boolean automatic,int count){automaticWorkers=automatic;manualWorkers=count;cancel();}
    public PixelEvaluator.Context currentContext(){return engine.numericalContext();}
    public int workers(){return engine.workers();}
    public String precisionSummary(){var context=engine.numericalContext();return context==null?"Awaiting first render":context.plan+"\n"+context.plan.reason();}
    public void computeMode(ComputeMode mode){engine.computeMode(mode);invalidateCaches=true;cancel();}
    public String backendSummary(){return engine.backendSummary();}
    public long cacheHits(){return cacheHits.get();}public long escalationEvents(){return escalations.get();}
    public long perturbationFallbacks(){var c=engine.numericalContext();return c==null?0:c.fallbackPixels.sum();}
    public long perturbationPixels(){var c=engine.numericalContext();return c==null?0:c.perturbationPixels.sum();}
    public long numericalPasses(){return numericalPasses.get();}
    public long generation(){return generation.get();}
    public synchronized long render(StudioDocument document,int width,int height,Consumer<Update> update,Consumer<Throwable> failure){
        long id=generation.incrementAndGet();if(activeJob!=null)activeJob.cancel();if(task!=null)task.cancel(false);coordinator.getQueue().clear();
        task=coordinator.submit(()->{
            BooleanSupplier cancelled=()->generation.get()!=id||Thread.currentThread().isInterrupted();
            try{calculate(document,width,height,cancelled,u->{if(!cancelled.getAsBoolean())update.accept(u);});}
            catch(CancellationException ignored){}catch(Throwable e){if(!cancelled.getAsBoolean())failure.accept(e);}
        });return id;
    }
    private void calculate(StudioDocument document,int width,int height,BooleanSupplier cancelled,Consumer<Update> listener)throws Exception {
        beganNanos=System.nanoTime();endedNanos=0;processedPixels.set(0);engine.configureWorkers(automaticWorkers?Runtime.getRuntime().availableProcessors():manualWorkers);if(invalidateCaches){caches.clear();invalidateCaches=false;}
        List<LayerState> visible=document.layers().stream().filter(l->l.visible()&&l.opacity()>0).toList();
        long needed=0;for(var layer:visible){int grid=layer.calculation().grid();needed+=(long)width*height*grid*grid;
            PrecisionController.analyse(layer.viewport(),width,height,grid,layer.calculation().limit(),layer.calculation().precision());
        }
        if(needed>sampleBudget())throw new IllegalArgumentException("The visible stack exceeds the heap-aware retained-sample budget (up to 32 million). Reduce layer quality, hide layers, or make the window smaller.");
        Set<UUID> present=new HashSet<>();document.layers().forEach(l->present.add(l.id()));caches.keySet().removeIf(k->!present.contains(k));
        List<Compositor.Input> rendered=new ArrayList<>();long[] totalReasons=new long[com.jenga.aeternalis.fractal.Iteration.Reason.values().length];int index=0;
        for(var layer:visible){
            if(cancelled.getAsBoolean())throw new CancellationException();
            Calculation c=layer.calculation();if((long)width*height*c.grid()*c.grid()>16_000_000)throw new IllegalArgumentException("Interactive layer exceeds 16 million samples. Use Network high-resolution PNG export.");RenderRequest key=new RenderRequest(layer.viewport(),width,height,c.limit(),c.grid(),c.order(),c.periodicity(),c.bailoutSquared(),c.previews(),layer.formula(),c.precision(),c.perturbation());
            Cache cache=caches.get(layer.id());
            if(cache==null||!cache.key.equals(key)){
                caches.remove(layer.id());numericalPasses.incrementAndGet();if(PrecisionController.analyse(layer.viewport(),width,height,c.grid(),c.limit(),c.precision()).mode().ordinal()>Math.max(PrecisionMode.DOUBLE.ordinal(),c.precision().ordinal()))escalations.incrementAndGet();
                List<RenderEngine.Tile> tiles=new CopyOnWriteArrayList<>();ConcurrentMap<Integer,RenderEngine.Tile> previews=new ConcurrentHashMap<>();
                AtomicReference<Throwable> failed=new AtomicReference<>();AtomicInteger done=new AtomicInteger();
                int tileSize=RenderEngine.tileSize(key);int count=((width+tileSize-1)/tileSize)*((height+tileSize-1)/tileSize);AtomicInteger totalTiles=new AtomicInteger(count);CountDownLatch finished=new CountDownLatch(1);
                activeJob=engine.render(key,t->{if(cancelled.getAsBoolean())return;if(t.step()<0){tiles.clear();previews.clear();done.set(0);processedPixels.set(0);escalations.incrementAndGet();return;}if(t.step()>1){previews.put(t.step(),t);return;}tiles.add(t);processedPixels.addAndGet((long)t.width()*t.height());totalTiles.set(t.total());done.incrementAndGet();if(t.completed()==t.total())finished.countDown();},e->{failed.set(e);while(finished.getCount()>0)finished.countDown();});
                long lastImage=0;
                while(!finished.await(80,TimeUnit.MILLISECONDS)){
                    if(cancelled.getAsBoolean()){activeJob.cancel();throw new CancellationException();}
                    if(failed.get()!=null)throw new RuntimeException(failed.get());
                    long now=System.nanoTime();int[] partial=null;
                    if(now-lastImage>750_000_000L){
                        List<RenderEngine.Tile> snapshot=new ArrayList<>(previews.values());snapshot.sort(Comparator.comparingInt(RenderEngine.Tile::step).reversed());snapshot.addAll(tiles);
                        int[] pixels=ColourRenderer.render(snapshot,width,height,c.grid(),layer.colours(),cancelled);
                        List<Compositor.Input> stack=new ArrayList<>(rendered);stack.add(new Compositor.Input(pixels,layer.opacity(),layer.blend()));partial=Compositor.compose(stack,width*height,cancelled);lastImage=System.nanoTime();
                    }
                    listener.accept(new Update(partial,width,height,(index+done.get()/(double)totalTiles.get())/Math.max(1,visible.size()),false,"Rendering "+layer.name()+" · "+done.get()+" / "+totalTiles.get()+" tiles"+(done.get()==0?" · refining precision-safe preview":""),numericalPasses.get(),totalReasons.clone()));
                }
                if(cancelled.getAsBoolean())throw new CancellationException();if(failed.get()!=null)throw new RuntimeException(failed.get());
                List<RenderEngine.Tile> complete=List.copyOf(tiles);long[] reasons=new long[totalReasons.length];
                for(var tile:complete)for(int i=0;i<reasons.length;i++)reasons[i]+=tile.classifications()[i];
                cache=new Cache(key,complete,reasons);caches.put(layer.id(),cache);trimCaches();
            }
            else cacheHits.incrementAndGet();
            if(cache.pixels==null||!layer.colours().equals(cache.colours)){
                cache.pixels=ColourRenderer.render(cache.tiles,width,height,c.grid(),layer.colours(),cancelled);cache.colours=layer.colours();
            }
            rendered.add(new Compositor.Input(cache.pixels,layer.opacity(),layer.blend()));for(int i=0;i<totalReasons.length;i++)totalReasons[i]+=cache.reasons[i];index++;
            int[] composite=Compositor.compose(rendered,width*height,cancelled);
            listener.accept(new Update(composite,width,height,index/(double)Math.max(1,visible.size()),index==visible.size(),index==visible.size()?"Render complete":"Composited "+layer.name(),numericalPasses.get(),totalReasons.clone()));
        }
        endedNanos=System.nanoTime();
        if(visible.isEmpty())listener.accept(new Update(new int[width*height],width,height,1,true,"Render complete",numericalPasses.get(),totalReasons));
    }
    public long sampleBudget(){return Math.min(32_000_000L,Math.max(65536L,(long)(Runtime.getRuntime().maxMemory()*.35)/8));}
    private void trimCaches(){long count=caches.values().stream().mapToLong(Cache::samples).sum();var iterator=caches.entrySet().iterator();while(count>sampleBudget()&&iterator.hasNext()){var entry=iterator.next();count-=entry.getValue().samples();iterator.remove();}}
    public synchronized void cancel(){generation.incrementAndGet();if(activeJob!=null)activeJob.cancel();if(task!=null)task.cancel(false);coordinator.getQueue().clear();}
    public void close(){cancel();coordinator.shutdownNow();engine.close();}
}
