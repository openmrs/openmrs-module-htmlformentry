package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

public class AppointmentsWidget implements Widget {
	
	private List<Appointment> appointments;
	
	private List<CheckboxWidget> checkboxWidgets = new ArrayList<CheckboxWidget>();
	
	public AppointmentsWidget(List<Appointment> appointments, FormEntryContext context) {
		this.appointments = appointments;
		String fieldName = context.registerWidget(this);
		
		// TODO: document why we need the special registration for form consistency
		int i = 1;
		for (Appointment appointment : appointments) {
			CheckboxWidget checkboxWidget = new CheckboxWidget();
			checkboxWidget.setLabel(
			    dateTimeFormat().format(appointment.getStartDateTime()) + " - " + renderProviderNames(appointment) + " - "
			            + (appointment.getLocation() != null ? appointment.getLocation().getName() : ""));
			checkboxWidget.setValue(appointment.getUuid());
			if (appointment.getFulfillingEncounters() != null && context.getExistingEncounter() != null
			        && appointment.getFulfillingEncounters().contains(context.getExistingEncounter())) {
				checkboxWidget.setInitialValue(appointment.getUuid());
			}
			context.registerWidget(checkboxWidget, fieldName + "_" + i++);
			checkboxWidgets.add(checkboxWidget);
		}
		
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		// TODO?
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		if (appointments == null || appointments.isEmpty()) {
			return "No appointments found"; // TODO translate, style
		}
		
		return checkboxWidgets.stream().map(checkboxWidget -> {
			return checkboxWidget.generateHtml(context);
		}).collect(Collectors.joining());
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
	
	// TODO move to util method?
	private SimpleDateFormat dateTimeFormat() {
		String df = Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_FORMATTER_DATETIME,
		    "yyyy-MM-dd, HH:mm:ss");
		if (StringUtils.hasText(df)) {
			return new SimpleDateFormat(df, Context.getLocale());
		} else {
			return Context.getDateTimeFormat();
		}
	}
	
	private String renderProviderNames(Appointment appointment) {
		if (appointment.getProviders() != null) {
			return appointment.getProviders().stream().map(provider -> {
				return provider.getProvider().getPerson() != null ? HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(
				    provider.getProvider().getPerson().getPersonName()) : provider.getProvider().getName();
			}).collect(Collectors.joining("; "));
		}
		return "";
		
	}
}
