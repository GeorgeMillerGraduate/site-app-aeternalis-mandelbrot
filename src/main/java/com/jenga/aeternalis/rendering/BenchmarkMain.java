package com.jenga.aeternalis.rendering;
import com.jenga.aeternalis.math.*;
import com.jenga.aeternalis.math.precision.*;
import com.jenga.aeternalis.fractal.*;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Reproducible end-to-end numerical tile benchmarks, including thread scheduling and GPU transfers. */
public final class BenchmarkMain {
    private record Scenario(String name,RenderRequest request){}
    private record Row(String scenario,int width,int height,int iterations,int grid,int workers,String mode,String precision,double medianMillis,double pixelsPerSecond,long gpuTiles,long fallbacks,String status){}
    private BenchmarkMain(){}
    private static double timed(RenderEngine engine,RenderRequest request)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();long start=System.nanoTime();
        engine.render(request,t->{if(t.step()==1&&t.completed()==t.total())done.countDown();},e->{error.set(e);while(done.getCount()>0)done.countDown();});if(!done.await(120,TimeUnit.SECONDS)){throw new IllegalStateException("Benchmark timed out");}if(error.get()!=null)throw new RuntimeException(error.get());return (System.nanoTime()-start)/1e6;
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args.length==0?"benchmarks":args[0]);Files.createDirectories(output);int logical=Runtime.getRuntime().availableProcessors();var gpu=GpuCompute.instance();var report=gpu.detection().get(45,TimeUnit.SECONDS);
        List<Scenario> scenes=List.of(
            new Scenario("Mandelbrot full",new RenderRequest(Viewport.HOME,960,640,800,1)),
            new Scenario("Seahorse detail",new RenderRequest(new Viewport(-.7435,.1314,.012),640,480,1400,1)),
            new Scenario("Julia 2x2",new RenderRequest(new Viewport(0,0,3.5),640,480,1000,2,ScanOrder.RADIAL,false,10,false,new FormulaSettings(FormulaSettings.Family.JULIA,3,-.8,.156))),
            new Scenario("Deep 1e-30 perturbation",new RenderRequest(Viewport.parse("-.75",".1","1e-30"),96,64,300,1)));
        List<Row> rows=new ArrayList<>();Set<Integer> counts=new LinkedHashSet<>(List.of(1,Math.max(1,logical/2),logical));
        for(int workers:counts)try(RenderEngine engine=new RenderEngine(workers)){
            engine.computeMode(ComputeMode.CPU);
            for(var scene:scenes){
                timed(engine,scene.request());double[] samples=new double[3];long before=gpu.tiles();for(int i=0;i<3;i++)samples[i]=timed(engine,scene.request());Arrays.sort(samples);
                var context=engine.numericalContext();rows.add(new Row(scene.name(),scene.request().width(),scene.request().height(),scene.request().limit(),scene.request().grid(),workers,"CPU",context.plan.mode().name(),samples[1],scene.request().width()*(double)scene.request().height()/(samples[1]/1000),gpu.tiles()-before,context.fallbackPixels.sum(),"measured"));
                System.out.printf(Locale.ROOT,"%s · %d workers · %.3f ms%n",scene.name(),workers,samples[1]);
            }
        }
        if(report.validated())for(var mode:new ComputeMode[]{ComputeMode.GPU,ComputeMode.AUTOMATIC})try(var engine=new RenderEngine(mode==ComputeMode.GPU?1:logical)){
            engine.computeMode(mode);for(var scene:scenes.subList(0,3)){
                timed(engine,scene.request());long before=gpu.tiles();double[] samples=new double[3];for(int i=0;i<3;i++)samples[i]=timed(engine,scene.request());Arrays.sort(samples);rows.add(new Row(scene.name(),scene.request().width(),scene.request().height(),scene.request().limit(),scene.request().grid(),engine.workers(),mode.name(),"DOUBLE",samples[1],scene.request().width()*(double)scene.request().height()/(samples[1]/1000),gpu.tiles()-before,0,gpu.tiles()>before?"measured":"CPU retained by validation/speed policy"));
            }
        }
        Map<String,Object> hardware=new LinkedHashMap<>();hardware.put("java",System.getProperty("java.version"));hardware.put("os",System.getProperty("os.name")+" "+System.getProperty("os.arch"));hardware.put("logicalProcessors",logical);hardware.put("maximumHeapMiB",Runtime.getRuntime().maxMemory()/1048576);hardware.put("gpu",report);hardware.put("warmupRenders",1);hardware.put("measuredRendersPerMedian",3);hardware.put("previewPasses",false);
        var json=new GsonBuilder().setPrettyPrinting().create();Files.writeString(output.resolve("hardware.json"),json.toJson(hardware));Files.writeString(output.resolve("results.json"),json.toJson(rows));
        StringBuilder csv=new StringBuilder("scenario,width,height,iterations,grid,workers,mode,precision,median_ms,pixels_per_second,gpu_tiles,fallbacks,status\n");for(var row:rows)csv.append(String.format(Locale.ROOT,"%s,%d,%d,%d,%d,%d,%s,%s,%.4f,%.0f,%d,%d,%s%n",row.scenario(),row.width(),row.height(),row.iterations(),row.grid(),row.workers(),row.mode(),row.precision(),row.medianMillis(),row.pixelsPerSecond(),row.gpuTiles(),row.fallbacks(),row.status()));Files.writeString(output.resolve("results.csv"),csv);
        StringBuilder md=new StringBuilder("# CPU/GPU rendering measurements\n\nActual measurements on "+hardware.get("os")+", Java "+hardware.get("java")+", "+logical+" logical processors. One warm-up render, then median of three timed renders for each configuration. These are numerical tile timings; colour/composition/UI presentation are excluded. Worker-pool scheduling and OpenCL transfer costs, when applicable, are included. Reference-orbit cache is warm after the warm-up. No general speedup claim is inferred from this host.\n\n| Scene | Workers | Backend | Precision | Median ms | Numerical pixels/s |\n|---|---:|---|---|---:|---:|\n");for(var row:rows)md.append(String.format(Locale.ROOT,"| %s | %d | %s | %s | %.3f | %.0f |%n",row.scenario(),row.workers(),row.mode(),row.precision(),row.medianMillis(),row.pixelsPerSecond()));
        md.append("\nGPU report: ").append(report.summary().replace("\n"," · ")).append("\n\n");if(!report.validated())md.append("**GPU and hybrid timings were not measured:** no validated OpenCL GPU is exposed on this host. Missing GPU results are not zeros or projected speedups. Run the same benchmark on the target Windows system.\n");md.append("\nFull logical-core count can be slower on small/cheap scenes because tile scheduling, allocation and memory contention matter. Compare each scene's rows instead of treating logical processors as an automatic performance optimum. No Vulkan or GPU double-double implementation is claimed.\n");Files.writeString(output.resolve("BENCHMARKS.md"),md);
    }
}
