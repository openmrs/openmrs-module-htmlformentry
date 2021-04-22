package org.openmrs.module.htmlformentry.widget;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

/**
 * A widget that allows the selection of a certain time-of-day. To handle both a date and time, see
 * {@see DateTimeWidget}.
 */
public class TimeWidget implements Widget {
	
	public static final String DEFAULT_TIME_FORMAT = "HH:mm";
	
	private Date initialValue;
	
	private boolean hidden;
	
	private boolean hideSeconds = false;
	
	private String timeFormat;
	
	public TimeWidget() {
		
	}
	
	private SimpleDateFormat timeFormat() {
		String df = timeFormat != null ? timeFormat
		        : Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_TIME_FORMAT);
		if (!StringUtils.hasText(df)) {
			df = DEFAULT_TIME_FORMAT;
		}
		return new SimpleDateFormat(df, Context.getLocale());
	}
	
	public String generateEditModeHtml(FormEntryContext context, String fieldName, Calendar valAsCal) {
		StringBuilder sb = new StringBuilder();
		
		if (hidden) {
			sb.append("<input type=\"hidden\" class=\"hfe-hours\" name=\"").append(fieldName).append("hours")
			        .append("\" value=\"" + new SimpleDateFormat("HH").format(initialValue) + "\"/>");
			sb.append("<input type=\"hidden\" class=\"hfe-minutes\" name=\"").append(fieldName).append("minutes")
			        .append("\" value=\"" + new SimpleDateFormat("mm").format(initialValue) + "\"/>");
			if (!hideSeconds) {
				sb.append("<input type=\"hidden\" class=\"hfe-seconds\" name=\"").append(fieldName).append("seconds")
				        .append("\" value=\"" + new SimpleDateFormat("ss").format(initialValue) + "\"/>");
			}
		} else {
			sb.append("<select class=\"hfe-hours\" name=\"").append(fieldName).append("hours").append("\">");
			for (int i = 0; i <= 23; ++i) {
				String label = "" + i;
				if (label.length() == 1)
					label = "0" + label;
				sb.append("<option value=\"" + i + "\"");
				if (valAsCal != null) {
					if (valAsCal.get(Calendar.HOUR_OF_DAY) == i)
						sb.append(" selected=\"true\"");
				}
				sb.append(">" + label + "</option>");
			}
			sb.append("</select>");
			sb.append(":");
			sb.append("<select class=\"hfe-minutes\" name=\"").append(fieldName).append("minutes").append("\">");
			for (int i = 0; i <= 59; ++i) {
				String label = "" + i;
				if (label.length() == 1)
					label = "0" + label;
				sb.append("<option value=\"" + i + "\"");
				if (valAsCal != null) {
					if (valAsCal.get(Calendar.MINUTE) == i)
						sb.append(" selected=\"true\"");
				}
				sb.append(">" + label + "</option>");
			}
			sb.append("</select>");
			if (!hideSeconds) {
				sb.append("<select class=\"hfe-seconds\" name=\"").append(fieldName).append("seconds").append("\">");
				for (int i = 0; i <= 59; ++i) {
					String label = "" + i;
					if (label.length() == 1)
						label = "0" + label;
					sb.append("<option value=\"" + i + "\"");
					if (valAsCal != null) {
						if (valAsCal.get(Calendar.SECOND) == i)
							sb.append(" selected=\"true\"");
					}
					sb.append(">" + label + "</option>");
				}
				sb.append("</select>");
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
			if (initialValue != null) {
				String toPrint = timeFormat().format(initialValue);
				return WidgetFactory.displayValue(toPrint);
			} else {
				return WidgetFactory.displayEmptyValue(hideSeconds ? "___:___" : "___:___:___");
			}
		} else {
			Calendar valAsCal = null;
			if (initialValue != null) {
				valAsCal = Calendar.getInstance();
				valAsCal.setTime(initialValue);
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(generateEditModeHtml(context, context.getFieldName(this), valAsCal));
			return sb.toString();
		}
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		return getValue(context, request, this);
	}
	
	/**
	 * It might be that TimeWidget itself was never registered when it is a piece of a construction
	 * involving other widgets. In that case this method allows to point to the widget that is actually
	 * registered.
	 * 
	 * @param registeredWidget The registered widget that TimeWidget is "part of".
	 * @see Widget#getValue(FormEntryContext, HttpServletRequest)
	 */
	public Object getValue(FormEntryContext context, HttpServletRequest request, Widget registeredWidget) {
		try {
			Integer h = (Integer) HtmlFormEntryUtil.getParameterAsType(request,
			    context.getFieldName(registeredWidget) + "hours", Integer.class);
			Integer m = (Integer) HtmlFormEntryUtil.getParameterAsType(request,
			    context.getFieldName(registeredWidget) + "minutes", Integer.class);
			Integer s = (Integer) HtmlFormEntryUtil.getParameterAsType(request,
			    context.getFieldName(registeredWidget) + "seconds", Integer.class);
			if (h == null && m == null)
				return null;
			if (h == null)
				h = 0;
			if (m == null)
				m = 0;
			if (s == null)
				s = 0;
			
			Calendar cal = Calendar.getInstance();
			cal.set(1900, 1, 1);
			cal.set(Calendar.HOUR_OF_DAY, h);
			cal.set(Calendar.MINUTE, m);
			cal.set(Calendar.SECOND, s);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Illegal value", ex);
		}
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
	 */
	@Override
	public void setInitialValue(Object value) {
		initialValue = (Date) value;
	}
	
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public void setHideSeconds(boolean hideSeconds) {
		this.hideSeconds = hideSeconds;
	}
	
	public boolean getHideSeconds() {
		return hideSeconds;
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}
}
