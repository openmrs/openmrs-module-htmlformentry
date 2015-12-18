package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class RelationshipTagTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	@Before
	public void loadData() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}
	
	// test creating a simple A to B relationship
	
	@Test
	public void testRelationshipTag_shouldCreateAtoBRelationship() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormAtoB";
			}
			
			@Override
			public Patient getPatient() {
				// as a sanity check, make sure this relationship hasn't already been created
				
				// now make sure the relationship has been created
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Patient parent = Context.getPatientService().getPatient(2);
				Person child = Context.getPersonService().getPerson(6);
				Assert.assertEquals(0, Context.getPersonService().getRelationships(parent, child, type).size());
				
				return parent;
			};
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "6");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// now make sure the relationship has been created
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Person parent = Context.getPersonService().getPerson(2);
				Person child = Context.getPersonService().getPerson(6);
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
			}
		}.run();
	}
	
	// test creating a simple B to A relationship
	
	@Test
	public void testRelationshipTag_shouldCreateBtoARelationship() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormBtoA";
			}
			
			@Override
			public Patient getPatient() {
				// as a sanity check, make sure this relationship hasn't already been created
				
				// now make sure the relationship has been created
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Person parent = Context.getPersonService().getPerson(6);
				Patient child = Context.getPatientService().getPatient(2);
				Assert.assertEquals(0, Context.getPersonService().getRelationships(parent, child, type).size());
				
				return child;
			};
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "6");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// now make sure the relationship has been created
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Person parent = Context.getPersonService().getPerson(6);
				Person child = Context.getPersonService().getPerson(2);
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
			}
		}.run();
	}
	
	// doesn't allow a relationship from the smae person to the same person
	
	@Test(expected = APIException.class)
	public void testRelationshipTag_shouldNotAllowCircularRelationship() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormBtoA";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "2");
			}
		}.run();
	}
	
	// test that the tag works with a form that also creates some obs
	
	@Test
	public void testRelationshipTag_shouldWorkWhenAlsoCreatingEncounterWithObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormAtoB";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:", "Weight:", "Allergy:",
				        "Allergy Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				request.addParameter(widgets.get("Weight:"), "70");
				request.addParameter(widgets.get("Allergy:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date:"), dateAsString(date));
				
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "6");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				results.assertObsCreatedCount(3);
				results.assertObsCreated(2, 70d);
				results.assertObsCreated(8, "Bee stings");
				results.assertObsCreated(1119, date);
				
				// now make sure the relationship has been created
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Person parent = Context.getPersonService().getPerson(2);
				Person child = Context.getPersonService().getPerson(6);
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
			}
		}.run();
	}
	
	// test that multiple relationship tags on a single form should create multiple relationships
	
	@Test
	public void testRelationshipTag_shouldCreateMultipleRelationships() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormMultiple";
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
				
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "6");
				request.addParameter("w10", "6");
				request.addParameter("w13", "7");
				
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// now make sure all three relationships have been created
				RelationshipType type1 = Context.getPersonService().getRelationshipType(1);
				RelationshipType type2 = Context.getPersonService().getRelationshipType(4);
				
				Person person1 = Context.getPersonService().getPerson(2);
				Person person2 = Context.getPersonService().getPerson(6);
				Person person3 = Context.getPersonService().getPerson(7);
				
				Assert.assertEquals(1, Context.getPersonService().getRelationships(person1, person2, type1).size());
				Assert.assertEquals(1, Context.getPersonService().getRelationships(person2, person1, type2).size());
				Assert.assertEquals(1, Context.getPersonService().getRelationships(person1, person3, type1).size());
				
			}
		}.run();
	}
	
	// test that multiple relationships are not created if only one widget on a multiple relationship form has data
	
	@Test
	public void testRelationshipTag_shouldNotCreateMultipleRelationshipsIfFieldsLeftEmpty() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormMultiple";
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
				
				// hack because person widget is a hidden input with no label	
				request.addParameter("w10", "6");
				
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// now make sure all three relationships have been created
				RelationshipType type1 = Context.getPersonService().getRelationshipType(1);
				RelationshipType type2 = Context.getPersonService().getRelationshipType(4);
				
				Person person1 = Context.getPersonService().getPerson(2);
				Person person2 = Context.getPersonService().getPerson(6);
				Person person3 = Context.getPersonService().getPerson(7);
				
				Assert.assertEquals(0, Context.getPersonService().getRelationships(person1, person2, type1).size());
				Assert.assertEquals(1, Context.getPersonService().getRelationships(person2, person1, type2).size());
				Assert.assertEquals(0, Context.getPersonService().getRelationships(person1, person3, type1).size());
				
			}
		}.run();
	}
	
	// test that if the same A to B relationship already exists, it will not create a duplicate
	
	@Test
	public void testRelationshipTag_shouldNotCreateDuplicateRelationship() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormAtoB";
			}
			
			@Override
			public Patient getPatient() {
				// preemptively create a relationship
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Patient parent = Context.getPatientService().getPatient(2);
				Person child = Context.getPersonService().getPerson(6);
				
				Relationship rel = new Relationship(parent, child, type);
				Context.getPersonService().saveRelationship(rel);
				
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
				return parent;
			};
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "6");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// now make sure the relationship has not been created twice
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Person parent = Context.getPersonService().getPerson(2);
				Person child = Context.getPersonService().getPerson(6);
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
			}
		}.run();
	}
	
	// test that if change existing relationship is set to false, adding a new relationship of the same type won't remove the existing relationship
	
	@Test
	public void testRelationshipTag_shouldNotOverrideExistingRelationshipIfChangeExistingRelationshipIsFalse()
	    throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormAtoB";
			}
			
			@Override
			public Patient getPatient() {
				// preemptively create a relationship
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Patient parent = Context.getPatientService().getPatient(2);
				Person child = Context.getPersonService().getPerson(6);
				
				Relationship rel = new Relationship(parent, child, type);
				Context.getPersonService().saveRelationship(rel);
				
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
				return parent;
			};
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "7");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// now make sure both relationships have been created
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Person parent = Context.getPersonService().getPerson(2);
				Person child1 = Context.getPersonService().getPerson(6);
				Person child2 = Context.getPersonService().getPerson(7);
				
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child1, type).size());
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child2, type).size());
				
			}
		}.run();
	}
	
	// test that if change existing relationship is set to true, adding a new relationship of the same type won't remove the existing relationship
	
	@Test
	public void testRelationshipTag_shouldOverrideExistingRelationshipIfChangeExistingRelationshipIsTrue() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormChangeExisting";
			}
			
			@Override
			public Patient getPatient() {
				// preemptively create a relationship
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Patient parent = Context.getPatientService().getPatient(2);
				Person child = Context.getPersonService().getPerson(6);
				
				Relationship rel = new Relationship(parent, child, type);
				Context.getPersonService().saveRelationship(rel);
				
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
				return parent;
			};
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// hack because person widget is a hidden input with no label	
				request.addParameter("w7", "7");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				// now make sure the old the relationship is gone and the new one has been created
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Person parent = Context.getPersonService().getPerson(2);
				Person child1 = Context.getPersonService().getPerson(6);
				Person child2 = Context.getPersonService().getPerson(7);
				
				Assert.assertEquals(0, Context.getPersonService().getRelationships(parent, child1, type).size());
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child2, type).size());
				
			}
		}.run();
	}
	
	// test that there is a form validation error if a required relationship isn't specified
	
	@Test
	public void testRelationshipTag_ValidationShouldFailIfRequiredRelationshipNotSpecified() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormRequired";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				// don't specify a relationship
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics--assert that there has been a validation error
				results.assertErrors(1);
			}
		}.run();
	}
	
	// test that there is no form validation error if no relationship is specified but the person already has a relationship of this type
	
	@Test
	public void testRelationshipTag_ValidationShouldNotFailIfRequiredRelationshipAlreadyExists() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormRequired";
			}
			
			@Override
			public Patient getPatient() {
				// preemptively create a relationship
				RelationshipType type = Context.getPersonService().getRelationshipType(1);
				Patient parent = Context.getPatientService().getPatient(2);
				Person child = Context.getPersonService().getPerson(6);
				
				Relationship rel = new Relationship(parent, child, type);
				Context.getPersonService().saveRelationship(rel);
				
				Assert.assertEquals(1, Context.getPersonService().getRelationships(parent, child, type).size());
				
				return parent;
			};
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Relationship:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				// don't specify a relationship
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				
				// check the basics
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
			}
		}.run();
	}
	
	// test that the form displays the proper of possible person matches when specified by program
	
	@Test
	public void testRelationshipTag_shouldShowAllPersonsEnrolledInProgramsAsOptions() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormSingleProgram";
			}
			
			@Override
			public Patient getPatient() {
				// note that we are using patient 6 instead of patient 2 here
				return Context.getPatientService().getPatient(6);
			};
			
			@Override
			public void testBlankFormHtml(String html) {
				// John Doe (from standard test data) is enrolled in program 1
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*John Doe", html);
			}
		}.run();
	}
	
	// test that person drop-down excludes the current person
	
	@Test
	public void testRelationshipTag_personDropDownShouldExcludeCurrentPerson() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormSingleProgram";
			}
			
			// use the standard patient #2 here
			
			@Override
			public void testBlankFormHtml(String html) {
				// John Doe (patient 2) is the current patient in this case, and so should be excluded
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*John Doe", html);
			}
		}.run();
	}
	
	// test that if multiple program is are specified, to be in the list the person must be in BOTH programs
	
	@Test
	public void testRelationshipTag_shouldShowAllPersonsEnrolledInProgramsAsOptionsShouldIntersectMultiplePrograms()
	    throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormTwoPrograms";
			}
			
			@Override
			public Patient getPatient() {
				// note that we are using patient 6 instead of patient 2 here
				return Context.getPatientService().getPatient(6);
			};
			
			@Override
			public void testBlankFormHtml(String html) {
				// John Doe (from standard test data) is enrolled in program 1, but not in program 3, so he SHOULDN'T appear
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*John Doe", html);
			}
		}.run();
	}
	
	// test that the form displays the proper of possible person matches when specified by person attribute
	
	@Test
	public void testRelationshipTag_shouldShowAllPersonsWithSingleAttributeAsOptions() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormSingleAttribute";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				// all the patients with a "Race" attribute 
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Johnny Test Doe", html);
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Collet Test Chebaskwony", html);
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Anet Test Oloo", html);
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Bruno Otterbourg", html);
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Hippocrates Of Cos", html);
			}
		}.run();
	}
	
	// test that the form displays the proper of possible person matches when specified by multiple person attributes
	
	@Test
	public void testRelationshipTag_shouldShowAllPersonsWithMultipleAttributesAsOptions() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormMultipleAttributes";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				// all the patients with a "Race" and "Civil Status" attribute 
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Johnny Test Doe", html);
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Collet Test Chebaskwony", html);
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Anet Test Oloo", html);
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Hippocrates Of Cos", html);
				
				// Bruno should now be excluded, since he doesn't have a civil status
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Bruno Otterbourg", html);
			}
		}.run();
	}
	
	// test that the form displays the proper of possible person matches when specified by person attribute with a value
	
	@Test
	public void testRelationshipTag_shouldShowAllPersonsWithSpecificAttributeValue() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormAttributeValue";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				// patient with Civil Status = 5 
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Johnny Test Doe", html);
				
				// All others that were previously included should not be excluded
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Bruno Otterbourg", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Collet Test Chebaskwony", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Anet Test Oloo", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Hippocrates Of Cos", html);
			}
		}.run();
	}
	
	// test that the form displays the proper of possible person matches when specified by person attribute with a value
	
	@Test
	public void testRelationshipTag_shouldShowAllPersonsWithSpecificAttributeValueSpecifiedByPatient() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormAttributeValueFromPatient";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				// patient with Civil Status = 6
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*Anet Test Oloo", html);
				
				// All others that were previously included should not be excluded
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Johnny Test Doe", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Bruno Otterbourg", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Collet Test Chebaskwony", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Hippocrates Of Cos", html);
			}
		}.run();
	}
	
	// test that the form displays the proper of possible person matches when specified by person attribute AND program
	
	@Test
	public void testRelationshipTag_shouldShowAllPersonsWithSpecificAttributeAndProgram() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormAttributeAndProgram";
			}
			
			@Override
			public Patient getPatient() {
				// note that we are using patient 6 instead of patient 2 here
				return Context.getPatientService().getPatient(6);
			};
			
			@Override
			public void testBlankFormHtml(String html) {
				// should contain John Doe (patient 2)
				TestUtil.assertFuzzyContains("select id=\"w7\" name = \"w7\".*John Doe", html);
				
				// patients with attribute, but not with program, should be excluded
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Anet Test Oloo", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Johnny Test Doe", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Bruno Otterbourg", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Collet Test Chebaskwony", html);
				TestUtil.assertFuzzyDoesNotContain("select id=\"w7\" name = \"w7\".*Hippocrates Of Cos", html);
			}
		}.run();
	}
	
	// test that the form displays existing relationships for a person
	@Test
	public void testRelationshipTag_shouldShowExistingRelationships() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "relationshipFormBtoA";
			}
			
			@Override
			public void testBlankFormHtml(String html) {
				// confirm the existing relationship has been displayed
				TestUtil.assertFuzzyContains("Doctor: </td><td> Hippocrates Of Cos", html);
			}
		}.run();
	}
}
