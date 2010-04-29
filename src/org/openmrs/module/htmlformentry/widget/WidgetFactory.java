package org.openmrs.module.htmlformentry.widget;

import org.openmrs.ConceptDatatype;
import org.openmrs.FormField;

/**
 * Contains shortcut methods to instantiate Widgets, and related utility methods.
 */
public class WidgetFactory {

	/**
	 * Used by {@see inferWidgetTypeHint(FormField)}.
	 */
    public enum WidgetTypeHint {
        NUMBER,
        TEXT,
        TEXTAREA,
        CHECKBOX,
        RADIO_BUTTONS,
        DROPDOWN,
        CHECKBOX_LIST,
        MULTISELECT,
        DATE,
        DATE_TIME
    }

    /**
     * Given a FormField, infers the related widget from the ConceptDatatype associated with that FormField.
     */
    public static WidgetTypeHint inferWidgetTypeHint(FormField formField) {
        ConceptDatatype dt = formField.getField().getConcept().getDatatype();
        if (dt.isText()) {
            return WidgetTypeHint.TEXT;
        } else if (dt.isNumeric()) {
            return WidgetTypeHint.NUMBER;
        } else if (dt.isDate()) {
            return WidgetTypeHint.DATE;
        } else if (dt.isCoded()) {
            if (formField.getField().isSelectMultiple())
                return WidgetTypeHint.CHECKBOX_LIST;
            else if (formField.getField().getDefaultValue() != null)
                return WidgetTypeHint.CHECKBOX;
            else
                return WidgetTypeHint.DROPDOWN;
        } else {
            throw new IllegalArgumentException(
                    "Autodetecting widget type from concept datatype not yet implemented for "
                            + dt.getName());
        }
    }

    /*
    public static Widget createWidget(FormEntryContext context, Map<String, Object> hints) {
        if (hints == null)
            throw new NullPointerException("hints must be provided");
        WidgetTypeHint typeHint = (WidgetTypeHint) hints.get("widgetType");
        if (typeHint == WidgetTypeHint.TEXT) {
            Widget w = new TextFieldWidget();
            context.registerWidget(w);
            return w;
        } else if (typeHint == WidgetTypeHint.DATE) {
            Widget w = new DateWidget();
            context.registerWidget(w);
            return w;
        } else if (typeHint == WidgetTypeHint.CHECKBOX) {
            String value = "true";
            String label = "";
            if (hints.containsKey("value"))
                value = hints.get("value").toString();
            if (hints.containsKey("label"))
                label = hints.get("label").toString();
            Widget w = new CheckboxWidget(label, value);
            context.registerWidget(w);
            return w;
        } else if (typeHint == WidgetTypeHint.NUMBER) {
            NumberFieldWidget w = new NumberFieldWidget((ConceptNumeric) hints.get("concept"));
            context.registerWidget(w);
            return w;
        } else if (typeHint == WidgetTypeHint.DROPDOWN) {
            DropdownWidget w = new DropdownWidget((Concept) hints.get("concept"));
            context.registerWidget(w);
            return w;
        } else {
            throw new IllegalArgumentException(typeHint + " not yet implemented");
        }
    }

    public static Widget createWidget(FormEntryContext context, WidgetTypeHint widgetType) {
        Map<String, Object> hints = new HashMap<String, Object>();
        hints.put("widgetType", widgetType);
        return createWidget(context, hints);
    }
    */
    
    /**
     * Formats a value for display as HTML.
     * 
     * @param the value to display
     * @return the HTML to display the value
     */
    public static String displayValue(String value) {
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");
        value = value.replace("\n", "<br/>");
        return "<span class=\"value\">" + value + "</span>";
    }
    
    /** 
     * Returns the HTML to display an empty value.
     * 
     * @param value
     * @return the HTML to display the empty value
     */
    public static String displayEmptyValue(String value) {
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");
        value = value.replace("\n", "<br/>");
        return "<span class=\"emptyValue\">" + value + "</span>";
    }
    
}
