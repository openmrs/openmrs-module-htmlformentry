package org.openmrs.module.htmlformentry.widget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;

/**
 * A widget that allows the selection of a certain time-of-day.  To handle both
 * a date and time, see {@see DateTimeWidget}.
 */
public class TimeWidget implements Widget {

	private Date initialValue;

	public TimeWidget() {

	}

	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() == Mode.VIEW) {
			String toPrint = "";
			if (initialValue != null) {
				DateFormat timeFormat = new SimpleDateFormat("HH:mm");
				toPrint = timeFormat.format(initialValue);
				return WidgetFactory.displayValue(toPrint);
			} else {
				toPrint = "___:___";
				return WidgetFactory.displayEmptyValue(toPrint);
			}
		} else {
			Calendar valAsCal = null;
			if (initialValue != null) {
				valAsCal = Calendar.getInstance();
				valAsCal.setTime(initialValue);
			}
			StringBuilder sb = new StringBuilder();
			sb.append("<select name=\"").append(context.getFieldName(this))
					.append("hours").append("\">");
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
			sb.append("<select name=\"").append(context.getFieldName(this))
					.append("minutes").append("\">");
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
			return sb.toString();
		}
	}

	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		try {
			Integer h = (Integer) HtmlFormEntryUtil.getParameterAsType(request,
					context.getFieldName(this) + "hours", Integer.class);
			Integer m = (Integer) HtmlFormEntryUtil.getParameterAsType(request,
					context.getFieldName(this) + "minutes", Integer.class);
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, h);
			cal.set(Calendar.MINUTE, m);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		} catch (Exception ex) {
			throw new IllegalArgumentException("Illegal value");
		}
	}

	/**
	 * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
	 */
	public void setInitialValue(Object value) {
		initialValue = (Date) value;
	}

}
