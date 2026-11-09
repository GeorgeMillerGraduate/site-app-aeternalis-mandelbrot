package com.jenga.aeternalis.rendering;
public enum ComputeMode {
    AUTOMATIC("Automatic · validated fastest"),CPU("CPU only"),GPU("GPU preferred · safe CPU fallback");
    private final String title;ComputeMode(String title){this.title=title;}public String toString(){return title;}
}
