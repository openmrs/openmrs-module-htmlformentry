package org.openmrs.module.htmlformentry.widget;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

public class DateTimeWidget implements Widget {

	Date initialValue;

	DateWidget dateWidget;

	TimeWidget timeWidget;

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

	public String generateHtml(FormEntryContext context) {
		// have the date and time widgets generate their HTML
		String dateHTML = dateWidget.generateHtml(context);
		String timeHTML = timeWidget.generateHtml(context);

		// combine them and return them
		return dateHTML + " " + timeHTML;
	}

	public Object getValue(FormEntryContext context, HttpServletRequest request) {

		// get the values from the associated date and time widgets
		Date date = dateWidget.getValue(context, request);
		Date time = (Date) timeWidget.getValue(context, request);

		return HtmlFormEntryUtil.combineDateAndTime(date, time);
	}

	public void setInitialValue(Object value) {
		initialValue = (Date) value;

		// set the underlying date and time widgets with the initial value
		dateWidget.setInitialValue(initialValue);
		timeWidget.setInitialValue(initialValue);
	}

	public DateWidget getDateWidget() {
		return dateWidget;
	}

	public TimeWidget getTimeWidget() {
		return timeWidget;
	}

}
