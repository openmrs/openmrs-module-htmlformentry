package org.openmrs.module.htmlformentry.element;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.Speciality;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionActions;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentServiceObsElementTest {

	private static final String SERVICE_UUID = "service-uuid-1";

	private static final String CONCEPT_ID = "5090";

	@Mock
	private FormEntryContext context;

	@Mock
	private FormEntrySession session;

	@Mock
	private AppointmentServiceDefinitionService appointmentServiceDefinitionService;

	private FormSubmissionActions actions;

	private MockedStatic<Context> mockedContext;

	private MockedStatic<HtmlFormEntryUtil> mockedHtmlFormEntryUtil;

	private Concept concept;

	@Before
	public void setup() throws Exception {
		mockedContext = mockStatic(Context.class);
		mockedHtmlFormEntryUtil = mockStatic(HtmlFormEntryUtil.class);

		ConceptDatatype textDatatype = new ConceptDatatype();
		textDatatype.setUuid(ConceptDatatype.TEXT_UUID);
		concept = new Concept(Integer.parseInt(CONCEPT_ID));
		concept.setDatatype(textDatatype);

		mockedHtmlFormEntryUtil.when(() -> HtmlFormEntryUtil.getConcept(CONCEPT_ID)).thenReturn(concept);
		mockedHtmlFormEntryUtil.when(() -> HtmlFormEntryUtil.createObs(any(Concept.class), any(), any(), any())).thenCallRealMethod();
		mockedContext.when(() -> Context.getService(AppointmentServiceDefinitionService.class))
		        .thenReturn(appointmentServiceDefinitionService);
		mockedContext.when(Context::getLocale).thenReturn(Locale.ENGLISH);

		when(appointmentServiceDefinitionService.getAllAppointmentServices(false))
		        .thenReturn(Collections.singletonList(makeService("Cardiology", 1, SERVICE_UUID, null)));

		when(context.getMode()).thenReturn(Mode.ENTER);
		when(session.getContext()).thenReturn(context);

		actions = new FormSubmissionActions();
		actions.beginPerson(new Patient(1));
		when(session.getSubmissionActions()).thenReturn(actions);
	}

	@After
	public void teardown() {
		mockedContext.close();
		mockedHtmlFormEntryUtil.close();
	}

	// --- Constructor validation ---

	@Test
	public void constructor_shouldThrowWhenConceptIdParamIsMissing() {
		assertThrows(BadFormDesignException.class, () -> new AppointmentServiceObsElement(context, new HashMap<>()));
	}

	@Test
	public void constructor_shouldThrowWhenConceptCannotBeFound() {
		mockedHtmlFormEntryUtil.when(() -> HtmlFormEntryUtil.getConcept("unknown")).thenReturn(null);
		assertThrows(BadFormDesignException.class,
		    () -> new AppointmentServiceObsElement(context, params("conceptId", "unknown")));
	}

	// --- Speciality filtering ---

	@Test
	public void constructor_shouldIncludeAllServicesWhenNoSpecialityFilterGiven() throws Exception {
		when(appointmentServiceDefinitionService.getAllAppointmentServices(false)).thenReturn(
		    Arrays.asList(makeService("Ortho", 1, "uuid-ortho", makeSpeciality("Orthopaedics", "spec-uuid-ortho")),
		        makeService("Cardiology", 2, "uuid-cardio", makeSpeciality("Cardiology", "spec-uuid-cardio"))));

		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));
		stubFieldName(element, "field");
		String html = element.generateHtml(context);

		assertThat(html.contains("uuid-ortho"), is(true));
		assertThat(html.contains("uuid-cardio"), is(true));
	}

	@Test
	public void constructor_shouldFilterServicesBySpeciality() throws Exception {
		when(appointmentServiceDefinitionService.getAllAppointmentServices(false)).thenReturn(
		    Arrays.asList(makeService("Ortho", 1, "uuid-ortho", makeSpeciality("Orthopaedics", "spec-uuid-ortho")),
		        makeService("Cardiology", 2, "uuid-cardio", makeSpeciality("Cardiology", "spec-uuid-cardio"))));

		Map<String, String> p = params("conceptId", CONCEPT_ID);
		p.put("specialities", "Cardiology");
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, p);
		stubFieldName(element, "field");
		String html = element.generateHtml(context);

		assertThat(html.contains("uuid-cardio"), is(true));
		assertThat(html.contains("uuid-ortho"), is(false));
	}

	@Test
	public void constructor_shouldMatchSpecialityFilterCaseInsensitively() throws Exception {
		when(appointmentServiceDefinitionService.getAllAppointmentServices(false))
		        .thenReturn(Collections.singletonList(makeService("Cardiology", 2, "uuid-cardio", makeSpeciality("Cardiology", "spec-uuid-cardio"))));

		Map<String, String> p = params("conceptId", CONCEPT_ID);
		p.put("specialities", "CARDIOLOGY");
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, p);
		stubFieldName(element, "field");
		String html = element.generateHtml(context);

		assertThat(html.contains("uuid-cardio"), is(true));
	}

	@Test
	public void constructor_shouldFilterServicesBySpecialityUuid() throws Exception {
		when(appointmentServiceDefinitionService.getAllAppointmentServices(false)).thenReturn(
		    Arrays.asList(makeService("Ortho", 1, "uuid-ortho", makeSpeciality("Orthopaedics", "spec-uuid-ortho")),
		        makeService("Cardiology", 2, "uuid-cardio", makeSpeciality("Cardiology", "spec-uuid-cardio"))));

		Map<String, String> p = params("conceptId", CONCEPT_ID);
		p.put("specialities", "spec-uuid-cardio");
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, p);
		stubFieldName(element, "field");
		String html = element.generateHtml(context);

		assertThat(html.contains("uuid-cardio"), is(true));
		assertThat(html.contains("uuid-ortho"), is(false));
	}

	@Test
	public void constructor_shouldExcludeServicesWithNoSpecialityWhenFilterIsSet() throws Exception {
		when(appointmentServiceDefinitionService.getAllAppointmentServices(false))
		        .thenReturn(Collections.singletonList(makeService("General", 3, "uuid-general", null)));

		Map<String, String> p = params("conceptId", CONCEPT_ID);
		p.put("specialities", "Cardiology");
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, p);
		stubFieldName(element, "field");
		String html = element.generateHtml(context);

		assertThat(html.contains("uuid-general"), is(false));
	}

	// --- handleSubmission: ENTER mode ---

	@Test
	public void handleSubmission_shouldCreateObsInEnterMode() throws Exception {
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));
		HttpServletRequest request = requestWithService(SERVICE_UUID, element);

		element.handleSubmission(session, request);

		List<Obs> obsToCreate = actions.getObsToCreate();
		assertEquals(1, obsToCreate.size());
		assertEquals(concept, obsToCreate.get(0).getConcept());
		assertEquals(SERVICE_UUID, obsToCreate.get(0).getValueText());
	}

	@Test
	public void handleSubmission_shouldNotCreateObsWhenValueIsEmpty() throws Exception {
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));
		HttpServletRequest request = requestWithService("", element);

		element.handleSubmission(session, request);

		assertEquals(0, actions.getObsToCreate().size());
	}

	// --- handleSubmission: EDIT mode ---

	@Test
	public void handleSubmission_shouldModifyExistingObsInEditMode() throws Exception {
		Obs existingObs = new Obs();
		existingObs.setConcept(concept);
		existingObs.setValueText("old-service-uuid");
		when(context.removeExistingObs(concept, (Concept) null)).thenReturn(existingObs);
		when(context.getMode()).thenReturn(Mode.EDIT);

		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));
		HttpServletRequest request = requestWithService(SERVICE_UUID, element);

		element.handleSubmission(session, request);

		// modifyObs voids the old obs and queues a new one
		assertEquals(1, actions.getObsToVoid().size());
		assertEquals(existingObs, actions.getObsToVoid().get(0));
		assertEquals(1, actions.getObsToCreate().size());
		assertEquals(SERVICE_UUID, actions.getObsToCreate().get(0).getValueText());
	}

	@Test
	public void handleSubmission_shouldVoidExistingObsWhenValueIsClearedInEditMode() throws Exception {
		Obs existingObs = new Obs();
		existingObs.setConcept(concept);
		existingObs.setValueText("old-service-uuid");
		when(context.removeExistingObs(concept, (Concept) null)).thenReturn(existingObs);
		when(context.getMode()).thenReturn(Mode.EDIT);

		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));
		HttpServletRequest request = requestWithService("", element);

		element.handleSubmission(session, request);

		assertEquals(1, actions.getObsToVoid().size());
		assertEquals(existingObs, actions.getObsToVoid().get(0));
		assertEquals(0, actions.getObsToCreate().size());
	}

	// --- VIEW mode display ---

	@Test
	public void generateHtml_shouldDisplayServiceNameInViewModeWhenInsideObsGroup() throws Exception {
		Obs existingObs = new Obs();
		existingObs.setConcept(concept);
		existingObs.setValueText(SERVICE_UUID);
		when(context.getCurrentObsGroupConcepts()).thenReturn(Collections.singletonList(new Concept()));
		when(context.getObsFromCurrentGroup(concept, (Concept) null)).thenReturn(existingObs);
		when(context.getMode()).thenReturn(Mode.VIEW);

		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));

		String html = element.generateHtml(context);

		assertThat(html, is("<span class=\"value\">Cardiology</span>"));
	}

	@Test
	public void generateHtml_shouldDisplayServiceNameInViewMode() throws Exception {
		Obs existingObs = new Obs();
		existingObs.setConcept(concept);
		existingObs.setValueText(SERVICE_UUID);
		when(context.removeExistingObs(concept, (Concept) null)).thenReturn(existingObs);
		when(context.getMode()).thenReturn(Mode.VIEW);

		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));

		String html = element.generateHtml(context);

		assertThat(html, is("<span class=\"value\">Cardiology</span>"));
	}

	// --- handleSubmission: VIEW mode ---

	@Test
	public void handleSubmission_shouldDoNothingInViewMode() throws Exception {
		when(context.getMode()).thenReturn(Mode.VIEW);

		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));
		HttpServletRequest request = requestWithService(SERVICE_UUID, element);

		element.handleSubmission(session, request);

		assertEquals(0, actions.getObsToCreate().size());
	}

	// --- controlId / form path stamping ---

	@Test
	public void handleSubmission_shouldStampObsWithControlFormPathWhenControlIdIsSet() throws Exception {
		Form form = new Form();
		form.setName("MyForm");
		form.setVersion("1.0");
		when(session.getForm()).thenReturn(form);
		doCallRealMethod().when(session).generateControlFormPath(anyString(), anyInt());

		Map<String, String> p = params("conceptId", CONCEPT_ID);
		p.put("controlId", "appt_service_obs");
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, p);
		HttpServletRequest request = requestWithService(SERVICE_UUID, element);

		element.handleSubmission(session, request);

		List<Obs> obsToCreate = actions.getObsToCreate();
		assertEquals(1, obsToCreate.size());
		assertEquals("HtmlFormEntry", obsToCreate.get(0).getFormFieldNamespace());
		assertEquals("MyForm.1.0/appt_service_obs-0", obsToCreate.get(0).getFormFieldPath());
	}

	@Test
	public void handleSubmission_shouldNotStampObsWithFormPathWhenNoControlId() throws Exception {
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, params("conceptId", CONCEPT_ID));
		HttpServletRequest request = requestWithService(SERVICE_UUID, element);

		element.handleSubmission(session, request);

		List<Obs> obsToCreate = actions.getObsToCreate();
		assertEquals(1, obsToCreate.size());
		assertNull(obsToCreate.get(0).getFormFieldNamespace());
		assertNull(obsToCreate.get(0).getFormFieldPath());
	}

	@Test
	public void constructor_shouldLookUpExistingObsByControlIdWhenSet() throws Exception {
		Obs existingObs = new Obs();
		existingObs.setConcept(concept);
		existingObs.setValueText(SERVICE_UUID);
		when(context.getObsFromExistingObs(concept, "appt_service_obs")).thenReturn(existingObs);

		Map<String, String> p = params("conceptId", CONCEPT_ID);
		p.put("controlId", "appt_service_obs");
		AppointmentServiceObsElement element = new AppointmentServiceObsElement(context, p);
		stubFieldName(element, "field");
		String html = element.generateHtml(context);

		// the existing obs UUID should be pre-selected
		assertThat(html.contains("selected"), is(true));
	}

	// --- Helpers ---

	private Map<String, String> params(String key, String value) {
		Map<String, String> map = new HashMap<>();
		map.put(key, value);
		return map;
	}

	private void stubFieldName(AppointmentServiceObsElement element, String fieldName) {
		when(context.getFieldName(element.serviceWidget)).thenReturn(fieldName);
	}

	private MockHttpServletRequest requestWithService(String uuid, AppointmentServiceObsElement element) {
		String fieldName = "service-field";
		stubFieldName(element, fieldName);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(fieldName, uuid);
		return request;
	}

	private AppointmentServiceDefinition makeService(String name, Integer id, String uuid, Speciality speciality) {
		AppointmentServiceDefinition svc = new AppointmentServiceDefinition();
		svc.setName(name);
		svc.setId(id);
		svc.setUuid(uuid);
		svc.setSpeciality(speciality);
		return svc;
	}

	private Speciality makeSpeciality(String name, String uuid) {
		Speciality speciality = new Speciality();
		speciality.setName(name);
		speciality.setUuid(uuid);
		return speciality;
	}
}
