package com.jenga.aeternalis.colouring;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Versioned UTF-8 palette files containing positions and RGB colours. */
public final class PaletteIO {
    private PaletteIO(){}
    public static void save(Path file,Gradient gradient)throws IOException {
        Files.writeString(file,encode(gradient));
    }
    public static String encode(Gradient gradient)throws IOException {
        Properties p=new Properties();p.setProperty("format","aeternalis-palette-1");p.setProperty("count",""+gradient.stops().size());p.setProperty("interpolation",gradient.mode().name());
        for(int i=0;i<gradient.stops().size();i++){var s=gradient.stops().get(i);p.setProperty("stop."+i,s.position()+","+String.format(Locale.ROOT,"%06X",s.rgb()));}
        StringWriter out=new StringWriter();p.store(out,"Aeternalis custom cubic palette");return out.toString();
    }
    public static Gradient load(Path file)throws IOException {
        if(Files.size(file)>65536)throw new IOException("Palette file exceeds 64 KiB.");
        return decode(Files.readString(file));
    }
    public static Gradient decode(String text)throws IOException {
        if(text.length()>65536)throw new IOException("Palette text exceeds 64 KiB.");
        Properties p=new Properties();p.load(new StringReader(text));
        try{
            if(!"aeternalis-palette-1".equals(p.getProperty("format")))throw new IllegalArgumentException("Unsupported palette format.");
            int n=Integer.parseInt(p.getProperty("count"));if(n<2||n>32)throw new IllegalArgumentException("Use 2–32 stops.");List<Gradient.Stop> stops=new ArrayList<>();
            for(int i=0;i<n;i++){String[] v=p.getProperty("stop."+i,"").split(",");if(v.length!=2||!v[1].matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Invalid colour stop.");stops.add(new Gradient.Stop(Double.parseDouble(v[0]),Integer.parseInt(v[1],16)));}
            return new Gradient(stops,Gradient.Mode.valueOf(p.getProperty("interpolation","SMOOTHSTEP")));
        }catch(IllegalArgumentException ex){throw new IOException("Invalid palette: "+ex.getMessage(),ex);}
    }
}
