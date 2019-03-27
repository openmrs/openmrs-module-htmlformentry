package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for generating and storing widgets for the FormEntryContext
 */
public class WidgetRegister {

    protected final Log log = LogFactory.getLog(getClass());

    private Integer sequenceNextVal = 1;
    private Map<Widget, String> fieldNames = new HashMap<Widget, String>();
    private Map<Widget, ErrorWidget> errorWidgets = new HashMap<Widget, ErrorWidget>();

    /**
     * Registers a widget within the Context
     *
     * @param widget the widget to register
     * @return the field id used to identify this widget in the HTML Form
     */
    public String registerWidget(Widget widget) {
        if (fieldNames.containsKey(widget)) {
            throw new IllegalArgumentException("This widget is already registered");
        }
        int thisVal = 0;
        synchronized (sequenceNextVal) {
            thisVal = sequenceNextVal;
            sequenceNextVal = sequenceNextVal + 1;
        }
        String fieldName = "w" + thisVal;
        fieldNames.put(widget, fieldName);
        if (log.isTraceEnabled()) {
            log.trace("Registered widget " + widget.getClass() + " as " + fieldName);
        }
        return fieldName;
    }

    /**
     * Registers an error widget within the Context
     *
     * @param widget: the widget to associate this error widget with
     * @param errorWidget: the error widget to register
     * @return the field id used to identify this widget in the HTML Form
     */
    public String registerErrorWidget(Widget widget, ErrorWidget errorWidget) {
        String errorWidgetId;
        if (!fieldNames.containsKey(errorWidget)) {
            errorWidgetId = registerWidget(errorWidget);
        }
        else {
            errorWidgetId = getFieldName(errorWidget);
        }
        errorWidgets.put(widget, errorWidget);
        return errorWidgetId;
    }

    /**
     * Gets the field id used to identify a specific widget within the HTML Form
     *
     * @param widget the widget
     * @return the field id associated with the widget in the HTML Form
     * @throws IllegalArgumentException if the given widget is not registered
     */
    public String getFieldName(Widget widget) {
        String fieldName = fieldNames.get(widget);
        if (fieldName == null) {
            throw new IllegalArgumentException("Widget not registered");
        }
        else {
            return fieldName;
        }
    }

    /**
     * Like {@link #getFieldName(Widget)} but returns null if the widget is not registered (instead
     * of throwing an exception).
     * @param widget
     */
    public String getFieldNameIfRegistered(Widget widget) {
        return fieldNames.get(widget);
    }

    /**
     * @return the widget that is registered for the given field name, or null if there is none
     */
    public Widget getWidgetByFieldName(String fieldName) {
        for (Map.Entry<Widget, String> e : fieldNames.entrySet()) {
            if (e.getValue().equals(fieldName)) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * Gets the field id used to identify a specific error widget within the HTML Form
     *
     * @param widget the widget
     * @return the field id associated with the error widget in the HTML Form
     */
    public String getErrorFieldId(Widget widget) {
        return getFieldName(errorWidgets.get(widget));
    }

    /**
     * Gets the fields ids for all currently registered error widgets
     *
     * @return a set of all the field ids for all currently registered error widgets
     */
    public Collection<String> getErrorDivIds() {
        Set<String> ret = new HashSet<String>();
        for (ErrorWidget e : errorWidgets.values()) {
            ret.add(getFieldName(e));
        }
        return ret;
    }

    public Map<Widget, String> getFieldNames() {
        return fieldNames;
    }


}
