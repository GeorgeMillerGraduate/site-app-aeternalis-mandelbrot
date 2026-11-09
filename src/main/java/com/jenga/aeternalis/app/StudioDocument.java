package com.jenga.aeternalis.app;
import java.util.*;
/** Bottom-to-top layer order, with an explicit active layer. */
public record StudioDocument(List<LayerState> layers,UUID active){
    public StudioDocument {
        layers=List.copyOf(layers);if(layers.isEmpty()||layers.size()>8)throw new IllegalArgumentException("Use 1–8 layers.");
        Set<UUID> ids=new HashSet<>();for(var layer:layers)if(!ids.add(layer.id()))throw new IllegalArgumentException("Duplicate layer IDs.");
        if(!ids.contains(active))throw new IllegalArgumentException("The active layer is missing.");
    }
    public static StudioDocument initial(){var l=LayerState.initial();return new StudioDocument(List.of(l),l.id());}
    public LayerState selected(){return layers.stream().filter(l->l.id().equals(active)).findFirst().orElseThrow();}
    public StudioDocument replace(LayerState layer){List<LayerState> copy=new ArrayList<>(layers);int at=-1;for(int i=0;i<copy.size();i++)if(copy.get(i).id().equals(layer.id()))at=i;if(at<0)throw new IllegalArgumentException("Unknown layer");copy.set(at,layer);return new StudioDocument(copy,active);}
    public StudioDocument select(UUID id){return new StudioDocument(layers,id);}
}
