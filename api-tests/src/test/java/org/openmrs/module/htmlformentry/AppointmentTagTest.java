package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.springframework.mock.web.MockHttpServletRequest;

public class AppointmentTagTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void loadTestData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/appointmentCheckInTest.xml");
	}
	
	@Test
	public void testAppointmentCheckInTag_shouldDisplayCheckboxesForScheduledAppointmentsForPatient() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "appointmentCheckInForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Appointments:" };
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("value=\"05f2ad92-1cc8-4cec-bf54-9cac0200746d\"/>", html);
				TestUtil.assertFuzzyContains("2024-08-16, 13:00:00 - of Cos, Hippocrates - Never Never Land", html);
				TestUtil.assertFuzzyContains("value=\"75504r42-3ca8-11e3-bf2b-0800271c1111\"/>", html);
				TestUtil.assertFuzzyContains("2024-08-15, 12:00:00 - of Cos, Hippocrates - Xanadu", html);
			}
		}.run();
	}
	
	@Test
	public void testAppointmentCheckInTag_shouldCheckPatientInForSingleAppointment() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "appointmentCheckInForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Appointments:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Appointments:"), "05f2ad92-1cc8-4cec-bf54-9cac0200746d");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				Appointment appointment1 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("05f2ad92-1cc8-4cec-bf54-9cac0200746d");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment1.getStatus());
				Assert.assertEquals(results.getEncounterCreated(),
				    appointment1.getFulfillingEncounters().stream().findFirst().get());
				
				// just confirm the other appointment has not been checked in or linked to the encounter
				Appointment appointment2 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("75504r42-3ca8-11e3-bf2b-0800271c1111");
				Assert.assertEquals(AppointmentStatus.Scheduled, appointment2.getStatus());
				Assert.assertEquals(0, appointment2.getFulfillingEncounters().size());
			}
		}.run();
	}
	
	@Test
	public void testAppointmentCheckInTag_shouldCheckPatientInForMultipleAppointments() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "appointmentCheckInForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Appointments:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Appointments:"), "05f2ad92-1cc8-4cec-bf54-9cac0200746d");
				request.addParameter(widgets.get("Appointments:").replace("_1", "_2"),
				    "75504r42-3ca8-11e3-bf2b-0800271c1111");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				
				Appointment appointment1 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("05f2ad92-1cc8-4cec-bf54-9cac0200746d");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment1.getStatus());
				Assert.assertEquals(results.getEncounterCreated(),
				    appointment1.getFulfillingEncounters().stream().findFirst().get());
				
				Appointment appointment2 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("75504r42-3ca8-11e3-bf2b-0800271c1111");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment2.getStatus());
				Assert.assertEquals(results.getEncounterCreated(),
				    appointment2.getFulfillingEncounters().stream().findFirst().get());
			}
		}.run();
	}
	
	@Test
	public void testAppointmentCheckInTag_editShouldDisassociateEncounterFromAppointmentAndAssociateNewEncounter()
	        throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "appointmentCheckInForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Appointments:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Appointments:"), "05f2ad92-1cc8-4cec-bf54-9cac0200746d");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Appointments:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Appointments:"), "");
				request.addParameter(widgets.get("Appointments:").replace("_1", "_2"),
				    "75504r42-3ca8-11e3-bf2b-0800271c1111");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				
				// sanity check
				Appointment appointment1 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("05f2ad92-1cc8-4cec-bf54-9cac0200746d");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment1.getStatus());
				Assert.assertEquals(results.getEncounterCreated(),
				    appointment1.getFulfillingEncounters().stream().findFirst().get());
				
				Appointment appointment2 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("75504r42-3ca8-11e3-bf2b-0800271c1111");
				Assert.assertEquals(AppointmentStatus.Scheduled, appointment2.getStatus());
				Assert.assertEquals(0, appointment2.getFulfillingEncounters().size());
			}
			
			// confirm that the associated appointment is now checked by default
			@Override
			public void testEditFormHtml(String html) {
				TestUtil.assertFuzzyContains("value=\"05f2ad92-1cc8-4cec-bf54-9cac0200746d\" checked=\"true\"/>", html);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				
				Appointment appointment1 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("05f2ad92-1cc8-4cec-bf54-9cac0200746d");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment1.getStatus()); // Note that we are NOT changing the status back to Scheduled
				// TODO debug after testing IRL
				Assert.assertEquals(0, appointment1.getFulfillingEncounters().size()); // but encounter should be removed
				
				Appointment appointment2 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("75504r42-3ca8-11e3-bf2b-0800271c1111");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment2.getStatus());
				Assert.assertEquals(results.getEncounterCreated(),
				    appointment2.getFulfillingEncounters().stream().findFirst().get());
			}
		}.run();
	}
	
	@Test
	public void testAppointmentCheckInTag_editingFormWithoutChangingStatusShouldNotCauseError() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "appointmentCheckInForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Appointments:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Appointments:"), "05f2ad92-1cc8-4cec-bf54-9cac0200746d");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Appointments:" };
			}
			
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// upon edit keep the same checked
				request.addParameter(widgets.get("Appointments:"), "05f2ad92-1cc8-4cec-bf54-9cac0200746d");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				
				// sanity check
				Appointment appointment1 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("05f2ad92-1cc8-4cec-bf54-9cac0200746d");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment1.getStatus());
				Assert.assertEquals(results.getEncounterCreated(),
				    appointment1.getFulfillingEncounters().stream().findFirst().get());
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				
				Appointment appointment1 = Context.getService(AppointmentsService.class)
				        .getAppointmentByUuid("05f2ad92-1cc8-4cec-bf54-9cac0200746d");
				Assert.assertEquals(AppointmentStatus.CheckedIn, appointment1.getStatus());
				Assert.assertEquals(results.getEncounterCreated(),
				    appointment1.getFulfillingEncounters().stream().findFirst().get());
			}
		}.run();
	}
}

// TODOS: make sure test to remove works
// TODOs: review specs
// TODOs: add to check in form for SL and format/test IRL!
// TODOs ticket separate functionality to pass in appointment to automatically mark as checked in
