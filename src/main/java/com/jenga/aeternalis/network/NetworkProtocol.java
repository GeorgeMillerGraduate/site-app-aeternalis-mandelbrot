package com.jenga.aeternalis.network;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
/** Bounded binary framing. Payloads are typed primitives and bounded UTF-8; never Java object serialization. */
public final class NetworkProtocol {
    public static final int MAGIC=0x4145544e,VERSION=1,MAX_PAYLOAD=8*1024*1024;
    public static final String ENGINE="aeternalis-tile-1/"+com.jenga.aeternalis.app.BuildInfo.VERSION;
    public enum Type {HELLO,HELLO_ACK,WORKER_CAPABILITIES,JOB_CONFIGURATION,TASK_REQUEST,TASK_ASSIGNMENT,TASK_PROGRESS,TASK_RESULT,TASK_CANCEL,HEARTBEAT,JOB_COMPLETE,ERROR,DISCONNECT}
    public record Message(Type type,byte[] payload){public DataInputStream input(){return new DataInputStream(new ByteArrayInputStream(payload));}}
    @FunctionalInterface public interface Payload {void write(DataOutputStream out)throws IOException;}
    public static final class Wire implements AutoCloseable {
        public final Socket socket;private final DataInputStream input;private final DataOutputStream output;public final AtomicLong sent=new AtomicLong(),received=new AtomicLong();
        public Wire(Socket socket)throws IOException{this.socket=socket;socket.setTcpNoDelay(true);socket.setKeepAlive(true);socket.setSoTimeout(12000);input=new DataInputStream(new BufferedInputStream(socket.getInputStream()));output=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));}
        public synchronized void send(Type type,Payload payload)throws IOException{ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(var out=new DataOutputStream(buffer)){payload.write(out);}byte[] bytes=buffer.toByteArray();if(bytes.length>MAX_PAYLOAD)throw new IOException("Message too large");output.writeInt(MAGIC);output.writeInt(VERSION);output.writeByte(type.ordinal());output.writeInt(bytes.length);output.write(bytes);output.flush();sent.addAndGet(13L+bytes.length);}
        public Message read()throws IOException{int magic=input.readInt(),version=input.readInt(),type=input.readUnsignedByte(),length=input.readInt();if(magic!=MAGIC||version!=VERSION||type>=Type.values().length||length<0||length>MAX_PAYLOAD)throw new IOException("Invalid protocol header/version/length");byte[] bytes=new byte[length];input.readFully(bytes);received.addAndGet(13L+length);return new Message(Type.values()[type],bytes);}
        public void close(){try{socket.close();}catch(IOException ignored){}}
    }
    public static void string(DataOutputStream out,String text)throws IOException{byte[] b=text.getBytes(StandardCharsets.UTF_8);if(b.length>2_000_000)throw new IOException("String too long");out.writeInt(b.length);out.write(b);}
    public static String string(DataInputStream in,int limit)throws IOException{int n=in.readInt();if(n<0||n>limit||n>in.available())throw new IOException("Invalid string length");byte[] b=new byte[n];in.readFully(b);try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(b)).toString();}catch(CharacterCodingException e){throw new IOException("Invalid UTF-8",e);}}
    public static void uuid(DataOutputStream out,UUID id)throws IOException{out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());}
    public static UUID uuid(DataInputStream in)throws IOException{return new UUID(in.readLong(),in.readLong());}
    public static void end(DataInputStream in)throws IOException{if(in.available()!=0)throw new IOException("Trailing payload data");}
    private NetworkProtocol(){}
}
