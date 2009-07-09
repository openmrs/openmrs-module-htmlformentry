package org.openmrs.module.htmlformentry.widget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.springframework.util.StringUtils;

public class LocationWidget implements Widget {
   
    private Location location;
    private List<Location> options;
    
    public LocationWidget() { }

    public String generateHtml(FormEntryContext context) {
        if (context.getMode() == Mode.VIEW) {
            if (location != null)
                return WidgetFactory.displayValue(location.getName());
            else
                return "";
        }
           
        StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"" + context.getFieldName(this) + "\">");
        sb.append("\n<option value=\"\">");
        sb.append(Context.getMessageSourceService().getMessage("htmlformentry.chooseALocation"));
        sb.append("</option");
        List<Location> useLocations;
        if (options != null) {
            useLocations = options;
        } else {
            useLocations = Context.getLocationService().getAllLocations();
            Collections.sort(useLocations, new Comparator<Location>() {
                public int compare(Location left, Location right) {
                    return left.getName().compareTo(right.getName());
                }
            });
        }
        for (Location l : useLocations) {
            sb.append("\n<option");
            if (location != null && location.equals(l))
                sb.append(" selected=\"true\"");
            sb.append(" value=\"" + l.getLocationId() + "\">").append(l.getName()).append("</option>");
        }
        sb.append("</select>");
        return sb.toString();
    }

    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        String val = request.getParameter(context.getFieldName(this));
        if (StringUtils.hasText(val))
            return HtmlFormEntryUtil.convertToType(val, Location.class);
        return null;
    }

    public void setInitialValue(Object initialValue) {
        location = (Location) initialValue;
    }

    public void setOptions(List<Location> options) {
        this.options = options;        
    }
    
}
