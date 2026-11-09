package com.jenga.aeternalis.ui;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
/** Scientific-notation field with commit validation and keyboard fine/coarse adjustments. */
public final class NumericField extends TextField {
    private static final PseudoClass INVALID=PseudoClass.getPseudoClass("invalid");
    private final double min,max,defaultValue;private java.math.BigDecimal step;
    public NumericField(double value,double min,double max,double step,String label){
        super(Double.toString(value));this.min=min;this.max=max;this.step=java.math.BigDecimal.valueOf(step);this.defaultValue=value;getStyleClass().add("numeric-field");setAccessibleText(label);
        setTooltip(new Tooltip(label+"\nScientific notation accepted. ↑/↓ adjusts; Shift = fine, Alt = coarse. Ctrl+Home resets."));
        textProperty().addListener((o,a,b)->pseudoClassStateChanged(INVALID,false));
        focusedProperty().addListener((o,a,focused)->{if(!focused)try{value();}catch(IllegalArgumentException ignored){}});
        setOnKeyPressed(e->{
            if(e.getCode()==KeyCode.HOME&&e.isControlDown()){setText(Double.toString(defaultValue));e.consume();}
            else if(e.getCode()==KeyCode.UP||e.getCode()==KeyCode.DOWN){
                try{var delta=this.step.multiply(java.math.BigDecimal.valueOf((e.isAltDown()?10:e.isShiftDown()?.1:1)*(e.getCode()==KeyCode.UP?1:-1)));var next=decimalValue().add(delta);next=next.max(java.math.BigDecimal.valueOf(min)).min(java.math.BigDecimal.valueOf(max));setText(next.toString());selectAll();}catch(IllegalArgumentException ignored){}e.consume();
            }
        });
        MenuItem reset=new MenuItem("Reset to default");reset.setOnAction(e->setText(Double.toString(defaultValue)));MenuItem copy=new MenuItem("Copy value");copy.setOnAction(e->{selectAll();copy();});MenuItem cutItem=new MenuItem("Cut");cutItem.setOnAction(e->cut());MenuItem pasteItem=new MenuItem("Paste");pasteItem.setOnAction(e->paste());MenuItem all=new MenuItem("Select all");all.setOnAction(e->selectAll());setContextMenu(new ContextMenu(cutItem,copy,pasteItem,all,new SeparatorMenuItem(),reset));
    }
    public double value(){
        try{double v=Double.parseDouble(getText().trim());if(!Double.isFinite(v)||v<min||v>max)throw new NumberFormatException();pseudoClassStateChanged(INVALID,false);return v;}
        catch(NumberFormatException e){pseudoClassStateChanged(INVALID,true);throw new IllegalArgumentException(getAccessibleText()+": enter a finite value in the allowed range.");}
    }
    public void step(double step){if(Double.isFinite(step)&&step>0)this.step=java.math.BigDecimal.valueOf(step);}
    public void step(java.math.BigDecimal step){if(step.signum()>0)this.step=step;}
    public java.math.BigDecimal decimalValue(){try{var value=new java.math.BigDecimal(getText().trim());if(value.compareTo(java.math.BigDecimal.valueOf(min))<0||value.compareTo(java.math.BigDecimal.valueOf(max))>0)throw new NumberFormatException();pseudoClassStateChanged(INVALID,false);return value;}catch(NumberFormatException e){pseudoClassStateChanged(INVALID,true);throw new IllegalArgumentException(getAccessibleText()+": enter a valid decimal or scientific-notation value.");}}
}
