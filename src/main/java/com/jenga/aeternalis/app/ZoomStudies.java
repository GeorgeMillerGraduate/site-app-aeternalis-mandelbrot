package com.jenga.aeternalis.app;

import com.jenga.aeternalis.math.Viewport;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

/** Offline, attributed catalogue. Decimal source strings never pass through binary doubles. */
public final class ZoomStudies {
    public static final String AUTHOR="Wolfgang Beyer", LICENSE="CC BY-SA 3.0";
    public static final String LICENSE_URL="https://creativecommons.org/licenses/by-sa/3.0/";
    public record Study(int index,String title,String real,String imaginary,String span,String image,String source,int iterations) {
        public Study { Objects.requireNonNull(title); Viewport.parse(real,imaginary,span); }
        public Viewport viewport(){return Viewport.parse(real,imaginary,span);}
    }
    private static final List<Study> ALL=load();
    public static List<Study> all(){return ALL;}
    private static List<Study> load(){
        try(var in=ZoomStudies.class.getResourceAsStream("/com/jenga/aeternalis/zoom-gallery.xml")){
            if(in==null)throw new IllegalStateException("Missing zoom gallery catalogue");
            var factory=DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
            var root=factory.newDocumentBuilder().parse(in).getDocumentElement();
            if(!root.getAttribute("version").equals("1"))throw new IllegalStateException("Unsupported gallery version");
            List<Study> result=new ArrayList<>();var nodes=root.getElementsByTagName("study");
            for(int i=0;i<nodes.getLength();i++){
                Element e=(Element)nodes.item(i);
                Study s=new Study(Integer.parseInt(e.getAttribute("index")),e.getAttribute("title"),e.getAttribute("real"),e.getAttribute("imaginary"),e.getAttribute("span"),e.getAttribute("image"),e.getAttribute("source"),Integer.parseInt(e.getAttribute("iterations")));
                if(s.index()!=i || (i>0&&result.getLast().viewport().spanExact().compareTo(s.viewport().spanExact())<=0))throw new IllegalStateException("Gallery must run widest to deepest");
                if(ZoomStudies.class.getResource(s.image())==null)throw new IllegalStateException("Missing thumbnail: "+s.image());
                result.add(s);
            }
            return List.copyOf(result);
        }catch(Exception e){throw new IllegalStateException("Could not load exact zoom catalogue",e);}
    }
    private ZoomStudies(){}
}
