package org.openmrs.htmlformentry.element;

import junit.framework.Assert;
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

import java.util.Date;
import java.util.List;
import java.util.Map;


public class ProgramAttributeElementTest extends BaseModuleContextSensitiveTest {

	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

	protected static final String PROGRAM_ATTRIBUTE_TEST_DATASET = "program-attribute-dataset.xml";

	PatientService ps;

	ProgramWorkflowService pws;

	@Before
	public void loadData() throws Exception {
		ps = Context.getPatientService();
		pws = Context.getProgramWorkflowService();
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(PROGRAM_ATTRIBUTE_TEST_DATASET));
	}

	@Test
	public void testSimplestFormFailure() throws Exception {
		final Date date = new Date();
		final Integer patientId = 2;
		final Integer programId = 10;
		//sanity check
		Assert.assertEquals(0,
				pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null, null, null, null, false)
						.size());

		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "programAttribute";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Program Attribute:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Program Attribute:"), "ABC123");

			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				List<PatientProgram> pps = pws.getPatientPrograms(ps.getPatient(patientId), pws.getProgram(programId), null,
						null, null, null, false);
				Assert.assertEquals(1, pps.size());
				// check the patient attribute
				PatientProgramAttribute ppa = pps.get(0).getAttributes().iterator().next();

				// check the values of the attribute
				Assert.assertEquals("ABC123", ppa.getValueReference());
			}
		}.run();
	}
}
