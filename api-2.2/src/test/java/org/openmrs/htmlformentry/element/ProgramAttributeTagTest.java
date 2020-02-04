package org.openmrs.htmlformentry.element;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ProgramAttributeTagTest extends BaseModuleContextSensitiveTest {

	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

	@Before
	public void loadData() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}

	@Test
	public void testProgramAttribute_shouldSave() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "programAttributeForm";
			}

			@Override
			public Patient getPatient()  {
				return Context.getPatientService().getPatient(1);
			};

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:",  "Attribute:"};
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Program Attribute:"), "ABCDEFGHI");
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

				// confirm that the patient program has an attribute type
				List<PatientProgram> patientProgramList = Context.getProgramWorkflowService().getPatientPrograms(patient, program, new Date(), null, null, null, false);
				// patient should only have one patient program
				Assert.assertEquals("Patient should only be enrolled in " + program.getName(), 1, patientProgramList.size());
				PatientProgram pp = patientProgramList.get(0);
				List<PatientProgramAttribute> programAttributes = pp.getActiveAttributes(Context.getProgramWorkflowService().getProgramAttributeType(1));
				Assert.assertEquals("One program attribute saved", 1, programAttributes.size());
				Assert.assertEquals("Program attribute value is ABCDEFGHI", "ABCDEFGHI", programAttributes.get(0).getValueReference());
			};

		}.run();
	}
}
