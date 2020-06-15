package org.openmrs.htmlformentry.element;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class EnrollInProgramElement2_2Test extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_REGRESSION_TEST_DATASET = "org/openmrs/module/htmlformentry/include/RegressionTest-data-openmrs-2.2.xml";
	
	PatientService ps;
	
	ProgramWorkflowService pws;
	
	@Before
	public void loadConcepts() throws Exception {
		ps = Context.getPatientService();
		pws = Context.getProgramWorkflowService();
		executeDataSet(XML_REGRESSION_TEST_DATASET);
	}
	
	@Test
	public void enrollInProgram_shouldEnrollAPatietntWhenPatientProgramAttributeIsSetByUuid() throws Exception {
		final Integer patientId = 2;
		final Integer programId = 10;
		// sanity check
		assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		// final Patient patient = Context.getPatientService().getPatient(patientId);
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithPatientProgramAttributeUuid";
			}
			
			@Override
			public Patient getPatient() {
				return ps.getPatient(patientId);
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Encounter Date:", "Encounter Location:", "Encounter Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Encounter Date:"), dateAsString(encounterDate));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Encounter Provider:"), "502");
			}
			
			// @Override
			// public void testResults(SubmissionResults results) {
			// results.assertNoErrors();
			// results.assertEncounterCreated();
			// List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId),
			// pws.getProgram(programId), null,
			// null, null, null, false);
			// assertEquals(1, pps.size());
			
			// make sure the patient program attribute has been set
			// Set<PatientProgramAttribute> attributes = pps.get(0).getAttributes();
			// assertEquals(1, attributes.size());
			// assertEquals(attributes, Context.getProgramWorkflowService()
			// .getPatientProgramAttributeByUuid("6de7ed10-53ad-11e1-8cb6-00248140a5eb"));
			
			// }
		}.run();
	}
	
}
