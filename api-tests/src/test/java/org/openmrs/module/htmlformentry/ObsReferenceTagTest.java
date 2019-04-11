package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.text.ParseException;
import java.util.Date;

// TODO: can encounter date be set on session somehow?  what if encounter date is changed?
// TODO: enter/edit mode, how do we handle not allowing encounter date editing
// TODO: tests for Single Option and Text Field in edit mode
// TODO: review if we need to support any more widget types: Checkbox, DateTime,
// TODO: Make sure we test that the reference message is *not* displayed in view mode
// TODO add "setInitialValue" to Widget?

public class ObsReferenceTagTest extends BaseModuleContextSensitiveTest {

    private Patient patient;

    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

    protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

    protected static final String XML_OBS_REFERENCE_TEST_DATASET = "obsReferenceTestDataSet";


    @Before
    public void before() throws Exception {
        executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
        executeDataSet("org/openmrs/module/htmlformentry/include/" + XML_OBS_REFERENCE_TEST_DATASET + ".xml");
        patient = Context.getPatientService().getPatient(2);
        LogicUtil.registerDefaultRules();
    }

    @Test
    public void viewSingleReferenceObsShouldShowNumericObsFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                TestUtil.assertFuzzyContains("Weight:70", html);  // TODO why isn't it including decimal point
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
                e.setProvider(Context.getPersonService().getPerson(502));
                TestUtil.addObs(e, 2, 12.3, null); // weight has conceptId 2
                return e;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                TestUtil.assertFuzzyContains("Weight:12.3", html);
            }
        }.run();
    }


    @Test
    public void viewSingleReferenceObsShouldShowValueCodedOFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
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
                e.setProvider(Context.getPersonService().getPerson(502));
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
    public void viewSingleReferenceObsShouldShowValueTextOFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
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
                e.setProvider(Context.getPersonService().getPerson(502));
                TestUtil.addObs(e, 8, "Cats", null);
                return e;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                TestUtil.assertFuzzyContains("Text: Cats", html);
            }
        }.run();
    }

    @Test
    public void viewSingleReferenceObsShouldShowValueDatetimeOFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithDatetimeValue";
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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                TestUtil.assertFuzzyContains("Last Food Assistance: 01/02/2003", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldShowReferenceNumericObsFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }


            @Override
            public boolean doEditEncounter() {
                return true;
            }

            @Override
            public void testEditFormHtml(String html) {
                TestUtil.assertFuzzyContains("Value of 70.0 recorded as part of Emergency", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldShowReferenceNumericObsWithCustomMessageFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }


            @Override
            public boolean doEditEncounter() {
                return true;
            }

            @Override
            public void testEditFormHtml(String html) {
                TestUtil.assertFuzzyContains("Some custom message with value 70.0 for encounter Emergency", html);
            }
        }.run();
    }


   /* @Test
    public void editSingleReferenceObsShouldNotShowReferenceNumericObsFromSameDateOutsideOfEncounterIfExistingEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
                TestUtil.addObs(e, 2, 12.3, null); // weight has conceptId 2
                return e;
            }


            @Override
            public boolean doEditEncounter() {
                return true;
            }

            @Override
            public void testEditFormHtml(String html) {
                TestUtil.assertFuzzyDoesNotContain("Previously entered value of 70.0", html);
            }
        }.run();
    }*/

}



