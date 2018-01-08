package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PatientTagTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void loadData() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}
	
	@Test
	public void testCreateMinimalPatient() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simpleCreatePatientForm";
			}
			
			@Override
			public Patient getPatient() {
				return new Patient();
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:" };
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(">>>>\n" + html);
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
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
			public String getFormName() {
				return "simplePatientAndEncounterForm";
			}
			
			@Override
			public Patient getPatient() {
				return new Patient();
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Date:", "Encounter Location:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getPersonName().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getPersonName().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("987654-1", results.getPatient().getPatientIdentifier().getIdentifier());
				
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
			public String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public boolean doViewEncounter() {
				return true;
			};
			
			@Override
			public Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Simon");
				request.setParameter(widgets.get("PersonName.familyName"), "paul");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
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
			public String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Date:", "Encounter Location:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Mark");
				request.setParameter(widgets.get("PersonName.familyName"), "waugh");
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
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
			public String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			}
			
			@Override
			public Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Weight:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Mike");
				request.setParameter(widgets.get("PersonName.familyName"), "Den");
				request.setParameter(widgets.get("Weight:"), "100");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
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
			public void testBlankFormHtml(String html) {
				System.out.println(">>>>\n" + html);
			}
			
		}.run();
	}
	
	@Test
	public void testEditObsWithoutEditingPatient() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			public Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Weight:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Weight:"), "100");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
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
			public String getFormName() {
				return "patientAndEncounterFormWithMultipleObs";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Weight:", "Date:",
				        "Encounter Location:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Mike");
				request.setParameter(widgets.get("PersonName.familyName"), "Den");
				request.setParameter(widgets.get("Weight:"), "100");
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
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
			public void testBlankFormHtml(String html) {
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
			public Patient getPatient() {
				Patient patient = Context.getPatientService().getPatient(2);
				patientDateChanged = patient.getDateChanged();
				return patient;
			}
			
			@Override
			public String getFormName() {
				return "singleObsForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Weight:"), "70");
			}
			
			@Override
			public boolean doViewPatient() {
				return true;
			}
			
			@Override
			public void testViewingPatient(Patient patient, String html) {
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
			public Patient getPatient() {
				Patient patient = Context.getPatientService().getPatient(2);
				patientDateChanged = patient.getDateChanged();
				return patient;
			}
			
			@Override
			public String getFormName() {
				return "multipleObsForm";
			}
			
			@Override
			public Patient getPatientToView() throws Exception {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Weight:", "Allergy:", "Allergy Date:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
			};
			
			@Override
			public boolean doViewPatient() {
				return true;
			}
			
			@Override
			public void testViewingPatient(Patient patient, String html) {
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
			public String getFormName() {
				return "singlePersonObsForm";
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
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
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Collet Test Chebaskwony", html); // make sure Collet Chebaskwony has been included
			}
		}.run();
	}
	
	@Test
	public void testCreatePatientWithObs() throws Exception {
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "patientAndObsForm";
			}
			
			@Override
			public Patient getPatient() {
				return new Patient();
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Identifier Type:", "Date:", "Encounter Location:",
				        "Provider:", "Weight:", "Allergy:", "Allergy Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
				request.setParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getPersonName().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getPersonName().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("987654-1", results.getPatient().getPatientIdentifier().getIdentifier());
				
				results.assertEncounterCreated();
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, new Double(75));
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, datePartOnly);
			}
		}.run();
	}

    @Test
    public void testCreatePatientAndEnrollInProgram() throws Exception {
        final Date date = new Date();

        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "patientAndEnrollInProgramForm";
            }

            @Override
            public Patient getPatient() {
                return new Patient();
            }

            @Override
            public String[] widgetLabels() {
                return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
                        "Identifier:", "Identifier Location:", "Identifier Type:", "Date:" };
            }

            @Override
            public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.setParameter(widgets.get("PersonName.givenName"), "Given");
                request.setParameter(widgets.get("PersonName.familyName"), "Family");
                request.setParameter(widgets.get("Gender:"), "F");
                request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
                request.setParameter(widgets.get("Identifier:"), "987654-1");
                request.setParameter(widgets.get("Identifier Location:"), "2");
                // hack because identifier type is a hidden input with no label
                request.setParameter("w17", "1");

                request.setParameter(widgets.get("Date:"), dateAsString(date));
            }

            @Override
            public void testResults(SubmissionResults results) {
                Date datePartOnly = ymdToDate(dateAsString(date));

                results.assertNoErrors();

                results.assertPatient();
                Assert.assertEquals("Given", results.getPatient().getPersonName().getGivenName());
                Assert.assertEquals("Family", results.getPatient().getPersonName().getFamilyName());
                Assert.assertEquals("F", results.getPatient().getGender());
                Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
                Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
                Assert.assertEquals("987654-1", results.getPatient().getPatientIdentifier().getIdentifier());

                List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(
                		results.getPatient(), null, null, null, null, null, false);
            	Assert.assertTrue(patientPrograms.size() == 1);
            }
        }.run();
    }

	@Test
	public void testEditPatientAndCreatingObs() throws Exception {
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "patientAndObsForm";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:" };
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Date:", "Encounter Location:", "Provider:", "Weight:",
				        "Allergy:", "Allergy Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, java.util.Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
				request.setParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, java.util.Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertEncounterCreated();
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, new Double(75));
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, datePartOnly);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getPersonName().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getPersonName().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("987654-1", results.getPatient().getPatientIdentifier().getIdentifier());
			}
		}.run();
	}
	
	@Test
	public void testEditingPatientAndEditingObs() throws Exception {
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "patientAndObsForm";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Date:", "Encounter Location:", "Provider:", "Weight:",
				        "Allergy:", "Allergy Date:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
				
				request.setParameter(widgets.get("Date:"), dateAsString(date));
				request.setParameter(widgets.get("Encounter Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				
				request.setParameter(widgets.get("Weight:"), "75");
				request.setParameter(widgets.get("Allergy:"), "Bee stings");
				request.setParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				Date datePartOnly = ymdToDate(dateAsString(date));
				
				results.assertNoErrors();
				
				results.assertPatient();
				Assert.assertEquals("Given", results.getPatient().getGivenName());
				Assert.assertEquals("Family", results.getPatient().getFamilyName());
				Assert.assertEquals("F", results.getPatient().getGender());
				Assert.assertEquals(datePartOnly, results.getPatient().getBirthdate());
				Assert.assertEquals(false, results.getPatient().getBirthdateEstimated());
				Assert.assertEquals("987654-1", results.getPatient().getPatientIdentifier().getIdentifier());
				
				results.assertEncounterEdited();
				Assert.assertEquals(datePartOnly, results.getEncounterCreated().getEncounterDatetime());
				results.assertProvider(502);
				results.assertLocation(2);
				
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2,new Double(75));
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, datePartOnly);
			}
		}.run();
	}
	
	@Test
	public void testCreatePatientWithAddress() throws Exception {
		
		// only run this test on OpenMRS 1.9 and above (since we have not configured it to work with old Address model)
		int majorVersion = Integer.parseInt(OpenmrsConstants.OPENMRS_VERSION_SHORT.split("\\.")[0]);
		int minorVersion = Integer.parseInt(OpenmrsConstants.OPENMRS_VERSION_SHORT.split("\\.")[1]);
		if (majorVersion == 1 && minorVersion < 9) {
			return;
		}
		
		setupAddressTemplate();	
		
		final Date date = new Date();
	
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "patientAddressForm";
			}
			
			@Override
			public Patient getPatient() {
				return new Patient();
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Location.address1", "Location.cityVillage",
				        "Location.stateProvince", "Location.postalCode" };
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(">>>>\n" + html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
				request.addParameter(widgets.get("Location.address1"), "410 w 10th St.");
				request.addParameter(widgets.get("Location.cityVillage"), "Indianapolis");
				request.addParameter(widgets.get("Location.stateProvince"), "Indiana");
				request.addParameter(widgets.get("Location.postalCode"), "46202");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				PersonAddress address = results.getPatient().getPersonAddress();
				Assert.assertEquals("410 w 10th St.", address.getAddress1());
				Assert.assertEquals("Indianapolis", address.getCityVillage());
				Assert.assertEquals("Indiana", address.getStateProvince());
				Assert.assertEquals("46202", address.getPostalCode());
				results.assertNoEncounterCreated();
			}
		}.run();
	}
	
	@Test
	public void testEditPatientWithAddress() throws Exception {
		
		// only run this test on OpenMRS 1.9 and above (since we have not configured it to work with old Address model)
		int majorVersion = Integer.parseInt(OpenmrsConstants.OPENMRS_VERSION_SHORT.split("\\.")[0]);
		int minorVersion = Integer.parseInt(OpenmrsConstants.OPENMRS_VERSION_SHORT.split("\\.")[1]);
		if (majorVersion == 1 && minorVersion < 9) {
			return;
		}
				
		final Date date = new Date();
		
		setupAddressTemplate();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "patientAddressForm";
			}
			
			@Override
			public Patient getPatient() {
				return new Patient();
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "PersonName.givenName", "PersonName.familyName", "Gender:", "Birthdate:",
				        "Identifier:", "Identifier Location:", "Location.address1", "Location.cityVillage",
				        "Location.stateProvince", "Location.postalCode" };
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] {"Location.address1", "Location.cityVillage",
				        "Location.stateProvince", "Location.postalCode" };
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(">>>>\n" + html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("PersonName.givenName"), "Given");
				request.setParameter(widgets.get("PersonName.familyName"), "Family");
				request.setParameter(widgets.get("Gender:"), "F");
				request.setParameter(widgets.get("Birthdate:"), dateAsString(date));
				request.setParameter(widgets.get("Identifier:"), "987654-1");
				request.setParameter(widgets.get("Identifier Location:"), "2");
				// hack because identifier type is a hidden input with no label
				request.setParameter("w17", "1");
				request.addParameter(widgets.get("Location.address1"), "410 w 10th St.");
				request.addParameter(widgets.get("Location.cityVillage"), "Indianapolis");
				request.addParameter(widgets.get("Location.stateProvince"), "Indiana");
				request.addParameter(widgets.get("Location.postalCode"), "46202");
				
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				PersonAddress address = results.getPatient().getPersonAddress();
				Assert.assertEquals("410 w 10th St.", address.getAddress1());
				Assert.assertEquals("Indianapolis", address.getCityVillage());
				Assert.assertEquals("Indiana", address.getStateProvince());
				Assert.assertEquals("46202", address.getPostalCode());
				results.assertNoEncounterCreated();
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Location.address1"), "273 Beaver Dam Rd.");
				request.addParameter(widgets.get("Location.cityVillage"), "Scituate");
				request.addParameter(widgets.get("Location.stateProvince"), "MA");
				request.addParameter(widgets.get("Location.postalCode"), "02066");	
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				PersonAddress address = results.getPatient().getPersonAddress();
				Assert.assertEquals("273 Beaver Dam Rd.", address.getAddress1());
				Assert.assertEquals("Scituate", address.getCityVillage());
				Assert.assertEquals("MA", address.getStateProvince());
				Assert.assertEquals("02066", address.getPostalCode());
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
	public void testCreatePatientBirthdateByAge() throws Exception {
		final Integer expectedAge = 42;
		Person person = new Person();
		person.setBirthdateFromAge(expectedAge, new Date());
		final Date expectedBirthDate = person.getBirthdate();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "createPatientBirthdateByAgeForm";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public boolean doViewEncounter() {
				return true;
			};
			
			@Override
			public Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Age:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Age:"), expectedAge.toString());
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				//the birthdate should have been computed basing on the entered age
				Assert.assertEquals(ymdToDate(dateAsString(expectedBirthDate)), ymdToDate(dateAsString(results.getPatient()
				        .getBirthdate())));
				results.assertEncounterEdited();
			}
		}.run();
	}
	
	/**
	 * Tests that the birthdate value wins in case both the age and birthdate are provided
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePatientBirthdateByBirthdateAndAge() throws Exception {
		
		final Integer expectedAge = 40;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -expectedAge);
		final Date expectedBirthDate = cal.getTime();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "editPatientBirthdateForm";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public boolean doViewEncounter() {
				return true;
			};
			
			@Override
			public Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Age:", "Birthdate:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Age:"), expectedAge.toString());
				request.setParameter(widgets.get("Birthdate:"), dateAsString(expectedBirthDate));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				//the birthdate should have been computed basing on the entered birthdate
				Assert.assertEquals(ymdToDate(dateAsString(expectedBirthDate)), results.getPatient().getBirthdate());
				results.assertEncounterEdited();
			}
		}.run();
	}
	
	
	
	/**
	 * Tests applying decimal ages to patients
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePatientBirthdateByBirthdateDecimalAge() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "createPatientBirthdateByAgeForm";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public boolean doViewEncounter() {
				return true;
			};
			
			@Override
			public Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Age:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Age:"), "5.2");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				//the birthdate should have been computed basing on the entered age
				long oneYear = Long.valueOf("31536000000");
				long oneMonth = Long.valueOf("2628000000");
				Assert.assertTrue(((new Date()).getTime() - oneYear*5) > results.getPatient().getBirthdate().getTime()); //birthdate is before 5 years ago
				Assert.assertTrue(((new Date()).getTime() - oneYear*5) < (results.getPatient().getBirthdate().getTime() + (4*oneMonth))); //birthdate + 4 months is greater than 5years ago
				results.assertEncounterEdited();
			}
		}.run();
	}
	
	
	/**
	 * Tests that the birthdate works with any date format string. orininally replicated HTML-339
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreatePatientBirthdateByBirthdateAndAgeAgainstNonStandardDateFormat() throws Exception {
		
		GlobalProperty gp = new GlobalProperty(HtmlFormEntryConstants.GP_DATE_FORMAT, "MM-yyyy-dd");
		Context.getAdministrationService().saveGlobalProperty(gp);
		Context.getUserContext().setLocale(new Locale("en", "US"));
		
		final Integer expectedAge = 40;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -expectedAge);
		final Date expectedBirthDate = cal.getTime();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "editPatientBirthdateForm";
			}
			
			@Override
			public Patient getPatientToEdit() {
				Patient p = Context.getPatientService().getPatient(2);
				return Context.getPatientService().getPatient(2);
			};
			
			@Override
			public boolean doViewEncounter() {
				return true;
			};
			
			@Override
			public Encounter getEncounterToView() {
				return Context.getEncounterService().getEncounter(101);
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] {"Birthdate:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Birthdate:"), dateAsString(expectedBirthDate));
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertPatient();
				//the birthdate should have been computed basing on the entered birthdate
				Assert.assertEquals(ymdToDate(dateAsString(expectedBirthDate)), results.getPatient().getBirthdate());
				results.assertEncounterEdited();
			}
		}.run();
	}
	
	
	
	private void setupAddressTemplate() {
		
		GlobalProperty property = new GlobalProperty ("layout.address.format", "<org.openmrs.layout.web.address.AddressTemplate>" +
																					"<nameMappings class=\"properties\">"+ 
																					"<property name=\"postalCode\" value=\"Location.postalCode\"/>" +
																					"<property name=\"address1\" value=\"Location.address1\"/>" +
																					"<property name=\"stateProvince\" value=\"Location.stateProvince\"/>" +
																					"<property name=\"cityVillage\" value=\"Location.cityVillage\"/>" +
																					"</nameMappings>" + 
																					"<sizeMappings class=\"properties\">" +
																					"<property name=\"postalCode\" value=\"10\"/>" +
																					"<property name=\"address1\" value=\"40\"/>" +
																					"<property name=\"stateProvince\" value=\"10\"/>" +
																					"<property name=\"cityVillage\" value=\"10\"/>" +
																					"</sizeMappings>" +						
        																			"<lineByLineFormat>" +
        																			"<string>address1</string>" +
        																			"<string>cityVillage stateProvince postalCode</string>" +
        																			"</lineByLineFormat>" +
        																			"</org.openmrs.layout.web.address.AddressTemplate>");	
		
		Context.getAdministrationService().saveGlobalProperty(property);
		
	}
}
