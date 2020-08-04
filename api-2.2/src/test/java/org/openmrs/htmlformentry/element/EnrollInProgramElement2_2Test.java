package org.openmrs.htmlformentry.element;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.AuditableInterceptor;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import junit.framework.Assert;

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
		
		// This is a temporary workaround for TRUNK-5491.
		Logger.getLogger(AuditableInterceptor.class).setLevel(Level.INFO);
		
		final Integer patientId = 2;
		final Integer programId = 10;
		final Patient patient = Context.getPatientService().getPatient(patientId);
		final PatientProgramAttribute ppa = Context.getProgramWorkflowService()
		        .getPatientProgramAttributeByUuid("9de7ed10-97ad-11e1-8cb6-00248150a7eb");
		// sanity check
		assertEquals(0,
		    pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
		            .size());
		final Date encounterDate = new Date();
		
		// enroll the patient in a test program.
		PatientProgram pp = new PatientProgram();
		pp.setPatient(patient);
		pp.setAttribute(ppa);
		Program program = pws.getProgram(programId);
		pp.setProgram(pws.getProgram(programId));
		final ProgramWorkflow wf = program.getWorkflow(100);
		final Date initialEnrollmentDate = new Date();
		pp.setDateEnrolled(initialEnrollmentDate);
		pp.transitionToState(wf.getState(200), initialEnrollmentDate);
		pws.savePatientProgram(pp);
		pp.getCurrentState(wf).getState();
		pp.getId();
		pp.getDateCompleted();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "enrollPatientInProgramSimpleFormWithPatientProgramAttribute";
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
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
				    null, null, null, false);
				assertEquals(1, pps.size());
				
				// make sure the patient program attribute has been set
				Set<PatientProgramAttribute> attributes = pps.get(0).getAttributes();
				Assert.assertEquals(1, attributes.size());
				Assert.assertEquals(attributes.toArray()[0], Context.getProgramWorkflowService()
				        .getPatientProgramAttributeByUuid("9de7ed10-97ad-11e1-8cb6-00248150a7eb"));
				
			}
		}.run();
	}
	
}
