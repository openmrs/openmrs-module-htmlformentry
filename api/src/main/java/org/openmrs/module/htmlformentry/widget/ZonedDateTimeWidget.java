package org.openmrs.module.htmlformentry.widget;

import static org.openmrs.util.TimeZoneUtil.toRFC3339;
import static org.openmrs.util.TimeZoneUtil.toUTCCalendar;

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
	
	/*
	 * The encapsulated TimeWidget should not be registered with FormEntryContext, it is assumed that ZonedDateTimeWidget is the registered widget
	 */
	private TimeWidget timeWidget;
	
	public ZonedDateTimeWidget() {
		timeWidget = new TimeWidget();
	}
	
	@Override
	protected DateFormat getHtmlDateFormat() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat;
	}
	
	public String generateHtml(FormEntryContext context) {
		
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			if (initialValue != null) {
				return WidgetFactory.displayValue(toRFC3339(initialValue), "rfc3339-date");
			} else {
				return WidgetFactory.displayEmptyValue(timeWidget.getHideSeconds() ? "___:___" : "___:___:___");
			}
		} else {
			StringBuilder sb = new StringBuilder();
			
			// the date part
			sb.append(super.generateHtml(context));
			
			// the time part
			Calendar valAsCal = initialValue != null ? toUTCCalendar(initialValue) : null;
			sb.append(timeWidget.generateEditModeHtml(context, context.getFieldName(this), valAsCal));
			
			// the timezone part
			sb.append("<input type=\"hidden\" class=\"hfe-timezone\" name=\"").append(context.getFieldName(this))
			        .append("timezone").append("\">");
			sb.append("</input>");
			
			return sb.toString();
		}
	}
	
	/**
	 * @return The timezone string info that was submitted along with the date-time value.
	 */
	public String getSubmittedTimezone(FormEntryContext context, HttpServletRequest request) {
		return (String) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "timezone", String.class);
	}
	
	@Override
	public Date getValue(FormEntryContext context, HttpServletRequest request) {
		try {
			Date time = (Date) timeWidget.getValue(context, request, this);
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
	
	@Override
	public void setHidden(boolean hidden) {
		super.setHidden(hidden);
		timeWidget.setHidden(hidden);
	}
	
	public void setHideSeconds(boolean hideSeconds) {
		timeWidget.setHideSeconds(hideSeconds);
	}
	
}
