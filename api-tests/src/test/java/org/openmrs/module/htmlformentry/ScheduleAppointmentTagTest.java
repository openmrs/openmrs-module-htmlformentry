package org.openmrs.module.htmlformentry;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Visit;
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

	// Text concept added in scheduleAppointmentTest.xml for storing appointment UUID obs
	private static final String APPT_UUID_CONCEPT = "c36006e5-9fbb-4f20-866b-0ece245615a7";

	// Widget-label note: the date/time widgets have no text label of their own that's safe to
	// anchor on ("Date" also matches the earlier "Encounter Date:" widget, and the wrapper's CSS
	// class name is also a substring of the <style> block emitted earlier in the HTML). Anchor on
	// the literal opening tag of the date/time wrapper instead: DATE_TIME_ANCHOR (no suffix) = the
	// appointment date hidden input (1st name="w" after the anchor), DATE_TIME_ANCHOR + "!!1" =
	// the time-hours select (2nd name="w" after the anchor; minutes/seconds are derived by
	// replacing "hours" in that field name).
	private static final String DATE_TIME_ANCHOR = "<p class=\"schedule-appointment-date-time\">";

	@Before
	public void loadTestData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/scheduleAppointmentTest.xml");
	}

	// ---------------------------------------------------------------
	// HTML rendering tests
	// ---------------------------------------------------------------

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

				// Placeholder options must carry an empty value, not the message text as value.
				// This ensures that validation correctly detects when nothing is selected.
				TestUtil.assertContains("<option value=\"\"", html);
				TestUtil.assertFuzzyDoesNotContain("value=\"Choose a location\"", html);
				TestUtil.assertFuzzyDoesNotContain("value=\"Choose a service\"", html);
				TestUtil.assertFuzzyDoesNotContain("value=\"Choose appointment type\"", html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldDefaultToScheduleAppointmentWhenOptional() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" optional=\"true\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				// "Schedule appointment" radio must be pre-checked
				TestUtil.assertFuzzyContains("value=\"yes\" checked", html);
				TestUtil.assertFuzzyDoesNotContain("value=\"no\" checked", html);
				// Fields wrapper must start visible
				TestUtil.assertFuzzyContains("display:block", html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldHideTypeDropdownWhenSingleTypeSpecified() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" type=\"Scheduled\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				// No type dropdown rendered when a single type is pre-selected
				TestUtil.assertFuzzyDoesNotContain("Appointment Type", html);
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
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" type=\"Scheduled,WalkIn\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
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
	public void scheduleAppointmentTag_shouldFilterServiceOptionsBySpecialities() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" specialities=\"Ortho\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				// Consultation is tagged with the Ortho speciality
				TestUtil.assertFuzzyContains("Consultation", html);
				// Zzz Cardio Follow-up is tagged with the Cardio speciality, filtered out
				TestUtil.assertFuzzyDoesNotContain("Zzz Cardio Follow-up", html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldSortServiceOptionsBySpecialityOrderWhenRequested() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" specialities=\"Cardio,Ortho\" sortServicesBy=\"specialityOrder\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Zzz Cardio Follow-up", html);
				TestUtil.assertFuzzyContains("Consultation", html);

				// specialities="Cardio,Ortho" puts Cardio first, so its service must appear
				// before Consultation (Ortho) even though "Consultation" sorts first alphabetically.
				int cardioIndex = html.indexOf("Zzz Cardio Follow-up");
				int orthoIndex = html.indexOf("Consultation");
				Assert.assertTrue("Expected Cardio service to appear before Ortho service in speciality order",
				        cardioIndex < orthoIndex);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldShowErrorForUnknownSpeciality() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\" specialities=\"NonExistentSpeciality\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("error", html);
				TestUtil.assertFuzzyContains("No appointment speciality found matching: NonExistentSpeciality", html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldRestrictLocationsToVisitLocationAndDescendants() throws Exception {
		final Visit visit = new Visit();
		visit.setLocation(Context.getLocationService().getLocationByUuid("9356400c-a5a2-4588-8f2b-2361b3446eb8")); // Boston

		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment restrictToCurrentVisitLocation=\"true\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public Visit getVisit() {
				return visit;
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Boston", html);
				TestUtil.assertFuzzyContains("Jamaica Plain", html);

				TestUtil.assertFuzzyDoesNotContain("Kigali", html);
				TestUtil.assertFuzzyDoesNotContain("Mirebalais", html);
				TestUtil.assertFuzzyDoesNotContain("Scituate", html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldNotRestrictLocationsWhenNoVisitInContext() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment restrictToCurrentVisitLocation=\"true\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Kigali", html);
				TestUtil.assertFuzzyContains("Mirebalais", html);
				TestUtil.assertFuzzyContains("Boston", html);
				TestUtil.assertFuzzyContains("Scituate", html);
				TestUtil.assertFuzzyContains("Jamaica Plain", html);
			}
		}.run();
	}

	// ---------------------------------------------------------------
	// Submission tests
	// ---------------------------------------------------------------

	@Test
	public void scheduleAppointmentTag_shouldCreateSpecificTimeAppointment() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type",
				        DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-01-15");

				String timeBase = widgets.get(DATE_TIME_ANCHOR + "!!1").replace("hours", "");
				request.setParameter(timeBase + "hours", "10");
				request.setParameter(timeBase + "minutes", "30");
				request.setParameter(timeBase + "seconds", "0");

				// Explicit duration of 45 min overrides the service default of 30 min
				request.setParameter(widgets.get("Duration (minutes)"), "45");
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
				Assert.assertEquals(Integer.parseInt(PROVIDER_ID),
				        (int) appt.getProviders().iterator().next().getProvider().getProviderId());

				Calendar startCal = Calendar.getInstance();
				startCal.setTime(appt.getStartDateTime());
				Assert.assertEquals(2025, startCal.get(Calendar.YEAR));
				Assert.assertEquals(Calendar.JANUARY, startCal.get(Calendar.MONTH));
				Assert.assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH));
				Assert.assertEquals(10, startCal.get(Calendar.HOUR_OF_DAY));
				Assert.assertEquals(30, startCal.get(Calendar.MINUTE));

				// End = start + 45 min (explicit input overrides service default of 30 min)
				long durationMs = appt.getEndDateTime().getTime() - appt.getStartDateTime().getTime();
				Assert.assertEquals(45 * 60 * 1000L, durationMs);
			}
		}.run();
	}

	// ---------------------------------------------------------------
	// Inline validation tests
	// ---------------------------------------------------------------

	@Test
	public void scheduleAppointmentTag_shouldRequireServiceInlineNotAsGlobalError() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type",
				        DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				// Service intentionally omitted
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-01-15");

				String timeBase = widgets.get(DATE_TIME_ANCHOR + "!!1").replace("hours", "");
				request.setParameter(timeBase + "hours", "10");
				request.setParameter(timeBase + "minutes", "30");
				request.setParameter(timeBase + "seconds", "0");

				request.setParameter(widgets.get("Duration (minutes)"), "30");
				request.setParameter(widgets.get("Provider") + "_hid", PROVIDER_ID);
			}

			@Override
			public void testResults(SubmissionResults results) {
				// Must produce an inline validation error, not reach handleSubmission
				// (which would throw a global "Appointment cannot be created without Service")
				results.assertErrors();
				Assert.assertTrue("Expected inline 'Service is required' error",
				        results.getValidationErrors().stream()
				                .anyMatch(e -> e.getError().contains("Service is required")));
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldRequireLocationInline() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type",
				        DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				// Location intentionally omitted
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-01-15");

				String timeBase = widgets.get(DATE_TIME_ANCHOR + "!!1").replace("hours", "");
				request.setParameter(timeBase + "hours", "10");
				request.setParameter(timeBase + "minutes", "30");
				request.setParameter(timeBase + "seconds", "0");

				request.setParameter(widgets.get("Duration (minutes)"), "30");
				request.setParameter(widgets.get("Provider") + "_hid", PROVIDER_ID);
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
				Assert.assertTrue("Expected inline 'Location is required' error",
				        results.getValidationErrors().stream()
				                .anyMatch(e -> e.getError().contains("Location is required")));
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldRequireProviderInline() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type",
				        DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-01-15");

				String timeBase = widgets.get(DATE_TIME_ANCHOR + "!!1").replace("hours", "");
				request.setParameter(timeBase + "hours", "10");
				request.setParameter(timeBase + "minutes", "30");
				request.setParameter(timeBase + "seconds", "0");

				request.setParameter(widgets.get("Duration (minutes)"), "30");
				// Provider intentionally omitted
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
				Assert.assertTrue("Expected inline 'Provider is required' error",
				        results.getValidationErrors().stream()
				                .anyMatch(e -> e.getError().contains("Provider is required")));
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldRequireTimeInline() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type",
				        DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-01-15");
				// Time intentionally omitted

				request.setParameter(widgets.get("Duration (minutes)"), "30");
				request.setParameter(widgets.get("Provider") + "_hid", PROVIDER_ID);
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
				Assert.assertTrue("Expected inline 'Appointment time is required' error",
				        results.getValidationErrors().stream()
				                .anyMatch(e -> e.getError().contains("Appointment time is required")));
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldRequireDurationInline() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type",
				        DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-01-15");

				String timeBase = widgets.get(DATE_TIME_ANCHOR + "!!1").replace("hours", "");
				request.setParameter(timeBase + "hours", "10");
				request.setParameter(timeBase + "minutes", "30");
				request.setParameter(timeBase + "seconds", "0");

				// Duration intentionally omitted
				request.setParameter(widgets.get("Provider") + "_hid", PROVIDER_ID);
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
				Assert.assertTrue("Expected inline 'Duration is required' error",
				        results.getValidationErrors().stream()
				                .anyMatch(e -> e.getError().contains("Duration is required")));
			}
		}.run();
	}

	// ---------------------------------------------------------------
	// Optional tag tests
	// ---------------------------------------------------------------

	@Test
	public void scheduleAppointmentTag_optional_shouldSkipWhenNoSelected() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform>Encounter Date: <encounterDate/><scheduleAppointment locationTag=\"Some Tag\" optional=\"true\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Schedule appointment" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Schedule appointment"), "no");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();

				AppointmentSearchRequest searchRequest = new AppointmentSearchRequest();
				searchRequest.setPatientUuid(PATIENT_UUID);
				searchRequest.setStartDate(new Date(0));
				List<Appointment> appointments = Context.getService(AppointmentsService.class).search(searchRequest);
				Assert.assertEquals(0, appointments.size());
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_optional_shouldCreateAppointmentWhenYesSelected() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform>Encounter Date: <encounterDate/><scheduleAppointment locationTag=\"Some Tag\" optional=\"true\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Schedule appointment", "Location", "Service",
				        "Appointment Type", DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)",
				        "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-02-20");
				request.setParameter(widgets.get("Schedule appointment"), "yes");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-02-20");

				String timeBase = widgets.get(DATE_TIME_ANCHOR + "!!1").replace("hours", "");
				request.setParameter(timeBase + "hours", "14");
				request.setParameter(timeBase + "minutes", "0");
				request.setParameter(timeBase + "seconds", "0");

				request.setParameter(widgets.get("Duration (minutes)"), "30");
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
				Assert.assertEquals(KIGALI_UUID, appointments.get(0).getLocation().getUuid());
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_optional_shouldRequireFieldsWhenYesSelected() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform>Encounter Date: <encounterDate/><scheduleAppointment locationTag=\"Some Tag\" optional=\"true\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Schedule appointment" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-02-20");
				// "yes" selected but no appointment fields filled
				request.setParameter(widgets.get("Schedule appointment"), "yes");
			}

			@Override
			public void testResults(SubmissionResults results) {
				// Required appointment fields must produce validation errors, not a global exception
				results.assertErrors();
			}
		}.run();
	}

	// ---------------------------------------------------------------
	// View mode test
	// ---------------------------------------------------------------

	@Test
	public void scheduleAppointmentTag_shouldShowErrorWhenUuidConceptMissing() throws Exception {
		// When appointmentUuidConcept is omitted the tag handler throws BadFormDesignException,
		// which the HFE framework renders as an inline error div.
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform><scheduleAppointment locationTag=\"Some Tag\"/><submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] {};
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("error", html);
			}
		}.run();
	}

	@Test
	public void scheduleAppointmentTag_shouldShowAppointmentDetailsInViewModeWithUuidConcept() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "scheduleAppointmentForm";
			}

			@Override
			public String getFormXml() {
				return "<htmlform>Encounter Date: <encounterDate/>"
				        + "<scheduleAppointment locationTag=\"Some Tag\" appointmentUuidConcept=\"" + APPT_UUID_CONCEPT + "\"/>"
				        + "<submit/></htmlform>";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Location", "Service", "Appointment Type",
				        DATE_TIME_ANCHOR, DATE_TIME_ANCHOR + "!!1", "Duration (minutes)", "Provider" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), "2025-01-15");
				request.setParameter(widgets.get("Location"), KIGALI_UUID);
				request.setParameter(widgets.get("Service"), CONSULTATION_UUID);
				request.setParameter(widgets.get("Appointment Type"), AppointmentKind.Scheduled.name());
				request.setParameter(widgets.get(DATE_TIME_ANCHOR), "2025-01-15");

				String timeBase = widgets.get(DATE_TIME_ANCHOR + "!!1").replace("hours", "");
				request.setParameter(timeBase + "hours", "10");
				request.setParameter(timeBase + "minutes", "30");
				request.setParameter(timeBase + "seconds", "0");

				request.setParameter(widgets.get("Duration (minutes)"), "30");
				request.setParameter(widgets.get("Provider") + "_hid", PROVIDER_ID);
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
			}

			@Override
			public boolean doViewEncounter() {
				return true;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Kigali", html);
				TestUtil.assertFuzzyContains("Consultation", html);
			}
		}.run();
	}

}
