package org.openmrs.module.htmlformentry.element;


public abstract class ValueStub {

    private Integer id;
    private String displayValue;
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getDisplayValue() {
        return displayValue;
    }
    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }
    
    /**
     * each specific type of ValueStub should implement its own equals(Object)
     * @see org.openmrs.module.htmlformentry.element.PersonStub#equals(java.lang.Object) for an example
     */
    @Override
    public abstract boolean equals(Object o);
}
