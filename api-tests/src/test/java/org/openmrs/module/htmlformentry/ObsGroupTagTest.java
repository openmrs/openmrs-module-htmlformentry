package org.openmrs.module.htmlformentry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.api.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;

public class ObsGroupTagTest extends BaseHtmlFormEntryTest {
	
	@Autowired
	@Qualifier("conceptService")
	ConceptService conceptService;
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
	}
	
	@Test
	public void testEmptyObsGroupIsNotDisplayed() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupShowIfEmptyFalse";
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				assertFalse(html.contains("It is displayed."));
			}
		}.run();
	}
	
	@Test
	public void testObsGroupIsDisplayed() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupShowIfEmptyFalse";
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				e.setDateCreated(new Date());
				
				TestUtil.addObsGroup(e, 23, new Date(), 18, Boolean.TRUE, new Date());
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				assertTrue(html.contains("It is displayed."));
			}
		}.run();
	}
	
	@Test
	public void testObsGroupHiddenObsDoesNotDisplay() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenObs";
			}
			
			public void testBlankFormHtml(String html) {
				String obsGroupHtml = html.split("obsgroup start")[1];
				String[] tags = obsGroupHtml.split("<");
				ArrayList<String> inputTags = new ArrayList<String>();
				for (String tag : tags) {
					if (tag.contains("input") && tag.contains("type=\"text\"")) {
						inputTags.add(tag);
					}
				}
				assertTrue("Should display only one input box within the obs group, found " + inputTags.size() + ": "
				        + obsGroupHtml,
				    inputTags.size() == 1);
			}
		};
	}
	
	@Test
	public void testObsGroupSubmitsHiddenObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Value:"), "Bee stings");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(1);
				results.assertObsGroupCreated(23, 80000, "Bee stings", 1000, conceptService.getConcept(1004));
			}
		}.run();
	}
	
	@Test
	public void testObsGroupDoesNotSaveHiddenObsWhenEmpty() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:", "Other:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Other:"), "160");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(0);
			}
		}.run();
	}
	
	@Test
	public void testObsGroupDeletesHiddenObsWhenCleared() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:", "Other:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Value:"), "Bee stings");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				String obsGroupHtml = html.split("obsgroup start")[1];
				String[] tags = obsGroupHtml.split("<");
				ArrayList<String> inputTags = new ArrayList<String>();
				for (String tag : tags) {
					if (tag.contains("input") && tag.contains("type=\"text\"")) {
						inputTags.add(tag);
					}
				}
				assertTrue("Should display only one input box within the obs group, found " + inputTags.size() + ": "
				        + obsGroupHtml,
				    inputTags.size() == 1);
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Value:"), "");
				request.addParameter(widgets.get("Other:"), "170");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(0);
				results.assertObsVoided(23, null); // assure the grouping concept has been voided
				results.assertObsVoided(1000, conceptService.getConcept(1004));
			}
			
		}.run();
	}
	
	@Test
	public void testObsGroupSubmitsHiddenObsOfTypeNumeric() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenNumericObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Value:"), "Bee stings");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(1);
				results.assertObsGroupCreated(23, 80000, "Bee stings", 50000, 2.0);
			}
		}.run();
	}
	
	@Test
	public void testObsGroupDoesNotSaveHiddenObsOfTypeNumericWhenEmpty() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenNumericObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:", "Other:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Other:"), "160");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(0);
			}
		}.run();
	}
	
	@Test
	public void testObsGroupDeletesHiddenObsOfTypeNumericWhenCleared() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenNumericObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:", "Other:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Value:"), "Bee stings");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(1);
				results.assertObsGroupCreated(23, 80000, "Bee stings", 50000, 2.0);
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				String obsGroupHtml = html.split("obsgroup start")[1];
				String[] tags = obsGroupHtml.split("<");
				ArrayList<String> inputTags = new ArrayList<String>();
				for (String tag : tags) {
					if (tag.contains("input") && tag.contains("type=\"text\"")) {
						inputTags.add(tag);
					}
				}
				assertTrue("Should display only one input box within the obs group, found " + inputTags.size() + ": "
				        + obsGroupHtml,
				    inputTags.size() == 1);
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Value:"), "");
				request.addParameter(widgets.get("Other:"), "170");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(0);
				results.assertObsVoided(23, null); // assure the grouping concept has been voided
				results.assertObsVoided(50000, 2.0);
			}
			
		}.run();
	}
	
	@Test
	public void testObsGroupSubmitsHiddenObsOfTypeText() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenTextObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Value:"), "Bee stings");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(1);
				results.assertObsGroupCreated(23, 80000, "Bee stings", 60000, "Bob");
			}
		}.run();
	}
	
	@Test
	public void testObsGroupDoesNotSaveHiddenObsOfTypeTextWhenEmpty() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenTextObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:", "Other:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Other:"), "160");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(0);
			}
		}.run();
	}
	
	@Test
	public void testObsGroupDeletesHiddenObsOfTypeTextWhenCleared() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithHiddenTextObs";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Value:", "Other:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Value:"), "Bee stings");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(1);
				results.assertObsGroupCreated(23, 80000, "Bee stings", 60000, "Bob");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				String obsGroupHtml = html.split("obsgroup start")[1];
				String[] tags = obsGroupHtml.split("<");
				ArrayList<String> inputTags = new ArrayList<String>();
				for (String tag : tags) {
					if (tag.contains("input") && tag.contains("type=\"text\"")) {
						inputTags.add(tag);
					}
				}
				assertTrue("Should display only one input box within the obs group, found " + inputTags.size() + ": "
				        + obsGroupHtml,
				    inputTags.size() == 1);
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Value:"), "");
				request.addParameter(widgets.get("Other:"), "170");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(0);
				results.assertObsVoided(23, null); // assure the grouping concept has been voided
				results.assertObsVoided(60000, "Bob");
			}
			
		}.run();
	}
}
