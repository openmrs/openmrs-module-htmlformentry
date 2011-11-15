package org.openmrs.module.htmlformentry.widget;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

/**
 * A widget that combines a {@see DateWidget} and a {@see TimeWidget} into a single widget
 * @author Mark
 *
 */
public class DateTimeWidget implements Widget {

	private Date initialValue;

	private DateWidget dateWidget;

	private TimeWidget timeWidget;

	public DateTimeWidget() {

		// create the Date and Time widgets
		dateWidget = new DateWidget();
		timeWidget = new TimeWidget();

	}

	public DateTimeWidget(DateWidget date, TimeWidget time) {

		// create the Date and Time widgets
		dateWidget = date;
		timeWidget = time;

	}

	 /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
     */
	@Override
    public String generateHtml(FormEntryContext context) {
		// have the date and time widgets generate their HTML
		String dateHTML = dateWidget.generateHtml(context);
		String timeHTML = timeWidget.generateHtml(context);

		// combine them and return them
		return dateHTML + " " + timeHTML;
	}

	 /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#getValue(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
     */
	@Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {

		// get the values from the associated date and time widgets
		Date date = dateWidget.getValue(context, request);
		Date time = (Date) timeWidget.getValue(context, request);

		return HtmlFormEntryUtil.combineDateAndTime(date, time);
	}

	 /**
     * @see org.openmrs.module.htmlformentry.widget.Widget#setInitialValue(java.lang.Object)
     */
	@Override
    public void setInitialValue(Object value) {
		initialValue = (Date) value;

		// set the underlying date and time widgets with the initial value
		dateWidget.setInitialValue(initialValue);
		timeWidget.setInitialValue(initialValue);
	}

	/**
	 * Gets the DateWidget associated with this widget
	 * 
	 * @return associated DateWidget
	 */
	public DateWidget getDateWidget() {
		return dateWidget;
	}

	/**
	 * Gets the TimeWidget associated with this widget
	 * 
	 * @return associated TimeWidget
	 */
	public TimeWidget getTimeWidget() {
		return timeWidget;
	}

}
