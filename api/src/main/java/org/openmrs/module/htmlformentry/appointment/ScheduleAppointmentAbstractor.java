package org.openmrs.module.htmlformentry.appointment;

import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.service.AppointmentsService;

/**
 * Calls to the AppointmentService are abstracted here to avoid runtime class loading issues in
 * instances where the Appointments module is not present.
 */
public class ScheduleAppointmentAbstractor {

	public void createAppointments(List<Object> appointments) {
		for (Object obj : appointments) {
			Appointment appointment = (Appointment) obj;
			Context.getService(AppointmentsService.class).validateAndSave(appointment);
		}
	}
}
