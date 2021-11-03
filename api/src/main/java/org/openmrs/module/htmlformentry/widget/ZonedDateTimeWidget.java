package org.openmrs.module.htmlformentry.widget;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.MissingRequiredPropertyException;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.DATETIME_FALLBACK_FORMAT;
import static org.openmrs.util.TimeZoneUtil.toClientTimezone;

public class ZonedDateTimeWidget extends DateWidget implements Widget {
	
	/*
	 * The encapsulated TimeWidget should not be registered with FormEntryContext, it is assumed that ZonedDateTimeWidget is the registered widget
	 */
	private TimeWidget timeWidget;
	
	public ZonedDateTimeWidget() {
		timeWidget = new TimeWidget();
	}
	
	private SimpleDateFormat datetimeFormat() {
		String df = Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_FORMATTER_DATETIME,
		    DATETIME_FALLBACK_FORMAT);
		if (StringUtils.isNotBlank(df)) {
			return new SimpleDateFormat(df, Context.getLocale());
		} else {
			return Context.getDateFormat();
		}
	}
	
	public String generateHtml(FormEntryContext context) {
		
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			boolean timezoneConversions = BooleanUtils.toBoolean(
			    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS));
			if (!timezoneConversions) {
				//The server and client are on the same timezone
				return WidgetFactory.displayValue(datetimeFormat().format(initialValue));
			}
			//If Timezone.Conversions is true but the UP with client timezone is empty, it shows an error, because we dont know the client timezone.
			if (timezoneConversions && StringUtils.isEmpty(this.getClientTimezone())) {
				throw new MissingRequiredPropertyException(
				        Context.getMessageSourceService().getMessage("htmlformentry.error.emptyClientTimezoneUserProperty"));
			}
			return WidgetFactory.displayValue(toClientTimezone(initialValue,
			    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_FORMATTER_DATETIME)));
		} else {
			StringBuilder sb = new StringBuilder();
			
			// the date part
			sb.append(super.generateHtml(context));
			
			// the time part
			Calendar valAsCal = Calendar.getInstance();
			if (initialValue != null) {
				SimpleDateFormat sdf = new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
				    HtmlFormEntryConstants.GP_FORMATTER_DATETIME, DATETIME_FALLBACK_FORMAT), Context.getLocale());
				try {
					String formatDateWithClientTZ = toClientTimezone(initialValue,
					    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_FORMATTER_DATETIME));
					if (StringUtils.isNotEmpty(formatDateWithClientTZ)) {
						valAsCal.setTime(sdf.parse(formatDateWithClientTZ));
					} else {
						valAsCal.setTime(initialValue);
					}
				}
				catch (Exception e) {
					valAsCal = null;
					sb.append(
					    Context.getMessageSourceService().getMessage("htmlformentry.error.formattingTimeForEncounterDate"));
				}
			} else {
				valAsCal = null;
			}
			
			sb.append(timeWidget.generateEditModeHtml(context, context.getFieldName(this), valAsCal));
			
			// the timezone part
			sb.append("<input type=\"hidden\" class=\"hfe-timezone\" name=\"").append(context.getFieldName(this))
			        .append("timezone").append("\">");
			sb.append("</input>");
			
			return sb.toString();
		}
	}
	
	/**
	 * @return The timezone string info saved as User Property.
	 */
	public String getClientTimezone() {
		return Context.getAuthenticatedUser().getUserProperty(HtmlFormEntryConstants.UP_CLIENT_TIMEZONE);
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
			cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
			cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
			cal.set(Calendar.MILLISECOND, 0);
			
			boolean timezoneConversions = BooleanUtils.toBoolean(
			    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS));
			
			String timezoneToConvert = null;
			
			if (timezoneConversions) {
				String timezoneParam = (String) HtmlFormEntryUtil.getParameterAsType(request,
				    context.getFieldName(this) + "timezone", String.class);
				if (StringUtils.isNotEmpty(timezoneParam)) {
					//Use the client timezone submitted with the form
					timezoneToConvert = timezoneParam;
				} else if (timezoneConversions && StringUtils.isNotEmpty(this.getClientTimezone())) {
					//Use the User Property with client timezone
					timezoneToConvert = this.getClientTimezone();
				}
				
			} else {
				//Server timezone is the same as client timezone.
				timezoneToConvert = TimeZone.getDefault().getID();
			}
			
			cal.setTimeZone(TimeZone.getTimeZone(timezoneToConvert));
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
