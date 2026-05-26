package org.openmrs.module.htmlformentry;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentKind;
import org.openmrs.module.appointments.model.AppointmentSearchRequest;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.springframework.mock.web.MockHttpServletRequest;

public class ScheduleAppointmentTagTest extends BaseHtmlFormEntryTest {

	// Kigali location (location_id=1001), tagged with "Some Tag" in RegressionTest data
	private static final String KIGALI_UUID = "321c1fcc-0459-4201-bf70-0b90535ba362";

	// Consultation service from scheduleAppointmentTest.xml (duration_mins=30)
	private static final String CONSULTATION_UUID = "c36006e5-9fbb-4f20-866b-0ece245615a6";

	// Provider 1 from standard test dataset
	private static final String PROVIDER_ID = "1";

	// Patient 2 (person_id=2) from standard test dataset
	private static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";

	@Before
	public void loadTestData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/scheduleAppointmentTest.xml");
	}

	@Test
	public void scheduleAppointmentTag_shouldRenderLocationAndServiceOptions() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				// Locations tagged with "Some Tag": Kigali and Mirebalais
				TestUtil.assertFuzzyContains("Kigali", html);
				TestUtil.assertFuzzyContains(KIGALI_UUID, html);

				// Service from test data
				TestUtil.assertFuzzyContains("Consultation", html);
				TestUtil.assertFuzzyContains(CONSULTATION_UUID, html);

				// Appointment kind options
				TestUtil.assertFuzzyContains(AppointmentKind.Scheduled.name(), html);
				TestUtil.assertFuzzyContains(AppointmentKind.WalkIn.name(), html);
				TestUtil.assertFuzzyContains(AppointmentKind.Virtual.name(), html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldCreateAppointmentOnSubmit() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type", "Appointment Time", "Appointment Time!!1", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get("Appointment Time"), "2025-01-15");

				// TimeWidget uses {fieldName}hours / minutes / seconds; strip the "hours" suffix to get the base name
				String timeWidgetBase = widgets.get("Appointment Time!!1").replace("hours", "");
				request.setParameter(timeWidgetBase + "hours", "10");
				request.setParameter(timeWidgetBase + "minutes", "30");
				request.setParameter(timeWidgetBase + "seconds", "0");

				// ProviderAjaxAutoCompleteWidget submits integer provider ID via the _hid hidden field
				request.setParameter(widgets.get("Provider") + "_hid", PROVIDER_ID);
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();

				AppointmentSearchRequest searchRequest = new AppointmentSearchRequest();
				searchRequest.setPatientUuid(PATIENT_UUID);
				searchRequest.setStartDate(new Date(0));
				List<Appointment> appointments = Context.getService(AppointmentsService.class).search(searchRequest);

				Assert.assertEquals(1, appointments.size());
				Appointment appt = appointments.get(0);

				Assert.assertEquals(AppointmentStatus.Scheduled, appt.getStatus());
				Assert.assertEquals(AppointmentKind.Scheduled, appt.getAppointmentKind());
				Assert.assertEquals(KIGALI_UUID, appt.getLocation().getUuid());
				Assert.assertEquals(CONSULTATION_UUID, appt.getService().getUuid());
				Assert.assertEquals(Integer.parseInt(PROVIDER_ID), (int) appt.getProviders().iterator().next().getProvider().getProviderId());

				// Verify start time is 2025-01-15 10:30
				Calendar startCal = Calendar.getInstance();
				startCal.setTime(appt.getStartDateTime());
				Assert.assertEquals(2025, startCal.get(Calendar.YEAR));
				Assert.assertEquals(Calendar.JANUARY, startCal.get(Calendar.MONTH));
				Assert.assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH));
				Assert.assertEquals(10, startCal.get(Calendar.HOUR_OF_DAY));
				Assert.assertEquals(30, startCal.get(Calendar.MINUTE));

				// Verify end time is 30 minutes after start (service default duration)
				long durationMs = appt.getEndDateTime().getTime() - appt.getStartDateTime().getTime();
				Assert.assertEquals(30 * 60 * 1000L, durationMs);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldPreselectTypeWhenSingleTypeSpecified() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" type=\"Scheduled\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Appointment Type", html);
				TestUtil.assertFuzzyContains(AppointmentKind.Scheduled.getValue(), html);
				TestUtil.assertFuzzyDoesNotContain(AppointmentKind.WalkIn.getValue(), html);
				TestUtil.assertFuzzyDoesNotContain(AppointmentKind.Virtual.getValue(), html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldFilterTypeDropdownToAllowedTypes() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" type=\"Scheduled,WalkIn\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains(AppointmentKind.Scheduled.getValue(), html);
				TestUtil.assertFuzzyContains(AppointmentKind.WalkIn.getValue(), html);
				TestUtil.assertFuzzyDoesNotContain(AppointmentKind.Virtual.getValue(), html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldRenderNothingInViewMode() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type", "Appointment Time", "Appointment Time!!1", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get("Appointment Time"), "2025-01-15");

				String timeWidgetBase = widgets.get("Appointment Time!!1").replace("hours", "");
				request.setParameter(timeWidgetBase + "hours", "10");
				request.setParameter(timeWidgetBase + "minutes", "30");
				request.setParameter(timeWidgetBase + "seconds", "0");

				// ProviderAjaxAutoCompleteWidget submits integer provider ID via the _hid hidden field
				request.setParameter(widgets.get("Provider") + "_hid", PROVIDER_ID);
			}

			@Override
			public boolean doViewEncounter() {
				return true;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Appointments app", html);
				TestUtil.assertFuzzyDoesNotContain("Kigali", html);
				TestUtil.assertFuzzyDoesNotContain("Consultation", html);
			}
		}.run();
	}
}
