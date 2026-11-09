package com.jenga.aeternalis.distributed;
import java.util.UUID;
/** Assignment identity is independent from tile identity, so reissued tasks reject late replies. */
public record RenderTask(UUID job,long generation,int tile,UUID assignment,int x,int y,int width,int height){}
