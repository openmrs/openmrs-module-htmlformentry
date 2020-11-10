package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class RedirectOnSaveTagTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void loadData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
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
				Assert.assertEquals("/my/module/page?patientId={{patient.id}}&encounterId={{encounter.id}}",
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
				Assert.assertEquals("test/Numeric", results.getFormEntrySession().getAfterSaveUrlTemplate());
			}
		}.run();
	}
}
