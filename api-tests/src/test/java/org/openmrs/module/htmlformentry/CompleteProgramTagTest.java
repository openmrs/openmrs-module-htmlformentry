package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests of the various program-related tags
 */
public class CompleteProgramTagTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void loadConcepts() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}
	
	@Test
	public void testCompleteProgram_shouldCompleteProgram() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "completeProgramForm";
			}
			
			@SuppressWarnings("deprecation")
            @Override
            public Patient getPatient()  {
				Patient patient = Context.getPatientService().getPatient(2);
				Program program = Context.getProgramWorkflowService().getProgram(1);
				
				// as a sanity check, make sure the patient is currently enrolled in the program before we run the test
				Assert.assertTrue("Patient should be in program", Context.getProgramWorkflowService().isInProgram(patient, program, new Date(), null));
				
				return patient;
			};
				
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
			}
			
			@SuppressWarnings("deprecation")
            @Override
            public void testResults(SubmissionResults results) {
				// do all the basic assertions to make sure the program was processed correctly
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// confirm that that patient is no longer in the program
				Patient patient = Context.getPatientService().getPatient(2);
				Program program = Context.getProgramWorkflowService().getProgram(1);
				Assert.assertFalse("Patient should no longer be in program", Context.getProgramWorkflowService().isInProgram(patient, program, new Date(), null));
				
				// but confirm that the patient was in the program in the past
				Assert.assertTrue("Patient should still be in program in the past", Context.getProgramWorkflowService().isInProgram(patient, program, null, new Date()));	
				
				// confirm that the proper program has been closed
				PatientProgram pp = Context.getProgramWorkflowService().getPatientProgramByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5");
				Assert.assertTrue("Program completion date should be current date", TestUtil.dateEquals(new Date(), pp.getDateCompleted()));
			};

		}.run();
	}
	
	@Test
	public void testCompleteProgram_editingFormShouldNotCauseErrors() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "completeProgramForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@SuppressWarnings("deprecation")
            @Override
            public void testEditedResults(SubmissionResults results) {
				// do all the basic assertions to make sure the program was processed correctly
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// confirm that that patient is no longer in the program
				Patient patient = Context.getPatientService().getPatient(2);
				Program program = Context.getProgramWorkflowService().getProgram(1);
				Assert.assertFalse("Patient should no longer be in program", Context.getProgramWorkflowService().isInProgram(patient, program, new Date(), null));
				
				// but confirm that the patient was in the program in the past
				Assert.assertTrue("Patient should still be in program in the past", Context.getProgramWorkflowService().isInProgram(patient, program, null, new Date()));	
				
				// confirm that the proper program has been closed
				PatientProgram pp = Context.getProgramWorkflowService().getPatientProgramByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5");
				Assert.assertTrue("Program completion date should be current date", TestUtil.dateEquals(new Date(), pp.getDateCompleted()));
			};
			

		}.run();
	}
		
}
