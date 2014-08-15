package org.openmrs.module.htmlformentry.handler;

import org.joda.time.DateMidnight;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MarkPatientDeadTagHandlerComponentTest extends BaseModuleContextSensitiveTest {

    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

    protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

    @Before
    public void loadData() throws Exception {
        executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
    }

    @Test
    public void testMarkPatientDead() throws Exception {
        final Date date = new DateMidnight().toDate();
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "markPatientDead";
            }

            @Override
            public String[] widgetLabels() {
                return new String[] { "Date:", "Location:", "Provider:", "Cause:" };
            }

            @Override
            public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.addParameter(widgets.get("Date:"), dateAsString(date));
                request.addParameter(widgets.get("Location:"), "2");
                request.addParameter(widgets.get("Provider:"), "502");
                request.addParameter(widgets.get("Cause:"), "1001");
            }

            @Override
            public void testResults(SubmissionResults results) {
                assertThat(results.getPatient().isDead(), is(true));
                assertThat(results.getPatient().getDeathDate(), is(date));
                assertThat(results.getPatient().getCauseOfDeath().getConceptId(), is(1001));
            }
        }.run();
    }

}
