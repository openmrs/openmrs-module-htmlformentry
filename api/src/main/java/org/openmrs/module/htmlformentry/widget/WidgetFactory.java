package org.openmrs.module.htmlformentry.widget;

import org.openmrs.ConceptDatatype;
import org.openmrs.FormField;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.obs.handler.TextHandler;
import org.openmrs.web.WebConstants;
import org.openmrs.web.controller.observation.handler.WebImageHandler;

import java.io.*;

import static org.openmrs.obs.handler.AbstractHandler.getComplexDataFile;

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
        UPLOAD_WIDGET,
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
        } else if (HtmlFormEntryConstants.COMPLEX_UUID.equals(dt.getUuid())){
                return WidgetTypeHint.UPLOAD_WIDGET;
        }
        else {
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

    /**
     * Returns the HTML to display the complex value. If the value is
     * an image it is displayed by <img> </img> tag else it is displayed
     * as hyperlink that can be downloaded by the user.
     *
     * @param obs the obs whose complex value needs to be displayed
     * @return the HTML code that renders the complex obs
     */
    public static String displayComplexValue(Obs obs) {
        String value = null;

        WebImageHandler webImageHandler = new WebImageHandler();

        File file = getComplexDataFile(obs);
        String fileName=file.getName();

       if (webImageHandler.getObs(obs,"VIEW").getComplexData().getData()!=null) { // If the file is an image then displaying using <img> tag
            value="<a href=\"/"+WebConstants.WEBAPP_NAME+"/complexObsServlet?obsId="+obs.getId()+"\">"+"<img style=\"border:1px solid black\" class=\"complexValue\" id="+obs.getId()+" src=\"/"+WebConstants.WEBAPP_NAME+"/complexObsServlet?obsId="+obs.getId()+"\""+" height=\"80\"" +" width=\"95\""+"/>"+"</a>"+"<p class=\"value\">"+fileName+"</p>";
        }
        else
        { /* The following two commented out lines, represent possible embedding of media into the Web-Browser that may be implemented at later stages. Currently only downloading of files is possible. */
         //value="<a href="+path+obs.getId()+"&viewType=onlyDownload"+">"+"<object data="+path+obs.getId()+"&viewType=onlyDownload"+" type=\"video/webm\" id="+obs.getId()+" class=\"complexValue\">"+"<p class=\"value\">"+fileName+"</p>"+"</object>"+"</a>";
         //value="<video controls=\"controls\"  id="+obs.getId()+" class=\"complexValue\">"+"<source src="+path+obs.getId()+"&viewType=onlyDownload"+" type=\"video/webm\""+"/>"+"</video>";
            value="<a href=\"/"+WebConstants.WEBAPP_NAME+"/module/htmlformentry/downloadfile.form?obsId="+obs.getId()+"\">"+"<object id="+obs.getId()+" class=\"complexValue\">"+"<p class=\"value\">"+fileName+"</p>"+"</object>"+"</a>";
        }
        return "<span>"+value+"</span>";
    }

}
