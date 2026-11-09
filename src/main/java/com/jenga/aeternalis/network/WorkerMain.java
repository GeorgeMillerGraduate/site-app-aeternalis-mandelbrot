package com.jenga.aeternalis.network;
/** Optional headless role for the same project/engine, useful on spare machines and in process integration tests. */
public final class WorkerMain {
    public static void main(String[] args)throws Exception{
        if(args.length<2||args.length>4)throw new IllegalArgumentException("WorkerMain HOST PORT [TOKEN] [THREADS]");
        WorkerClient client=new WorkerClient(args[0],Integer.parseInt(args[1]),args.length>2?args[2]:"",args.length>3?Integer.parseInt(args[3]):Math.min(256,Runtime.getRuntime().availableProcessors()));Runtime.getRuntime().addShutdownHook(new Thread(client::close));
        System.out.println("Aeternalis Worker "+NetworkProtocol.ENGINE+" connecting to "+args[0]+":"+args[1]);
        try{new java.util.concurrent.CountDownLatch(1).await();}finally{client.close();}
    }
    private WorkerMain(){}
}
