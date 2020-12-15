package org.openmrs.module.htmlformentry.widget;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ZonedDateTimeWidget extends DateWidget implements Widget {
	
	public static final String DEFAULT_TIME_FORMAT = "HH:mm";
	
	protected Date initialValue;
	
	protected boolean hidden;
	
	protected boolean hideSeconds = false;
	
	private String timeFormat;
	
	public ZonedDateTimeWidget() {
	}
	
	protected SimpleDateFormat timeFormat() {
		String df = timeFormat != null ? timeFormat
		        : Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_TIME_FORMAT);
		if (!StringUtils.hasText(df)) {
			df = DEFAULT_TIME_FORMAT;
		}
		return new SimpleDateFormat(df, Context.getLocale());
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			String toPrint = "";
			if (initialValue != null) {
				StringBuilder sb = new StringBuilder();
				String formatTime = "dd/MM/yyyy HH:mm:ss";
				SimpleDateFormat utcFormatter = new SimpleDateFormat(formatTime);
				utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
				sb.append("<span id=\"dateTimeWithTimezone\" class=\"value\">").append(utcFormatter.format(initialValue))
				        .append("</span>");
				return sb.toString();
			} else {
				StringBuilder sb = new StringBuilder();
				toPrint = "________";
				sb.append(WidgetFactory.displayEmptyValue(toPrint));
				if (hideSeconds) {
					toPrint = "___:___";
				} else {
					toPrint = "___:___:___";
				}
				sb.append(WidgetFactory.displayEmptyValue(toPrint));
				return sb.toString();
			}
		} else {
			Calendar valAsCal = null;
			if (initialValue != null) {
				valAsCal = Calendar.getInstance();
				valAsCal.setTime(initialValue);
				
			}
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
				if ("true".equals(
				    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_SHOW_DATE_FORMAT))) {
					sb.append(" (" + dateFormat().toLocalizedPattern().toLowerCase() + ")");
				}
				
				sb.append("<script>setupDatePicker('" + jsDateFormat() + "', '" + getYearsRange() + "','"
				        + getLocaleForJquery() + "', '#" + fieldName + "-display', '#" + fieldName + "'");
				if (initialValue != null)
					sb.append(", '" + new SimpleDateFormat("yyyy-MM-dd").format(initialValue) + "'");
				sb.append(")</script>");
			}
			
			if (hidden) {
				sb.append("<input type=\"hidden\" class=\"hfe-hours\" name=\"").append(context.getFieldName(this))
				        .append("hours").append("\" value=\"" + new SimpleDateFormat("HH").format(initialValue) + "\"/>");
				sb.append("<input type=\"hidden\" class=\"hfe-minutes\" name=\"").append(context.getFieldName(this))
				        .append("minutes").append("\" value=\"" + new SimpleDateFormat("mm").format(initialValue) + "\"/>");
				if (!hideSeconds) {
					sb.append("<input type=\"hidden\" class=\"hfe-seconds\" name=\"").append(context.getFieldName(this))
					        .append("seconds")
					        .append("\" value=\"" + new SimpleDateFormat("ss").format(initialValue) + "\"/>");
				}
			} else {
				
				sb.append("<select class=\"hfe-hours\" name=\"").append(context.getFieldName(this)).append("hours")
				        .append("\">");
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
				sb.append("<select class=\"hfe-minutes\" name=\"").append(context.getFieldName(this)).append("minutes")
				        .append("\">");
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
					sb.append("<select class=\"hfe-seconds\" name=\"").append(context.getFieldName(this)).append("seconds")
					        .append("\">");
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
				sb.append("<input type=\"hidden\" class=\"hfe-timeZone\" name=\"").append(context.getFieldName(this))
				        .append("timeZone").append("\">");
				sb.append("</input>");
			}
			return sb.toString();
		}
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	
	/**
	 * @return The timezone string info that was submitted as part of the time submission.
	 */
	public String getSubmittedTimeZone(FormEntryContext context, HttpServletRequest request) {
		return (String) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "timeZone", String.class);
	}
	
	//@Override
	public Date getValue(FormEntryContext context, HttpServletRequest request) {
		try {
			
			Integer h = (Integer) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "hours",
			    Integer.class);
			Integer m = (Integer) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "minutes",
			    Integer.class);
			Integer s = (Integer) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "seconds",
			    Integer.class);
			if (h == null && m == null)
				return null;
			if (h == null)
				h = 0;
			if (m == null)
				m = 0;
			if (s == null)
				s = 0;
			
			if (h == 0 && m == 0 && s == 0) {
				return null;
			}
			
			String timezone = (String) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "timeZone",
			    String.class);
			Date d = (Date) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this), Date.class);
			
			Calendar calDateTime = Calendar.getInstance();
			calDateTime.setTime(d);
			
			calDateTime.set(Calendar.HOUR_OF_DAY, h);
			calDateTime.set(Calendar.MINUTE, m);
			calDateTime.set(Calendar.SECOND, s);
			calDateTime.set(Calendar.MILLISECOND, 0);
			if (org.apache.commons.lang.StringUtils.isNotEmpty(timezone)) {
				TimeZone tz = TimeZone.getTimeZone(timezone);
				calDateTime.setTimeZone(tz);
			}
			
			return calDateTime.getTime();
			
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Illegal value");
		}
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
	 */
	@Override
	public void setInitialValue(Object value) {
		initialValue = (Date) value;
	}
	
	public Object getInitialValue() {
		return this.initialValue;
	}
	
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public void setHideSeconds(boolean hideSeconds) {
		this.hideSeconds = hideSeconds;
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}
	
}
