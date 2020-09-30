package org.openmrs.module.htmlformentry;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.Module;
import org.openmrs.module.metadatamapping.MetadataSource;
import org.openmrs.module.metadatamapping.MetadataTermMapping;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests of the various program-related tags
 */
public class CompleteProgramTagTest extends BaseHtmlFormEntryTest {
	
	private static Module module = new Module("metadatamapping", "metadatamapping", "packageName", "author", "desc",
	        "1.3.4");
	
	@Before
	public void loadConcepts() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		loadTestMappings();
	}
	
	private void loadTestMappings() {
		MetadataSource metadataSource = new MetadataSource();
		metadataSource.setName("source");
		metadataSource.setDateCreated(new Date());
		metadataSource.setRetired(false);
		metadataSource = Context.getService(MetadataMappingService.class).saveMetadataSource(metadataSource);
		
		MetadataTermMapping metadataTermMapping1 = new MetadataTermMapping(metadataSource, "CODE",
		        Context.getProgramWorkflowService().getProgram(1));
		metadataTermMapping1.setName("mapping1");
		Context.getService(MetadataMappingService.class).saveMetadataTermMapping(metadataTermMapping1);
		
		MetadataTermMapping metadataTermMapping2 = new MetadataTermMapping(metadataSource, "CODE2",
		        Context.getUserService().getRole("Provider"));
		metadataTermMapping2.setName("mapping2");
		Context.getService(MetadataMappingService.class).saveMetadataTermMapping(metadataTermMapping2);
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
			public Patient getPatient() {
				Patient patient = Context.getPatientService().getPatient(2);
				Program program = Context.getProgramWorkflowService().getProgram(1);
				
				// as a sanity check, make sure the patient is currently enrolled in the program before we run the test
				Assert.assertTrue("Patient should be in program", isInProgram(patient, program));
				
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
				Assert.assertFalse("Patient should no longer be in program", isInProgram(patient, program));
				
				// but confirm that the patient was in the program in the past
				Assert.assertTrue("Patient should still be in program in the past", everInProgram(patient, program));
				
				// confirm that the proper program has been closed
				PatientProgram pp = Context.getProgramWorkflowService()
				        .getPatientProgramByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5");
				Assert.assertTrue("Program completion date should be current date",
				    TestUtil.dateEquals(new Date(), pp.getDateCompleted()));
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
				Assert.assertFalse("Patient should no longer be in program", isInProgram(patient, program));
				
				// but confirm that the patient was in the program in the past
				Assert.assertTrue("Patient should still be in program in the past", everInProgram(patient, program));
				
				// confirm that the proper program has been closed
				PatientProgram pp = Context.getProgramWorkflowService()
				        .getPatientProgramByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5");
				Assert.assertTrue("Program completion date should be current date",
				    TestUtil.dateEquals(new Date(), pp.getDateCompleted()));
			};
			
		}.run();
	}
	
	@Test
	public void testCompleteProgram_shouldCompleteProgramAndRecordOutcome() throws Exception {
		
		new RegressionTestHelper() {
			
			final Date date = new Date();
			
			@Override
			public String getFormName() {
				return "completeProgramWithOutcomeForm";
			}
			
			@SuppressWarnings("deprecation")
			@Override
			public Patient getPatient() {
				Patient patient = Context.getPatientService().getPatient(8);
				Program program = Context.getProgramWorkflowService().getProgram(10);
				
				// as a sanity check, make sure the patient is currently enrolled in the program before we run the test
				Assert.assertTrue("Patient should be in program", isInProgram(patient, program));
				
				return patient;
			};
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Outcome:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Outcome:"), "4202");
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
				Patient patient = Context.getPatientService().getPatient(8);
				Program program = Context.getProgramWorkflowService().getProgram(10);
				Assert.assertFalse("Patient should no longer be in program", isInProgram(patient, program));
				
				// but confirm that the patient was in the program in the past
				Assert.assertTrue("Patient should still be in program in the past", everInProgram(patient, program));
				
				// confirm that the proper program has been closed
				PatientProgram pp = Context.getProgramWorkflowService()
				        .getPatientProgramByUuid("31396060-03aa-102d-b0e3-001ec94a0cc5");
				Assert.assertTrue("Program completion date should be current date",
				    TestUtil.dateEquals(new Date(), pp.getDateCompleted()));
				
				// Confirm that the patient program has the right outcome
				Assert.assertEquals("Program should have recorded outcome", Context.getConceptService().getConcept(4202),
				    pp.getOutcome());
			};
			
		}.run();
	}
	
	@Test
	public void testCompleteProgram_shouldCompleteProgramMapping() throws Exception {
		new RegressionTestHelper() {
			
			final Date date = new Date();
			
			@Override
			public String getFormName() {
				return "completeProgramFormMapping";
			}
			
			@SuppressWarnings("deprecation")
			@Override
			public void testBlankFormHtml(String html) {
				super.testBlankFormHtml(html);
				Assert.assertTrue("Should check the html form", html.contains(
				    "<option value=\"\" selected=\"true\">Choose a Provider</option><option value=\"502\">Hippocrates of Cos</option>"));
			}
			
			@SuppressWarnings("deprecation")
			@Override
			public Patient getPatient() {
				Patient patient = Context.getPatientService().getPatient(2);
				Program program = Context.getProgramWorkflowService().getProgram(1);
				
				// as a sanity check, make sure the patient is currently enrolled in the program before we run the test
				Assert.assertTrue("Patient should be in program", isInProgram(patient, program));
				
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
				Assert.assertFalse("Patient should no longer be in program", isInProgram(patient, program));
				
				// but confirm that the patient was in the program in the past
				Assert.assertTrue("Patient should still be in program in the past", everInProgram(patient, program));
				
				// confirm that the proper program has been closed
				PatientProgram pp = Context.getProgramWorkflowService()
				        .getPatientProgramByUuid("32296060-03aa-102d-b0e3-001ec94a0cc5");
				Assert.assertTrue("Program completion date should be current date",
				    TestUtil.dateEquals(new Date(), pp.getDateCompleted()));
			};
			
		}.run();
	}
	
	/**
	 * @return the ProgramWorkflowService.isInProgram method removed in 2.x. Copied below for
	 *         consistency.
	 */
	public boolean isInProgram(Patient patient, Program program) {
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		Date onDate = getDateAtMidnight(new Date());
		for (PatientProgram pp : pws.getPatientPrograms(patient, program, null, null, null, null, false)) {
			if (pp.getActive(onDate)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return the ProgramWorkflowService.isInProgram method removed in 2.x. Copied below for
	 *         consistency.
	 */
	public boolean everInProgram(Patient patient, Program program) {
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		Date onDate = new Date();
		for (PatientProgram pp : pws.getPatientPrograms(patient, program, null, null, null, null, false)) {
			if (pp.getDateEnrolled().before(onDate)) {
				return true;
			}
		}
		return false;
	}
	
	public Date getDateAtMidnight(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}
	
}
