package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.joda.time.DateTime;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentSearchRequest;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.AppointmentsWidget;

public class AppointmentsElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	private AppointmentsWidget appointmentsWidget;
	
	private List<Appointment> appointments = new ArrayList<>();
	
	public AppointmentsElement(FormEntryContext context) {
		Patient patient = context.getExistingPatient();
		if (patient != null) {
			
			// first, get all scheduled appointments for this patient
			AppointmentSearchRequest request = new AppointmentSearchRequest();
			request.setPatientUuid(patient.getUuid());
			request.setStartDate(new DateTime().minusYears(1000).toDate()); // TODO hack, we want all appts for patient regardless of start date, but start date is required, this will start to fail in a thousand years
			appointments = Context.getService(AppointmentsService.class).search(request);
			
			appointments.sort(Comparator.comparing(Appointment::getStartDateTime).reversed());
			appointments.removeIf(appointment -> appointment.getStatus() != AppointmentStatus.Scheduled
			        && (appointment.getFulfillingEncounters() == null
			                && !appointment.getFulfillingEncounters().contains(context.getExistingEncounter())));
		}
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		appointmentsWidget = new AppointmentsWidget(appointments, context);
		return appointmentsWidget.generateHtml(context);
	}
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		return null;
	}
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		List<String> selectedAppointmentUuids = (List<String>) appointmentsWidget.getValue(session.getContext(), submission);
		List<Appointment> appointmentsToMarkCheckedIn = new ArrayList<>();
		List<Appointment> appointmentsToDisassociateFromEncounter = new ArrayList<>();
		
		// find appointments that need to be marked as checked in
		for (String uuid : selectedAppointmentUuids) {
			appointments.stream().filter(appointment -> appointment.getUuid().equals(uuid)).findFirst()
			        .ifPresent(appointment -> {
				        appointmentsToMarkCheckedIn.add(appointment);
			        });
		}
		if (appointmentsToMarkCheckedIn.size() > 0) {
			session.getSubmissionActions()
			        .setAppointmentsToMarkCheckedInAndAssociateWithEncounter(appointmentsToMarkCheckedIn);
		}
		
		// find appointments that need to be disassociated from the encounter
		appointments.stream()
		        .filter(appointment -> (!selectedAppointmentUuids.contains(appointment.getUuid())
		                && appointment.getFulfillingEncounters() != null
		                && appointment.getFulfillingEncounters().contains(session.getEncounter())))
		        .forEach(appointment -> {
			        appointmentsToDisassociateFromEncounter.add(appointment);
		        });
		if (appointmentsToDisassociateFromEncounter.size() > 0) {
			session.getSubmissionActions()
			        .setAppointmentsToDisassociateFromEncounter(appointmentsToDisassociateFromEncounter);
		}
	}
}
