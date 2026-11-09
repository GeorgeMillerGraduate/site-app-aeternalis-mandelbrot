package com.jenga.aeternalis.distributed;
import com.jenga.aeternalis.network.*;
import com.jenga.aeternalis.rendering.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.security.MessageDigest;
/** Actual TCP and separate JVMs on loopback. This is not a physical-LAN speedup claim. */
public final class NetworkBenchmark {
    private record Scene(String name,RenderRequest request){}
    private record Run(double milliseconds,String hash,long remoteTiles,long sent,long received,double overheadMillis){}
    private record Row(String scene,int remoteProcesses,int masterThreads,int workerThreads,double medianMillis,double speedup,long remoteTiles,long sent,long received,double overheadMillis,String hash){}
    private static Run run(MasterServer server,RenderRequest request)throws Exception{
        CompletableFuture<Void> done=new CompletableFuture<>();double[] samples=new double[request.width()*request.height()*request.grid()*request.grid()];long tx=server.computers().stream().mapToLong(MasterServer.Computer::sent).sum(),rx=server.computers().stream().mapToLong(MasterServer.Computer::received).sum(),start=System.nanoTime();
        DistributedJob job=server.render(request,64,0,t->{if(t.step()<0){Arrays.fill(samples,0);return;}for(int y=0;y<t.height();y++)System.arraycopy(t.values(),y*t.width()*request.grid()*request.grid(),samples,((t.y()+y)*request.width()+t.x())*request.grid()*request.grid(),t.width()*request.grid()*request.grid());if(t.completed()==t.total())done.complete(null);},done::completeExceptionally,c->{});
        done.get(120,TimeUnit.SECONDS);job.completion.get(5,TimeUnit.SECONDS);double ms=(System.nanoTime()-start)/1e6;MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] bytes=new byte[8];for(double v:samples){long bits=Double.doubleToLongBits(v);for(int n=0;n<8;n++)bytes[n]=(byte)(bits>>>(n*8));digest.update(bytes);}long remote=job.contributions().stream().filter(c->!c.computer().equals("Master")).mapToLong(DistributedJob.Contribution::tiles).sum();var computers=server.computers();return new Run(ms,HexFormat.of().formatHex(digest.digest()),remote,computers.stream().mapToLong(MasterServer.Computer::sent).sum()-tx,computers.stream().mapToLong(MasterServer.Computer::received).sum()-rx,computers.stream().mapToDouble(MasterServer.Computer::overheadMillis).average().orElse(0));
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args.length>0?args[0]:"benchmarks/network");Files.createDirectories(output);List<Scene> scenes=List.of(
            new Scene("Seahorse",new RenderRequest(new Viewport(-.7435,.1314,.012),640,480,2400,1)),
            new Scene("Newton n8",new RenderRequest(new Viewport(0,0,3.5),640,480,160,1,ScanOrder.RADIAL,false,10,false,new FormulaSettings(FormulaSettings.Family.NEWTON,8,0,0)))
        );List<Row> rows=new ArrayList<>();Map<String,String> hashes=new HashMap<>();Map<String,Double> baseline=new HashMap<>();
        for(int workers=0;workers<=3;workers++){
            List<Process> processes=new ArrayList<>();try(var server=new MasterServer("127.0.0.1",0,"benchmark",2)){
                try{for(int n=0;n<workers;n++)processes.add(new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx256m","-cp",System.getProperty("java.class.path"),"com.jenga.aeternalis.network.WorkerMain","127.0.0.1",""+server.port(),"benchmark","1").redirectErrorStream(true).redirectOutput(output.resolve("worker-"+workers+"-"+n+".log").toFile()).start());
                    long timeout=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(server.workerCount()<workers){if(System.nanoTime()>timeout)throw new IllegalStateException("Workers did not connect");Thread.sleep(20);}
                    for(var scene:scenes){run(server,scene.request());List<Run> timed=new ArrayList<>();for(int repeat=0;repeat<3;repeat++)timed.add(run(server,scene.request()));timed.sort(Comparator.comparingDouble(Run::milliseconds));Run median=timed.get(1);String reference=hashes.putIfAbsent(scene.name(),median.hash());if(reference!=null&&!reference.equals(median.hash()))throw new AssertionError("Output hash differs: "+scene.name());if(workers==0)baseline.put(scene.name(),median.milliseconds());rows.add(new Row(scene.name(),workers,2,1,median.milliseconds(),baseline.get(scene.name())/median.milliseconds(),median.remoteTiles(),median.sent(),median.received(),median.overheadMillis(),median.hash()));System.out.println(scene.name()+" workers="+workers+" median="+median.milliseconds()+"ms remoteTiles="+median.remoteTiles());}
                }finally{for(var process:processes)process.destroy();for(var process:processes)if(!process.waitFor(3,TimeUnit.SECONDS))process.destroyForcibly();}
            }
        }
        Files.writeString(output.resolve("results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));StringBuilder csv=new StringBuilder("scene,remote_processes,master_threads,worker_threads,median_ms,measured_speedup,remote_tiles,tx_bytes,rx_bytes,noncompute_roundtrip_ms,sha256\n"),md=new StringBuilder("# Actual loopback multi-process benchmarks\n\nJava "+System.getProperty("java.version")+", "+System.getProperty("os.name")+" "+System.getProperty("os.arch")+", "+Runtime.getRuntime().availableProcessors()+" logical processors. Each worker is a separate JVM with one compute thread; the Master uses two compute threads. One warm-up then median of three numerical renders. TCP, serialization, scheduling and process execution are timed; colour/UI/PNG encoding are excluded. All processes share one physical host and its CPU/memory; these are **not physical-LAN benchmarks**. Speedup is measured against the same scheduler with only the two-thread Master. Noncompute task round trip includes queueing/scheduling, not just network transmission. Output SHA-256 hashes match across all configurations.\n\n| Scene | Worker JVMs | Median ms | Measured speedup | Remote tiles | TX bytes | RX bytes |\n|---|---:|---:|---:|---:|---:|---:|\n");
        for(var r:rows){csv.append(String.format(Locale.ROOT,"%s,%d,%d,%d,%.3f,%.3f,%d,%d,%d,%.3f,%s%n",r.scene(),r.remoteProcesses(),r.masterThreads(),r.workerThreads(),r.medianMillis(),r.speedup(),r.remoteTiles(),r.sent(),r.received(),r.overheadMillis(),r.hash()));md.append(String.format(Locale.ROOT,"| %s | %d | %.3f | %.3f× | %d | %d | %d |%n",r.scene(),r.remoteProcesses(),r.medianMillis(),r.speedup(),r.remoteTiles(),r.sent(),r.received()));}
        md.append("\nFaster completion is workload-dependent. This host's loopback results do not predict performance on separate home computers, Wi-Fi or Ethernet.\n");Files.writeString(output.resolve("results.csv"),csv);Files.writeString(output.resolve("BENCHMARKS.md"),md);
    }
    private NetworkBenchmark(){}
}
