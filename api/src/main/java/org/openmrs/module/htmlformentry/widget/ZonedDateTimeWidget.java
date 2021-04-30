package org.openmrs.module.htmlformentry.widget;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

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
		String df = Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.FORMATTER_DATETIME_NAME,
		    "dd-MM-yyyy, HH:mm:ss");
		if (org.springframework.util.StringUtils.hasText(df)) {
			return new SimpleDateFormat(df, Context.getLocale());
		} else {
			return Context.getDateFormat();
		}
	}
	
	public String generateHtml(FormEntryContext context) {
		
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			boolean timezonesConversions = BooleanUtils.toBoolean(
			    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS));
			if (BooleanUtils.isNotTrue(timezonesConversions)) {
				return datetimeFormat().format(initialValue);
			}
			//If Timezone.Conversions if true but the UP with client timezone is empty, we dont show any date
			if (BooleanUtils.isTrue(timezonesConversions) && StringUtils.isEmpty(this.getUP_clientTimezone())) {
				return WidgetFactory.displayEmptyValue(timeWidget.getHideSeconds() ? "___:___" : "___:___:___");
			}
			return toClientTimezone(initialValue,
			    Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.FORMATTER_DATETIME_NAME , "dd-MM-yyyy, HH:mm:ss"));
		} else {
			StringBuilder sb = new StringBuilder();
			
			// the date part
			sb.append(super.generateHtml(context));
			
			// the time part
			Calendar valAsCal = Calendar.getInstance();
			if (initialValue != null) {
				SimpleDateFormat sdf = new SimpleDateFormat(
				        Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.FORMATTER_DATETIME_NAME),
				        Context.getLocale());
				try {
					String dateClientTZ = toClientTimezone(initialValue, Context.getAdministrationService()
					        .getGlobalProperty(HtmlFormEntryConstants.FORMATTER_DATETIME_NAME));
					if (StringUtils.isNotEmpty(dateClientTZ)) {
						valAsCal.setTime(sdf.parse(dateClientTZ));
					} else {
						valAsCal.setTime(initialValue);
					}
				}
				catch (ParseException e) {
					valAsCal = null;
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
	public String getUP_clientTimezone() {
		return Context.getAuthenticatedUser().getUserProperty(HtmlFormEntryConstants.CLIENT_TIMEZONE);
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
			if (StringUtils.isNotEmpty(this.getUP_clientTimezone())) {
				cal.setTimeZone(TimeZone.getTimeZone(this.getUP_clientTimezone()));
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
