package com.jenga.aeternalis;
import com.jenga.aeternalis.network.*;
import com.jenga.aeternalis.distributed.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.math.precision.*;
import com.jenga.aeternalis.colouring.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.file.*;
import java.net.*;
import java.io.*;
class NetworkRenderingTest {
    @TempDir Path dir;
    static void await(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);while(!condition.getAsBoolean()){if(System.nanoTime()>end)throw new AssertionError("Network condition timed out");Thread.sleep(15);}}
    static RenderRequest request(FormulaSettings.Family family,String span){return new RenderRequest(Viewport.parse("-.743643887037151000000000000000001",".131825904205330000000000000000002",span),33,25,100,2,ScanOrder.RADIAL,false,10,false,new FormulaSettings(family,7,-.8,.156),PrecisionMode.AUTOMATIC,false);}
    static List<RenderEngine.Tile> distributed(MasterServer server,RenderRequest r)throws Exception{
        List<RenderEngine.Tile> tiles=new CopyOnWriteArrayList<>();CompletableFuture<Void> result=new CompletableFuture<>();DistributedJob job=server.render(r,16,1,t->{if(t.step()<0)tiles.clear();else{tiles.add(t);if(t.completed()==t.total())result.complete(null);}},result::completeExceptionally,c->{});try{result.get(30,TimeUnit.SECONDS);}catch(TimeoutException ex){throw new AssertionError("Timed out "+job.done()+"/"+job.total+" precision="+job.context().plan+" error="+server.lastError()+" computers="+server.computers(),ex);}job.completion.get(2,TimeUnit.SECONDS);assertEquals(job.total,tiles.size());return List.copyOf(tiles);
    }
    static double[] samples(List<RenderEngine.Tile> tiles,int width,int height,int grid){double[] out=new double[width*height*grid*grid];for(var t:tiles)for(int y=0;y<t.height();y++)System.arraycopy(t.values(),y*t.width()*grid*grid,out,((t.y()+y)*width+t.x())*grid*grid,t.width()*grid*grid);return out;}
    @AfterEach void cleanup(){NetworkService.instance().close();}
    @Test void tcpWorkersMatchLocalSamplesForEveryFormulaAndPrecisionRestarts()throws Exception{
        try(var master=new MasterServer("127.0.0.1",0,"secret",2);var worker=new WorkerClient("127.0.0.1",master.port(),"secret",2)){
            master.localEnabled(false);await(()->master.workerCount()==1&&master.computers().getFirst().slots()==2);
            for(var family:FormulaSettings.Family.values())for(String span:new String[]{".03","1e-18","1e-40","1e-90"}){
                System.out.println("TCP equality "+family+" "+span);var r=request(family,span);List<RenderEngine.Tile> remote=distributed(master,r);var plan=master.job().context().plan;var exact=new RenderRequest(r.viewport(),r.width(),r.height(),r.limit(),r.grid(),r.order(),false,r.bailoutSquared(),false,r.formula(),plan.mode(),false);
                List<RenderEngine.Tile> local=new CopyOnWriteArrayList<>();CompletableFuture<Void> done=new CompletableFuture<>();try(var engine=new RenderEngine(2)){engine.render(exact,t->{if(t.step()==1){local.add(t);if(t.completed()==t.total())done.complete(null);}},done::completeExceptionally);done.get(15,TimeUnit.SECONDS);}
                assertArrayEquals(samples(local,r.width(),r.height(),r.grid()),samples(remote,r.width(),r.height(),r.grid()),family+" "+span);assertTrue(worker.status().tiles()>0);
                if(span.equals("1e-18"))assertEquals(PrecisionMode.DOUBLE_DOUBLE,plan.mode());if(span.equals("1e-40"))assertEquals(PrecisionMode.QUAD_DOUBLE,plan.mode());if(span.equals("1e-90"))assertEquals(PrecisionMode.ARBITRARY,plan.mode());
            }
        }
    }
    @Test void heterogeneousWorkersParticipateAndDisconnectedLeasesAreReissued()throws Exception{
        try(var master=new MasterServer("127.0.0.1",0,"",1);var one=new WorkerClient("127.0.0.1",master.port(),"",1);var two=new WorkerClient("127.0.0.1",master.port(),"",2)){
            await(()->master.workerCount()==2);distributed(master,request(FormulaSettings.Family.JULIA,".2"));assertEquals(2,master.workerCount());
            master.localEnabled(false);var longRequest=new RenderRequest(new Viewport(0,0,.2),256,128,3000,1,ScanOrder.SIMPLE,false,4,false,new FormulaSettings(FormulaSettings.Family.JULIA,3,0,0),PrecisionMode.DOUBLE,false);
            CompletableFuture<Void> done=new CompletableFuture<>();DistributedJob job=master.render(longRequest,16,1,t->{if(t.completed()==t.total())done.complete(null);},done::completeExceptionally,c->{});await(()->job.inFlight()>0);one.close();two.close();master.localEnabled(true);done.get(20,TimeUnit.SECONDS);assertTrue(job.complete());
            CompletableFuture<Void> discarded=new CompletableFuture<>();var cancelledJob=master.render(longRequest,16,1,t->{},discarded::completeExceptionally,c->{});cancelledJob.cancel();assertTrue(cancelledJob.cancelled());
            try(var reconnected=new WorkerClient("127.0.0.1",master.port(),"",2)){await(()->master.workerCount()>=1);distributed(master,request(FormulaSettings.Family.NEWTON,".2"));assertTrue(master.job().complete());}
        }
    }
    @Test void leaseOwnershipRejectsDuplicatesAndOldGenerations()throws Exception{
        var r=NetworkService.explicit(request(FormulaSettings.Family.MANDELBROT,".03"));AtomicInteger outputs=new AtomicInteger();DistributedJob job=new DistributedJob(r,16,1,t->outputs.incrementAndGet(),e->{},c->{},()->{});RenderTask first=job.claim("lost");job.release("lost");RenderTask replacement=job.claim("new");assertEquals(first.tile(),replacement.tile());assertNotEquals(first.assignment(),replacement.assignment());var tile=TileKernel.calculate(job.context(),replacement.x(),replacement.y(),replacement.width(),replacement.height(),()->false,p->{});assertFalse(job.accept(first,"lost",tile,false));assertTrue(job.accept(replacement,"new",tile,false));assertFalse(job.accept(replacement,"new",tile,false));assertEquals(1,outputs.get());job.cancel();assertNull(job.claim("new"));
    }
    @Test void badTokenAndVersionAreRejectedAndFramesAreBounded()throws Exception{
        try(var master=new MasterServer("127.0.0.1",0,"secret",1)){
            for(String engine:new String[]{"wrong-version",NetworkProtocol.ENGINE})try(var socket=new Socket("127.0.0.1",master.port());var wire=new NetworkProtocol.Wire(socket)){
                wire.send(NetworkProtocol.Type.HELLO,out->{NetworkProtocol.string(out,engine);NetworkProtocol.string(out,"wrong-token");NetworkProtocol.uuid(out,UUID.randomUUID());NetworkProtocol.string(out,"bad");});assertEquals(NetworkProtocol.Type.ERROR,wire.read().type());assertEquals(0,master.workerCount());
            }
        }
        try(var listener=new ServerSocket(0);var client=new Socket("127.0.0.1",listener.getLocalPort());var peer=listener.accept();var wire=new NetworkProtocol.Wire(peer)){
            var out=new DataOutputStream(client.getOutputStream());out.writeInt(NetworkProtocol.MAGIC);out.writeInt(1);out.writeByte(0);out.writeInt(Integer.MAX_VALUE);out.flush();assertThrows(IOException.class,wire::read);
        }
    }
    @Test void diskBackedPngMatchesLayeredHistogramComposition()throws Exception{
        var base=LayerState.initial().calculation(new Calculation(80,2,ScanOrder.RADIAL,false,10,false));var c=base.colours();base=base.colours(new ColourSettings(c.gradient(),c.transfer(),c.density(),c.scale(),c.offset(),c.power(),c.repeat(),c.reverse(),true,c.interior(),true));var top=base.duplicate().formula(new FormulaSettings(FormulaSettings.Family.NEWTON,5,0,0)).presentation("Newton",true,.6,com.jenga.aeternalis.layers.BlendMode.SCREEN);var doc=new StudioDocument(List.of(base,top),base.id());CompletableFuture<int[]> expected=new CompletableFuture<>();
        try(var renderer=new DocumentRenderer()){renderer.render(doc,73,51,u->{if(u.complete())expected.complete(u.pixels());},expected::completeExceptionally);expected.get(10,TimeUnit.SECONDS);}
        Path output=dir.resolve("image.png");DiskPngExport.render(doc,73,51,output,new AtomicBoolean(),s->{});var image=javax.imageio.ImageIO.read(output.toFile());assertArrayEquals(expected.get(),image.getRGB(0,0,73,51,null,0,73));
        AtomicBoolean cancel=new AtomicBoolean(true);Path incomplete=dir.resolve("cancel.png");assertThrows(CancellationException.class,()->DiskPngExport.render(doc,73,51,incomplete,cancel,s->{}));assertFalse(Files.exists(incomplete));
    }
    @Test void interactiveEngineRoutesThroughTheNetworkSession()throws Exception{
        var service=NetworkService.instance();service.host("127.0.0.1",0,"",1);service.settings(NetworkService.Mode.DISTRIBUTED,60,16);try(var worker=new WorkerClient("127.0.0.1",service.master().port(),"",1)){await(()->service.master().workerCount()==1);service.master().localEnabled(false);CompletableFuture<DocumentRenderer.Update> finished=new CompletableFuture<>();try(var renderer=new DocumentRenderer()){renderer.render(StudioDocument.initial(),70,50,u->{if(u.complete())finished.complete(u);},finished::completeExceptionally);assertEquals(3500,finished.get(15,TimeUnit.SECONDS).pixels().length);assertTrue(renderer.backendSummary().startsWith("LAN"));assertTrue(worker.status().tiles()>0);}}
    }
    @Test void cancellationThenNewJobIgnoresOldNetworkTraffic()throws Exception{
        try(var master=new MasterServer("127.0.0.1",0,"",1);var worker=new WorkerClient("127.0.0.1",master.port(),"",1)){
            master.localEnabled(false);await(()->master.workerCount()==1);var slow=new RenderRequest(new Viewport(0,0,.1),128,128,100000,1,ScanOrder.SIMPLE,false,4,false,new FormulaSettings(FormulaSettings.Family.JULIA,3,0,0),PrecisionMode.DOUBLE,false);
            var job=master.render(slow,16,1,t->{},e->{},c->{});await(()->job.inFlight()>0);job.cancel();var tiles=distributed(master,request(FormulaSettings.Family.NEWTON,".03"));assertFalse(tiles.isEmpty());assertTrue(master.job().complete());
        }
    }
    @Test void workerAutomaticallyReconnectsToRestartedMaster()throws Exception{
        MasterServer first=new MasterServer("127.0.0.1",0,"reconnect",1);int port=first.port();try(var worker=new WorkerClient("127.0.0.1",port,"reconnect",1)){
            await(()->first.workerCount()==1);first.close();try(var second=new MasterServer("127.0.0.1",port,"reconnect",1)){second.localEnabled(false);await(()->second.workerCount()==1);distributed(second,request(FormulaSettings.Family.MANDELBROT,".03"));assertTrue(worker.status().tiles()>0);}
        }finally{first.close();}
    }
    @Test void collisionAcrossTwoOtherwiseUniqueTilesRestartsGlobally(){
        java.math.BigDecimal span=new java.math.BigDecimal(Math.ulp(.75)).multiply(new java.math.BigDecimal("31.68"));
        var view=new Viewport(new java.math.BigDecimal(".75"),new java.math.BigDecimal(".2"),span);
        var request=new RenderRequest(view,32,16,32,1,ScanOrder.SIMPLE,false,4,false,FormulaSettings.DEFAULT,PrecisionMode.DOUBLE,false);AtomicInteger resets=new AtomicInteger();
        var job=new DistributedJob(request,16,1,t->{if(t.step()<0)resets.incrementAndGet();},e->{},c->{},()->{});
        RenderTask left=job.claim("computer A");assertEquals(1,left.generation());assertEquals(0,left.x());
        var result=TileKernel.calculate(job.context(),left.x(),left.y(),left.width(),left.height(),()->false,p->{});assertTrue(job.accept(left,"computer A",result,false));assertEquals(1,job.done());
        RenderTask next=job.claim("computer B");assertEquals(2,next.generation());assertEquals(PrecisionMode.DOUBLE_DOUBLE,job.context().plan.mode());assertEquals(1,resets.get());assertEquals(0,job.done());assertFalse(job.owns(left,"computer A"));job.cancel();
    }
}
