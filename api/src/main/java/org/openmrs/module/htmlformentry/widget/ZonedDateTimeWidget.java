package org.openmrs.module.htmlformentry.widget;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
//import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

public class ZonedDateTimeWidget extends DateWidget implements Widget {
	
	protected boolean hideSeconds = false;
	
	public ZonedDateTimeWidget() {
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			if (initialValue != null) {
				SimpleDateFormat utcFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
				return new StringBuilder().append("<span id=\"dateTimeWithTimezone\" class=\"value\">")
				        .append(utcFormatter.format(initialValue)).append("</span>").toString();
			} else {
				return new StringBuilder().append(WidgetFactory.displayEmptyValue("________"))
				        .append(WidgetFactory.displayEmptyValue(hideSeconds ? "___:___" : "___:___:___")).toString();
			}
		} else {
			Calendar valAsCal = null;
			SimpleDateFormat utcDateFormatter = new SimpleDateFormat("dd/MM/yyyy");
			utcDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
			if (initialValue != null) {
				SimpleDateFormat utcDateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				utcDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
				utcDateTimeFormatter.format(initialValue);
				valAsCal = utcDateTimeFormatter.getCalendar();
			}
			StringBuilder sb = new StringBuilder();
			String fieldName = context.getFieldName(this);
			sb.append("<input type=\"text\" size=\"10\" id=\"").append(fieldName).append("-display\"/>");
			sb.append("<input type=\"hidden\" name=\"").append(fieldName).append("\" id=\"").append(fieldName).append("\"");
			if (getOnChangeFunction() != null) {
				sb.append(" onChange=\"" + getOnChangeFunction() + "\" ");
			}
			sb.append(" />");
			if ("true".equals(
			    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_SHOW_DATE_FORMAT))) {
				sb.append(" (" + dateFormat().toLocalizedPattern().toLowerCase() + ")");
			}
			
			sb.append("<script>setupDatePicker('" + jsDateFormat() + "', '" + getYearsRange() + "','" + getLocaleForJquery()
			        + "', '#" + fieldName + "-display', '#" + fieldName + "'");
			if (initialValue != null) {
				sb.append(", '" + utcDateFormatter.format(initialValue) + "'");
			}
			sb.append(")</script>");
			sb.append("<select class=\"hfe-hours\" name=\"").append(context.getFieldName(this)).append("hours")
			        .append("\">");
			for (int i = 0; i <= 23; ++i) {
				String label = "" + i;
				if (label.length() == 1) {
					label = "0" + label;
				}
				sb.append("<option value=\"" + i + "\"");
				if (valAsCal != null) {
					if (valAsCal.get(Calendar.HOUR_OF_DAY) == i) {
						sb.append(" selected=\"true\"");
					}
				}
				sb.append(">" + label + "</option>");
			}
			sb.append("</select>");
			sb.append(":");
			sb.append("<select class=\"hfe-minutes\" name=\"").append(context.getFieldName(this)).append("minutes")
			        .append("\">");
			for (int i = 0; i <= 59; ++i) {
				String label = "" + i;
				if (label.length() == 1) {
					label = "0" + label;
				}
				sb.append("<option value=\"" + i + "\"");
				if (valAsCal != null) {
					if (valAsCal.get(Calendar.MINUTE) == i) {
						sb.append(" selected=\"true\"");
					}
				}
				sb.append(">" + label + "</option>");
			}
			sb.append("</select>");
			sb.append("<select class=\"hfe-seconds\" name=\"").append(context.getFieldName(this)).append("seconds")
			        .append("\">");
			for (int i = 0; i <= 59; ++i) {
				String label = "" + i;
				if (label.length() == 1) {
					label = "0" + label;
				}
				sb.append("<option value=\"" + i + "\"");
				if (valAsCal != null) {
					if (valAsCal.get(Calendar.SECOND) == i) {
						sb.append(" selected=\"true\"");
					}
				}
				sb.append(">" + label + "</option>");
			}
			sb.append("</select>");
			sb.append("<input type=\"hidden\" class=\"hfe-timezone\" name=\"").append(context.getFieldName(this))
			        .append("timezone").append("\">");
			sb.append("</input>");
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
	public String getSubmittedTimezone(FormEntryContext context, HttpServletRequest request) {
		return (String) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "timezone", String.class);
	}
	
	@Override
	public Date getValue(FormEntryContext context, HttpServletRequest request) {
		try {
			Date time = (Date) TimeWidget.getValue(context, request, this);
			Calendar timeCal = Calendar.getInstance();
			timeCal.setTime(time);
			Date date = super.getValue(context, request);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			String timezoneParam = (String) HtmlFormEntryUtil.getParameterAsType(request,
			    context.getFieldName(this) + "timezone", String.class);
			cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
			cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
			cal.set(Calendar.MILLISECOND, 0);
			if (StringUtils.isNotEmpty(timezoneParam)) {
				cal.setTimeZone(TimeZone.getTimeZone(timezoneParam));
			}
			return cal.getTime();
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Illegal value");
		}
	}
	
}
