package org.openmrs.module.htmlformentry.widget;

import org.openmrs.ConceptNumeric;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.compatibility.ConceptCompatibility;
import org.openmrs.util.OpenmrsUtil;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * A widget that implements an input field that takes a numeric answer.
 */
public class NumberFieldWidget implements Widget {

    private Number initialValue;
    private boolean floatingPoint = true;
    private Double absoluteMinimum;
    private Double absoluteMaximum;
    private Integer numberFieldSize = 5;

    /**
     * Creates a widget with certain absolute maximum and minimum values. Floating point numbers are allowed if floatingPoint=true.
     * 
     * @param absoluteMinimum
     * @param absoluteMaximum
     * @param floatingPoint
     */
    public NumberFieldWidget(Double absoluteMinimum, Double absoluteMaximum, boolean floatingPoint) {
    	this.absoluteMinimum = absoluteMinimum;
    	this.absoluteMaximum = absoluteMaximum;
    	this.floatingPoint = floatingPoint;
    }
    
    /**
     * Creates a widget with certain absolute maximum and minimum values as defined by a specific numeric Concept
     * 
     * @param concept
     * @param size, the size of the text field to render
     */
    public NumberFieldWidget(ConceptNumeric concept, String size) {
        if (concept != null) {
        	
        	ConceptCompatibility conceptCompatibility = Context.getRegisteredComponent("htmlformentry.ConceptCompatibility", ConceptCompatibility.class);
           
        	setAbsoluteMaximum(concept.getHiAbsolute());
            setAbsoluteMinimum(concept.getLowAbsolute());
            setFloatingPoint(conceptCompatibility.isAllowDecimal(concept));
            if (size != null && !size.equals("")){
                try {
                    setNumberFieldSize(Integer.valueOf(size));
                } catch (Exception ex){
                    throw new IllegalArgumentException("Value for 'size' attribute in numeric obs must be a number.");
                }
            }
        }
    }
    
    /**
     * Returns whether or not this widget accepts floating point values
     * 
     * @return true/false
     */
    public boolean isFloatingPoint() {
        return floatingPoint;
    }

    /**
     * Sets whether or not this widget accepts floating point values
     * 
     * @param floatingPoint
     */
    public void setFloatingPoint(boolean floatingPoint) {
        this.floatingPoint = floatingPoint;
    }

    /**
     * Gets the absolute minimum value allowed for this widget
     * 
     * @return absoluteMinimum
     */
    public Double getAbsoluteMinimum() {
        return absoluteMinimum;
    }

    /**
     * Sets the absolute minimum value allowed for this widget
     * 
     * @param minimum
     */
    public void setAbsoluteMinimum(Double minimum) {
        this.absoluteMinimum = minimum;
    }
    
    /**
     * Gets the absolute maximum value allowed for this widget 
     * 
     * @return absoluteMaximum
     */
    public Double getAbsoluteMaximum() {
        return absoluteMaximum;
    }

    /**
     * Sets the absolute maximum value allows for this widget
     * 
     * @param maximum
     */
    public void setAbsoluteMaximum(Double maximum) {
        this.absoluteMaximum = maximum;
    }

    @Override
    public void setInitialValue(Object initialValue) {
        this.initialValue = (Number) initialValue;
    }

    @Override
    public String generateHtml(FormEntryContext context) {
        StringBuilder sb = new StringBuilder();
        if (context.getMode() == Mode.VIEW) {
            String toPrint = "";
            if (initialValue != null) {
                toPrint = userFriendlyDisplay(initialValue);
                return WidgetFactory.displayValue(toPrint);
            } else {
                toPrint = "____";
                return WidgetFactory.displayEmptyValue(toPrint);
            }
        } else {
            String id = context.getFieldName(this);
            String errorId = context.getErrorFieldId(this);
            sb.append("<input type=\"text\" size=\"" + numberFieldSize + "\" id=\"" + id + "\" name=\"" + id + "\"");
            if (initialValue != null) {
                sb.append(" value=\"" + userFriendlyDisplay(initialValue) + "\"");
            }
            if (context.isAutomaticClientSideValidation()) {
                sb.append(" onBlur=\"checkNumber(this,'" + errorId + "'," + floatingPoint + ",");
                sb.append(absoluteMinimum + ",");
                sb.append(absoluteMaximum + ",");
                sb.append(getLocalizedErrorMessages() + ")\"");
            }
            if (context.isClientSideValidationHints()) {
                if (absoluteMinimum != null) {
                    sb.append(" min=\"" + absoluteMinimum + "\"");
                }
                if (absoluteMaximum != null) {
                    sb.append(" max=\"" + absoluteMaximum + "\"");
                }
                List<String> classes = new ArrayList<String>();
                classes.add(floatingPoint ? "number" : "integer");
                if (absoluteMinimum != null || absoluteMaximum != null) {
                    classes.add("numeric-range");
                }
                sb.append(" class=\"" + OpenmrsUtil.join(classes, " ") + "\"");
            }
            sb.append("/>");
        }
        return sb.toString();
    }

    private String userFriendlyDisplay(Number number) {
        if (number == null) {
            return "";
        }
        else if (number.doubleValue() == number.intValue()) {
            return "" + number.intValue();
        }
        else {
            return "" + number.toString();
        }
    }

    private String getLocalizedErrorMessages() {
        StringBuffer localizedErrorMessages = new StringBuffer();
        localizedErrorMessages.append("{ notANumber: '" + Context.getMessageSourceService().getMessage("htmlformentry.error.notANumber") +"',");
        localizedErrorMessages.append("notAnInteger: '" + Context.getMessageSourceService().getMessage("htmlformentry.error.notAnInteger") +"',");
        localizedErrorMessages.append("notLessThan: '" + Context.getMessageSourceService().getMessage("htmlformentry.error.notLessThan") +"',");
        localizedErrorMessages.append("notGreaterThan: '" + Context.getMessageSourceService().getMessage("htmlformentry.error.notGreaterThan") +"' } ");
        return localizedErrorMessages.toString();
    }

    @Override
    public Double getValue(FormEntryContext context, HttpServletRequest request) {
        try {
            Double d = (Double) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this), Double.class);
            if (d != null && absoluteMinimum != null && d < absoluteMinimum)
                throw new IllegalArgumentException(Context.getMessageSourceService().getMessage("htmlformentry.error.mustBeAtLeast") + " " + absoluteMinimum);
            if (d != null && absoluteMaximum != null && d > absoluteMaximum)
                throw new IllegalArgumentException(Context.getMessageSourceService().getMessage("htmlformentry.error.notGreaterThan") + " " + absoluteMaximum);
            return d;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(Context.getMessageSourceService().getMessage("htmlformentry.error.notANumber"));
        }
    }

    public void setNumberFieldSize(Integer numberFieldSize) {
        this.numberFieldSize = numberFieldSize;
    }



}
