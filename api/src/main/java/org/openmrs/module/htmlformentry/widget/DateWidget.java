package org.openmrs.module.htmlformentry.widget;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A widget that allows the selection of a specific day, month, and year. To handle both
 * a date and time, see {@see DateTimeWidget}.
 */
public class DateWidget implements Widget {
    
    private Date initialValue;
    private String onChangeFunction;
    private String dateFormat;
    private boolean hidden = false;

    public DateWidget() { }

    private SimpleDateFormat dateFormat() {
    	String df = dateFormat != null ? dateFormat : Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_DATE_FORMAT); 
    	if (StringUtils.hasText(df)) {
    		return new SimpleDateFormat(df, Context.getLocale());
    	} else {
    		return Context.getDateFormat();
    	}
    }
    public String getYearsRange()
    {
    	 return Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_YEARS_RANGE, "110,20");
    }
    private String jsDateFormat() {
    	String ret = dateFormat().toPattern();
    	if (ret.contains("yyyy"))
    		ret = ret.replaceAll("yyyy", "yy"); // jquery uses yy for 4-digit years
    	else if (ret.contains("yy"))
    		ret = ret.replaceAll("yy", "y"); // jquery uses y for 2-digit years
    	if (ret.contains("MMMM"))
    		ret = ret.replaceAll("MMMM", "MM"); // jquery uses MM for long month name 
    	else if (ret.contains("MMM"))
    		ret = ret.replaceAll("MMM", "M"); // jquery uses M for short month name 
    	else if (ret.contains("MM"))
    		ret = ret.replaceAll("MM", "mm"); // jquery uses mm for 2-digit month
    	else
    		ret = ret.replaceAll("M", "m"); // jquery uses m for month with no leading zero
    	return ret;
    }
    
    private String getLocaleForJquery() {
    	Locale loc = Context.getLocale();
    	String ret = loc.getLanguage();
    	if (StringUtils.hasText(loc.getCountry())) {
    		ret += "-" + loc.getCountry();
    	}
    	return ret;
    }
    
    @Override
    public String generateHtml(FormEntryContext context) {
        if (context.getMode() == Mode.VIEW) {
            String toPrint = "";
            if (initialValue != null) {
                toPrint = dateFormat().format(initialValue);
                return WidgetFactory.displayValue(toPrint);
            } else {
                toPrint = "________";
                return WidgetFactory.displayEmptyValue(toPrint);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            String fieldName = context.getFieldName(this);
            if (!hidden) {
                sb.append("<input type=\"text\" size=\"10\" id=\"").append(fieldName).append("-display\"/>");
            }
            sb.append("<input type=\"hidden\" name=\"").append(fieldName).append("\" id=\"").append(fieldName).append("\"");
            if (onChangeFunction != null) {
                sb.append(" onChange=\"" + onChangeFunction + "\" ");
            }
            if (hidden && initialValue != null) {
                // set the value here, since it won't be set by the ui widget
                sb.append(" value=\"" + new SimpleDateFormat("yyyy-MM-dd").format(initialValue) + "\"");
            }
            sb.append(" />");

            if (!hidden) {
                if ("true".equals(Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_SHOW_DATE_FORMAT))) {
                    sb.append(" (" + dateFormat().toPattern().toLowerCase() + ")");
                }

                sb.append("<script>setupDatePicker('" + jsDateFormat() + "', '" + getYearsRange() + "','" + getLocaleForJquery() + "', '#" + fieldName + "-display', '#" + fieldName + "'");
                if (initialValue != null)
                    sb.append(", '" + new SimpleDateFormat("yyyy-MM-dd").format(initialValue) + "'");
                sb.append(")</script>");
            }
            return sb.toString();
        }
    }

    @Override
    public Date getValue(FormEntryContext context, HttpServletRequest request) {
        try {
            Date d = (Date) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this), Date.class);
            return d;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Illegal value");
        }
    }

    @Override
    public void setInitialValue(Object value) {
        initialValue = (Date) value;
    }

	public void setOnChangeFunction(String onChangeFunction) {
		this.onChangeFunction = onChangeFunction;
	}
	
	/**
	 * @param dateFormat the dateFormat to set
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isHidden() {
        return hidden;
    }
}
