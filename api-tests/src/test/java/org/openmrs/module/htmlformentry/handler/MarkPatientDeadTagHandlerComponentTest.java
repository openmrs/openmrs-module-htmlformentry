package org.openmrs.module.htmlformentry.handler;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.springframework.mock.web.MockHttpServletRequest;

public class MarkPatientDeadTagHandlerComponentTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void loadData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
	}
	
	@Test
	public void testMarkPatientDead() throws Exception {
		final Date date = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
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
	
	@Test
	public void testMarkPatientDeadWithCheckbox() throws Exception {
		final Date date = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "markPatientDeadWithCheckbox";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "MarkPatientDead:", "Date:", "Location:", "Provider:", "Cause:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Cause:"), "1001");
				request.addParameter(widgets.get("MarkPatientDead:"), "false");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				assertThat(results.getPatient().isDead(), is(false));
			}
		}.run();
	}
}
