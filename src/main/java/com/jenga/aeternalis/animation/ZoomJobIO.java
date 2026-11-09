package com.jenga.aeternalis.animation;

import com.google.gson.*;
import com.jenga.aeternalis.app.*;
import com.jenga.aeternalis.math.Viewport;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/** Self-contained XML job. Exact navigation plus a frozen complete layer/palette snapshot. No external entities. */
public final class ZoomJobIO {
    private ZoomJobIO(){}
    public static void atomicMove(Path from,Path to)throws IOException{try{Files.move(from,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ex){Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);}}
    public static void save(Path file,ZoomJob job)throws IOException{
        Path parent=file.toAbsolutePath().getParent();Files.createDirectories(parent);Path tmp=Files.createTempFile(parent,"zoom-",".tmp");
        try{
            var d=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();Element root=d.createElement("aeternalis-zoom");d.appendChild(root);root.setAttribute("version","1");root.setAttribute("application",BuildInfo.VERSION);
            root.setAttribute("width",""+job.width());root.setAttribute("height",""+job.height());root.setAttribute("fps",""+job.fps());root.setAttribute("spanRetention",job.factor().toPlainString());root.setAttribute("frames",""+job.frames());
            coordinate(d,root,"start",job.start());coordinate(d,root,"destination",job.target());
            Element snapshot=d.createElement("document-json");snapshot.setTextContent(new Gson().toJson(job.destination()));root.appendChild(snapshot);
            TransformerFactory tf=TransformerFactory.newInstance();tf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING,true);Transformer tr=tf.newTransformer();tr.setOutputProperty(OutputKeys.INDENT,"yes");tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
            try(OutputStream out=Files.newOutputStream(tmp)){tr.transform(new DOMSource(d),new StreamResult(out));}atomicMove(tmp,file.toAbsolutePath());
        }catch(Exception e){throw new IOException("Cannot save zoom job: "+e.getMessage(),e);}finally{Files.deleteIfExists(tmp);}
    }
    private static void coordinate(Document d,Element root,String name,Viewport v){Element e=d.createElement(name);e.setAttribute("real",v.realExact().toString());e.setAttribute("imaginary",v.imaginaryExact().toString());e.setAttribute("span",v.spanExact().toString());root.appendChild(e);}
    public static ZoomJob load(Path file)throws IOException{
        if(Files.size(file)>3_000_000)throw new IOException("Zoom job exceeds 3 MB.");
        try{
            var f=DocumentBuilderFactory.newInstance();f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);f.setXIncludeAware(false);f.setExpandEntityReferences(false);
            Document d;try(InputStream in=Files.newInputStream(file)){d=f.newDocumentBuilder().parse(in);}Element r=d.getDocumentElement();
            if(!r.getTagName().equals("aeternalis-zoom")||!r.getAttribute("version").equals("1"))throw new IOException("Unsupported zoom job.");
            Element start=(Element)r.getElementsByTagName("start").item(0),end=(Element)r.getElementsByTagName("destination").item(0);
            var document=new Gson().fromJson(r.getElementsByTagName("document-json").item(0).getTextContent(),StudioDocument.class);
            ZoomJob job=new ZoomJob(document,parse(start),Integer.parseInt(r.getAttribute("width")),Integer.parseInt(r.getAttribute("height")),new BigDecimal(r.getAttribute("spanRetention")),Integer.parseInt(r.getAttribute("fps")));
            if(!job.target().equals(parse(end))||job.frames()!=Integer.parseInt(r.getAttribute("frames")))throw new IOException("Inconsistent zoom coordinates/frame count.");return job;
        }catch(Exception e){throw new IOException("Invalid zoom XML: "+e.getMessage(),e);}
    }
    private static Viewport parse(Element e){return Viewport.parse(e.getAttribute("real"),e.getAttribute("imaginary"),e.getAttribute("span"));}
}
