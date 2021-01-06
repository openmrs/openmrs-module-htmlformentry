package org.openmrs.module.htmlformentry.widget;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ZonedDateTimeWidget extends DateWidget implements Widget {
	
	protected boolean hideSeconds = false;
	
	public ZonedDateTimeWidget() {
	}
	
	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == FormEntryContext.Mode.VIEW) {
			String toPrint = "";
			if (getInitialValue() != null) {
				StringBuilder sb = new StringBuilder();
				String formatTime = "dd/MM/yyyy HH:mm:ss";
				SimpleDateFormat utcFormatter = new SimpleDateFormat(formatTime);
				utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
				sb.append("<span id=\"dateTimeWithTimezone\" class=\"value\">")
				        .append(utcFormatter.format(getInitialValue())).append("</span>");
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
			if (getInitialValue() != null) {
				valAsCal = Calendar.getInstance();
				valAsCal.setTime((Date) getInitialValue());
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
			if (getInitialValue() != null) {
				sb.append(", '" + new SimpleDateFormat("yyyy-MM-dd").format(getInitialValue()) + "'");
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
			Date timeOnly = (Date) TimeWidget.getValue(context, request, this);
			Calendar time = Calendar.getInstance();
			time.setTime(timeOnly);
			Date dateOnly = super.getValue(context, request);
			Calendar calDateTime = Calendar.getInstance();
			calDateTime.setTime(dateOnly);
			String timezone = (String) HtmlFormEntryUtil.getParameterAsType(request, context.getFieldName(this) + "timezone",
			    String.class);
			Date aaa = calDateTime.getTime();
			Date bbb = time.getTime();
			calDateTime.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
			calDateTime.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
			calDateTime.set(Calendar.SECOND, time.get(Calendar.SECOND));
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
	
}
