package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;

import java.util.Date;

public class ObsGroupMatchingTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void loadData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
	}
	
	@Test
	public void viewObsgroupsWithMultipleAnswerConceptIds() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupsWithMultipleAnswerConceptIds";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 1002,
				    Context.getConceptService().getConcept(1119), new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1005), new Date(), 1002,
				    Context.getConceptService().getConcept(2474), new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1008), new Date(), 1002,
				    Context.getConceptService().getConcept(3017), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains(
				    "<span class=\"value\">\\[X]&#160;Effect1</span><span class=\"value\">Option1</span>", html);
				TestUtil.assertContains(
				    "<span class=\"value\">\\[X]&#160;Effect2</span><span class=\"value\">Option2</span>", html);
				TestUtil.assertContains(
				    "<span class=\"value\">\\[X]&#160;Effect3</span><span class=\"value\">Option3</span>", html);
			}
			
		}.run();
	}
	
	@Test
	public void viewObsgroupsWithMultipleAnswerConceptIdsAndMiddleBlank() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupsWithMultipleAnswerConceptIds";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 1002,
				    Context.getConceptService().getConcept(1119), new Date());
				// TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 1002, null, new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1008), new Date(), 1002,
				    Context.getConceptService().getConcept(3017), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains(
				    "<span class=\"value\">\\[X]&#160;Effect1</span><span class=\"value\">Option1</span>", html);
				TestUtil.assertContains(
				    "<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Effect2</span><span class=\"emptyValue\">____________</span>",
				    html);
				TestUtil.assertContains(
				    "<span class=\"value\">\\[X]&#160;Effect3</span><span class=\"value\">Option3</span>", html);
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
					e.addProvider(Context.getEncounterService().getEncounterRole(1),
					    Context.getProviderService().getProvider(1));
					
					// first create two ALLERGY CONSTRUCT obsgroups that contain ALLERGY CODED obs with different answer values
					TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date());
					TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date());
					
					// now add a third obsgroups of type ANOTHER ALLERGY CONSTRUCT that also contains a ALLERGY CODED obs with a different answer value
					TestUtil.addObsGroup(e, 1004, new Date(), 1000, Context.getConceptService().getConcept(1003),
					    new Date());
					
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
					e.addProvider(Context.getEncounterService().getEncounterRole(1),
					    Context.getProviderService().getProvider(1));
					
					// first create two ALLERGY CONSTRUCT obsgroups, both with ALLERGY CODED obs, but with different answer values
					TestUtil.addObsGroup(e, 7, new Date(), 80000, Context.getConceptService().getConcept(1001), new Date());
					TestUtil.addObsGroup(e, 7, new Date(), 80000, Context.getConceptService().getConcept(1002), new Date());
					
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
					e.addProvider(Context.getEncounterService().getEncounterRole(1),
					    Context.getProviderService().getProvider(1));
					
					// create two ALLERGY CONSTRUCT obsgroups, with different text values, one of 01/02/2003, and one with the date set to null
					TestUtil.addObsGroup(e, 70000, new Date(), 80000, "Dogs", new Date(), 1119, date, new Date());
					TestUtil.addObsGroup(e, 70000, new Date(), 80000, "Cats", new Date(), 1119, null, new Date());
					
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 80000,
				    "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 80000,
				    "foo3", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo1", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 80000,
				    "foo3", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 80000,
				    "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo1", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo1", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 80000,
				    "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 80000,
				    "foo3", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 80000,
				    "foo1", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 80000,
				    "foo3", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 80000,
				    "foo1", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 80000,
				    "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo3", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 80000,
				    "foo1", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo3", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo1", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "foo3", new Date());
				
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
	public void testMultipleObsGroupsWithOneAnswerThatDoesNotMatchForm() throws Exception {
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// create three obsgroups with the identical structures but with different answer values for the obs;
				// the answer values for the 1st and 3rd match the form, but the 2nd does not
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 80000,
				    "foo1", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(656), new Date(), 80000,
				    "foo2", new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 80000,
				    "foo3", new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 1</span>", html);
				TestUtil.assertFuzzyContains("foo1", html);
				
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;Label 2</span>", html);
				TestUtil.assertFuzzyContains("foo3", html);
				
				// the third obs group on the form, with answer concept 1003 for concept 1000, should not be matched to obs group with answer concept 656 for concept 1000 we set above
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;Label 3</span>", html);
				TestUtil.assertFuzzyDoesNotContain("foo2", html);
			}
			
			@Override
			public void testFormViewSessionAttribute(FormEntrySession formEntrySession) {
				Assert.assertFalse(formEntrySession.getContext().isGuessingInd());
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 80000,
				    "bar4", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "bar1", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 80000,
				    "bar4", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 80000,
				    "bar1", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "bar2", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 80000,
				    "bar3", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, null, new Date(), 80000, "bar4", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1005), new Date(), 80000,
				    "bar5", new Date());
				
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
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 80000,
				    "bar1", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 80000,
				    "bar2", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 80000,
				    "bar3", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1004), new Date(), 80000,
				    "bar4", new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1005), new Date(), 80000,
				    "bar5", new Date());
				
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
	
	/**
	 * The previous tests tested that if we had 3 obs groups of the same type, but with different
	 * question/answers, they were matched back up properly The following test(s) test that if we have a
	 * single obs group with multiple obs with the same question, matching still works
	 *
	 * @throws Exception
	 */
	@Test
	public void testSingleObsGroupWithMultipleCheckboxAnswersForSameQuestionConceptSingleAnswer() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithMultipleCheckboxAnswersForSameQuestionConcept";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo1</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo2</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo3</span>", html);
			}
			
		}.run();
	}
	
	@Test
	public void testSingleObsGroupWithMultipleCheckboxAnswersForSameQuestionConceptTwoAnswers() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithMultipleCheckboxAnswersForSameQuestionConcept";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 1000,
				    Context.getConceptService().getConcept(1003), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo1</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo2</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo3</span>", html);
			}
			
		}.run();
	}
	
	@Test
	public void testSingleObsGroupWithMultipleCheckboxAnswersForSameQuestionConceptThreeAnswers() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupWithMultipleCheckboxAnswersForSameQuestionConcept";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 1000,
				    Context.getConceptService().getConcept(1003), new Date(), 1000,
				    Context.getConceptService().getConcept(1001), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo1</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo2</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo3</span>", html);
			}
			
		}.run();
	}
	
	@Test
	public void testRepeatingObsGroupWithMultipleCheckboxAnswersForSameQuestionConceptSingleAnswers() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupRepeatingWithMultipleCheckboxAnswersForSameQuestionConcept";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1002, Context.getConceptService().getConcept(2474), new Date(), 1000,
				    Context.getConceptService().getConcept(1003), new Date(), 1000,
				    Context.getConceptService().getConcept(1001), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;bar1</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo1</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo2</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo3</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;bar2</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo4</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo5</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo6</span>", html);
			}
			
		}.run();
	}
	
	@Test
	public void testRepeatingObsGroupWithMultipleCheckboxAnswersForSameQuestionConceptTwoAnswers() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupRepeatingWithMultipleCheckboxAnswersForSameQuestionConcept";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1002, Context.getConceptService().getConcept(2474), new Date(), 1000,
				    Context.getConceptService().getConcept(1003), new Date(), 1000,
				    Context.getConceptService().getConcept(1001), new Date());
				
				TestUtil.addObsGroup(e, 7, new Date(), 1002, Context.getConceptService().getConcept(1119), new Date(), 1000,
				    Context.getConceptService().getConcept(1003), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;bar1</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo1</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo2</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo3</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;bar2</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo4</span>", html);
				TestUtil.assertContains("<span class=\"emptyValue\">\\[&#160;&#160;]&#160;foo5</span>", html);
				TestUtil.assertContains("<span class=\"value\">\\[X]&#160;foo6</span>", html);
			}
			
		}.run();
	}
	
	@Test
	public void viewObsgroupsWithMultipleHiddenAnswerConceptIds() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "obsGroupsWithMultipleHiddenAnswerConceptIds";
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
				
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1008), new Date(), 1002,
				    Context.getConceptService().getConcept(3017), new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1005), new Date(), 1002,
				    Context.getConceptService().getConcept(2474), new Date());
				
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertContains(
				    "<span id=\"1\" class=\"obs-field\"><span class=\"emptyValue\">____________</span></span>", html);
				TestUtil.assertContains("<span id=\"2\" class=\"obs-field\"><span class=\"value\">Option2</span></span>",
				    html);
				TestUtil.assertContains("<span id=\"3\" class=\"obs-field\"><span class=\"value\">Option3</span></span>",
				    html);
			}
			
		}.run();
	}
	
}
