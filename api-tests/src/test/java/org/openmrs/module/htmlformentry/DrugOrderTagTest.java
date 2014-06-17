package org.openmrs.module.htmlformentry;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Drug;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;


public class DrugOrderTagTest extends BaseModuleContextSensitiveTest {
	
protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	protected static final String XML_DRUG_ORDER_ELEMENT_DATASET = "drugOrderElementDataSet";

	@Before
    public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_DRUG_ORDER_ELEMENT_DATASET));
    }
	
	@Test
	public void testDrugOrderTag_shouldCreateDrugOrder() throws Exception {
		new RegressionTestHelper() {
			final Date date = new Date();
			@Override
			public String getFormName() {
				return "drugOrderTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
				
			}
			
			@Override
			public void testBlankFormHtml(String html){
				System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//drug
				request.addParameter("w7", "2");
				//start date
				request.addParameter("w9", dateAsString(date));
				//dose
				request.addParameter("w11", "1");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				Encounter e = results.getEncounterCreated();
				
				Set<Order> orders = e.getOrders();
				
				Drug drug = Context.getConceptService().getDrug(2);
				assertThat(orders, contains(allOf(hasProperty("drug", is(drug)), hasProperty("dose", is(1.0)), 
					hasProperty("startDate", is(ymdToDate(dateAsString(date)))))));
			}
		}.run();
	}
}
