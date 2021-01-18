package org.openmrs.module.htmlformentry.widget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

public class ZonedDateTimeWidget extends DateWidget implements Widget {
	
	protected boolean hideSeconds = false;
	
	public ZonedDateTimeWidget() {
	}
	
	@Override
	protected DateFormat getHtmlDateFormat() {
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat;
	}
	
	/*
	 * TODO: Probably needs to be moved to a util class for the whole HFE to rely on this whenever needed
	 */
	public static DateFormat getUTCDateFormat() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat;
	}
	
	public String generateHtml(FormEntryContext context) {
		
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			if (initialValue != null) {
				return new StringBuilder().append("<span id=\"dateTimeWithTimezone\" class=\"value\">")
				        .append(getUTCDateFormat().format(initialValue)).append("</span>").toString();
			} else {
				return WidgetFactory.displayEmptyValue(hideSeconds ? "___:___" : "___:___:___");
			}
		} else {
			StringBuilder sb = new StringBuilder();
			
			// the date part
			sb.append(super.generateHtml(context));
			
			// the time part
			Calendar valAsCal = null;
			if (initialValue != null) {
				DateFormat utcFormat = getUTCDateFormat();
				utcFormat.format(initialValue);
				valAsCal = utcFormat.getCalendar();
			}
			sb.append(new TimeWidget().generateEditModeHtml(context, context.getFieldName(this), valAsCal));
			
			// the timezone part
			sb.append("<input type=\"hidden\" class=\"hfe-timezone\" name=\"").append(context.getFieldName(this))
			        .append("timezone").append("\">");
			sb.append("</input>");
			
			return sb.toString();
		}
	}
	
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
			throw new IllegalArgumentException("Illegal value", ex);
		}
	}
	
}
