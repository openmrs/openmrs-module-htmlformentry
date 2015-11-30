package org.openmrs.module.htmlformentry;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.openmrs.test.OpenmrsMatchers.hasId;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ImmunizationTagTest extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		executeDataSet(XML_DATASET_PATH + "immunizationTagTest.xml");
	}
	
	@Test
	public void testImmunizationTag_shouldCreateAndEditObsGroup() throws Exception {
		new RegressionTestHelper() {
			
			final Date date = new Date();
			
			Date editedDate;
			
			@Override
			public String getFormName() {
				return "immunizationTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
				
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				//System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//w7 is the checkbox for Polio
				request.addParameter("w7", "true");
				//w9 is the date for Bacille
				request.addParameter("w9", dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(2);
				
				Set<Concept> immunizations = new HashSet<Concept>();
				Set<String> dates = new HashSet<String>();
				
				Set<Obs> observations = results.getEncounterCreated().getObs();
				for (Obs obs : observations) {
					if (obs.getConcept().getId().equals(984)) {
						immunizations.add(obs.getValueCoded());
					}
					if (obs.getConcept().getId().equals(1410)) {
						dates.add(dateAsString(obs.getValueDate()));
					}
				}
				assertThat(immunizations, containsInAnyOrder(hasId(886), hasId(783)));
				assertThat(dates, contains(dateAsString(date)));
			}
			
			public void setupEditRequest(MockHttpServletRequest request, java.util.Map<String,String> widgets) {
				Calendar yesterday = Calendar.getInstance();
				yesterday.add(Calendar.DATE, -1);
				editedDate = yesterday.getTime();
				
				//w9 is the date for Bacille
				request.addParameter("w9", dateAsString(editedDate));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(2);
				
				Set<Concept> immunizations = new HashSet<Concept>();
				Set<String> dates = new HashSet<String>();
				
				Set<Obs> observations = results.getEncounterCreated().getObs();
				for (Obs obs : observations) {
					if (obs.getConcept().getId().equals(984)) {
						immunizations.add(obs.getValueCoded());
					}
					if (obs.getConcept().getId().equals(1410)) {
						dates.add(dateAsString(obs.getValueDate()));
					}
				}
				assertThat(immunizations, containsInAnyOrder(hasId(886), hasId(783)));
				assertThat(dates, contains(dateAsString(editedDate)));
			}
		}.run();
	}
	
	@Test
	public void testImmunizationTag_shouldNotAllowDateInFuture() throws Exception {
		new RegressionTestHelper() {
			
			final Date date = new Date();
						
			@Override
			public String getFormName() {
				return "immunizationTestForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
				
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				//System.out.println(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//w7 is the checkbox for Polio
				request.addParameter("w7", "true");
				
				//w9 is the date for Bacille
				Calendar futureDate = Calendar.getInstance();
				futureDate.add(Calendar.DATE, 2);
				request.addParameter("w9", dateAsString(futureDate.getTime()));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(1);
				
				FormSubmissionError error = results.getValidationErrors().get(0);
				assertThat(error.getError(), is("htmlformentry.error.cannotBeInFuture"));
				
			}
		}.run();
	}
}
