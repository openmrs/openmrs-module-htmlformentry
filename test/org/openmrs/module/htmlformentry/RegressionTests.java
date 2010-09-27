package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptSet;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
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
				return new String[] { "Date:", "Location:", "Provider:" };
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
				return new String[] { "Date:", "Location:", "Provider:" };
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
				return new String[] { "Date:", "Location:", "Provider:", "Weight:" };
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
				return new String[] { "Date:", "Location:", "Provider:", "Weight:", "Allergy:", "Allergy Date:" };
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
				return new String[] { "Date:", "Location:", "Provider:", "Weight:", "Allergy:", "Allergy Date:" };
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
				return new String[] { "Date:", "Location:", "Provider:", "Allergy 1:", "Allergy Date 1:", "Allergy 3:",
				        "Allergy Date 3:" };
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
				TestUtil.assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos", html);
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
				TestUtil.addObs(e, 2, 12.3, null); // weight has conceptId 2
				return e;
			}
			
			void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos Weight:12.3", html);
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
				TestUtil.addObs(e, 1, 965.0, null); // this is a CD4 Count
				return e;
			}
			
			void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyEquals("Date:01/02/2003 Location:Xanadu Provider:Hippocrates of Cos Weight:", html);
			}
		}.run();
	}
	
	@Test
	public void viewObsgroupsWithCodedValues() throws Exception {
		new RegressionTestHelper() {
			
			String getFormName() {
				return "obsGroupsWithCodedValuesForm";
			}
			
			Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.setProvider(Context.getPersonService().getPerson(502));
				
				// create three obsgroups with the identical structures but with different answer values for the ALLERGY CODED obs
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date(), 9, date,
				    new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date(), 9, date,
				    new Date());
				TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1003), new Date(), 9, date,
				    new Date());
				
				return e;
			}
			
			void testViewingEncounter(Encounter encounter, String html) {
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
				
				String getFormName() {
					return "obsGroupsWithDifferentGroupingConceptsButSameMemberConceptsForm";
				}
				
				Encounter getEncounterToView() throws Exception {
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
					//if 1004 contains 1003, then you should see 
					
					return e;
				}
				
				void testViewingEncounter(Encounter encounter, String html) {
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
				
				String getFormName() {
					return "obsGroupsWithSameGroupingConceptButDifferentMemberConceptsForm";
				}
				
				Encounter getEncounterToView() throws Exception {
					Encounter e = new Encounter();
					e.setPatient(getPatient());
					Date date = Context.getDateFormat().parse("01/02/2003");
					e.setDateCreated(new Date());
					e.setEncounterDatetime(date);
					e.setLocation(Context.getLocationService().getLocation(2));
					e.setProvider(Context.getPersonService().getPerson(502));
					
					// first create two ALLERGY CONSTRUCT obsgroups, both with ALLERGY CODED obs, but with different answer values
					TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1001), new Date());
					TestUtil.addObsGroup(e, 7, new Date(), 1000, Context.getConceptService().getConcept(1002), new Date());
					
					// now create another ALLERGY CONSTRUCT obsgroup, but with a HYPER-ALLERGY CODED obs, and a different answer value
					TestUtil.addObsGroup(e, 7, new Date(), 1005, Context.getConceptService().getConcept(1003), new Date());
					
					return e;
				}
				
				void testViewingEncounter(Encounter encounter, String html) {
					// assert that in the rendered form view the view for grouping concept_id 1004 doesn't find a group -- it shouldn't
				    // because all obs groups are concept_id 7.
				    TestUtil.assertFuzzyContains("Hyper-Allergy 1: <span class=\"emptyValue\">____</span>", html);
				}
				
			}.run();
		}
		
	}
	

	/**
	 * 
	 * Builds the full DST model, and ensures proper recognition of nested obs groups.
	 * The basic model is:  Encounter --> TUBERCULOSIS DRUG SENSITIVITY TEST CONSTRUCT
	 * TUBERCULOSIS DRUG SENSITIVITY TEST CONSTRUCT owns 'DST Start Date' obs and multiple TUBERCULOSIS DRUG SENSITIVITY TEST RESULT 
	 * TUBERCULOSIS DRUG SENSITIVITY TEST RESULT owns a result, and 'colonies' obs
	 * 
	 * Yea yea, i know a test should test one component, but this is the most complex single encounter obs model
	 * that anyone will ever build with an htmlform in practice...  
	 * 
	 * @throws Exception
	 */
	@Test
    public void viewDSTModelWithNestedObsGroupsAndConceptSelectTag() throws Exception {
	        
            new RegressionTestHelper() {
                

                String getFormName() {
                    return "multiLevelObsGroup1";
                }
                
                    Encounter getEncounterToView() throws Exception {
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
                
                void testViewingEncounter(Encounter encounter, String html) {
//                    Concept dstParent = Context.getConceptService().getConcept(3040);
//                    for (ConceptSet s : dstParent.getConceptSets()){
//                        System.out.println(s.getConcept() + " " + s.getConceptSet());
//                        for (ConceptSet sInner :s.getConcept().getConceptSets()){
//                            System.out.println(sInner.getConcept() + " " + sInner.getConceptSet());
//                            for (ConceptAnswer a :sInner.getConcept().getAnswers()){
//                                System.out.println(a.getConcept() + " has possible answer " + a.getAnswerConcept());
//                               
//                            }
//                        }
//                    }
                   // System.out.println(html);
                    TestUtil.assertFuzzyContains("R <span class=\"value\">S</span>", html);
                    TestUtil.assertFuzzyContains("ISONIAZID <span class=\"value\">Resistant</span>", html);
                    TestUtil.assertFuzzyContains("INH colonies: <span class=\"value\">200.0</span>", html);
                    TestUtil.assertFuzzyContains("DST Result Date <span class=\"value\">01/02/2003</span>", html);
                    TestUtil.assertFuzzyDoesNotContain("400", html);
                    //this tests that the Ethionamide result *wasn't* returned
                    //this is because the xml doens't expect the ethionamide result to include colonies, 
                        //which we included.
                    TestUtil.assertFuzzyDoesNotContain("Intermediate", html);

                }
                
            }.run();
        
    }

//	@Test
//  public void testDSTModelSubmission() throws Exception {
//      final Date date = new Date();
//      new RegressionTestHelper() {
//          
//          String getFormName() {
//              return "simplestForm";
//          }
//          
//          String[] widgetLabels() {
//              return new String[] { "Date:", "Location:", "Provider:" };
//          }
//          
//          void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
//              request.addParameter(widgets.get("Date:"), dateAsString(date));
//              request.addParameter(widgets.get("Location:"), "2");
//              request.addParameter(widgets.get("Provider:"), "502");
//          }
//          
//          void testResults(SubmissionResults results) {
//              results.assertNoErrors();
//              results.assertEncounterCreated();
//          }
//      }.run();
//  }
	
}
