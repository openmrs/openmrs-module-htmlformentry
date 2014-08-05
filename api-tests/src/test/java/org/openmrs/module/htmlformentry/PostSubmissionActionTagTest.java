package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.test.TestCustomSubmissionAction;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PostSubmissionActionTagTest extends BaseModuleContextSensitiveTest {

    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

    protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

    @Before
    public void loadData() throws Exception {
        executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
    }

    @Test
    public void testCustomPostSubmissionAction() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {
            @Override
            public String getFormName() {
                return "postSubmissionAction";
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
                assertThat(TestCustomSubmissionAction.getNumberOfCalls(), is(1));
            }
        }.run();
    }

}
