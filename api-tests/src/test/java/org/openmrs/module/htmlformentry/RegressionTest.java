package org.openmrs.module.htmlformentry;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsGroup;
import org.openmrs.obs.ComplexData;
import org.springframework.mock.web.MockHttpServletRequest;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

public class RegressionTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void loadData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/providerRoles-dataset.xml");
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
	public void testSimplestFormSuccessNoLocationAndNoProvider() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormLocationAndProviderNotRequiredByTagAttribute";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertNoLocation();
				results.assertNoProvider();
			}
		}.run();
	}
	
	@Test
	/**
	 * No location or provider passed to the form, but the required value for the fields is true, so the
	 * encounter should not be created
	 */
	public void testSimplestFormFailLocationAndProviderNotEnteredWhileRequired() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormLocationAndProviderRequiredByTagAttribute";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(2);
				results.assertNoEncounterCreated();
			}
		}.run();
	}
	
	@Test
	public void testSimplestFormSuccessWithNoProviderRequired() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormProviderNotRequiredByTagAttribute";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertLocation(2);
				results.assertNoProvider();
			}
		}.run();
	}
	
	@Test
	/**
	 * No provider passed to the form, with the required flag set to true so encounter should not be
	 * created
	 */
	public void testSimplestFormFailProviderNotEnteredWhileRequired() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormProviderRequiredByTagAttribute";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(1);
				results.assertNoEncounterCreated();
			}
		}.run();
	}
	
	@Test
	public void testSimplestFormSuccessWithNoLocation() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormLocationNotRequiredByTagAttribute";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertNoLocation();
			}
		}.run();
	}
	
	@Test
	/**
	 * No location passed to the form, with the required flag set to true so encounter should not be
	 * created
	 */
	public void testSimplestFormFailWithLocationNotEnteredWhileRequired() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormLocationRequiredByTagAttribute";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Provider:"), "502");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(1);
				results.assertNoEncounterCreated();
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
				results.assertObsCreated(5089, 70d);
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
				results.assertObsCreated(5089, 70d);
				results.assertObsCreated(80000, "Bee stings");
				results.assertObsCreated(1119, date);
			}
		}.run();
	}
	
	@Test
	public void testMultipleObsFormFailure() throws Exception {
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
				request.addParameter(widgets.get("Allergy Date:"), "date");//wrong input
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
				results.assertNoEncounterCreated();
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
				results.assertObsCreated(5089, new Double(70));
				results.assertObsGroupCreated(70000, 80000, "Bee stings", 1119, date); // allergy construct
			}
		}.run();
	}
	
	@Test
	public void testSingleObsGroupFormFailure() throws Exception {
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
				request.addParameter(widgets.get("Allergy Date:"), "date");//wrong input
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
				results.assertNoEncounterCreated();
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
				results.assertObsGroupCreated(70000, 80000, "Bee stings", 1119, date);
				results.assertObsGroupCreated(70000, 80000, "Penicillin");
			}
		}.run();
	}
	
	@Test
	public void testMultipleObsGroupFormFailure() throws Exception {
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
				request.addParameter(widgets.get("Allergy Date 1:"), "date");//wrong input
				request.addParameter(widgets.get("Allergy 3:"), "Penicillin");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors();
				results.assertNoEncounterCreated();
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				TestUtil.addObs(e, 5089, 12.3, null); // weight has conceptId 5089
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
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
	
	/**
	 * Builds the full DST model, and ensures proper recognition of nested obs groups. The basic model
	 * is: Encounter --> TUBERCULOSIS DRUG SENSITIVITY TEST CONSTRUCT TUBERCULOSIS DRUG SENSITIVITY TEST
	 * CONSTRUCT owns 'DST Start Date' obs and multiple TUBERCULOSIS DRUG SENSITIVITY TEST RESULT
	 * TUBERCULOSIS DRUG SENSITIVITY TEST RESULT owns a result, and 'colonies' obs Yea yea, i know a
	 * test should test one component, but this is the most complex single encounter obs model that
	 * anyone will ever build with an htmlform in practice...
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
				e.setEncounterType(Context.getEncounterService().getEncounterType(1));
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
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
				TestUtil.assertFuzzyContains("INH colonies: <span class=\"value\">200</span>", html);
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				TestUtil.addObs(e, 60000, "blah blah", null);
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
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "velocityForm";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Last weight: 50.0", html);
				TestUtil.assertFuzzyContains("Gender: M", html);
				List<Encounter> encounters = Context.getEncounterService().getEncountersByPatient(getPatient());
				Encounter latestEncounter = encounters.get(encounters.size() - 1);
				TestUtil.assertFuzzyContains("Location: " + latestEncounter.getLocation(), html);
			}
		}.run();
	}
	
	@Test
	public void testLatestVelocityExpressionsWithDate() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "velocityFormLatestWithDate";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Latest weight: 50.0", html);
				TestUtil.assertFuzzyContains("Latest weight with Date: 70.0", html);
				TestUtil.assertFuzzyContains("Location with Date: Unknown Location", html);
				TestUtil.assertFuzzyContains("Location for Scheduled: Unknown Location", html);
			}
		}.run();
	}
	
	/**
	 * These tests use a different patient so as no to create conflicts with other tests
	 *
	 * @throws Exception
	 */
	@Test
	public void testLatestVelocityExpressionsForWorkFlowWithDate() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(8);
			}
			
			@Override
			public String getFormName() {
				return "velocityFormLatestWithDate";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Workflow State Concept May End: 10002", html);
				TestUtil.assertFuzzyContains("Workflow State Concept September End: 10003", html);
			}
		}.run();
	}
	
	@Test
	public void testVelocityExpressionWithNoValueShouldReturnEmptyString() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "velocityFormExpressionWithNoValueShouldReturnEmptyString";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains(" Last Something:  units", html);
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
				results.assertObsCreated(5089, 70d);
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
				results.assertObsCreated(5089, 75d);
				results.assertObsVoided(5089, 70d);
			};
			
		}.run();
	}
	
	// see https://issues.openmrs.org/browse/HTML-678
	@Test
	public void testEditShouldNotClearTimeComponentForm() throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		final Date date = cal.getTime();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormWithoutProvider";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			// instead of editing the existing encounter, create an encounter with an encounter datetime with a time component
			@Override
			public Encounter getEncounterToEdit() {
				Encounter encounter = new Encounter();
				encounter.setEncounterDatetime(date);
				encounter.setLocation(Context.getLocationService().getLocation(2));
				encounter.setDateCreated(date);
				encounter.setPatient(Context.getPatientService().getPatient(2));
				return encounter;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterDatetime(date);
			};
			
		}.run();
	}
	
	// to make sure https://issues.openmrs.org/browse/HTML-678 doesn't break date editing
	@Test
	public void testEditShouldEditDate() throws Exception {
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		final Date date = cal.getTime();
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "simplestFormWithoutProvider";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2015-07-08");
				request.addParameter(widgets.get("Location:"), "2");
				
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			// instead of editing the existing encounter, create an encounter with an encounter datetime with a time component
			@Override
			public Encounter getEncounterToEdit() {
				Encounter encounter = new Encounter();
				encounter.setEncounterDatetime(date);
				encounter.setLocation(Context.getLocationService().getLocation(2));
				encounter.setDateCreated(date);
				encounter.setPatient(Context.getPatientService().getPatient(2));
				return encounter;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Location:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2015-08-08");
				request.addParameter(widgets.get("Location:"), "2");
				
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterDatetime(date);
			};
			
		}.run();
	}
	
	/**
	 * /** This is supposed to be a regression test for HTML-135, but I couldn't get it to successfully
	 * fail. There must be a difference between editing a form in production, versus in this unit test
	 * framework.
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
			}
			
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
				results.assertObsCreated(5089, 75d);
				results.assertObsVoided(5089, 50d);
				results.assertObsCreated(80000, "Bee stings");
				results.assertObsVoided(80000, "Penicillin");
			};
			
		}.run();
	}
	
	@Test
	public void shouldReturnObsGroupSchemaCorrectly() throws Exception {
		Form form = new Form();
		HtmlForm htmlform = new HtmlForm();
		htmlform.setForm(form);
		form.setEncounterType(new EncounterType());
		htmlform.setDateChanged(new Date());
		htmlform.setXmlData(
		    new TestUtil().loadXmlFromFile("org/openmrs/module/htmlformentry/include/obsGroupSchemaTest.xml"));
		FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), htmlform, null);
		session.getHtmlToDisplay();
		HtmlFormSchema hfs = session.getContext().getSchema();
		
		// one top-level field
		Assert.assertThat(hfs.getFields().size(), is(1));
		Assert.assertThat(((ObsField) hfs.getFields().get(0)).getQuestion().getId(), is(60000));
		
		// one top-level section with one field
		Assert.assertThat(hfs.getSections().size(), is(1));
		Assert.assertThat(hfs.getSections().get(0).getName(), is("Section One"));
		Assert.assertThat(hfs.getSections().get(0).getFields().size(), is(1));
		Assert.assertThat(((ObsField) hfs.getSections().get(0).getFields().get(0)).getQuestion().getId(), is(3032));
		
		// one nested section
		Assert.assertThat(hfs.getSections().get(0).getSections().size(), is(1));
		Assert.assertThat(hfs.getSections().get(0).getSections().get(0).getName(), is("Section One Inner One"));
		
		// now handle the obs in the nested section
		List<HtmlFormField> fields = hfs.getSections().get(0).getSections().get(0).getFields();
		Assert.assertThat(fields.size(), is(3));
		Assert.assertTrue(fields.get(0) instanceof ObsGroup);
		ObsGroup obsGroupField = (ObsGroup) fields.get(0);
		Assert.assertThat(obsGroupField.getLabel(), is("obsgroup1004"));
		Assert.assertTrue(fields.get(1) instanceof ObsField);
		Assert.assertThat(((ObsField) fields.get(1)).getQuestions().size(), is(3));
		Assert.assertThat(((ObsField) fields.get(1)).getAnswers().size(), is(1));
		Assert.assertTrue(fields.get(2) instanceof ObsField);
		Assert.assertThat(((ObsField) fields.get(2)).getQuestion().getId(), is(1000));
		Assert.assertThat(((ObsField) fields.get(2)).getAnswers().size(), is(2));
		Assert.assertThat(((ObsField) fields.get(2)).getAnswers().get(0).getConcept().getId(), is(2474));
		Assert.assertThat(((ObsField) fields.get(2)).getAnswers().get(1).getConcept().getId(), is(3017));
		
		// now the obs in the obsgroup
		List<HtmlFormField> obsGroupFields = obsGroupField.getChildren();
		Assert.assertThat(obsGroupFields.size(), is(2));
		Assert.assertTrue(obsGroupFields.get(0) instanceof ObsGroup);
		ObsGroup nestedObsGroup = (ObsGroup) obsGroupFields.get(0);
		Assert.assertThat(nestedObsGroup.getLabel(), is("obsgroup7"));
		Assert.assertTrue(obsGroupFields.get(1) instanceof ObsField);
		Assert.assertThat(((ObsField) obsGroupFields.get(1)).getQuestion().getId(), is(1005));
		
		Assert.assertThat(nestedObsGroup.getChildren().size(), is(1));
		Assert.assertTrue(nestedObsGroup.getChildren().get(0) instanceof ObsField);
		Assert.assertThat(((ObsField) nestedObsGroup.getChildren().get(0)).getQuestion().getId(), is(1000));
		
		// test the "flattened" results
		Set<HtmlFormField> allFields = hfs.getAllFields();
		Assert.assertThat(hfs.getAllFields().size(), is(8));
		Assert.assertThat(hfs.getAllSections().size(), is(2));
		
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
				results.assertObsCreated(5089, 70d);
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
				TestUtil.assertFuzzyContains("<input type=\"button\" class=\"submitButton\" value=\"Enter Form\"", html);
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
				    "<input type=\"button\" class=\"submitButton someOtherCSSClassReference\" value=\"submit label test\"",
				    html);
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
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
	public void saveFormWithLocationObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleLocationObsForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Favorite Health Clinic to Eat:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Favorite Health Clinic to Eat:"), "2");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				
				Obs locationObs = results.getEncounterCreated().getObs().iterator().next();
				
				Assert.assertEquals("2", locationObs.getValueText());
				Assert.assertNull(locationObs.getValueCoded());
				Assert.assertEquals("org.openmrs.Location", locationObs.getComment());
			}
		}.run();
	}
	
	@Test
	public void editFormWithLocationObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleLocationObsForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Favorite Health Clinic to Eat:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Favorite Health Clinic to Eat:"), "2");
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Favorite Health Clinic to Eat:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Favorite Health Clinic to Eat:"), "3");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				;
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				
				Obs locationObs = results.getEncounterCreated().getObs().iterator().next();
				
				Assert.assertEquals("3", locationObs.getValueText());
				Assert.assertNull(locationObs.getValueCoded());
				Assert.assertEquals("org.openmrs.Location", locationObs.getComment());
			};
			
		}.run();
	}
	
	@Test
	public void viewFormWithProviderObs() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleProviderObsForm";
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(getLocation(2));
				e.addProvider(getEncounterRole(1), getProvider(1));
				TestUtil.addObs(e, 19, "1003", null); // see providerRoles-dataset.xml
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				System.out.println(html);
				TestUtil.assertFuzzyContains("Horatio", html);
				TestUtil.assertFuzzyContains("Hornblower", html); // make sure provider name is on the form
			}
		}.run();
	}
	
	@Test
	public void saveFormWithProviderObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleProviderObsForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Binome:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Binome:"), "1005");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				
				Obs providerObs = results.getEncounterCreated().getObs().iterator().next();
				
				Assert.assertEquals("1005", providerObs.getValueText());
				Assert.assertNull(providerObs.getValueCoded());
				Assert.assertEquals("org.openmrs.Provider", providerObs.getComment());
			}
		}.run();
	}
	
	@Test
	public void editFormWithProviderObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleProviderObsForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Binome:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Binome:"), "1005");
			}
			
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Binome:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Binome:"), "1003");
			}
			
			@Override
			public void testEditFormHtml(String html) {
				// Should contain empty option, and 4 options (2 roles, 2 non-retired providers per role)
				TestUtil.assertContains("Choose a Provider", html);
				TestUtil.assertContains("<option value=\"1003\">", html);
				TestUtil.assertContains("<option value=\"1004\">", html);
				TestUtil.assertContains("<option value=\"1005\" selected=\"true\">", html);
				TestUtil.assertContains("<option value=\"1006\">", html);
				System.out.println(html);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				;
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(1);
				
				Obs providerObs = results.getEncounterCreated().getObs().iterator().next();
				
				Assert.assertEquals("1003", providerObs.getValueText());
				Assert.assertNull(providerObs.getValueCoded());
				Assert.assertEquals("org.openmrs.Provider", providerObs.getComment());
			};
			
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
				e.setEncounterType(Context.getEncounterService().getEncounterType(1));
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				Obs obs = new Obs();
				obs.setConcept(Context.getConceptService().getConcept(18)); // Concept 18 is a boolean
				obs.setValueBoolean(true); // set this obs to a valid Boolean value
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				Obs obs = new Obs();
				obs.setConcept(Context.getConceptService().getConcept(4));
				obs.setValueNumeric(null); // set this obs to an invalid Boolean value
				
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
	public void testEditEncounterLocationWithAutocomplete() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "locationAutocompleteForm";
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
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Location:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Location:"), "3");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(3);
			};
			
		}.run();
	}
	
	@Test
	public void testEditEncounterProviderWithAutocomplete() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "providerAutocompleteForm";
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
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Provider:" };
			};
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// todo create a new provider in data set and change the provider into it
				request.setParameter(widgets.get("Provider:"), "502");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502); // add a new value
				results.assertLocation(2);
			};
			
		}.run();
	}
	
	@Test
	public void testIfModeTag() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "ifModeForm";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				TestUtil.assertFuzzyContains("Entry mode <input", html);
				TestUtil.assertFuzzyDoesNotContain("View mode", html);
				TestUtil.assertFuzzyDoesNotContain("Edit mode", html);
				
				TestUtil.assertFuzzyContains("Include True Enter", html);
				TestUtil.assertFuzzyDoesNotContain("Include True View", html);
				TestUtil.assertFuzzyDoesNotContain("Include True Edit", html);
				
				TestUtil.assertFuzzyDoesNotContain("Include False Enter", html);
				TestUtil.assertFuzzyContains("Include False View", html);
				TestUtil.assertFuzzyContains("Include False Edit", html);
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyDoesNotContain("Entry mode", html);
				TestUtil.assertContains("View mode", html);
				TestUtil.assertFuzzyDoesNotContain("Edit mode", html);
				
				TestUtil.assertFuzzyDoesNotContain("Include True Enter", html);
				TestUtil.assertFuzzyContains("Include True View", html);
				TestUtil.assertFuzzyDoesNotContain("Include True Edit", html);
				
				TestUtil.assertFuzzyContains("Include False Enter", html);
				TestUtil.assertFuzzyDoesNotContain("Include False View", html);
				TestUtil.assertFuzzyContains("Include False Edit", html);
			}
			
			@Override
			public void testEditFormHtml(String html) {
				TestUtil.assertFuzzyDoesNotContain("Entry mode", html);
				TestUtil.assertFuzzyDoesNotContain("View mode", html);
				TestUtil.assertContains("Edit mode", html);
				
				TestUtil.assertFuzzyDoesNotContain("Include True Enter", html);
				TestUtil.assertFuzzyDoesNotContain("Include True View", html);
				TestUtil.assertFuzzyContains("Include True Edit", html);
				
				TestUtil.assertFuzzyContains("Include False Enter", html);
				TestUtil.assertFuzzyContains("Include False View", html);
				TestUtil.assertFuzzyDoesNotContain("Include False Edit", html);
			}
			
		}.run();
	}
	
	/**
	 * Do not save nested Obs groups if no Obs are saved within them
	 */
	@Test
	public void nestedObsGroup_doNotSaveThoseWithoutObs() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "multiLevelObsGroup2";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "INH colonies:", "Result Date:" };
			}
			
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("INH colonies:"), "");
				request.addParameter(widgets.get("Result Date:"), "");
			}
			
			public void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(0);
				results.assertObsGroupCreatedCount(0);
			}
		}.run();
	}
	
	@Test
	public void testEncounterDateWithTimeComponent() throws Exception {
		
		final Date date = new Date();
		
		new RegressionTestHelper() {
			
			@Override
			public void testBlankFormHtml(String html) {
				System.out.println(html);
			}
			
			@Override
			public String getFormName() {
				return "simplestFormWithTimeComponent";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.MILLISECOND, 0);
				
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				// hack since time components don't have labels, have to specify actual widget names
				request.addParameter("w1hours", String.valueOf(cal.get(Calendar.HOUR_OF_DAY)));
				request.addParameter("w1minutes", String.valueOf(cal.get(Calendar.MINUTE)));
				request.addParameter("w1seconds", String.valueOf(cal.get(Calendar.SECOND)));
				request.setParameter("w1timezone", TimeZone.getDefault().getID()); // making sure that client tz = server tz
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.MILLISECOND, 0);
				
				results.assertEncounterDatetime(cal.getTime());
			}
		}.run();
	}
	
	@Test
	public void shouldViewComplexObsProperly() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithUploader";
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				BufferedImage image = createImage();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("11/08/2012");
				e.setDateCreated(new Date());
				e.setEncounterType(Context.getEncounterService().getEncounterType(1));
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				Obs obs = new Obs();
				obs.setConcept(Context.getConceptService().getConcept(6100));
				obs.setComplexData(new ComplexData("complex_obs_image_test.gif", image));
				obs.setValueComplex("gif image |complex_obs_image_test.gif");
				
				e.addObs(obs);
				Context.getEncounterService().saveEncounter(e);
				return e;
			}
		}.run();
	}
	
	@Test
	public void testRedisplayNumericObsWithSpecificAnswers() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithSpecificAnswers";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "CD4 COUNT:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("CD4 COUNT:"), "400");
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void testEditFormHtml(String html) {
				TestUtil.assertContains("<option value=\"400\" selected=\"true\">400</option>", html);
			}
			
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDate_shouldNotAllowDateInFuture() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDate";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "3000-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoEncounterCreated();
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDate_shouldNotAllowDateAfterEncounterDate() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDate";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new DateTime(2012, 10, 10, 0, 0, 0, 0).toDate()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2014-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoEncounterCreated();
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndAllowFutureDatesTrue_shouldAllowDateInFuture() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndAllowFutureDatesTrue";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "3000-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsCreated(1119, "3000-10-10");
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndAllowFutureTimesTrue_shouldAllowTimeInFutureOfEncounterDate() throws Exception {
		final Date date = DateUtils.parseDate("2010-05-19", "yyyy-MM-dd");
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndAllowFutureTimesTrue";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				super.testBlankFormHtml(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2010-05-19");
				request.setParameter("w9hours", "13");
				request.setParameter("w9minutes", "42");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				Set<Obs> obsSet = results.getEncounterCreated().getObs();
				assertThat(obsSet.size(), equalTo(1));
				Obs obs = obsSet.iterator().next();
				assertThat(obs, notNullValue());
				assertThat(obs.getConcept().getConceptId(), equalTo(1007));
				assertThat(obs.getValueDatetime(), notNullValue());
				String datetime = DateFormatUtils.format(obs.getValueDatetime(), "yyyy-MM-dd-HH-mm");
				assertThat(datetime, equalTo("2010-05-19-13-42"));
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndAllowFutureTimesTrue_shouldNotAllowTimeInFutureOfCurrentDate() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndAllowFutureTimesTrue";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				super.testBlankFormHtml(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), dateAsString(date));
				// note: theoretically this test will fail if run at 23:59:59
				request.setParameter("w9hours", "23");
				request.setParameter("w9minutes", "59");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be in the future", errors.get(0).getError());
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndAllowFutureTimesTrue_shouldNotAllowDateInFuture() throws Exception {
		final Date date = DateUtils.parseDate("2010-05-19", "yyyy-MM-dd");
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndAllowFutureTimesTrue";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				super.testBlankFormHtml(html);
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2010-05-20");
				request.setParameter("w9hours", "13");
				request.setParameter("w9minutes", "42");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDate_shouldNotAllowObsDateAfterEncounterDateOnEdit() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDate";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			// when creating form, use a valid value
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2010-10-10");
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2010-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
				results.assertNoErrors();
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Obs date:" };
			};
			
			// now, while editing, set the obs date after the encounter date
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Obs date:"), "2011-10-10");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			};
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndAllowFutureDatesTrue_shouldAllowObsDateAfterEncounterDateOnEdit()
	        throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndAllowFutureDatesTrue";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			// when creating form, use a valid value
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2010-10-10");
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2010-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
				results.assertNoErrors();
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Obs date:" };
			};
			
			// now, while editing, set the obs date after the encounter date
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Obs date:"), "2011-10-10");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
			};
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndAllowPastDatesFalse_shouldPreventDateInPast() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndAllowPastDatesFalse";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2020-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be before encounter date", errors.get(0).getError());
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndAllowPastDatesFalse_shouldPreventDateInPastOnEdit() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndAllowPastDatesFalse";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			// when creating form, use a valid value
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2010-10-10");
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2010-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
				results.assertNoErrors();
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Obs date:" };
			};
			
			// now, while editing, set the obs date after the encounter date
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Obs date:"), "2009-10-10");
			};
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be before encounter date", errors.get(0).getError());
			};
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDate_shouldNotFailIfEncounterAndObsDateBothEditedWithValidValues() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDate";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			// when creating form, use a valid value
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2010-10-10");
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2010-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
				results.assertNoErrors();
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Obs date:" };
			}
			
			// now, while editing, edit both the encounter date and the obs date, set obs date before encounter date
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Date:"), "2011-10-11");
				request.setParameter(widgets.get("Obs date:"), "2011-10-10");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
			}
			
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDate_shouldFailIfEncounterAndObsDateBothEditedWithInvalidValues() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDate";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			// when creating form, use a valid value
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2010-10-10");
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2010-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertEncounterCreated();
				results.assertNoErrors();
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Date:", "Obs date:" };
			}
			
			// now, while editing, edit both the encounter date and the obs date, set obs date after encounter date
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Date:"), "2011-10-09");
				request.setParameter(widgets.get("Obs date:"), "2011-10-10");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			}
			
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndNoEncounter_shouldFailObsDateSetPastEncounterDateOnEdit() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndNoEncounter";
			}
			
			@Override
			public boolean doCreateEncounter() {
				return false;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter encounter = new Encounter();
				encounter.setEncounterDatetime(new DateTime(2010, 10, 10, 0, 0, 0).toDate());
				encounter.setLocation(Context.getLocationService().getLocation(2));
				encounter.setDateCreated(new Date());
				encounter.setPatient(Context.getPatientService().getPatient(2));
				return encounter;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Obs date:" };
			}
			
			// now, while editing, edit both the encounter date and the obs date, set obs date after encounter date
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Obs date:"), "2011-10-10");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			}
			
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndNoEncounter_shouldNotFailIfObsDateSetBeforeEncounterDateOnEdit()
	        throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndNoEncounter";
			}
			
			@Override
			public boolean doCreateEncounter() {
				return false;
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter encounter = new Encounter();
				encounter.setEncounterDatetime(new DateTime(2010, 10, 10, 0, 0, 0).toDate());
				encounter.setLocation(Context.getLocationService().getLocation(2));
				encounter.setDateCreated(new Date());
				encounter.setPatient(Context.getPatientService().getPatient(2));
				return encounter;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Obs date:" };
			}
			
			// now, while editing, edit both the encounter date and the obs date, set obs date after encounter date
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Obs date:"), "2009-10-10");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
			}
			
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithDateAndEncounterDateAfterObsTag_shouldNotAllowObsDateInFuture() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndEncounterDateAfterObsTag";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2010-10-10");
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "3000-10-10");
			}
			
			@Override
			
			public void testResults(SubmissionResults results) {
				results.assertNoEncounterCreated();
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			}
		}.run();
	}
	
	// this test currently fails because Encounter Date tag is after Obs tag on the form; should be fixed or ticketed
	@Test
	@Ignore
	public void testSingleObsFormWithDateAndEncounterDateAfterObsTag_shouldNotAllowObsDateAfterEncounterDate()
	        throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithDateAndEncounterDateAfterObsTag";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Obs date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), "2010-10-10");
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Obs date:"), "2011-10-10");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoEncounterCreated();
				results.assertErrors(1);
				List<FormSubmissionError> errors = results.getValidationErrors();
				Assert.assertEquals("Cannot be after encounter date", errors.get(0).getError());
			}
		}.run();
	}
	
	// util method used by previous unit test
	private BufferedImage createImage() {
		int width = 10;
		int height = 10;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
		int[] colorArray = new int[3];
		int h = 255;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (i == 0 || j == 0 || i == width - 1 || j == height - 1
				        || (i > width / 3 && i < 2 * width / 3) && (j > height / 3 && j < 2 * height / 3)) {
					colorArray[0] = h;
					colorArray[1] = h;
					colorArray[2] = 0;
				} else {
					colorArray[0] = 0;
					colorArray[1] = 0;
					colorArray[2] = h;
				}
				raster.setPixel(i, j, colorArray);
			}
		}
		
		return image;
	}
	
	private Provider getProvider(Integer providerId) {
		return Context.getProviderService().getProvider(providerId);
	}
	
	private Location getLocation(Integer locationId) {
		return Context.getLocationService().getLocation(locationId);
	}
	
	private EncounterRole getEncounterRole(Integer encounterRoleId) {
		return Context.getEncounterService().getEncounterRole(encounterRoleId);
	}
}
