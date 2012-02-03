package org.openmrs.module.htmlformentry;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class PatientTagTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void loadData() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}
	
	/**
	 * This test currently fails, perhaps because the ability to create a patient without
	 * simultaneously creating an encounter was not implemented in HTML-94.
	 */
	@Test
	public void testCreateMinimalPatient() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "simpleCreatePatientForm";
			}
			
			@Override
			Patient getPatient() {
				return new Patient();
			}
			
			@Override
			String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:" };
			}
			
			@Override
			void testBlankFormHtml(String html) {
				System.out.println(">>>>\n" + html);
			}
			
			@Override
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "9234923dfasd2");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "2");
				
			}
			
			@Override
			void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				results.assertNoEncounterCreated();
			}
		}.run();
	}
	
	@Test
	public void testCreatePatientAndEncounter() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "simplePatientAndEncounterForm";
			}
			
			@Override
			Patient getPatient() {
				return new Patient();
			}
			
			@Override
			String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Date:", "Encounter Location:", "Provider:" };
			}
			
			@Override
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "9234923dfasd2");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "2");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
			}
			
			@Override
			void testResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getPersonName().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getPersonName().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("9234923dfasd2", results.getPatient().getPatientIdentifier().getIdentifier());
				
				results.assertEncounterCreated();
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
			}
		}.run();
	}
	
	@Test
	public void testEditPatientDetailsWithoutEditingEncounter() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			boolean doViewEncounter() {
				return true;
			};
			
			@Override
			Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName" };
			}
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Simon");
				request.setParameter(widgets.get("PersonName.familyName"), "paul");
			}
			
			@Override
			void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				Assert.assertEquals("Simon", results.getPatient().getGivenName());
				Assert.assertEquals("paul", results.getPatient().getFamilyName());
				Assert.assertEquals("M", results.getPatient().getGender());
				results.assertEncounterEdited();
			}
			
		}.run();
	}
	
	@Test
	public void testEditPatientDetailsAndEncounterDetails() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Date:", "Encounter Location:" };
			}
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Mark");
				request.setParameter(widgets.get("PersonName.familyName"), "waugh");
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
			}
			
			@Override
			void testEditedResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				results.assertNoErrors();
				results.assertPatient();
				results.getPatient().getPersonName();
				Assert.assertEquals("Mark", results.getPatient().getGivenName());
				Assert.assertEquals("waugh", results.getPatient().getFamilyName());
				Assert.assertEquals("M", results.getPatient().getGender());
				
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertEncounterEdited();
			}
			
		}.run();
	}
	
	@Test
	/**
	 * TODO Testcase Fails with error org.hibernate.PropertyValueException: not-null property
	 * references a null or transient value: org.openmrs.PersonName.dateCreated
	 */
	public void testEditPatientNameAndMultipleObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			}
			
			@Override
			Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Weight:" };
			}
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Mike");
				request.setParameter(widgets.get("PersonName.familyName"), "Den");
				request.setParameter(widgets.get("Weight:"), "100");
			}
			
			@Override
			void testEditedResults(SubmissionResults results) {
				@SuppressWarnings("unused")
				Date datePartOnly = ymdToDate(dateAsString(date));
				results.assertNoErrors();
				results.assertPatient();
				results.getPatient().getPersonName();
				Assert.assertEquals("Mike", results.getPatient().getGivenName());
				Assert.assertEquals("Den", results.getPatient().getFamilyName());
				Assert.assertEquals("M", results.getPatient().getGender());
				
				results.assertObsCreated(2, 100d);
				
			}
			
			@Override
			void testBlankFormHtml(String html) {
				System.out.println(">>>>\n" + html);
			}
			
		}.run();
	}
	
	@Test
	public void testEditObsWithoutEditingPatient() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "Weight:" };
			};
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Weight:"), "100");
			};
			
			@Override
			void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				results.assertEncounterEdited();
				results.assertObsCreated(2, 100d);
			};
			
		}.run();
	}
	
	@Test
	public void testEditPatientNameAndMultipleObsAndEncounter() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Weight:", "Date:",
				        "Encounter Location:" };
			}
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Mike");
				request.setParameter(widgets.get("PersonName.familyName"), "Den");
				request.setParameter(widgets.get("Weight:"), "100");
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				
			}
			
			@Override
			void testEditedResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				results.assertNoErrors();
				results.assertPatient();
				results.getPatient().getPersonName();
				Assert.assertEquals("Mike", results.getPatient().getGivenName());
				Assert.assertEquals("Den", results.getPatient().getFamilyName());
				Assert.assertEquals("M", results.getPatient().getGender());
				
				results.assertObsCreated(2, 100d);
				
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertEncounterEdited();
			}
			
			@Override
			void testBlankFormHtml(String html) {
				System.out.println(">>>>\n" + html);
			}
			
		}.run();
	}
	
	@Test
	public void submittingFormWithoutPatientTagsShouldNotUpdatePatientDateChanged() throws Exception {
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			public Date patientDateChanged;
			
			@Override
			Patient getPatient() {
				Patient patient = Context.getPatientService().getPatient(2);
				patientDateChanged = patient.getDateChanged();
				return patient;
			}
			
			@Override
			String getFormName() {
				return "singleObsForm";
			}
			
			@Override
			String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:" };
			}
			
			@Override
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Weight:"), "70");
			}
			
			@Override
			boolean doViewPatient() {
				return true;
			}
			
			@Override
			void testViewingPatient(Patient patient, String html) {
				// confirm that the patient date changed has not changed
				Assert.assertTrue("patient date changed has been errorneously changed", patient.getDateChanged() == null
				        && patientDateChanged == null || patient.getDateChanged().equals(patientDateChanged));
			}
			
		}.run();
	}
	
	@Test
	public void editingFormWithoutPatientTagsShouldNotUpdatePatientDateChanged() throws Exception {
		new RegressionTestHelper() {
			
			public Date patientDateChanged;
			
			@Override
			Patient getPatient() {
				Patient patient = Context.getPatientService().getPatient(2);
				patientDateChanged = patient.getDateChanged();
				return patient;
			}
			
			@Override
			String getFormName() {
				return "multipleObsForm";
			}
			
			@Override
			Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "Weight:", "Allergy:", "Allergy Date:" };
			};
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
			};
			
			@Override
			boolean doViewPatient() {
				return true;
			}
			
			@Override
			void testViewingPatient(Patient patient, String html) {
				// confirm that the patient date changed has not changed
				Assert.assertTrue("patient date changed has been errorneously changed", patient.getDateChanged() == null
				        && patientDateChanged == null || patient.getDateChanged().equals(patientDateChanged));
			}
			
		}.run();
	}
	
	@Test
	public void viewFormWithPersonObs() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "singlePersonObsForm";
			}
			
			@Override
			Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.setProvider(Context.getPersonService().getPerson(502));
				TestUtil.addObs(e, 19, "7 - Collet Test Chebaskwony", null);
				return e;
			}
			
			@Override
			void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Collet Test Chebaskwony", html); // make sure Collet Chebaskwony has been included
			}
		}.run();
	}
	
	@Test
	public void testCreatePatientWithObs() throws Exception {
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndObsForm";
			}
			
			@Override
			Patient getPatient() {
				return new Patient();
			}
			
			@Override
			String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Identifier Type:", "Date:", "Encounter Location:",
				        "Provider:", "Weight:", "Allergy:", "Allergy Date:" };
			}
			
			@Override
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "9234923dfasd2");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "2");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
				request.setParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			
			@Override
			void testResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getPersonName().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getPersonName().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("9234923dfasd2", results.getPatient().getPatientIdentifier().getIdentifier());
				
				results.assertEncounterCreated();
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, 75);
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, datePartOnly);
			}
		}.run();
	}
	
	@Test
	public void testEditPatientAndCreatingObs() throws Exception {
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndObsForm";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:" };
			}
			
			String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Date:", "Encounter Location:", "Provider:", "Weight:",
				        "Allergy:", "Allergy Date:" };
			}

			
			@Override
			void setupRequest(MockHttpServletRequest request, java.util.Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "9234923dfasd2");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "2");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
				request.setParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, java.util.Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "9234923dfasd2");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "2");
			}
			
			@Override
			void testResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertEncounterCreated();
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, 75);
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, datePartOnly);
			}
			
			@Override
			void testEditedResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getPersonName().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getPersonName().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("9234923dfasd2", results.getPatient().getPatientIdentifier().getIdentifier());
			}
		}.run();
	}
	
	@Test
	public void testEditingPatientAndEditingObs() throws Exception {
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAndObsForm";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			}
			
			@Override
			Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Date:", "Encounter Location:", "Provider:", "Weight:",
				        "Allergy:", "Allergy Date:" };
			}
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "9234923dfasd2");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "2");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
				request.setParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			
			@Override
			void testEditedResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("9234923dfasd2", results.getPatient().getPatientIdentifier().getIdentifier());
				
				results.assertEncounterEdited();
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, 75);
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, datePartOnly);
			}
		}.run();
	}
	
	/**
	 * Tests that address data is captured, not that this should typically check fields defined in
	 * the default template layout. This test fails if we run it against 1.6 version because of the
	 * logic in AddressSupport.getInstance() in 1.6
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testAddPatientAddress() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "patientAddressForm";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			boolean doViewEncounter() {
				return true;
			};
			
			@Override
			Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabels() {
				return new String[] { "PersonAddress.address1", "PersonAddress.address2", "PersonAddress.cityVillage",
				        "PersonAddress.stateProvince", "PersonAddress.postalCode" };
			}
			
			@Override
			void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("PersonAddress.address1"), "410 w 10th St.");
				request.addParameter(widgets.get("PersonAddress.address2"), "Suite 2000");
				request.addParameter(widgets.get("PersonAddress.cityVillage"), "Indianapolis");
				request.addParameter(widgets.get("PersonAddress.stateProvince"), "Indiana");
				request.addParameter(widgets.get("PersonAddress.postalCode"), "46202");
			}
			
			@Override
			void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				PersonAddress address = results.getPatient().getPersonAddress();
				Assert.assertEquals("410 w 10th St.", address.getAddress1());
				Assert.assertEquals("Suite 2000", address.getAddress2());
				Assert.assertEquals("Indianapolis", address.getCityVillage());
				Assert.assertEquals("Indiana", address.getStateProvince());
				Assert.assertEquals("46202", address.getPostalCode());
				results.assertNoEncounterCreated();
			}
		}.run();
	}
	
	/**
	 * Tests that the age value wins in case both the age and birthdate are provided
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testCreatePatientBirthdateByAge() throws Exception {
		final Integer expectedAge = 40;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -expectedAge);
		final Date expectedBirthDate = cal.getTime();
		new RegressionTestHelper() {
			
			@Override
			String getFormName() {
				return "editPatientBirthdateForm";
			}
			
			@Override
			Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			boolean doViewEncounter() {
				return true;
			};
			
			@Override
			Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			String[] widgetLabelsForEdit() {
				return new String[] { "Age:", "Birthdate:" };
			}
			
			@Override
			void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Age:"), expectedAge.toString());
				request.setParameter(widgets.get("Birthdate:"), dateAsString(new Date()));
			}
			
			@Override
			void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				//the birthdate should have been computed basing on the entered age
				Assert.assertEquals(ymdToDate(dateAsString(expectedBirthDate)), results.getPatient().getBirthdate());
				results.assertEncounterEdited();
			}
		}.run();
	}
}
