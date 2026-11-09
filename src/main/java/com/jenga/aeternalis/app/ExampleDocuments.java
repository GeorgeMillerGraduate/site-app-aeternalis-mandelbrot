package com.jenga.aeternalis.app;
import com.jenga.aeternalis.fractal.*;
import com.jenga.aeternalis.colouring.*;
import com.jenga.aeternalis.layers.*;
import com.jenga.aeternalis.math.Viewport;
import com.jenga.aeternalis.persistence.ProjectIO;
import java.nio.file.*;
import java.util.*;
/** Original editable starting compositions, not screenshots or external formulas. */
public final class ExampleDocuments {
    private ExampleDocuments(){}
    private static ColourSettings colours(Palette p,boolean transparent){return new ColourSettings(p.gradient().withMode(Gradient.Mode.CATMULL_ROM),ColourSettings.Transfer.SQRT,.65,128,.05,.5,true,false,false,0x080d16,transparent);}
    public static Map<String,StudioDocument> gallery(){
        Map<String,StudioDocument> result=new LinkedHashMap<>();
        var base=LayerState.initial().presentation("Copper Mandelbrot",true,1,BlendMode.NORMAL).colours(colours(Palette.EMBER,false));
        var julia=LayerState.initial().presentation("Julia tracery",true,.62,BlendMode.SCREEN).formula(new FormulaSettings(FormulaSettings.Family.JULIA,3,-.8,.156)).view(new Viewport(0,0,3.3)).colours(colours(Palette.NOCTURNE,true));
        result.put("Copper & Nocturne · layered",new StudioDocument(List.of(base,julia),julia.id()));
        var newton=LayerState.initial().presentation("Three roots",true,1,BlendMode.NORMAL).formula(new FormulaSettings(FormulaSettings.Family.NEWTON,3,0,0)).view(new Viewport(0,0,3.2)).colours(new ColourSettings(Palette.HERITAGE.gradient().withMode(Gradient.Mode.CATMULL_ROM),ColourSettings.Transfer.LINEAR,1,64,0,.5,true,false,false,0x080d16));
        result.put("Newton · three basins",new StudioDocument(List.of(newton),newton.id()));
        var ship=LayerState.initial().presentation("Newton five roots",true,1,BlendMode.NORMAL).formula(new FormulaSettings(FormulaSettings.Family.NEWTON,5,0,0)).view(new Viewport(0,0,3.5)).colours(colours(Palette.EMBER,false));
        result.put("Newton · five basins",new StudioDocument(List.of(ship),ship.id()));
        var multi=LayerState.initial().presentation("Newton eight roots",true,1,BlendMode.NORMAL).formula(new FormulaSettings(FormulaSettings.Family.NEWTON,8,0,0)).view(new Viewport(0,0,3)).colours(colours(Palette.OBSERVATORY,false));
        result.put("Newton · eight basins",new StudioDocument(List.of(multi),multi.id()));return result;
    }
    public static void main(String[] args)throws Exception{
        Path directory=Path.of(args[0]);Files.createDirectories(directory);int n=1;
        for(var entry:gallery().entrySet())ProjectIO.save(directory.resolve("Example-"+(n++)+".aeternalis"),entry.getValue());
    }
}
