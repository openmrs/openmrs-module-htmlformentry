package org.openmrs.module.htmlformentry.widget;

import org.apache.commons.lang.BooleanUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.openmrs.util.TimeZoneUtil.toClientTimezone;

/**
 * A widget that allows the selection of a specific day, month, and year. To handle both a date and
 * time, see {@see DateTimeWidget}.
 */
public class DateWidget implements Widget {
	
	protected Date initialValue;
	
	private String onChangeFunction;
	
	private String dateFormat;
	
	private Date maxDate;
	
	private boolean hidden = false;
	
	public DateWidget() {
	}
	
	private SimpleDateFormat dateFormat() {
		String df = dateFormat != null ? dateFormat
		        : Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_DATE_FORMAT);
		if (StringUtils.hasText(df)) {
			return new SimpleDateFormat(df, Context.getLocale());
		} else {
			return Context.getDateFormat();
		}
	}
	
	public SimpleDateFormat getDateFormatForDisplay() {
		return dateFormat();
	}
	
	public String getYearsRange() {
		return Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_YEARS_RANGE, "110,20");
	}
	
	public String jsDateFormat() {
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
	
	public String getLocaleForJquery() {
		Locale loc = Context.getLocale();
		String ret = loc.getLanguage();
		if (StringUtils.hasText(loc.getCountry())) {
			ret += "-" + loc.getCountry();
		}
		return ret;
	}
	
	/**
	 * @return The date format to print dates in the generated HTML.
	 */
	protected DateFormat getHtmlDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
			if (initialValue != null) {
				String toPrint = dateFormat().format(initialValue);
				return WidgetFactory.displayValue(toPrint);
			} else {
				return WidgetFactory.displayEmptyValue("________");
			}
		} else {
			String dateToDisplay = "";
			StringBuilder sb = new StringBuilder();
			String fieldName = context.getFieldName(this);
			boolean timezoneConversions = BooleanUtils.toBoolean(
			    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS));
			
			if (timezoneConversions) {
				dateToDisplay = toClientTimezone(initialValue, "yyyy-MM-dd");
			} else {
				if (initialValue != null) {
					dateToDisplay = getHtmlDateFormat().format(initialValue);
				}
			}
			
			if (!hidden) {
				sb.append("<input type=\"text\" size=\"10\" id=\"").append(fieldName).append("-display\"/>");
			}
			sb.append("<input type=\"hidden\" name=\"").append(fieldName).append("\" id=\"").append(fieldName).append("\"");
			if (onChangeFunction != null) {
				sb.append(" onChange=\"" + onChangeFunction + "\" ");
			}
			if (hidden && initialValue != null) {
				// set the value here, since it won't be set by the ui widget
				sb.append(" value=\"").append(dateToDisplay).append("\"");
			}
			sb.append(" />");
			
			if (!hidden) {
				if ("true".equals(
				    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_SHOW_DATE_FORMAT))) {
					sb.append(" (" + dateFormat().toLocalizedPattern().toLowerCase() + ")");
				}
				
				sb.append("<script>setupDatePicker('" + jsDateFormat() + "', '" + getYearsRange() + "','"
				        + getLocaleForJquery() + "', '#" + fieldName + "-display', '#" + fieldName + "', "
				        + (initialValue != null ? "'" + dateToDisplay + "'" : "null") + ", "
				        + (maxDate != null ? "'" + getHtmlDateFormat().format(maxDate) + "'" : "null") + ")</script>");
			}
			
			return sb.toString();
		}
	}
	
	@Override
	public Date getValue(FormEntryContext context, HttpServletRequest request) {
		try {
			Date d = (Date) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this), Date.class);
			return d;
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Illegal value");
		}
	}
	
	@Override
	public void setInitialValue(Object value) {
		initialValue = (Date) value;
	}
	
	public Object getInitialValue() {
		return this.initialValue;
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
	
	public String getOnChangeFunction() {
		return onChangeFunction;
	}
	
	public String getDateFormat() {
		return dateFormat;
	}
	
	public Date getMaxDate() {
		return maxDate;
	}
	
	public void setMaxDate(Date maxDate) {
		this.maxDate = maxDate;
	}
	
	public DateWidget clone() {
		DateWidget clone = new DateWidget();
		clone.setInitialValue(this.getInitialValue());
		clone.setOnChangeFunction(this.getOnChangeFunction());
		clone.setHidden(this.isHidden());
		clone.setDateFormat(this.getDateFormat());
		clone.setMaxDate(this.getMaxDate());
		return clone;
	}
}
