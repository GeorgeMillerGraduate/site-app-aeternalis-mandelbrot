package com.jenga.aeternalis.rendering;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
/** Optional read-only display inventory; graphics presence never establishes compute usability. */
public final class GraphicsInventory {
    public record Adapter(String name,String vendor){}
    private GraphicsInventory(){}
    public static Adapter detect(){
        try{
            String os=System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if(os.contains("win")){
                Process process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-Command","Get-CimInstance Win32_VideoController | Select-Object Name,AdapterCompatibility | ConvertTo-Json -Compress").redirectErrorStream(true).start();
                if(!process.waitFor(3,TimeUnit.SECONDS)){process.destroyForcibly();return unknown();}
                String text=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);JsonElement root=JsonParser.parseString(text);List<String> names=new ArrayList<>(),vendors=new ArrayList<>();
                Iterable<JsonElement> adapters=root.isJsonArray()?root.getAsJsonArray():List.of(root);
                for(JsonElement entry:adapters)if(entry.isJsonObject()){var object=entry.getAsJsonObject();if(object.has("Name")&&!object.get("Name").isJsonNull())names.add(object.get("Name").getAsString());if(object.has("AdapterCompatibility")&&!object.get("AdapterCompatibility").isJsonNull())vendors.add(object.get("AdapterCompatibility").getAsString());}
                if(!names.isEmpty())return new Adapter(String.join(" / ",names),String.join(" / ",vendors));
            }else if(os.contains("linux")){
                Path drm=Path.of("/sys/class/drm");if(Files.isDirectory(drm))try(var cards=Files.list(drm)){
                    for(Path card:cards.filter(p->p.getFileName().toString().matches("card[0-9]+" )).toList()){
                        Path vendor=card.resolve("device/vendor"),device=card.resolve("device/device");if(!Files.isRegularFile(vendor))continue;String id=Files.readString(vendor).trim();String brand=switch(id){case "0x10de"->"NVIDIA";case "0x1002"->"AMD";case "0x8086"->"Intel";default->"PCI vendor "+id;};return new Adapter(brand+" display adapter "+(Files.isRegularFile(device)?Files.readString(device).trim():""),brand);
                    }
                }
            }
        }catch(Exception ignored){}return unknown();
    }
    private static Adapter unknown(){return new Adapter("Graphics adapter not exposed to this process","Unknown");}
}
