package com.jenga.aeternalis.rendering;

import org.jocl.*;
import static org.jocl.CL.*;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.math.Viewport;
import com.jenga.aeternalis.math.precision.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/** Optional OpenCL FP64 compute. Availability, numerical validation and transfer-inclusive speed are distinct states. */
public final class GpuCompute {
    public record Report(String name,String vendor,String api,boolean fp64,long globalMemory,long maxAllocation,int computeUnits,boolean validated,boolean faster,double compilationMillis,double cpuMillis,double gpuMillis,String detail) {
        public String summary(){return name+" · "+vendor+"\n"+api+" · FP64 "+(fp64?"yes":globalMemory>0?"no":"not established")+" · "+(computeUnits>0?computeUnits+" compute units":"unit count unknown")+"\n"+(globalMemory>0?(globalMemory/(1024*1024))+" MiB global / "+(maxAllocation/(1024*1024))+" MiB allocation":"Compute memory limits not reported")+"\n"+(validated?"Validated FP64 kernel":"Compute unavailable or not validated")+"\n"+detail;}
    }
    public record Result(double[] values,long iterations,long[] classifications){}
    private static final GpuCompute INSTANCE=new GpuCompute();
    public static GpuCompute instance(){return INSTANCE;}
    private final AtomicBoolean busy=new AtomicBoolean();private final LongAdder tiles=new LongAdder(),failures=new LongAdder();
    private volatile Report report=new Report("Not yet detected","Unknown","Probing OpenCL",false,0,0,0,false,false,0,0,0,"Detection runs off the JavaFX thread.");
    private volatile Driver driver;private final CompletableFuture<Report> detection;
    private GpuCompute(){detection=CompletableFuture.supplyAsync(this::probe,r->{Thread t=new Thread(r,"aeternalis-gpu-probe");t.setDaemon(true);t.start();});}
    public Report report(){return report;}public CompletableFuture<Report> detection(){return detection;}public long tiles(){return tiles.sum();}public long failures(){return failures.sum();}
    public boolean eligible(RenderRequest request,PrecisionPlan plan,ComputeMode mode){
        var r=report;return mode!=ComputeMode.CPU&&r.validated()&&(mode==ComputeMode.GPU||r.faster())&&plan.mode()==PrecisionMode.DOUBLE&&request.limit()<=20000&&(request.formula().family()==FormulaSettings.Family.MANDELBROT||request.formula().family()==FormulaSettings.Family.JULIA);
    }
    public Result tryTile(RenderRequest request,PrecisionPlan plan,ComputeMode mode,int x,int y,int w,int h,BooleanSupplier cancelled){
        if(!eligible(request,plan,mode)||!busy.compareAndSet(false,true))return null;
        try{Result result=driver.run(request,x,y,w,h,cancelled);tiles.increment();return result;}
        catch(CancellationException e){throw e;}catch(Throwable error){failures.increment();Report old=report;report=new Report(old.name(),old.vendor(),old.api(),old.fp64(),old.globalMemory(),old.maxAllocation(),old.computeUnits(),false,false,old.compilationMillis(),old.cpuMillis(),old.gpuMillis(),"GPU failed; CPU fallback: "+error.getClass().getSimpleName());return null;}
        finally{busy.set(false);}
    }
    private Report probe(){
        try{
            if(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")&&!Boolean.getBoolean("aeternalis.forceOpenCLProbe")){
                var vendors=java.nio.file.Path.of("/etc/OpenCL/vendors");boolean configured=false;
                if(java.nio.file.Files.isDirectory(vendors))try(var files=java.nio.file.Files.list(vendors)){configured=files.anyMatch(p->p.toString().endsWith(".icd"));}
                if(!configured){var display=GraphicsInventory.detect();return report=new Report(display.name(),display.vendor(),"No OpenCL ICD registered",false,0,0,0,false,false,0,0,0,"A loader library alone is not a usable compute device. CPU retained. Vulkan compute is not implemented. Advanced direct-loader probing can be enabled with aeternalis.forceOpenCLProbe.");}
            }
            CL.setExceptionsEnabled(true);int[] count={0};clGetPlatformIDs(0,null,count);if(count[0]==0)throw new IllegalStateException("No OpenCL platform exposed.");
            cl_platform_id[] platforms=new cl_platform_id[count[0]];clGetPlatformIDs(platforms.length,platforms,null);
            for(var platform:platforms){
                int[] number={0};try{clGetDeviceIDs(platform,CL_DEVICE_TYPE_GPU,0,null,number);}catch(CLException ignored){continue;}
                cl_device_id[] devices=new cl_device_id[number[0]];clGetDeviceIDs(platform,CL_DEVICE_TYPE_GPU,devices.length,devices,null);
                for(var device:devices){
                    String name=string(device,CL_DEVICE_NAME),vendor=string(device,CL_DEVICE_VENDOR),api=string(device,CL_DEVICE_VERSION);long fp=number(device,CL_DEVICE_DOUBLE_FP_CONFIG),memory=number(device,CL_DEVICE_GLOBAL_MEM_SIZE),max=number(device,CL_DEVICE_MAX_MEM_ALLOC_SIZE);int units=integer(device,CL_DEVICE_MAX_COMPUTE_UNITS);
                    if(fp==0||(fp&CL_FP_ROUND_TO_NEAREST)==0||(fp&CL_FP_DENORM)==0){report=new Report(name,vendor,api,false,memory,max,units,false,false,0,0,0,"FP64 round-to-nearest and subnormal support are required. CPU retained.");continue;}
                    long began=System.nanoTime();Driver candidate=new Driver(platform,device,max);double compilation=(System.nanoTime()-began)/1e6;
                    try{
                        validate(candidate);RenderRequest fixture=new RenderRequest(new Viewport(-.7435,.1314,.012),64,64,900,1);
                        candidate.run(fixture,0,0,64,64,()->false);cpuFixture(fixture);double[] gpuTimes=new double[3],cpuTimes=new double[3];
                        for(int i=0;i<3;i++){long t=System.nanoTime();candidate.run(fixture,0,0,64,64,()->false);gpuTimes[i]=(System.nanoTime()-t)/1e6;t=System.nanoTime();cpuFixture(fixture);cpuTimes[i]=(System.nanoTime()-t)/1e6;}
                        Arrays.sort(gpuTimes);Arrays.sort(cpuTimes);driver=candidate;
                        return report=new Report(name,vendor,api,true,memory,max,units,true,gpuTimes[1]<cpuTimes[1]*.9,compilation,cpuTimes[1],gpuTimes[1],"Mandelbrot/Julia FP64 validated against CPU and high-precision samples. Auto uses GPU only when measured faster, including transfers. Vulkan compute is not implemented.");
                    }catch(Throwable failure){candidate.close();report=new Report(name,vendor,api,true,memory,max,units,false,false,compilation,0,0,"Kernel validation failed; CPU retained: "+failure.getMessage());}
                }
            }
            if(report.name().equals("Not yet detected"))report=new Report("No GPU compute device exposed","Unknown","OpenCL loader accessible",false,0,0,0,false,false,0,0,0,"No OpenCL GPU device was enumerated. JavaFX graphics acceleration is not a compute capability. Vulkan compute is not implemented.");
        }catch(Throwable e){var display=GraphicsInventory.detect();report=new Report(display.name(),display.vendor(),"OpenCL unavailable",false,0,0,0,false,false,0,0,0,"CPU fallback. Probe: "+e.getClass().getSimpleName()+" · "+String.valueOf(e.getMessage()).replace('\n',' '));}
        return report;
    }
    private static void validate(Driver driver){
        for(var family:new FormulaSettings.Family[]{FormulaSettings.Family.MANDELBROT,FormulaSettings.Family.JULIA}){
            var formula=new FormulaSettings(family,3,-.8,.156);var request=new RenderRequest(Viewport.HOME,24,16,600,1,ScanOrder.RADIAL,false,10,false,formula);
            Result result=driver.run(request,0,0,24,16,()->false);var cpu=formula.evaluator(10,false);var reference=new PreciseFormula<>(Arithmetic.forPlan(new PrecisionPlan(PrecisionMode.ARBITRARY,80,250,false,"GPU oracle")),formula,10);
            for(int y=0;y<16;y++)for(int x=0;x<24;x++){
                double r=Viewport.HOME.realAt(x+.5,24),i=Viewport.HOME.imaginaryAt(y+.5,24,16);var v=cpu.evaluate(r,i,600,()->false);double actual=result.values()[y*24+x];
                if(v.colourable()!=Double.isFinite(actual)||v.colourable()&&Math.abs(v.smooth()-actual)>2e-7)throw new IllegalStateException("CPU/GPU sample mismatch");
                if((x+24*y)%13==0){var high=reference.evaluate(java.math.BigDecimal.valueOf(r),java.math.BigDecimal.valueOf(i),600,()->false);if(high.colourable()!=v.colourable())throw new IllegalStateException("High-precision classification mismatch");}
            }
        }
    }
    private static void cpuFixture(RenderRequest request){var formula=request.formula().evaluator(request.bailoutSquared(),false);for(int y=0;y<request.height();y++)for(int x=0;x<request.width();x++)formula.evaluate(request.viewport().realAt(x+.5,request.width()),request.viewport().imaginaryAt(y+.5,request.width(),request.height()),request.limit(),()->false);}
    private static String string(cl_device_id device,int name){long[] size={0};clGetDeviceInfo(device,name,0,null,size);byte[] bytes=new byte[(int)size[0]];clGetDeviceInfo(device,name,bytes.length,Pointer.to(bytes),null);return new String(bytes,0,Math.max(0,bytes.length-1),StandardCharsets.UTF_8);}
    private static long number(cl_device_id device,int name){long[] value={0};clGetDeviceInfo(device,name,Sizeof.cl_long,Pointer.to(value),null);return value[0];}
    private static int integer(cl_device_id device,int name){int[] value={0};clGetDeviceInfo(device,name,Sizeof.cl_int,Pointer.to(value),null);return value[0];}
    private static final class Driver implements AutoCloseable {
        private cl_context context;private cl_command_queue queue;private cl_program program;private cl_kernel kernel;private final long maxAllocation;
        Driver(cl_platform_id platform,cl_device_id device,long maxAllocation)throws Exception {
            this.maxAllocation=maxAllocation;try{cl_context_properties properties=new cl_context_properties();properties.addProperty(CL_CONTEXT_PLATFORM,platform);context=clCreateContext(properties,1,new cl_device_id[]{device},null,null,null);queue=clCreateCommandQueue(context,device,0,null);
            String source;try(var in=GpuCompute.class.getResourceAsStream("/com/jenga/aeternalis/rendering/escape-fp64.cl")){source=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}program=clCreateProgramWithSource(context,1,new String[]{source},null,null);clBuildProgram(program,0,null,"-cl-std=CL1.2 -cl-opt-disable",null,null);kernel=clCreateKernel(program,"escape_fp64",null);}catch(Exception|LinkageError failure){close();throw failure;}
        }
        Result run(RenderRequest r,int x,int y,int w,int h,BooleanSupplier cancelled){
            int total=w*h*r.grid()*r.grid();if(total*8L>maxAllocation)throw new IllegalStateException("Device allocation limit");
            double[] values=new double[total];int[] counts=new int[total],reasons=new int[total];cl_mem data=null,iterations=null,classification=null;
            try{
                data=clCreateBuffer(context,CL_MEM_WRITE_ONLY,(long)total*Sizeof.cl_double,null,null);iterations=clCreateBuffer(context,CL_MEM_WRITE_ONLY,(long)total*Sizeof.cl_int,null,null);classification=clCreateBuffer(context,CL_MEM_WRITE_ONLY,(long)total*Sizeof.cl_int,null,null);
                int at=0;clSetKernelArg(kernel,at++,Sizeof.cl_mem,Pointer.to(data));clSetKernelArg(kernel,at++,Sizeof.cl_mem,Pointer.to(iterations));clSetKernelArg(kernel,at++,Sizeof.cl_mem,Pointer.to(classification));
                for(int value:new int[]{total,x,y,w,r.width(),r.height(),r.grid(),r.limit()})clSetKernelArg(kernel,at++,Sizeof.cl_int,Pointer.to(new int[]{value}));
                boolean julia=r.formula().family()==FormulaSettings.Family.JULIA;double threshold=julia?Math.max(r.bailoutSquared(),Math.pow(Math.hypot(r.formula().juliaReal(),r.formula().juliaImaginary())+1,2)):r.bailoutSquared();
                for(double value:new double[]{r.viewport().real(),r.viewport().imaginary(),r.viewport().span(),threshold})clSetKernelArg(kernel,at++,Sizeof.cl_double,Pointer.to(new double[]{value}));
                clSetKernelArg(kernel,at++,Sizeof.cl_int,Pointer.to(new int[]{julia?1:0}));for(double value:new double[]{r.formula().juliaReal(),r.formula().juliaImaginary()})clSetKernelArg(kernel,at++,Sizeof.cl_double,Pointer.to(new double[]{value}));
                for(int offset=0;offset<total;offset+=256){if(cancelled.getAsBoolean())throw new CancellationException();clEnqueueNDRangeKernel(queue,kernel,1,new long[]{offset},new long[]{Math.min(256,total-offset)},null,0,null,null);clFinish(queue);}
                clEnqueueReadBuffer(queue,data,CL_TRUE,0,(long)total*Sizeof.cl_double,Pointer.to(values),0,null,null);clEnqueueReadBuffer(queue,iterations,CL_TRUE,0,(long)total*Sizeof.cl_int,Pointer.to(counts),0,null,null);clEnqueueReadBuffer(queue,classification,CL_TRUE,0,(long)total*Sizeof.cl_int,Pointer.to(reasons),0,null,null);
                long sum=0;long[] classes=new long[Iteration.Reason.values().length];var cpu=r.formula().evaluator(r.bailoutSquared(),r.periodicity());
                for(int n=0;n<total;n++){
                    if(reasons[n]<0){int spp=r.grid()*r.grid(),pixel=n/spp,sub=n%spp;double px=x+pixel%w+(sub%r.grid()+.5)/r.grid(),py=y+pixel/w+(sub/r.grid()+.5)/r.grid();var v=cpu.evaluate(r.viewport().realAt(px,r.width()),r.viewport().imaginaryAt(py,r.width(),r.height()),r.limit(),cancelled);values[n]=v.colourable()?v.smooth():Double.NaN;counts[n]=v.iterations();reasons[n]=v.reason().ordinal();}
                    sum+=counts[n];classes[reasons[n]]++;
                }return new Result(values,sum,classes);
            }finally{if(data!=null)clReleaseMemObject(data);if(iterations!=null)clReleaseMemObject(iterations);if(classification!=null)clReleaseMemObject(classification);}
        }
        public void close(){if(kernel!=null)clReleaseKernel(kernel);if(program!=null)clReleaseProgram(program);if(queue!=null)clReleaseCommandQueue(queue);if(context!=null)clReleaseContext(context);kernel=null;program=null;queue=null;context=null;}
    }
}
