package org.openmrs.module.htmlformentry.element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentKind;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.service.SpecialityService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionActions;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ProviderAjaxAutoCompleteWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class ScheduleAppointmentElementTest {

	private static final String CONCEPT_ID = "99001";

	private static final String APPT_UUID = "test-appt-uuid-1234";

	@Mock
	private FormEntryContext context;

	@Mock
	private FormEntrySession session;

	@Mock
	private AppointmentServiceDefinitionService appointmentServiceDefinitionService;

	@Mock
	private AppointmentsService appointmentsService;

	@Mock
	private SpecialityService specialityService;

	@Mock
	private LocationService locationService;

	@Mock
	private MessageSourceService messageSourceService;

	private FormSubmissionActions actions;

	private MockedStatic<Context> mockedContext;

	private MockedStatic<HtmlFormEntryUtil> mockedHtmlFormEntryUtil;

	private Concept concept;

	@Before
	public void setup() throws Exception {
		mockedContext = mockStatic(Context.class);
		mockedHtmlFormEntryUtil = mockStatic(HtmlFormEntryUtil.class);

		AdministrationService administrationService = mock(AdministrationService.class);

		ConceptDatatype textDatatype = new ConceptDatatype();
		textDatatype.setUuid(ConceptDatatype.TEXT_UUID);
		concept = new Concept(Integer.parseInt(CONCEPT_ID));
		concept.setDatatype(textDatatype);

		mockedHtmlFormEntryUtil.when(() -> HtmlFormEntryUtil.getConcept(CONCEPT_ID)).thenReturn(concept);
		mockedHtmlFormEntryUtil.when(() -> HtmlFormEntryUtil.createObs(any(Concept.class), any(), any(), any()))
		        .thenCallRealMethod();
		// real conversion logic per requested type, since different widgets request different types
		// (DateWidget wants a Date, TimeWidget wants Integers) and a blanket canned value breaks whichever
		// widget didn't ask for that type
		mockedHtmlFormEntryUtil
		        .when(() -> HtmlFormEntryUtil.getParameterAsType(any(HttpServletRequest.class), anyString(),
		            any(Class.class)))
		        .thenAnswer(invocation -> {
			        HttpServletRequest req = invocation.getArgument(0);
			        String name = invocation.getArgument(1);
			        Class<?> clazz = invocation.getArgument(2);
			        String val = req.getParameter(name);
			        if (val == null) {
				        return null;
			        }
			        if (Date.class.isAssignableFrom(clazz)) {
				        return new SimpleDateFormat("yyyy-MM-dd").parse(val);
			        }
			        if (Integer.class.isAssignableFrom(clazz)) {
				        return Integer.valueOf(val);
			        }
			        return val;
		        });

		mockedContext.when(Context::getAdministrationService).thenReturn(administrationService);
		mockedContext.when(Context::getMessageSourceService).thenReturn(messageSourceService);
		mockedContext.when(Context::getLocationService).thenReturn(locationService);
		mockedContext.when(() -> Context.getService(AppointmentServiceDefinitionService.class))
		        .thenReturn(appointmentServiceDefinitionService);
		mockedContext.when(() -> Context.getService(SpecialityService.class)).thenReturn(specialityService);
		mockedContext.when(() -> Context.getService(AppointmentsService.class)).thenReturn(appointmentsService);

		when(administrationService.getGlobalProperty(anyString(), anyString())).thenReturn("false");
		lenient().when(administrationService.getGlobalProperty(anyString())).thenReturn(null);
		when(messageSourceService.getMessage(anyString())).thenReturn("label");
		when(locationService.getLocationTagByName(anyString())).thenReturn(null);
		when(locationService.getAllLocations(false)).thenReturn(Collections.emptyList());
		when(locationService.getLocationByUuid(anyString())).thenReturn(null);
		when(appointmentServiceDefinitionService.getAllAppointmentServices(false)).thenReturn(Collections.emptyList());
		when(specialityService.getAllSpecialities()).thenReturn(Collections.emptyList());

		when(context.getMode()).thenReturn(Mode.ENTER);
		when(session.getContext()).thenReturn(context);

		actions = new FormSubmissionActions();
		actions.beginPerson(new Patient(1));
		when(session.getSubmissionActions()).thenReturn(actions);
		when(session.getPatient()).thenReturn(new Patient(1));
		when(session.getEncounter()).thenReturn(new Encounter());
	}

	@After
	public void teardown() {
		try {
			if (mockedContext != null) {
				mockedContext.close();
			}
		} finally {
			if (mockedHtmlFormEntryUtil != null) {
				mockedHtmlFormEntryUtil.close();
			}
		}
	}

	// --- controlId: VIEW mode constructor ---

	@Test
	public void constructor_viewMode_withControlId_shouldFetchObsByControlId() throws Exception {
		Obs obs = new Obs();
		obs.setValueText(APPT_UUID);
		when(context.getMode()).thenReturn(Mode.VIEW);
		when(context.getObsFromExistingObs(concept, "apptCtrl")).thenReturn(obs);
		when(appointmentsService.getAppointmentByUuid(APPT_UUID)).thenReturn(new Appointment());

		new ScheduleAppointmentElement(context, params("appointmentUuidConcept", CONCEPT_ID, "controlId", "apptCtrl"));

		verify(context).getObsFromExistingObs(concept, "apptCtrl");
		verify(context, never()).removeExistingObs(concept, (Concept) null);
	}

	@Test
	public void constructor_viewMode_withoutControlId_shouldFetchObsByRemoveExistingObs() throws Exception {
		Obs obs = new Obs();
		obs.setValueText(APPT_UUID);
		when(context.getMode()).thenReturn(Mode.VIEW);
		when(context.removeExistingObs(concept, (Concept) null)).thenReturn(obs);
		when(appointmentsService.getAppointmentByUuid(APPT_UUID)).thenReturn(new Appointment());

		new ScheduleAppointmentElement(context, params("appointmentUuidConcept", CONCEPT_ID));

		verify(context).removeExistingObs(concept, (Concept) null);
		verify(context, never()).getObsFromExistingObs(any(Concept.class), anyString());
	}

	// --- controlId: handleSubmission ---

	@Test
	public void handleSubmission_withControlId_shouldStampUuidObsWithFormPath() throws Exception {
		Form form = new Form();
		form.setName("MyForm");
		form.setVersion("1.0");
		when(session.getForm()).thenReturn(form);
		doCallRealMethod().when(session).generateControlFormPath(anyString(), anyInt());

		Map<String, String> p = enterParams();
		p.put("controlId", "apptCtrl");
		ScheduleAppointmentElement element = new ScheduleAppointmentElement(context, p);

		element.handleSubmission(session, buildRequest());

		List<Obs> obsToCreate = actions.getObsToCreate();
		assertEquals(1, obsToCreate.size());
		assertEquals("HtmlFormEntry", obsToCreate.get(0).getFormFieldNamespace());
		assertEquals("MyForm.1.0/apptCtrl-0", obsToCreate.get(0).getFormFieldPath());
	}

	@Test
	public void handleSubmission_withoutControlId_shouldNotStampUuidObsWithFormPath() throws Exception {
		ScheduleAppointmentElement element = new ScheduleAppointmentElement(context, enterParams());

		element.handleSubmission(session, buildRequest());

		List<Obs> obsToCreate = actions.getObsToCreate();
		assertEquals(1, obsToCreate.size());
		assertNull(obsToCreate.get(0).getFormFieldNamespace());
		assertNull(obsToCreate.get(0).getFormFieldPath());
	}

	// --- Helpers ---

	private Map<String, String> params(String... kvs) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < kvs.length; i += 2) {
			map.put(kvs[i], kvs[i + 1]);
		}
		return map;
	}

	private Map<String, String> enterParams() {
		Map<String, String> p = new HashMap<>();
		p.put("appointmentUuidConcept", CONCEPT_ID);
		p.put("locationTag", "Some Tag");
		p.put("type", "Scheduled"); // single type → no type dropdown, value pre-selected
		return p;
	}

	private MockHttpServletRequest buildRequest() {
		when(context.getFieldName(any(DropdownWidget.class))).thenReturn("dd");
		when(context.getFieldName(any(TextFieldWidget.class))).thenReturn("tf");
		when(context.getFieldName(any(DateWidget.class))).thenReturn("dt");
		lenient().when(context.getFieldName(any(TimeWidget.class))).thenReturn("time");
		when(context.getFieldName(any(ProviderAjaxAutoCompleteWidget.class))).thenReturn("prov");

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.addParameter("dd", AppointmentKind.Scheduled.name()); // all DropdownWidgets share "dd"; value must be a valid AppointmentKind for the type widget
		req.addParameter("dt", "2025-01-15");         // date (parsed as yyyy-MM-dd by HtmlFormEntryUtil)
		// prov_hid not set → provider = null (appointment is created without provider)
		return req;
	}
}
