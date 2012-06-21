package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.HtmlFormSection;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class RegressionTest extends BaseModuleContextSensitiveTest {

	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

	@Before
	public void loadData() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}

	@Test
	public void testSimplestFormFailure() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "simplestForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(3); // date, location, and provider are required
				results.assertNoEncounterCreated();
			}
		}.run();
	}

	@Test
	public void testSimplestFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "simplestForm";
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
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
			}
		}.run();
	}

	@Test
	public void testSingleObsFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

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
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				results.assertObsCreated(2, 70d);
			}
		}.run();
	}

	@Test
	public void testMultipleObsFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:", "Allergy:", "Allergy Date:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
				request.addParameter(widgets.get("Allergy:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, 70d);
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, date);
			}
		}.run();
	}

	@Test
	public void testSingleObsGroupFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleObsGroupForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:", "Allergy:", "Allergy Date:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
				request.addParameter(widgets.get("Allergy:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsGroupCreatedCount(1);
				results.assertObsLeafCreatedCount(3); // 2 in the obs group, 1 for weight
				results.assertObsCreated(2, 70);
				results.assertObsGroupCreated(7, 8, "Bee stings", 1119, date); // allergy construct
			}
		}.run();
	}
 
	@Test
	public void testMultipleObsGroupFormSuccess() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy 1:", "Allergy Date 1:", "Allergy 3:",
				"Allergy Date 3:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// for fun let's fill out part of allergy 1 and allergy 3, but leave allergy 2 blank.
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Allergy 1:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date 1:"), dateAsString(date));
				request.addParameter(widgets.get("Allergy 3:"), "Penicillin");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsGroupCreatedCount(2);
				results.assertObsLeafCreatedCount(3);
				results.assertObsGroupCreated(7, 8, "Bee stings", 1119, date);
				results.assertObsGroupCreated(7, 8, "Penicillin");
			}
		}.run();
	}

	

	
	@Test
	public void viewEmptyEncounterSuccess() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "simplestForm";
			}

			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.setProvider(Context.getPersonService().getPerson(502));
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos", html);
			}
		}.run();
	}

	@Test
	public void viewSingleObsEncounterSuccess() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleObsForm";
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
				TestUtil.assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos Weight:12.3", html);
			}
		}.run();
	}

	@Test
	public void viewSingleObsEncounterWithObsOfAnotherConcept() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleObsForm";
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
				TestUtil.addObs(e, 1, 965.0, null); // this is a CD4 Count
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos Weight:", html);
			}
		}.run();
	}

	@Test
	public void viewObsgroupsWithCodedValues() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsGroupsWithCodedValuesForm";
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

				// create three obsgroups with the identical structures but with different answer values for the ALLERGY CODED obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 1119,
						date, new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 1119,
						date, new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 1119,
						date, new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Allergy \\d: CATS", html);
				TestUtil.assertFuzzyContains("Allergy \\d: OPENMRS", html);
				TestUtil.assertFuzzyContains("Allergy \\d: PENICILLIN", html);
				TestUtil.assertFuzzyContains("Allergy Date 1: 01/02/2003", html);
				TestUtil.assertFuzzyContains("Allergy Date 2: 01/02/2003", html);
				TestUtil.assertFuzzyContains("Allergy Date 3: 01/02/2003", html);
			}

		}.run();
	}

	@Test
	public void viewObsGroupsWithDifferentGroupingConceptsButSameMemberConcepts() throws Exception {
		// need to test multiple times because sometimes there can be a "lucky" match
		for (int rep = 1; rep < 30; rep++) {

			new RegressionTestHelper() {

				@Override
				public String getFormName() {
					return "obsGroupsWithDifferentGroupingConceptsButSameMemberConceptsForm";
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

					// first create two ALLERGY CONSTRUCT obsgroups that contain ALLERGY CODED obs with different answer values
					TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date());
					TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date());

					// now add a third obsgroups of type ANOTHER ALLERGY CONSTRUCT that also contains a ALLERGY CODED obs with a different answer value
					TestUtil.addObsGroup(e, 1004, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date());

					return e;
				}

				@Override
				public void testViewingEncounter(Encounter encounter, String html) {
					// assert that in the rendered form view the value for the ALLERGY CODED obs within the OTHER ALLERGY CONSTRUCT 
					// is OPENMRS (i.e., concept 1003)
					TestUtil.assertFuzzyContains("Another Allergy Construct Allergy 1: OPENMRS", html);
				}
			}.run();
		}
	}

	@Test
	public void viewObsGroupsWithSameGroupingConceptButDifferentMemberConcepts() throws Exception {

		// need to test multiple times because sometimes there can be a "lucky" match
		for (int rep = 1; rep < 30; rep++) {
			new RegressionTestHelper() {

				@Override
				public String getFormName() {
					return "obsGroupsWithSameGroupingConceptButDifferentMemberConceptsForm";
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

					// first create two ALLERGY CONSTRUCT obsgroups, both with ALLERGY CODED obs, but with different answer values
					TestUtil.addObsGroup(e, 7, new Date(), 8, Context.getConceptService().getConcept(1001), new Date());
					TestUtil.addObsGroup(e, 7, new Date(), 8, Context.getConceptService().getConcept(1002), new Date());

					return e;
				}

				@Override
				public void testViewingEncounter(Encounter encounter, String html) {
					// assert that in the rendered form view the view for grouping concept_id 1004 doesn't find a group -- it shouldn't
					// because all obs groups are concept_id 7.
					TestUtil.assertFuzzyContains("Hyper-Allergy 1: <span class=\"emptyValue\">____</span>", html);
				}

			}.run();
		}

	}

	
	@Test
	public void viewIdenticalObsGroups() throws Exception {
		// need to test multiple times because sometimes there can be a "lucky" match
		for (int rep = 1; rep < 30; rep++) {
			new RegressionTestHelper() {
	
				@Override
				public String getFormName() {
					return "multipleObsGroupForm";
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
	
					// create two ALLERGY CONSTRUCT obsgroups, with different text values, one of 01/02/2003, and one with the date set to null
					TestUtil.addObsGroup(e, 7, new Date(), 8,"Dogs", new Date(), 1119, date, new Date());
					TestUtil.addObsGroup(e, 7, new Date(), 8, "Cats", new Date(), 1119, null, new Date());
	
					return e;
				}
	
				@Override
				public void testViewingEncounter(Encounter encounter, String html) {
					// we can't control what order these two obs groups appear in, but we should make sure that the proper text answer is always linked to the proper date answer
					// the "Dogs" entry should be linked to the 01/02/2003 date, while the "Cats" entry should be linked to the null date 
					TestUtil.assertFuzzyContains("Cats Allergy Date \\d: ________", html);
					TestUtil.assertFuzzyContains("Dogs Allergy Date \\d: 01/02/2003", html);
				}
	
			}.run();
		}
	}

	
	/**
	 * Builds the full DST model, and ensures proper recognition of nested obs groups. The basic
	 * model is: Encounter --> TUBERCULOSIS DRUG SENSITIVITY TEST CONSTRUCT TUBERCULOSIS DRUG
	 * SENSITIVITY TEST CONSTRUCT owns 'DST Start Date' obs and multiple TUBERCULOSIS DRUG
	 * SENSITIVITY TEST RESULT TUBERCULOSIS DRUG SENSITIVITY TEST RESULT owns a result, and
	 * 'colonies' obs Yea yea, i know a test should test one component, but this is the most complex
	 * single encounter obs model that anyone will ever build with an htmlform in practice...
	 * 
	 * @throws Exception
	 */
	@Test
	public void viewDSTModelWithNestedObsGroupsAndConceptSelectTag() throws Exception {

		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multiLevelObsGroup1";
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

				// first create DST parent group
				Obs dstParent = TestUtil.createObs(e, 3040, null, date);
				e.addObs(dstParent);

				Obs resultParent = TestUtil.createObs(e, 3025, null, date);
				dstParent.addGroupMember(resultParent);
				Obs resultParent2 = TestUtil.createObs(e, 3025, null, date);
				dstParent.addGroupMember(resultParent2);
				Obs resultParent3 = TestUtil.createObs(e, 3025, null, date);
				dstParent.addGroupMember(resultParent3);

				Obs dstStartDate = TestUtil.createObs(e, 3032, date, date);
				dstParent.addGroupMember(dstStartDate);

				//let's make rifampin susceptible -- 2474 is susceptible
				Obs drugResult = TestUtil.createObs(e, 2474, Context.getConceptService().getConcept(767), date);
				resultParent.addGroupMember(drugResult);

				//let's make INH resistant 1441 is resistant
				Obs drugResult2 = TestUtil.createObs(e, 1441, Context.getConceptService().getConcept(656), date);
				resultParent2.addGroupMember(drugResult2);
				//and add colonies for just INH
				Obs colonies1 = TestUtil.createObs(e, 3016, 200, date);
				resultParent2.addGroupMember(colonies1);

				//let's make ETHIO intermediate
				Obs drugResult4 = TestUtil.createObs(e, 3017, Context.getConceptService().getConcept(1414), date);
				resultParent3.addGroupMember(drugResult4);
				//and add colonies for ETHIO
				Obs colonies3 = TestUtil.createObs(e, 3016, 500, date);
				resultParent3.addGroupMember(colonies3);

				//THINGS THAT SHOULD BE IGNORED:
				//THESE TEST THE BEHAVIOR THAT IF AN OBS GROUP CONCEPT IS UNIQUE AT THAT LEVEL IN AN OBS GROUP HIERARCHY,
				//IT WILL BE RETURNED EVEN IF THE MEMBER OBS DONT 'SUPPORT' THE obsgroup SCHEMA
				//let's add some 'right' data at the 'wrong' place in the hierarchy:
				//let's put another colonies obs in the wrong place in the hierarchy, with colonies value 400
				Obs colonies2 = TestUtil.createObs(e, 3016, 400, date);
				dstParent.addGroupMember(colonies2);
				//and here's a drug result added directly to the encounter (bypassing the DST parentConstructObs)
				Obs drugResult3 = TestUtil.createObs(e, 3017, Context.getConceptService().getConcept(767), date);
				resultParent3.addGroupMember(drugResult3);
				e.addObs(resultParent3);

				e = Context.getEncounterService().saveEncounter(e);
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("R <span class=\"value\">S</span>", html);
				TestUtil.assertFuzzyContains("ISONIAZID <span class=\"value\">Resistant</span>", html);
				TestUtil.assertFuzzyContains("INH colonies: <span class=\"value\">200.0</span>", html);
				TestUtil.assertFuzzyContains("DST Result Date <span class=\"value\">01/02/2003</span>", html);
				TestUtil.assertFuzzyContains("Intermediate", html);
				TestUtil.assertFuzzyDoesNotContain("400", html);
			}

		}.run();

	}

	@Test
	public void viewSingleObsEncounterWithObsOfTextDatatype() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleObsForm2";
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
				TestUtil.addObs(e, 6, "blah blah", null);
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("blah blah", html);
			}

		}.run();
	}

	@Test
	public void testVelocityExpressions() throws Exception {
		LogicUtil.registerDefaultRules();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "velocityForm";
			}

			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Last weight: 50.0", html);
				TestUtil.assertFuzzyContains("Gender: M", html);
				TestUtil.assertFuzzyContains("Location: Test Location", html);
			}
		}.run();
	}

	@Test
	public void testEditSingleObsForm() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

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
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				results.assertObsCreated(2, 70d);
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Weight:" };
			};

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Weight:"), "75");
			};

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				// TODO: for some reason starting in 1.8. the dateChanged is not set here
				// see HTML-233: Unit test testEditSingleObsForm(org.openmrs.module.htmlformentry.RegressionTest) fails when tested against (at least) OpenMRS 1.8.2 and above
				//results.assertEncounterEdited();
				results.assertObsCreated(2, 75d);
				results.assertObsVoided(2, 70d);
			};

		}.run();
	}

	/**
	 * This is supposed to be a regression test for HTML-135, but I couldn't get it to successfully
	 * fail. There must be a difference between editing a form in production, versus in this unit
	 * test framework.
	 */
	@Test
	public void testEditMultipleObsForm() throws Exception {
		new RegressionTestHelper() {

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
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterEdited();
				results.assertObsCreated(2, 75d);
				results.assertObsVoided(2, 50d);
				results.assertObsCreated(8, "Bee stings");
				results.assertObsVoided(8, "Penicillin");
			};

		}.run();
	}

	/**
	 * This test verifies that a) a root Section gets created, and b) that nested obsGroups are
	 * working correctly in the schema. You know that 'a' is working if conceptId = 6 shows up in
	 * section 0, even though it is the last obs tag in the form. you can inspect the results for
	 * 'b': the 'ret' variable is a string representation of the schema where sections are enclosed
	 * by parentheses (), and obsGroup members are enclosed by brackets [].
	 */
	@Test
	public void shouldReturnObsGroupSchemaCorrectly() throws Exception {
		Form form = new Form();
		HtmlForm htmlform = new HtmlForm();
		htmlform.setForm(form);
		form.setEncounterType(new EncounterType());
		htmlform.setDateChanged(new Date());
		htmlform.setXmlData(new TestUtil().loadXmlFromFile(XML_DATASET_PATH + "obsGroupSchemaTest.xml"));
		FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlform);
		HtmlFormSchema hfs = session.getContext().getSchema();
		String ret = "";
		int count = 0;
		for (HtmlFormSection fes : hfs.getSections()) {
			ret += "section " + count + " (";
			for (HtmlFormField hff : fes.getFields()) {
				ret = shouldReturnObsGroupSchemaCorrectlyHelper(hff, count, ret);
			}
			ret += ") ";
			count++;
		}
		Assert.assertTrue(ret
				.equals("section 0 ( concept 6 ) section 1 ( concept 3032 ) section 2 ( ObsGroup=1004 [ ObsGroup=7 [ concept 1000 ] concept 1005 ] concept null ) "));
	}

	/**
	 * This iterates through nested obsGroups and is used by shouldReturnObsGroupSchemaCorrectly()
	 * 
	 * @param hff
	 * @param count
	 * @param ret
	 * @return
	 */
	private String shouldReturnObsGroupSchemaCorrectlyHelper(HtmlFormField hff, int count, String ret) {
		if (hff instanceof ObsField) {
			ObsField of = (ObsField) hff;
			ret += " concept " + of.getQuestion() + " ";

		} else if (hff instanceof ObsGroup) {
			ObsGroup og = (ObsGroup) hff;
			ret += " ObsGroup=" + og.getConcept() + " [";
			for (HtmlFormField hffInner : og.getChildren())
				ret = shouldReturnObsGroupSchemaCorrectlyHelper(hffInner, count, ret);
					ret += "]";
		}
		return ret;
	}

	@Test
	public void testDatatypes() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "dataTypesForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:", "Date Obs:", "Time Obs:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
				request.addParameter(widgets.get("Date Obs:"), dateAsString(date));
				request.addParameter(widgets.get("Time Obs:"), "7");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, 70d);
				results.assertObsCreated(1008, date);
				results.assertObsCreated(1009, "07:00:00");
			}
		}.run();
	}

	@Test
	public void testSubmitButtonLabelAndStyle() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "simplestForm";
			}

			@Override
			public void testBlankFormHtml(String html) {
				//simplest form should contain default label and class
				TestUtil.assertFuzzyContains(
						"<input type=\"button\" class=\"submitButton\" value=\"htmlformentry.enterFormButton\"", html);
				return;
			}

		}.run();

		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "submitButtonLabelAndStyleForm";
			}

			@Override
			public void testBlankFormHtml(String html) {
				//submitButtonLabelAndStyleForm has both custom label and style
				TestUtil.assertFuzzyContains(
						"<input type=\"button\" class=\"someOtherCSSClassReference\" value=\"submit label test\"", html);
				return;
			}

		}.run();

		new RegressionTestHelper() {

			java.util.Locale locale = null;

			@Override
			public String getFormName() {
				locale = Context.getLocale(); //save off the locale 
				Context.setLocale(new java.util.Locale("fr")); //set it to fr
				return "submitButtonLabelCodeForm";
			}

			@Override
			public void testBlankFormHtml(String html) {
				//submit_button has translation reference code
				TestUtil.assertFuzzyContains("<input type=\"button\" class=\"submitButton\" value=\"I don't think so\"",
						html);
				Context.setLocale(locale); //switch back locale
				return;
			}

		}.run();

	}

	/**
	 * TODO refactor this to the same format as the other tests
	 */
	@Test
	public void testApplyMacros() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("<htmlform>");
		sb.append("<macros>");
		sb.append("count=1, 2, 3");
		sb.append("</macros>");
		sb.append("You can count like $count");
		sb.append("</htmlform>");

		HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator();

		String result = generator.applyMacros(sb.toString()).trim();
		Assert.assertEquals("<htmlform>You can count like 1, 2, 3</htmlform>", result);
	}

	@Test
	public void viewFormWithLocationObs() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleLocationObsForm";
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
				TestUtil.addObs(e, 19, "2", null); // this is a location
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Xanadu", html); // make sure Xanadu has been included
			}
		}.run();
	}

	@Test
	public void viewFormWithLocationObsNewFormat() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleLocationObsForm";
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
				TestUtil.addObs(e, 19, "2 - Xanadu", null); // this is a location
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Xanadu", html); // make sure Xanadu has been included
			}
		}.run();
	}

	@Test
	public void answerConceptIdsShouldMapToAnswerLabels() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleObsWithAnswersAndLabels";
			}

			@Override
			public void testBlankFormHtml(String html) {

				// test to make sure that the concept ids have been mapped to the correct labels
				// we search for the value attribute (ie. value="1001"), and then make sure the corresponding label
				// (ie. PENCILLIN) appears following it before a new tag starts ( [^<]* means zero or more characters of any type except "<")

				TestUtil.assertFuzzyContains("value=\"1001\"[^<]*PENICILLIN", html);
				TestUtil.assertFuzzyContains("value=\"1002\"[^<]*CATS", html);
				TestUtil.assertFuzzyContains("value=\"1003\"[^<]*OPENMRS", html);
			}

		}.run();
	}

	@Test
	public void testMultipleObsGroupsOneEmptyValueFirstScrambleEntryOrder() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupDifferentAnswerConceptIdForm";
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

				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 8, "foo2",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 8, "foo3",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo1",
						new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo2", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}	

	@Test
	public void testMultipleObsGroupsOneEmptyValueFirstReverseEntryOrder() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupDifferentAnswerConceptIdForm";
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

				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 8, "foo3",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 8, "foo2",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo1",
						new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo2", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}	

	@Test
	public void testMultipleObsGroupsOneEmptyValueFirst() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupDifferentAnswerConceptIdForm";
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

				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo1",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 8, "foo2",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 8, "foo3",
						new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo2", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}	

	@Test
	public void testMultipleObsGroupsOneEmptyValueMiddle() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupDifferentAnswerConceptIdForm";
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

				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 8, "foo1",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo2",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 8, "foo3",
						new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);

				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo2", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}	

	@Test
	public void testMultipleObsGroupsOneEmptyValueLast() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupDifferentAnswerConceptIdForm";
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

				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 8, "foo1",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 8, "foo2",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo3",
						new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);

				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo2", html);

				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}	

	@Test
	public void testMultipleObsGroupsTwoEmptyValues() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupDifferentAnswerConceptIdForm";
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

				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 8, "foo1",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo2",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo3",
						new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);

				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo2", html);

				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertTrue(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}	

	@Test
	public void testMultipleObsGroupsThreeEmptyValues() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "multipleObsGroupDifferentAnswerConceptIdForm";
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

				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo1",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo2",
						new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "foo3",
						new Date());

				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);

				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo2", html);

				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertTrue(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}

	@Test
	public void testObsGroupRepeats() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsGroupRepeatTestForm";
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

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 8, "bar4",
						new Date());
				
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo1</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo2</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo3</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo4</span>", html);
				TestUtil.assertFuzzyContains("bar4", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo5</span>", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}

	@Test
	public void testObsGroupRepeatsWithOneCompleteEntryAndOnePartiallyFilledInEntry() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsGroupRepeatTestForm";
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

				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "bar1",
						new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 8, "bar4",
						new Date());
				
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo1</span>", html);
				TestUtil.assertFuzzyContains("bar1", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo2</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo3</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo4</span>", html);
				TestUtil.assertFuzzyContains("bar4", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo5</span>", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertTrue(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}

	@Test
	public void testObsGroupRepeatsWithEveryOtherPartiallyFilledInEntry() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsGroupRepeatTestForm";
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

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 8, "bar1", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "bar2", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 8, "bar3", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 8, "bar4", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1005), new Date(), 8, "bar5", new Date());
				
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo1</span>", html);
				TestUtil.assertFuzzyContains("bar1", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo2</span>", html);
				TestUtil.assertFuzzyContains("bar2", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo3</span>", html);
				TestUtil.assertFuzzyContains("bar3", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo4</span>", html);
				TestUtil.assertFuzzyContains("bar4", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo5</span>", html);
				TestUtil.assertFuzzyContains("bar5", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertTrue(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}

	@Test
	public void testObsGroupRepeatsWithEveryFieldFilledIn() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsGroupRepeatTestForm";
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

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 8, "bar1", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 8, "bar2", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 8, "bar3", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 8, "bar4", new Date());

				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1005), new Date(), 8, "bar5", new Date());
				
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo1</span>", html);
				TestUtil.assertFuzzyContains("bar1", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo2</span>", html);
				TestUtil.assertFuzzyContains("bar2", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo3</span>", html);
				TestUtil.assertFuzzyContains("bar3", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo4</span>", html);
				TestUtil.assertFuzzyContains("bar4", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo5</span>", html);
				TestUtil.assertFuzzyContains("bar5", html);
			}

			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
			}

		}.run();
	}
	
	// unit tests for HTML-327
	@Test
	public void shouldViewCheckboxObsProperly() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleObsFormWithCheckbox";
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

				Obs obs = new Obs();
				obs.setConcept(Context.getConceptService().getConcept(4));
				obs.setValueNumeric(new Double(1));  // set this obs to a valid Boolean value 
						
				e.addObs(obs);
				Context.getEncounterService().saveEncounter(e);
				
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				// we just want trigger a view to confirm that this doesn't cause a NPE, which was happened before the fix for HTML-327
			}


		}.run();
	}
	
	@Test(expected = RuntimeException.class)
	public void invalidBooleanObsValueShouldThrowException() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "singleObsFormWithCheckbox";
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

				Obs obs = new Obs();
				obs.setConcept(Context.getConceptService().getConcept(4));
				obs.setValueNumeric(null);  // set this obs to an invalid Boolean value
						
				e.addObs(obs);
				Context.getEncounterService().saveEncounter(e);
				
				return e;
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				// we just want trigger a view to confirm that the invalid boolean value causes an exception to be thrown
			}


		}.run();
	}
	
	@Test
	public void testSettingEncounterType() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "encounterWithEncounterTypeSelect";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Encounter Type:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Encounter Type:"), "2");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertEncounterType(2);
			}
		}.run();
	}
}
