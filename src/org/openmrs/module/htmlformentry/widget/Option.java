package org.openmrs.module.htmlformentry.widget;

public class Option {

    private String label;
    private String value;
    private boolean selected = false;
    
    public Option() {
        value = "";
        label = "";
    }

    public Option(String label, String value, boolean selected) {
        this.label = label;
        this.value = value;
        this.selected = selected;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getValueOrLabel() {
        if (value != null)
            return value;
        else
            return label;
    }
}
