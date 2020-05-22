package org.openmrs.htmlformentry.element;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.ConditionElement;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.module.htmlformentry.handler.ConditionTagHandler;
import org.openmrs.module.htmlformentry.handler.ConditionTagHandlerSupport2_3;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConditionTagTest extends BaseModuleContextSensitiveTest {

	private ConditionElement conditionElement;
	// field names
	private String conditionSearchWidget;
	private String conditionStatusWidget;
	private String onsetDateWidget;
	private String endDateWidget;
	
	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data-openmrs-2.30.xml");
		ConditionTagHandler handler = ((ConditionTagHandler) HtmlFormEntryUtil.getService().getHandlers().get("condition"));
		conditionElement = ((ConditionTagHandlerSupport2_3)handler.getHandler()).getConditionElement();
		conditionElement.setMessageSourceService(mockedMessageSourceService());
	}
	
	@Test
	public void shouldRecordAndEditCondition() throws Exception {	
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "conditionForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}
						
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(conditionSearchWidget, "Some non coded value");
				request.addParameter(conditionStatusWidget, "active");
				request.addParameter(onsetDateWidget, "2014-02-11");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Set<Condition> conditions = results.getEncounterCreated().getConditions();
				
				results.assertNoErrors();
				Assert.assertEquals(1, conditions.size());
				
				Condition condition = conditions.iterator().next();
				Assert.assertEquals(ConditionClinicalStatus.ACTIVE, condition.getClinicalStatus());
				Assert.assertEquals("Some non coded value", condition.getCondition().getNonCoded());
				Assert.assertEquals("2014-02-11", dateAsString(condition.getOnsetDate()));
				Assert.assertNotNull(condition.getId());
			}
						
			@Override
			public void testFormEntrySessionAttribute(FormEntrySession formEntrySession) {
				initializeSelfContainedWidgets(formEntrySession.getContext());
			}
				
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// this holds the selected concept - the internal id for the selected concept
				String hiddenInputElementName = conditionSearchWidget + "_hid";
				
				// do edit
				request.setParameter(conditionSearchWidget, "Epilepsy");
				// Set the selected concept
				request.setParameter(hiddenInputElementName, "3476");
				request.setParameter(conditionStatusWidget, "inactive");
				request.setParameter(endDateWidget, "2020-04-10");
				request.setParameter(onsetDateWidget, "2014-02-11");

			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				// setup
				Set<Condition> conditions = results.getEncounterCreated().getConditions();
				Concept expectedCondition = Context.getConceptService().getConceptByName("Epilepsy");
				
				results.assertNoErrors();
				Assert.assertEquals(1, conditions.size());
				
				Condition condition = conditions.iterator().next();
				Assert.assertEquals(ConditionClinicalStatus.INACTIVE, condition.getClinicalStatus());
				Assert.assertEquals(expectedCondition, condition.getCondition().getCoded());
				Assert.assertEquals("2014-02-11", dateAsString(condition.getOnsetDate()));
				Assert.assertEquals("2020-04-10", dateAsString(condition.getEndDate()));

			}
			
		}.run();
	}
	
	@Test
	public void shouldInitializeDefaultValues() throws Exception {
		new RegressionTestHelper() {
			
            @Override
            public String getFormName() {
                return "conditionForm";
            }
            
            @Override
            public Encounter getEncounterToView() throws Exception {
                return Context.getEncounterService().getEncounter(101);
        	}
            
            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
            	// Verify for condition
            	assertTrue(html.contains("Condition: <span class=\"value\">Edema</span>"));
            	// Verify for condition status
            	assertTrue(html.contains("Status: <span class=\"value\">inactive</span>"));
            	// Verify for onset date 
            	assertTrue(html.contains("Onset Date: <span class=\"value\">12/01/2017</span>"));
            	// Verify for end date
            	assertTrue(html.contains("End Date: <span class=\"value\">15/01/2019</span>"));

        	}
            
            @Override
            public Patient getPatientToEdit() {
        		return getPatient();
        	}
            
            @Override
            public Encounter getEncounterToEdit() {
                return Context.getEncounterService().getEncounter(101);
        	}
                        
            @Override
            public void testEditFormHtml(String html) {
            	// Verify the condition default value - 'Edema'
            	assertTrue(html.contains("<input type=\"text\"  id=\"w7\" name=\"w7\"  onfocus=\"setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis','null');\"class=\"autoCompleteText\"onchange=\"setValWhenAutocompleteFieldBlanked(this)\" onblur=\"onBlurAutocomplete(this)\" value=\"Edema\"/>"));
            	// Verify the condition status - 'Inactive'
            	assertTrue(html.contains("<input type=\"radio\" id=\"w9_1\" name=\"w9\" value=\"inactive\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/>"));
            	// Verify the onset date - '2017-01-12'
            	assertTrue(html.contains("<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w11-display', '#w11', '2017-01-12')</script>"));
            	// Verify the end date - '2019-01-15'
            	assertTrue(html.contains("<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w12-display', '#w12', '2019-01-15')</script>"));

        	}
            
        }.run();
	}
	
	private MessageSourceService mockedMessageSourceService() {
		MessageSourceService ret = mock(MessageSourceService.class);
		when(ret.getMessage("coreapps.conditionui.condition")).thenReturn("Condition");
		when(ret.getMessage("coreapps.conditionui.active.label")).thenReturn("active");
		when(ret.getMessage("coreapps.conditionui.inactive.label")).thenReturn("inactive");
		when(ret.getMessage("htmlformentry.conditionui.historyOf.label")).thenReturn("history-of");
		when(ret.getMessage("coreapps.conditionui.status")).thenReturn("Status");
		when(ret.getMessage("coreapps.conditionui.onsetdate")).thenReturn("Onset Date");
		when(ret.getMessage("coreapps.stopDate.label")).thenReturn("End Date");

		return ret;
		
	}
	
	private void initializeSelfContainedWidgets(FormEntryContext context) {
		conditionSearchWidget = context.getFieldName(conditionElement.getConditionSearchWidget());
		conditionStatusWidget = context.getFieldName(conditionElement.getConditionStatusesWidget());
		onsetDateWidget = context.getFieldName(conditionElement.getOnSetDateWidget());
		endDateWidget = context.getFieldName(conditionElement.getEndDateWidget());
	}
}
