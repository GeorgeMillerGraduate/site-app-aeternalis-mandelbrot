package com.jenga.aeternalis.colouring;
/** Factory colour stops; the editor chooses the cubic interpolation mode. */
public enum Palette {
    WIKIPEDIA("Wikipedia · Beyer blue & gold",new Gradient(java.util.List.of(new Gradient.Stop(0,0x000764),new Gradient.Stop(.16,0x206bcb),new Gradient.Stop(.42,0xedffff),new Gradient.Stop(.6425,0xffaa00),new Gradient.Stop(.8575,0x310230),new Gradient.Stop(1,0x000764)))),
    OBSERVATORY("Observatory",0x081a26,0x145d70,0x57b6aa,0xf1dfaa,0xb77739,0x3b2035,0x081a26),
    EMBER("Copper & Ember",0x160f24,0x5c2744,0xbd563b,0xf9bb68,0xffefc6,0x6a443d,0x160f24),
    NOCTURNE("Nocturne",0x080b26,0x313c87,0x8477c4,0xe0d4ed,0x6ac6d0,0x144c69,0x080b26),
    HERITAGE("George’s 2025 blue & gold",0x00086a,0x378bda,0xf6fbe1,0xfda000,0x00086a),
    INK("Ink & Ivory",0x111c27,0x456c79,0xc0d2cd,0xf8edcf,0x111c27);
    private final String title;private final Gradient gradient;
    Palette(String title,Gradient gradient){this.title=title;this.gradient=gradient;}
    Palette(String title,int... colours){this.title=title;gradient=Gradient.of(colours);}
    public Gradient gradient(){return gradient;}
    public int colour(double smooth,double offset){double t=smooth/64+offset;return gradient.argb(t-Math.floor(t));}
    public String toString(){return title;}
}
