package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;


public class RegressionTests extends BaseModuleContextSensitiveTest {

	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	protected static final String XML_CONCEPT_DATASET_PATH = XML_DATASET_PATH + "RegressionTest-data.xml";
	
	@Before
	public void loadConcepts() throws Exception {
        executeDataSet(XML_CONCEPT_DATASET_PATH);
	}
	
	@Test
	public void testSimplestFormFailure() throws Exception {
		new RegressionTestHelper() {
			String getFormName() {
				return "simplestForm";
			}
			String[] widgetLabels() {
				return new String[] {"Date:", "Location:", "Provider:"};
			}
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
			}
			void testResults(SubmissionResults results) {
				results.assertErrors(3); // date, location, and provider are required
				results.assertNoEncounterCreated();
			}
		}.run();
	}

	
	@Test
	public void testSimplestFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			String getFormName() {
				return "simplestForm";
			}
			String[] widgetLabels() {
				return new String[] {"Date:", "Location:", "Provider:"};
			}
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
			}
			void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
			}
		}.run();
	}
	
	
	@Test
	public void testSingleObsFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			String getFormName() {
				return "singleObsForm";
			}
			String[] widgetLabels() {
				return new String[] {"Date:", "Location:", "Provider:", "Weight:"};
			}
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
			}
			void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
				results.assertObsCreatedCount(1);
				results.assertObsCreated(2, 70d);
			}
		}.run();
	}
	
}
