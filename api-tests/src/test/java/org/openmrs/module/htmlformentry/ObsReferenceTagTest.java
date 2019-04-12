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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                TestUtil.assertFuzzyDoesNotContain("Value of 70.0 kg recorded as part of Emergency on 01/02/2003", html);
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
                TestUtil.assertFuzzyContains("Value of 70.0 kg recorded as part of Emergency on 01/02/2003", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldNotShowReferenceNumericObsFromSameDateOutsideOfEncounterIfShowReferenceMessageFalse() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithNumericValueAndShowReferenceMessageFalse";
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
                TestUtil.assertFuzzyDoesNotContain("Value of 70.0 kg recorded as part of Emergency on 01/02/2003", html);
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
                TestUtil.assertFuzzyContains("Some custom message with value 70.0 kg for encounter Emergency", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldPrepopulateWidgetWithReferenceNumericObsFromSameDateOutsideOfEncounterIfNoEncounterValueAndPrepopulateTrue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithNumericValueAndPrepopulateTrue";
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
                TestUtil.assertFuzzyContains("value=\"70\"", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldNotPrepopulateWidgetWithReferenceNumericObsFromSameDateOutsideOfEncounterIfNoEncounterValueAndPrepopulateFalse() throws Exception {
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
                TestUtil.assertFuzzyDoesNotContain("value=\"70\"", html);
            }
        }.run();
    }


    @Test
    public void editSingleReferenceObsShouldShowReferenceCodedObsFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }


            @Override
            public boolean doEditEncounter() {
                return true;
            }

            @Override
            public void testEditFormHtml(String html) {
                TestUtil.assertFuzzyContains("Value of PENICILLIN recorded as part of Emergency on 01/02/2003", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldPrepopulateWidgetWithReferenceCodedObsFromSameDateOutsideOfEncounterIfNoEncounterValueAndPrepopulateTrue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithCodedValueAndPrepopulateTrue";
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
                TestUtil.assertFuzzyContains("value=\"1001\" selected=\"true\"", html);
            }
        }.run();
    }


    @Test
    public void editSingleReferenceObsShouldShowReferenceTextObsFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }


            @Override
            public boolean doEditEncounter() {
                return true;
            }

            @Override
            public void testEditFormHtml(String html) {
                TestUtil.assertFuzzyContains("Value of Penicillin recorded as part of Emergency on 01/02/2003", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldPrepopulateWidgetWithReferenceTextObsFromSameDateOutsideOfEncounterIfNoEncounterValueAndPrepopulateTrue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithTextValueAndPrepopulateTrue";
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
                TestUtil.assertFuzzyContains("value=\"Penicillin\"", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldShowReferenceDateObsFromSameDateOutsideOfEncounterIfNoEncounterValue() throws Exception {
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
                e.setProvider(Context.getPersonService().getPerson(502));
                return e;
            }


            @Override
            public boolean doEditEncounter() {
                return true;
            }

            @Override
            public void testEditFormHtml(String html) {
                TestUtil.assertFuzzyContains("Value of 2003-02-01 recorded as part of Emergency on 01/02/2003", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldPrepopulateWidgetWithReferenceDateObsFromSameDateOutsideOfEncounterIfNoEncounterValueAndPrepopulateTrue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithDateValueAndPrepopulateTrue";
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
                // TODO: hacky, based on exact format of datepicker, but fine for now... we can remove in the future if this breaks anything
                TestUtil.assertFuzzyContains("'2003-02-01'", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldNotHideDataEntryWidgetIfRestrictDataEntryIfReferenceObsPresentNotSet() throws Exception {
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
                TestUtil.assertFuzzyContains("Weight: <input type=\"text\"", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldHideDataEntryWidgetIfRestrictDataEntryIfReferenceObsPresentIsTrue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithNumericValueAndRestrictDataEntryTrue";
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
                // this should be wrapped in a span so it can be hidden
                TestUtil.assertFuzzyDoesNotContain("Weight: <input type=\"text\"", html);
                TestUtil.assertFuzzyContains("Weight: <span", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldNotAllowOverrideIfOverrideNotSetToTrue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithNumericValueAndRestrictDataEntryTrue";
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
                TestUtil.assertFuzzyDoesNotContain("<button type=\"button\" onclick=\"jQuery\\(\'#\\w+-restrict\'\\).show\\(\\)\">", html);
            }
        }.run();
    }

    @Test
    public void editSingleReferenceObsShouldAllowOverrideIfOverrideSetToTrue() throws Exception {
        new RegressionTestHelper() {

            @Override
            public String getFormName() {
                return "singleObsReferenceFormWithNumericValueAndAllowOverrideTrue";
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
                TestUtil.assertFuzzyContains("<button type=\"button\" onclick=\"jQuery\\(\'#\\w+-restrict\'\\).show\\(\\)\">", html);
            }
        }.run();
    }


}



