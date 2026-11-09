package com.jenga.aeternalis.ui;
import javafx.scene.control.Spinner;
import javafx.util.StringConverter;
/** Strict editor commit: a typed iteration count can never silently render the previous value. */
public final class IterationInput {
    public static int parse(String text){
        if(text==null||!text.trim().matches("[0-9]{1,6}"))throw new IllegalArgumentException("Iteration limit must be a whole number from 32 to 100000.");
        int n=Integer.parseInt(text.trim());if(n<32||n>100000)throw new IllegalArgumentException("Iteration limit must be from 32 to 100000.");return n;
    }
    public static void install(Spinner<Integer> spinner){
        spinner.setEditable(true);spinner.getEditor().setAccessibleText("Iteration limit · type a whole number from 32 to 100000");
        spinner.getValueFactory().setConverter(new StringConverter<>(){public String toString(Integer n){return n==null?"":n.toString();}public Integer fromString(String s){return parse(s);}});
    }
    public static int commit(Spinner<Integer> spinner){int n=parse(spinner.getEditor().getText());spinner.getValueFactory().setValue(n);return n;}
    private IterationInput(){}
}
