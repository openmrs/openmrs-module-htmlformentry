package org.openmrs.htmlformentry.element;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ProgramAttributeTagHandlerTest extends BaseModuleContextSensitiveTest {

	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

	protected static final String PROGRAM_ATTRIBUTE_TEST_DATASET = "program-attribute-dataset.xml";

	PatientService ps;

	ProgramWorkflowService pws;

	@Before
	public void loadData() throws Exception {
		ps = Context.getPatientService();
		pws = Context.getProgramWorkflowService();
		executeDataSet(XML_DATASET_PATH + PROGRAM_ATTRIBUTE_TEST_DATASET);
	}

	@Test
	public void testProgramAttributeWithPatientNotEnrolledInaProgram() throws Exception {
		final Date date = new Date();
		final Integer patientId = 2;
		final Integer programId =3;
		final String patientAttributeValue = "ABC123";
		//sanity check
		Assert.assertEquals(0,
				pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
						.size());

		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "patientProgramAttribute";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Program Attribute:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				request.addParameter(widgets.get("Program Attribute:"), patientAttributeValue);
			}

			@Override
			public void testResults(SubmissionResults results) {
				// patient enrolled in a program
				results.assertNoErrors();
				results.assertEncounterCreated();
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
						null, null, null, false);
				Assert.assertEquals(1, pps.size());
				PatientProgram pp = pps.get(0);
				Assert.assertTrue(TestUtil.dateEquals(new Date(), pp.getDateEnrolled()));
				Collection<PatientProgramAttribute> ppas = pp.getActiveAttributes();
				Assert.assertEquals(1, ppas.size());
				PatientProgramAttribute ppa = ppas.iterator().next();
				Assert.assertEquals(patientAttributeValue, ppa.getValueReference());
			}
		}.run();
	}

	@Test
	public void testProgramAttributeWithPatientEnrolledInaProgram() throws Exception {
		final Date date = new Date();
		final Integer patientId = 2;
		final Integer programId = 2;
		final Integer programAttributeTypeId = 2;
		final String patientAttributeValue = "ENR123";
		//sanity check - patient already enrolled in a program
		Assert.assertEquals(1,
				pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
						.size());

		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "patientProgramAttributeEnrolledPatient";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Program Attribute:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				request.addParameter(widgets.get("Program Attribute:"), patientAttributeValue);
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(1);
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
						null, null, null, false);
				Assert.assertEquals(1, pps.size());
				// check the patient attribute
				Set<PatientProgramAttribute> patientProgramAttributes = pps.get(0).getAttributes();
				Assert.assertTrue(patientProgramAttributes.size() > 0); // some patient program attributes exist
				for (PatientProgramAttribute ppa : patientProgramAttributes) {
					if (ppa.getAttributeType().getId().equals(programAttributeTypeId)) {
						// check the values of the attribute
						Assert.assertEquals(patientAttributeValue, ppa.getValueReference());
					}
				}
			}
		}.run();
	}
}
