package com.jenga.aeternalis.network;
import com.jenga.aeternalis.distributed.*;
import com.jenga.aeternalis.rendering.*;
import com.google.gson.Gson;
import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import static com.jenga.aeternalis.network.NetworkProtocol.*;
/** TCP pull scheduler: one bounded in-flight assignment per worker slot, global leases and automatic requeue. */
public final class MasterServer implements AutoCloseable {
    public record Computer(String id,String name,String address,int cores,int threads,long memory,int slots,long tiles,double cpuSeconds,double averageMillis,long sent,long received,double overheadMillis){}
    private final ServerSocket server;private final String token;private final ExecutorService io=Executors.newVirtualThreadPerTaskExecutor();private final ThreadPoolExecutor local;private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"aeternalis-network-heartbeat");t.setDaemon(true);return t;});
    private final Map<UUID,Peer> peers=new ConcurrentHashMap<>();private final Map<UUID,Node> nodes=new ConcurrentHashMap<>();private final AtomicBoolean closed=new AtomicBoolean();private volatile DistributedJob job;private volatile String lastError="";private final Map<String,String> labels=new ConcurrentHashMap<>();private volatile boolean localEnabled=true;private volatile boolean overlay;private final int localThreads;private volatile long taskTimeoutMillis=600_000;private volatile double observedRate=20_000_000.;private final AtomicLong localNanos=new AtomicLong(),localIterations=new AtomicLong();
    private static final class Node {final UUID id;final String name,address;final int cores,threads;final long memory;final AtomicLong tiles=new AtomicLong(),nanos=new AtomicLong(),sent=new AtomicLong(),received=new AtomicLong(),overhead=new AtomicLong();Node(UUID id,String name,String address,int cores,int threads,long memory){this.id=id;this.name=name;this.address=address;this.cores=cores;this.threads=threads;this.memory=memory;}}
    private final class Peer {final UUID id=UUID.randomUUID();final AtomicBoolean notifying=new AtomicBoolean(),pinging=new AtomicBoolean();final Wire wire;volatile Node node;volatile boolean ready;volatile long assigned;volatile RenderTask task;UUID configured;long configuredGeneration;Peer(Socket s)throws IOException{wire=new Wire(s);}void close(){wire.close();}}
    public MasterServer(String bind,int port,String token,int threads)throws IOException{
        if(port<0||port>65535||threads<1||threads>256||token.length()>256)throw new IllegalArgumentException("Invalid server settings");this.token=token;localThreads=threads;local=new ThreadPoolExecutor(threads,threads,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(threads),r->{Thread t=new Thread(r,"aeternalis-master-compute");t.setDaemon(true);return t;});server=new ServerSocket();server.setReuseAddress(true);bindServer(bind,port);io.submit(this::acceptLoop);clock.scheduleAtFixedRate(this::tick,2,2,TimeUnit.SECONDS);
    }
    private void bindServer(String bind,int port)throws IOException{
        long end=System.nanoTime()+2_000_000_000L;while(true)try{server.bind(new InetSocketAddress(bind,port),64);return;}catch(BindException error){if(System.nanoTime()>=end){try{server.close();}catch(IOException ignored){}throw error;}try{Thread.sleep(50);}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException("Server start interrupted",ex);}}
    }
    public String lastError(){return lastError;}public String label(String owner){return labels.getOrDefault(owner,owner);}
    public int port(){return server.getLocalPort();}public String bindAddress(){return server.getInetAddress().getHostAddress();}public int localThreads(){return localThreads;}public void localEnabled(boolean value){localEnabled=value;}public boolean localEnabled(){return localEnabled;}public void overlay(boolean value){overlay=value;}public boolean overlay(){return overlay;}public void timeout(long milliseconds){if(milliseconds<1000||milliseconds>3_600_000)throw new IllegalArgumentException("Task timeout 1–3600 seconds");taskTimeoutMillis=milliseconds;}
    public DistributedJob job(){return job;}public int workerCount(){return (int)peers.values().stream().filter(p->p.node!=null).map(p->p.node.id).distinct().count();}
    public List<Computer> computers(){List<Computer> list=new ArrayList<>();for(Node n:nodes.values()){int slots=(int)peers.values().stream().filter(p->p.node==n).count();list.add(new Computer(n.id.toString(),n.name,n.address,n.cores,n.threads,n.memory,slots,n.tiles.get(),n.nanos.get()/1e9,n.nanos.get()/1e6/Math.max(1,n.tiles.get()),n.sent.get()+peers.values().stream().filter(p->p.node==n).mapToLong(p->p.wire.sent.get()).sum(),n.received.get()+peers.values().stream().filter(p->p.node==n).mapToLong(p->p.wire.received.get()).sum(),n.overhead.get()/1e6/Math.max(1,n.tiles.get())));}return list;}
    public double iterationsPerSecond(){return Math.max(1_000_000,observedRate)*localThreads;}
    private synchronized void observe(long iterations,long nanos){if(iterations>100&&nanos>0)observedRate=.8*observedRate+.2*iterations*1e9/nanos;}
    public synchronized DistributedJob render(RenderRequest request,int tileSize,double estimate,Consumer<RenderEngine.Tile> output,Consumer<Throwable> failure,Consumer<PixelEvaluator.Context> precision){
        if(closed.get())throw new IllegalStateException("Server closed");if(!localEnabled&&workerCount()==0)throw new IllegalStateException("Connect at least one Worker or enable local Master rendering");if(job!=null)job.cancel();local.getQueue().clear();
        DistributedJob next=new DistributedJob(NetworkService.explicit(request),tileSize,estimate,output,failure,precision,this::notifyChanged);job=next;
        for(int n=0;n<localThreads;n++)local.submit(()->localLoop(next));notifyChanged();return next;
    }
    private void localLoop(DistributedJob current){try{while(!closed.get()&&!current.stopped()){
        if(!localEnabled){Thread.sleep(50);continue;}RenderTask task=current.claim("Master");if(task==null){Thread.sleep(20);continue;}var context=current.context();
        try{var result=TileKernel.calculate(context,task.x(),task.y(),task.width(),task.height(),()->closed.get()||current.stopped()||current.generation()!=task.generation(),p->{});localNanos.addAndGet(result.nanos());localIterations.addAndGet(result.iterations());observe(result.iterations(),result.nanos());current.accept(task,"Master",result,overlay);}
        catch(CancellationException ignored){}catch(com.jenga.aeternalis.math.precision.CoordinateCollision error){current.collision(task,"Master");}catch(Throwable error){current.fail(error);}
    }}catch(InterruptedException ex){Thread.currentThread().interrupt();}}
    private void acceptLoop(){while(!closed.get())try{Socket socket=server.accept();if(peers.size()>=Math.max(4,Math.min(512,Runtime.getRuntime().maxMemory()/24_000_000))){socket.close();continue;}Peer peer=new Peer(socket);peers.put(peer.id,peer);io.submit(()->serve(peer));}catch(IOException error){if(!closed.get())System.err.println("Network accept: "+error.getMessage());}}
    private void serve(Peer peer){try{
        Message hello=peer.wire.read();if(hello.type()!=Type.HELLO)throw new IOException("HELLO required");var in=hello.input();String engine=string(in,128),supplied=string(in,256);UUID group=uuid(in);String name=string(in,128);end(in);
        if(!ENGINE.equals(engine))throw new IOException("Incompatible application/rendering-engine version");if(!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new IOException("Session token rejected");
        Message caps=peer.wire.read();if(caps.type()!=Type.WORKER_CAPABILITIES)throw new IOException("Capabilities required");in=caps.input();int cores=in.readInt(),threads=in.readInt();long memory=in.readLong();int precisions=in.readInt();end(in);if(cores<1||cores>4096||threads<1||threads>256||memory<1||precisions!=15)throw new IOException("Unsupported capabilities");
        Node node=nodes.computeIfAbsent(group,k->new Node(k,name,peer.wire.socket.getInetAddress().getHostAddress(),cores,threads,memory));synchronized(node){if(node.threads!=threads||node.cores!=cores||peers.values().stream().filter(p->p.node==node).count()>=threads)throw new IOException("Too many or inconsistent worker slots");peer.node=node;}
        peer.wire.send(Type.HELLO_ACK,out->{uuid(out,peer.id);string(out,ENGINE);});
        peer.ready=true;labels.put(peer.id.toString(),name+" ["+group.toString().substring(0,8)+"]");
        while(!closed.get()){
            Message message=peer.wire.read();in=message.input();DistributedJob current=job;
            switch(message.type()){
                case TASK_REQUEST->{end(in);if(peer.task!=null&&(current==null||!current.owns(peer.task,peer.id.toString())))peer.task=null;if(peer.task!=null)throw new IOException("Only one assignment per slot is permitted");RenderTask task=current==null||current.stopped()?null:current.claim(peer.id.toString());
                    if(task==null){peer.wire.send(current!=null&&current.complete()?Type.JOB_COMPLETE:Type.HEARTBEAT,out->out.writeBoolean(true));break;}
                    peer.task=task;if(!task.job().equals(peer.configured)||task.generation()!=peer.configuredGeneration){peer.wire.send(Type.JOB_CONFIGURATION,out->{uuid(out,task.job());out.writeLong(task.generation());string(out,new Gson().toJson(current.request()));});peer.configured=task.job();peer.configuredGeneration=task.generation();}
                    peer.assigned=System.nanoTime();peer.wire.send(Type.TASK_ASSIGNMENT,out->TaskCodec.task(out,task));
                }
                case TASK_RESULT->{RenderTask task=TaskCodec.task(in);if(peer.task==null||!peer.task.equals(task)){break;}if(current==null||!current.owns(task,peer.id.toString())){peer.task=null;break;}var tile=TaskCodec.result(in,task,current.request());peer.task=null;
                    if(current.accept(task,peer.id.toString(),tile,overlay)){node.tiles.incrementAndGet();node.nanos.addAndGet(tile.nanos());node.overhead.addAndGet(Math.max(0,System.nanoTime()-peer.assigned-tile.nanos()));}}
                case TASK_PROGRESS->{RenderTask task=TaskCodec.task(in);double fraction=in.readDouble();end(in);if(!Double.isFinite(fraction)||fraction<0||fraction>1)throw new IOException("Invalid progress");}
                case HEARTBEAT->{end(in);}
                case ERROR->{String error=string(in,4096);end(in);if(peer.task!=null&&current!=null&&error.startsWith("COLLISION:"))current.collision(peer.task,peer.id.toString());else throw new IOException("Worker compute error: "+error);peer.task=null;}
                case DISCONNECT->{end(in);return;}
                default->throw new IOException("Unexpected message "+message.type());
            }

        }
    }catch(Exception error){lastError=error.getMessage()==null?"Connection closed":error.getMessage();try{peer.wire.send(Type.ERROR,out->string(out,error.getMessage()==null?"Connection closed":error.getMessage()));}catch(IOException ignored){}}
    finally{peer.close();peers.remove(peer.id);if(peer.node!=null){peer.node.sent.addAndGet(peer.wire.sent.get());peer.node.received.addAndGet(peer.wire.received.get());}DistributedJob current=job;if(current!=null)current.release(peer.id.toString());}}
    private void notifyChanged(){if(closed.get())return;for(Peer peer:peers.values())if(peer.ready&&peer.notifying.compareAndSet(false,true))io.submit(()->{try{
        RenderTask task=peer.task;DistributedJob current=job;
        if(task!=null&&(current==null||current.stopped()||task.generation()!=current.generation()||!task.job().equals(current.id)))peer.wire.send(Type.TASK_CANCEL,out->TaskCodec.task(out,task));
        else if(task==null&&current!=null&&!current.stopped())peer.wire.send(Type.HEARTBEAT,out->out.writeByte(2));
    }catch(IOException error){peer.close();}finally{peer.notifying.set(false);}});}
    private void tick(){for(Peer peer:peers.values())if(peer.ready&&peer.pinging.compareAndSet(false,true))io.submit(()->{try{DistributedJob current=job;if(peer.task!=null&&current!=null&&current.expired(peer.task,taskTimeoutMillis)){peer.close();return;}peer.wire.send(Type.HEARTBEAT,out->{});}catch(IOException error){peer.close();}finally{peer.pinging.set(false);}});}
    public void close(){if(!closed.compareAndSet(false,true))return;try{server.close();}catch(IOException ignored){}if(job!=null)job.cancel();peers.values().forEach(Peer::close);clock.shutdownNow();local.shutdownNow();io.shutdownNow();}
}
