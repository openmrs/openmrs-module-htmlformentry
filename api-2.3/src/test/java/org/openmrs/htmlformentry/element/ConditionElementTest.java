package org.openmrs.htmlformentry.element;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ConditionService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.element.ConditionElement;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class ConditionElementTest {
	
	private ConditionElement element;
	
	private MockHttpServletRequest request;
	
	@Mock
	private MessageSourceService messageSourceService;
	
	@Mock
	private ConditionService conditionService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private FormEntrySession session;
	
	@Mock
	private FormEntryContext context;
	
	@Mock
	private AdministrationService adminService;
	
	@Mock
	private ConceptSearchAutocompleteWidget conditionSearchWidget;
	
	@Mock
	private TextFieldWidget additionalDetailWidget;
	
	@Mock
	private RadioButtonsWidget conditionStatusesWidget;
	
	private Encounter encounter;
	
	@Before
	public void setup() {
		// Stub services
		mockStatic(Context.class);
		when(Context.getConditionService()).thenReturn(conditionService);
		when(Context.getMessageSourceService()).thenReturn(messageSourceService);
		when(Context.getConceptService()).thenReturn(conceptService);
		when(Context.getAdministrationService()).thenReturn(adminService);
		
		doAnswer(new Answer<Concept>() {
			
			@Override
			public Concept answer(InvocationOnMock invocation) throws Throwable {
				return new Concept((Integer) invocation.getArguments()[0]);
			}
			
		}).when(conceptService).getConcept(any(Integer.class));
		
		doAnswer(new Answer<ConceptClass>() {
			
			@Override
			public ConceptClass answer(InvocationOnMock invocation) throws Throwable {
				ConceptClass conceptClass = new ConceptClass();
				conceptClass.setName((String) invocation.getArguments()[0]);
				return conceptClass;
			}
			
		}).when(conceptService).getConceptClassByName(any(String.class));
		
		// Setup html form session context
		when(context.getMode()).thenReturn(Mode.ENTER);
		request = new MockHttpServletRequest();
		when(session.getContext()).thenReturn(context);
		when(session.getEncounter()).thenReturn(new Encounter());
		when(session.getPatient()).thenReturn(new Patient(1));
		
		// setup condition element
		element = spy(new ConditionElement());
		element.setConditionSearchWidget(conditionSearchWidget);
		element.setConditionStatusesWidget(conditionStatusesWidget);
		element.setAdditionalDetailWidget(additionalDetailWidget);
		encounter = session.getEncounter();
		element.setTagControlId("my_condition_tag");
	}
	
	@Test
	public void handleSubmission_shouldCreateNewCondition() {
		// setup
		when(conditionSearchWidget.getValue(context, request)).thenReturn("1519");
		when(conditionStatusesWidget.getValue(context, request)).thenReturn("active");
		
		// replay
		element.handleSubmission(session, request);
		
		// verify		
		Set<Condition> conditions = encounter.getConditions();
		Assert.assertEquals(1, conditions.size());
		
		Condition condition = conditions.iterator().next();
		Assert.assertEquals(ConditionClinicalStatus.ACTIVE, condition.getClinicalStatus());
		Assert.assertThat(condition.getCondition().getCoded().getId(), is(1519));
		
	}
	
	@Test
	public void handleSubmission_shouldCreateInactiveCondition() {
		// setup
		when(conditionSearchWidget.getValue(context, request)).thenReturn("1519");
		when(conditionStatusesWidget.getValue(context, request)).thenReturn("inactive");
		
		// replay
		element.handleSubmission(session, request);
		
		// verify
		Set<Condition> conditions = encounter.getConditions();
		Assert.assertEquals(1, conditions.size());
		
		Condition condition = conditions.iterator().next();
		Assert.assertEquals(ConditionClinicalStatus.INACTIVE, condition.getClinicalStatus());
		Assert.assertThat(condition.getCondition().getCoded().getId(), is(1519));
	}
	
	@Test
	public void handleSubmission_shouldCreateNewConditionWithAdditionalDetail() {
		// setup
		when(additionalDetailWidget.getValue(context, request)).thenReturn("Additional detail");
		when(conditionSearchWidget.getValue(context, request)).thenReturn("1519");
		when(conditionStatusesWidget.getValue(context, request)).thenReturn("active");
		
		// replay
		element.setAdditionalDetailVisible(true);
		element.handleSubmission(session, request);
		
		// verify
		Set<Condition> conditions = encounter.getConditions();
		Assert.assertEquals(1, conditions.size());
		
		Condition condition = conditions.iterator().next();
		Assert.assertEquals("Additional detail", condition.getAdditionalDetail());
	}
	
	@Test
	public void handleSubmission_shouldSupportNonCodedValues() {
		// setup
		request.addParameter("condition-field-name", "Typed in non-coded value");
		when(context.getFieldName(conditionSearchWidget)).thenReturn("condition-field-name");
		when(conditionSearchWidget.getValue(context, request)).thenReturn("");
		
		// replay
		element.setRequired(true);
		element.handleSubmission(session, request);
		
		// verify
		Set<Condition> conditions = encounter.getConditions();
		Assert.assertEquals(1, conditions.size());
		
		Condition condition = conditions.iterator().next();
		Assert.assertEquals("Typed in non-coded value", condition.getCondition().getNonCoded());
	}
	
	@Test
	public void handleSubmission_shouldNotAttemptSavingWhenNoConditionWasGivenAndIsNotRequired() {
		// setup
		when(conditionSearchWidget.getValue(context, request)).thenReturn("");
		
		// replay
		element.handleSubmission(session, request);
		
		// verify
		ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
		verify(conditionService, times(0)).saveCondition(captor.capture());
	}
	
	@Test
	public void handleSubmission_shouldSupportFormField() {
		// setup
		element.setTagControlId("my_condition_tag");
		when(conditionSearchWidget.getValue(context, request)).thenReturn("1519");
		when(conditionStatusesWidget.getValue(context, request)).thenReturn("active");
		
		// Mock session
		Form form = new Form();
		form.setName("MyForm");
		form.setVersion("1.0");
		when(session.getForm()).thenReturn(form);
		doCallRealMethod().when(session).generateControlFormPath(anyString(), anyInt());
		
		// replay
		element.handleSubmission(session, request);
		
		// verify
		Set<Condition> conditions = encounter.getConditions();
		Assert.assertEquals(1, conditions.size());
		
		Condition condition = conditions.iterator().next();
		Assert.assertEquals(ConditionClinicalStatus.ACTIVE, condition.getClinicalStatus());
		Assert.assertThat(condition.getCondition().getCoded().getId(), is(1519));
		Assert.assertEquals("HtmlFormEntry^MyForm.1.0/my_condition_tag-0", condition.getFormNamespaceAndPath());
	}
	
	@Test
	public void handleSubmission_shouldNotSubmitTagWithPresetConceptAndWithoutStatus() {
		
		// Mock condition search widget
		when(conditionSearchWidget.getValue(context, request)).thenReturn("1519");
		
		// Test
		element.setPresetConcept(new Concept());
		element.handleSubmission(session, request);
		
		// Verify
		Set<Condition> conditions = encounter.getConditions();
		Assert.assertEquals(0, conditions.size());
	}
	
	@Test
	public void validateSubmission_shouldFailValidationWhenConditionIsNotGivenAndIsRequired() {
		// setup
		element.setRequired(true);
		when(conditionSearchWidget.getValue(context, request)).thenReturn(null);
		when(messageSourceService.getMessage("htmlformentry.conditionui.condition.required"))
		        .thenReturn("A condition is required");
		
		// replay
		List<FormSubmissionError> errors = (List<FormSubmissionError>) element.validateSubmission(context, request);
		
		// verify
		Assert.assertEquals("A condition is required", errors.get(0).getError());
	}
	
	@Test
	public void htmlForConditionSearchWidget_shouldGetConceptSourceClassesFromGP() {
		// setup
		element.setMessageSourceService(messageSourceService);
		when(adminService.getGlobalProperty(ConditionElement.GLOBAL_PROPERTY_CONDITIONS_CRITERIA))
		        .thenReturn("Diagnosis,Finding");
		
		// replay
		String html = element.htmlForConditionSearchWidget(context);
		
		// verify
		Assert.assertTrue(html.contains("setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis,Finding','null')"));
	}
	
	@Test
	public void htmlForConditionSearchWidget_shouldUseDiagnosisConceptClassAsDefaultConceptSource() {
		// setup
		element.setMessageSourceService(messageSourceService);
		when(adminService.getGlobalProperty(ConditionElement.GLOBAL_PROPERTY_CONDITIONS_CRITERIA)).thenReturn(null);
		
		// replay
		String html = element.htmlForConditionSearchWidget(context);
		
		// verify
		Assert.assertTrue(html.contains("setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis','null')"));
		
	}
	
	@Test
	public void generateHtml_shouldThrowWhenMultipleConditionsWithSameControlId() {
		// setup
		when(conditionSearchWidget.getValue(context, request)).thenReturn("1519");
		when(conditionStatusesWidget.getValue(context, request)).thenReturn("active");
		when(context.getMode()).thenReturn(Mode.VIEW);
		
		// an encounter with two conditions located with the same control id
		Encounter encounter = new Encounter();
		Condition c1 = new Condition();
		c1.setFormField("HtmlFormEntry", "MyForm.1.0/my_condition_tag-0");
		Condition c2 = new Condition();
		c2.setFormField("HtmlFormEntry", "MyForm.1.0/my_condition_tag-0");
		
		encounter.addCondition(c1);
		encounter.addCondition(c2);
		when(context.getExistingEncounter()).thenReturn(encounter);
		
		Form form = new Form();
		form.setName("MyForm");
		form.setVersion("1.0");
		when(session.getForm()).thenReturn(form);
		
		// replay
		assertThrows(IllegalStateException.class, () -> element.generateHtml(context));
	}
}
