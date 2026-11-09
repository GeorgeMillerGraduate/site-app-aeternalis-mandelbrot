package com.jenga.aeternalis.network;
import com.jenga.aeternalis.distributed.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.math.precision.*;
import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static com.jenga.aeternalis.network.NetworkProtocol.*;
/** A reusable CPU pool plus one TCP pull slot per configured compute thread. Slots reconnect independently. */
public final class WorkerClient implements AutoCloseable {
    public record Status(String id,int slots,int threads,int cores,long tiles,double cpuSeconds,double averageMillis,long sent,long received,int active,String error,String tasks,double seconds){}
    private final UUID group=UUID.randomUUID();private final String host,token,name;private final int port,threads;private final ExecutorService io=Executors.newVirtualThreadPerTaskExecutor(),compute;private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"aeternalis-worker-heartbeat");t.setDaemon(true);return t;});private final AtomicBoolean closed=new AtomicBoolean();private final Set<Slot> slots=ConcurrentHashMap.newKeySet();private final AtomicLong tiles=new AtomicLong(),nanos=new AtomicLong(),sent=new AtomicLong(),received=new AtomicLong();private final long started=System.nanoTime();private volatile String error="Connecting";private final java.util.LinkedHashMap<String,PixelEvaluator.Context> contexts=new java.util.LinkedHashMap<>();
    private static final class Work {final RenderTask task;final AtomicBoolean cancelled=new AtomicBoolean();volatile double progress;Work(RenderTask task){this.task=task;}}
    private static final class Slot {final Wire wire;volatile Work work;UUID job;long generation;PixelEvaluator.Context context;final AtomicBoolean requested=new AtomicBoolean();Slot(Socket s)throws IOException{wire=new Wire(s);}}
    public WorkerClient(String host,int port,String token,int threads)throws IOException{
        if(host.isBlank()||port<1||port>65535||threads<1||threads>256||token.length()>256)throw new IllegalArgumentException("Invalid Worker settings");this.host=host;this.port=port;this.token=token;this.threads=threads;String hostname;try{hostname=InetAddress.getLocalHost().getHostName();}catch(Exception e){hostname="Worker";}name=hostname;
        compute=new ThreadPoolExecutor(threads,threads,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(threads),r->{Thread t=new Thread(r,"aeternalis-worker-compute");t.setDaemon(true);return t;});for(int n=0;n<threads;n++)io.submit(this::slotLoop);timer.scheduleAtFixedRate(this::tick,100,100,TimeUnit.MILLISECONDS);
    }
    private synchronized PixelEvaluator.Context context(UUID job,long generation,RenderRequest r)throws IOException{String key=job+":"+generation;var existing=contexts.get(key);if(existing!=null){if(!existing.request.equals(r))throw new IOException("Conflicting job configuration");return existing;}var created=new PixelEvaluator.Context(r);contexts.put(key,created);while(contexts.size()>4)contexts.remove(contexts.keySet().iterator().next());return created;}
    public Status status(){return new Status(group.toString(),slots.size(),threads,Runtime.getRuntime().availableProcessors(),tiles.get(),nanos.get()/1e9,nanos.get()/1e6/Math.max(1,tiles.get()),sent.get()+slots.stream().mapToLong(s->s.wire.sent.get()).sum(),received.get()+slots.stream().mapToLong(s->s.wire.received.get()).sum(),(int)slots.stream().filter(s->s.work!=null).count(),error,slots.stream().map(s->s.work).filter(java.util.Objects::nonNull).limit(8).map(w->"tile "+w.task.tile()+" gen "+w.task.generation()).collect(java.util.stream.Collectors.joining(", ")),(System.nanoTime()-started)/1e9);}
    private void request(Slot slot)throws IOException{if(slot.work==null&&slot.requested.compareAndSet(false,true))slot.wire.send(Type.TASK_REQUEST,out->{});}
    private void slotLoop(){while(!closed.get()){
        Slot slot=null;try{
            Socket socket=new Socket();socket.connect(new InetSocketAddress(host,port),3500);slot=new Slot(socket);final Slot current=slot;
            slot.wire.send(Type.HELLO,out->{string(out,ENGINE);string(out,token);uuid(out,group);string(out,name);});slot.wire.send(Type.WORKER_CAPABILITIES,out->{out.writeInt(Runtime.getRuntime().availableProcessors());out.writeInt(threads);out.writeLong(Runtime.getRuntime().maxMemory());out.writeInt(15);});
            Message ack=slot.wire.read();if(ack.type()==Type.ERROR)throw new IOException(string(ack.input(),4096));if(ack.type()!=Type.HELLO_ACK)throw new IOException("Missing HELLO_ACK");var ackIn=ack.input();uuid(ackIn);if(!ENGINE.equals(string(ackIn,128)))throw new IOException("Incompatible Master");end(ackIn);slots.add(slot);error="Connected to "+host+":"+port;request(slot);
            while(!closed.get()){
                Message message=slot.wire.read();var in=message.input();
                switch(message.type()){
                    case JOB_CONFIGURATION->{UUID job=uuid(in);long generation=in.readLong();String text=string(in,2_000_000);end(in);if(generation<1)throw new IOException("Invalid generation");RenderRequest r=new Gson().fromJson(text,RenderRequest.class);if(r.precision()==PrecisionMode.AUTOMATIC)throw new IOException("Master must select an explicit generation precision");slot.job=job;slot.generation=generation;slot.context=context(job,generation,r);}
                    case TASK_ASSIGNMENT->{RenderTask task=TaskCodec.task(in);end(in);if(slot.work!=null||slot.context==null||!task.job().equals(slot.job)||task.generation()!=slot.generation||task.tile()<0||task.width()<1||task.height()<1||task.width()>256||task.height()>256||task.x()<0||task.y()<0||(long)task.x()+task.width()>slot.context.request.width()||(long)task.y()+task.height()>slot.context.request.height())throw new IOException("Invalid task assignment");Work work=new Work(task);slot.work=work;slot.requested.set(false);PixelEvaluator.Context context=slot.context;compute.submit(()->calculate(current,work,context));}
                    case TASK_CANCEL->{RenderTask task=TaskCodec.task(in);end(in);Work active=slot.work;if(active!=null&&active.task.equals(task))active.cancelled.set(true);}
                    case HEARTBEAT->{if(in.available()>0){int kind=in.readUnsignedByte();end(in);if(kind==2)request(slot);else slot.requested.set(false);}}
                    case JOB_COMPLETE->{if(in.available()>0)in.readBoolean();end(in);slot.requested.set(false);}
                    case ERROR->throw new IOException(string(in,4096));
                    case DISCONNECT->{end(in);throw new IOException("Master disconnected");}
                    default->throw new IOException("Unexpected Master message "+message.type());
                }
            }
        }catch(Exception ex){if(!closed.get())error=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();}
        finally{if(slot!=null){if(slot.work!=null)slot.work.cancelled.set(true);slots.remove(slot);sent.addAndGet(slot.wire.sent.get());received.addAndGet(slot.wire.received.get());slot.wire.close();}}
        if(!closed.get())try{Thread.sleep(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
    }}
    private void calculate(Slot slot,Work work,PixelEvaluator.Context context){
        try{RenderTask t=work.task;var result=TileKernel.calculate(context,t.x(),t.y(),t.width(),t.height(),()->closed.get()||work.cancelled.get()||!slots.contains(slot),p->work.progress=p);
            if(!work.cancelled.get()&&slots.contains(slot)){slot.wire.send(Type.TASK_RESULT,out->TaskCodec.result(out,t,result));tiles.incrementAndGet();nanos.addAndGet(result.nanos());}
        }catch(CancellationException ignored){}catch(Exception ex){try{slot.wire.send(Type.ERROR,out->string(out,(ex instanceof CoordinateCollision?"COLLISION:":"COMPUTE:")+ex.getMessage()));}catch(IOException ignored){}}
        finally{if(slot.work==work)slot.work=null;try{if(!closed.get()&&slots.contains(slot))request(slot);}catch(IOException ex){slot.wire.close();}}
    }
    private long lastHeartbeat;
    private void tick(){boolean heartbeat=System.nanoTime()-lastHeartbeat>2_000_000_000L;if(heartbeat)lastHeartbeat=System.nanoTime();for(Slot slot:slots)try{if(heartbeat){Work work=slot.work;if(work==null)slot.wire.send(Type.HEARTBEAT,out->{});else slot.wire.send(Type.TASK_PROGRESS,out->{TaskCodec.task(out,work.task);out.writeDouble(work.progress);});}request(slot);}catch(IOException error){slot.wire.close();}}
    public void close(){if(!closed.compareAndSet(false,true))return;for(Slot slot:slots){if(slot.work!=null)slot.work.cancelled.set(true);slot.wire.close();}timer.shutdownNow();compute.shutdownNow();io.shutdownNow();}
}
