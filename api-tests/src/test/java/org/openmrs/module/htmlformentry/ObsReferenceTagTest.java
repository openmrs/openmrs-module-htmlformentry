package org.openmrs.module.htmlformentry;

import java.text.ParseException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;

public class ObsReferenceTagTest extends BaseHtmlFormEntryTest {
	
	private Patient patient;
	
	@Before
	public void before() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/obsReferenceTestDataSet.xml");
		patient = Context.getPatientService().getPatient(2);
	}
	
	@Test
	public void viewSingleReferenceObsShouldShowNumericObsFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithNumericValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				// note that there are two weight obs in our test data, we want to make sure it pulls the one with the latest time, which has a value of 75kg
				TestUtil.assertFuzzyContains("Weight:75", html); // TODO why isn't it including decimal point
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsReferenceMessageShouldNotBeDisplayedInViewMode() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithNumericValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				// note that there are two weight obs in our test data, we want to make sure it pulls the one with the latest time, which has a value of 75kg
				TestUtil.assertFuzzyDoesNotContain("Value of 75.0 kg recorded as part of Emergency on 01/02/2003", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldPreferNumericObsFromEncounter() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithNumericValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return Context.getPatientService().getPatient(2);
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				TestUtil.addObs(e, 5089, 12.3, null); // weight has conceptId 2
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Weight:12.3", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldShowValueCodedOFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithCodedValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Coded: PENICILLIN", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldPreferCodedObsFromEncounter() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithCodedValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				TestUtil.addObs(e, 1000, Context.getConceptService().getConcept(1002), null);
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Coded: CATS", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldShowValueTextOFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithTextValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Text: Penicillin", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldPreferTextObsFromEncounter() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithTextValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				TestUtil.addObs(e, 80000, "Cats", null);
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Text: Cats", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldShowValueDatetimeFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithDateValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Last Food Assistance: 01/02/2003", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldSetBooleanCheckboxTrueFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithCheckboxSetTrueInTestData";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Boolean:(.*)[X]", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldShowCodedCheckboxFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithCodedCheckboxValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Coded Checkbox:(.*)[X]", html);
			}
		}.run();
	}
	
	@Test
	public void viewSingleReferenceObsShouldNotShowCodedCheckboxIfNoAnswerMatch() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithUnmatchedCodedCheckboxValue";
			}
			
			@Override
			public Patient getPatientToView() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyDoesNotContain("Coded Checkbox:(.*)[X]", html);
			}
		}.run();
	}
	
	@Test
	public void editSingleReferenceObsShouldShowReferenceNumericObsFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithNumericValue";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				try {
					Date date = Context.getDateFormat().parse("01/02/2003");
					e.setDateCreated(new Date());
					e.setEncounterDatetime(date);
				}
				catch (ParseException ex) {
					throw new RuntimeException();
				}
				
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				// note that there are two weight obs in our test data, we want to make sure it pulls the one with the latest datetime, which has a value of 75kg
				TestUtil.assertFuzzyContains("-reference-view\" title=\"(.*)\"><span class=\"value\">75</span>", html);
				TestUtil.assertFuzzyContains("-reference-edit\" style=\"display:none\"><input type=\"text\" size=\"5\" i",
				    html);
			}
		}.run();
	}
	
	@Test
	public void editSingleReferenceObsShouldShowReferenceNumericObsWithCustomMessageFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithNumericValueAndCustomMessage";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				try {
					Date date = Context.getDateFormat().parse("01/02/2003");
					e.setDateCreated(new Date());
					e.setEncounterDatetime(date);
				}
				catch (ParseException ex) {
					throw new RuntimeException();
				}
				
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				// note that there are two weight obs in our test data, we want to make sure it pulls the one with the latest time, which has a value of 75kg
				TestUtil.assertFuzzyContains("Some custom message with value 75.0 kg for encounter Emergency", html);
			}
		}.run();
	}
	
	@Test
	public void editSingleReferenceObsShouldShowReferenceCodedObsFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithCodedValue";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				try {
					Date date = Context.getDateFormat().parse("01/02/2003");
					e.setDateCreated(new Date());
					e.setEncounterDatetime(date);
				}
				catch (ParseException ex) {
					throw new RuntimeException();
				}
				
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				TestUtil.assertFuzzyContains("(Emergency on 01/02/2003)", html);
			}
		}.run();
	}
	
	@Test
	public void editSingleReferenceObsShouldShowReferenceTextObsFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithTextValue";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				try {
					Date date = Context.getDateFormat().parse("01/02/2003");
					e.setDateCreated(new Date());
					e.setEncounterDatetime(date);
				}
				catch (ParseException ex) {
					throw new RuntimeException();
				}
				
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				TestUtil.assertFuzzyContains("(Emergency on 01/02/2003)", html);
			}
		}.run();
	}
	
	@Test
	public void editSingleReferenceObsShouldShowReferenceDateObsFromSameDateOutsideOfEncounterIfNoEncounterValue()
	        throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithDateValue";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				try {
					Date date = Context.getDateFormat().parse("01/02/2003");
					e.setDateCreated(new Date());
					e.setEncounterDatetime(date);
				}
				catch (ParseException ex) {
					throw new RuntimeException();
				}
				
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				TestUtil.assertFuzzyContains("(Emergency on 01/02/2003)", html);
			}
		}.run();
	}
	
	@Test
	public void editSingleReferenceObsShouldNotShowReferenceObsIfExistingObsIsPresent() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsReferenceFormWithNumericValue";
			}
			
			@Override
			public Patient getPatientToEdit() {
				return patient;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				try {
					Date date = Context.getDateFormat().parse("01/02/2003");
					e.setDateCreated(new Date());
					e.setEncounterDatetime(date);
					e.setLocation(Context.getLocationService().getLocation(2));
					e.addProvider(Context.getEncounterService().getEncounterRole(1),
					    Context.getProviderService().getProvider(1));
					TestUtil.addObs(e, 5089, 12.3, null); // weight has conceptId 2
				}
				catch (ParseException ex) {
					throw new RuntimeException();
				}
				
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				return e;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				// this should be wrapped in a span so it can be hidden
				TestUtil.assertFuzzyContains("Weight: <input type=\"text\"", html);
				TestUtil.assertFuzzyDoesNotContain("reference-view", html);
				TestUtil.assertFuzzyDoesNotContain("reference-edit", html);
			}
		}.run();
	}
	
}
