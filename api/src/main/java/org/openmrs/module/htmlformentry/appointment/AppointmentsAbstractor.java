package org.openmrs.module.htmlformentry.appointment;

import java.util.Collections;
import java.util.List;

import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;

/**
 * Calls to the AppointmentService are abstracted here, rather than directly accessed in the Form
 * Entry Session to avoid runtime class loading issues in instances where the Appointments module is
 * not present
 */
public class AppointmentsAbstractor {
	
	public void markAppointmentsAsCheckedInAndAssociateWithEncounter(List<Object> appointments, Encounter encounter) {
		for (Object appointmentObject : appointments) {
			Appointment appointment = (Appointment) appointmentObject;
			if (appointment.getStatus() == AppointmentStatus.Scheduled) {
				Context.getService(AppointmentsService.class).changeStatus(appointment,
				    AppointmentStatus.CheckedIn.toString(), encounter.getEncounterDatetime());
			}
			if (appointment.getFulfillingEncounters() != null) {
				appointment.getFulfillingEncounters().add(encounter);
			} else {
				appointment.setFulfillingEncounters(Collections.singleton(encounter));
			}
			// see: https://bahmni.atlassian.net/browse/BAH-3855 for why we need to call the Supplier<Appointment> version of validateAndSave
			Context.getService(AppointmentsService.class).validateAndSave(() -> appointment);
		}
	}
	
	public void disassociateAppointmentsFromEncounter(List<Object> appointments, Encounter encounter) {
		for (Object appointmentObject : appointments) {
			Appointment appointment = (Appointment) appointmentObject;
			if (appointment.getFulfillingEncounters() != null) {
				appointment.getFulfillingEncounters().remove(encounter);
				// see: https://bahmni.atlassian.net/browse/BAH-3855 for why we need to call the Supplier<Appointment> version of validateAndSave
				Context.getService(AppointmentsService.class).validateAndSave(() -> appointment);
			}
		}
	}
}
