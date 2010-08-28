package org.openmrs.module.htmlformentry;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper.SubmissionResults;
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
				results.assertNoErrors();
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
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsCreatedCount(1);
				results.assertObsCreated(2, 70d);
			}
		}.run();
	}
	
	@Test
	public void testMultipleObsFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			String getFormName() {
				return "multipleObsForm";
			}
			String[] widgetLabels() {
				return new String[] {"Date:", "Location:", "Provider:", "Weight:", "Allergy:", "Allergy Date:"};
			}
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
				request.addParameter(widgets.get("Allergy:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, 70d);
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(9, date);
			}
		}.run();
	}

	@Test
	public void testSingleObsGroupFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			String getFormName() {
				return "singleObsGroupForm";
			}
			String[] widgetLabels() {
				return new String[] {"Date:", "Location:", "Provider:", "Weight:", "Allergy:", "Allergy Date:"};
			}
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
				request.addParameter(widgets.get("Allergy:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(1);
				results.assertObsLeafCreatedCount(3); // 2 in the obs group, 1 for weight
				results.assertObsCreated(2, 70);
				results.assertObsGroupCreated(7, 8, "Bee stings", 9, date); // allergy construct
			}
		}.run();
	}

	@Test
	public void testMultipleObsGroupFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			String getFormName() {
				return "multipleObsGroupForm";
			}
			String[] widgetLabels() {
				return new String[] {"Date:", "Location:", "Provider:", "Allergy 1:", "Allergy Date 1:", "Allergy 3:", "Allergy Date 3:"};
			}
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// for fun let's fill out part of allergy 1 and allergy 3, but leave allergy 2 blank.
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Allergy 1:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date 1:"), dateAsString(date));
				request.addParameter(widgets.get("Allergy 3:"), "Penicillin");
			}
			void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsGroupCreatedCount(2);
				results.assertObsLeafCreatedCount(3);
				results.assertObsGroupCreated(7, 8, "Bee stings", 9, date);
				results.assertObsGroupCreated(7, 8, "Penicillin");
			}
		}.run();
	}
	
	@Test
	public void viewEmptyEncounterSuccess() throws Exception {
		new RegressionTestHelper() {
			String getFormName() {
				return "simplestForm";
			}
            Encounter getEncounterToView() throws Exception {
            	Encounter e = new Encounter();
            	Date date = Context.getDateFormat().parse("01/02/2003");
            	e.setDateCreated(new Date());
        		e.setEncounterDatetime(date);
        		e.setLocation(Context.getLocationService().getLocation(2));
        		e.setProvider(Context.getPersonService().getPerson(502));
        		return e;
            }
            void testViewingEncounter(Encounter encounter, String html) {
            	assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos", html);
            }
		}.run();
	}
	
	@Test
	public void viewSingleObsEncounterSuccess() throws Exception {
		new RegressionTestHelper() {
			String getFormName() {
				return "singleObsForm";
			}
            Encounter getEncounterToView() throws Exception {
            	Encounter e = new Encounter();
            	e.setPatient(getPatient());
            	Date date = Context.getDateFormat().parse("01/02/2003");
            	e.setDateCreated(new Date());
        		e.setEncounterDatetime(date);
        		e.setLocation(Context.getLocationService().getLocation(2));
        		e.setProvider(Context.getPersonService().getPerson(502));
        		addObs(e, 2, 12.3, null); // weight has conceptId 2
        		return e;
            }
            void testViewingEncounter(Encounter encounter, String html) {
            	assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos Weight:12.3", html);
            }
		}.run();
	}
	
	@Test
	public void viewSingleObsEncounterWithObsOfAnotherConcept() throws Exception {
		new RegressionTestHelper() {
			String getFormName() {
				return "singleObsForm";
			}
            Encounter getEncounterToView() throws Exception {
            	Encounter e = new Encounter();
            	e.setPatient(getPatient());
            	Date date = Context.getDateFormat().parse("01/02/2003");
            	e.setDateCreated(new Date());
        		e.setEncounterDatetime(date);
        		e.setLocation(Context.getLocationService().getLocation(2));
        		e.setProvider(Context.getPersonService().getPerson(502));
        		addObs(e, 1, 965.0, null); // this is a CD4 Count
        		return e;
            }
            void testViewingEncounter(Encounter encounter, String html) {
            	assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos Weight:", html);
            }
		}.run();
	}

}
