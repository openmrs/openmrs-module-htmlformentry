package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.Map;

public class RedirectOnSaveTagTest extends BaseModuleContextSensitiveTest {

    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

    protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

    @Before
    public void loadData() throws Exception {
        executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
    }

    @Test
    public void testRedirectToUrlTemplate() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {
            @Override
            public String getFormName() {
                return "redirectToUrlTemplate";
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
            public void testResults(SubmissionResults results) {
                Assert.assertEquals(
                        "/my/module/page?patientId={{patient.id}}&encounterId={{encounter.id}}",
                        results.getFormEntrySession().getAfterSaveUrlTemplate());
            }
        }.run();
    }

    @Test
    public void testRedirectToUrlScript() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {
            @Override
            public String getFormName() {
                return "redirectToUrlScript";
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
            public void testResults(SubmissionResults results) {
                Assert.assertEquals(
                        "test/Numeric",
                        results.getFormEntrySession().getAfterSaveUrlTemplate());
            }
        }.run();
    }
}
