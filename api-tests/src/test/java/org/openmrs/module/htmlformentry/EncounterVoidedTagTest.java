package org.openmrs.module.htmlformentry;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;


public class EncounterVoidedTagTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void loadConcepts() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}
	
	@Test
	public void testVoidingMultipleObsForm() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "multipleObsFormToVoid";
			}
			
			@Override
			public Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Weight:", "Allergy:", "Allergy Date:", "Void:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Void:"), "true");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterVoided();
			};

		}.run();
	}
	
	@Test
	public void shouuldNotVoidEncounterIfVoidedSetToFalse () throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "multipleObsFormToVoid";
			}
			
			@Override
			public Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Weight:", "Allergy:", "Allergy Date:", "Void:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Void:"), "false");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterNotVoided();
			};

		}.run();
	}
	
	@Test
	public void shouuldVoidByFormSchemaIfVoidEncounterByHtmlFormSchemaGlobalPropertySetToTrue () throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// set the appropriate global property to true
				Context.getAdministrationService().saveGlobalProperty(new GlobalProperty("htmlformentry.voidEncounterByHtmlFormSchema", "true"));
			}
			
			@Override
			public String getFormName() {
				return "multipleObsFormToVoid";
			}
			
			@Override
			public Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Weight:", "Allergy:", "Allergy Date:", "Void:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Void:"), "true");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				
				// in this case the whole encounter should not be voided because not all the obs associated with the encounter 
				// were in the form schema
				results.assertEncounterNotVoided();
				
				// we rely on the specific tests of the voidEncounterByHtmlFormSchema() method that are in 
				// HtmlFormEntryUtil test to test more complex use cases
			};

		}.run();
	}
	
	@Test
	public void shouuldVoidEntireEncounterIfVoidEncounterByHtmlFormSchemaGlobalPropertySetToFalse () throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// set the appropriate global property to true
				Context.getAdministrationService().saveGlobalProperty(new GlobalProperty("htmlformentry.voidEncounterByHtmlFormSchema", "false"));
			}
			
			@Override
			public String getFormName() {
				return "multipleObsFormToVoid";
			}
			
			@Override
			public Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Weight:", "Allergy:", "Allergy Date:", "Void:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Void:"), "true");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterVoided();
			};

		}.run();
	}
	
	
}
