package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;

public class AppointmentsWidget implements Widget {
	
	private List<Appointment> appointments;
	
	private List<CheckboxWidget> checkboxWidgets = new ArrayList<CheckboxWidget>();
	
	private String clazz = null;
	
	public AppointmentsWidget(List<Appointment> appointments, FormEntryContext context, String clazz) {
		
		this.clazz = clazz;
		
		this.appointments = appointments;
		String fieldName = context.registerWidget(this);
		
		// note that we are relying on the register widget to generate a single unique field name,
		// and then we are appending the uuid to that field name to create unique field names for each checkbox
		// this is to ensure that this widget consistently increments the field name sequential value only once,
		// otherwise, if the number of matches appointments changed between when the form was opened and the form was saved,
		// widget names would be inconsistent, wreaking havoc on the form
		for (Appointment appointment : appointments) {
			
			// compare dates so that we can highlight any that match the encounter darw
			boolean appointmentDateMatchesEncounterDate = appointment.getStartDateTime() != null
			        && new DateTime(appointment.getStartDateTime()).withTimeAtStartOfDay()
			                .equals(new DateTime(context.getBestApproximationOfEncounterDate()).withTimeAtStartOfDay());
			
			CheckboxWidget checkboxWidget = new CheckboxWidget();
			checkboxWidget.setLabel((appointmentDateMatchesEncounterDate ? "<strong>" : "")
			        + HtmlFormEntryUtil.getDateTimeFormat().format(appointment.getStartDateTime()) + " - "
			        + renderProviderNames(appointment) + " - "
			        + (appointment.getLocation() != null ? appointment.getLocation().getName() : "")
			        + (appointmentDateMatchesEncounterDate ? "</strong>" : ""));
			checkboxWidget.setValue(appointment.getUuid());
			if (appointment.getFulfillingEncounters() != null && context.getExistingEncounter() != null
			        && appointment.getFulfillingEncounters().contains(context.getExistingEncounter())) {
				checkboxWidget.setInitialValue(appointment.getUuid());
			}
			context.registerWidget(checkboxWidget, fieldName + "_" + appointment.getId());
			checkboxWidgets.add(checkboxWidget);
		}
		
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		// the constructor takes care of setting the initial value for each checkbox
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		if (appointments == null || appointments.isEmpty()) {
			return Context.getMessageSourceService().getMessage("htmlformentry.appointments.noAppointmentsFound");
		}
		
		return checkboxWidgets.stream().map(checkboxWidget -> {
			return checkboxWidget.generateHtml(context);
		}).map((html) -> "<span" + (clazz != null ? " class=\"" + clazz + "\" " : "") + ">" + html + "</span>")
		        .collect(Collectors.joining());
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		List<String> selectedAppointmentUuids = new ArrayList<String>();
		for (CheckboxWidget checkboxWidget : checkboxWidgets) {
			String value = (String) checkboxWidget.getValue(context, request);
			if (value != null) {
				selectedAppointmentUuids.add(value);
			}
		}
		return selectedAppointmentUuids;
	}
	
	private String renderProviderNames(Appointment appointment) {
		if (appointment.getProviders() != null) {
			return appointment.getProviders().stream().map(provider -> {
				return HtmlFormEntryUtil.getProviderName(provider.getProvider());
			}).collect(Collectors.joining("; "));
		}
		return "";
	}
}
