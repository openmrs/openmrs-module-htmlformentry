/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;


public class ExitFromCareTagTest extends BaseModuleContextSensitiveTest {

    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";

    ObsService os;
    ConceptService cs;
    @Before
    public void loadData() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
        os = Context.getObsService();
        cs = Context.getConceptService();
	}

    @Test
    public void exitFromCare_shouldExitFromCareWithValidDateAndReasonWithoutPatientDeath() throws Exception{

        final Date date = new Date();
        new RegressionTestHelper(){

            @Override
            public String getFormName() {
                return "exitFromCareForm";
            }

            @Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
			}

            @Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
              request.addParameter(widgets.get("Date:"), dateAsString(date));
			  request.addParameter(widgets.get("Location:"), "2");
			  request.addParameter(widgets.get("Provider:"), "502");
              request.setParameter(widgets.get("Exit From Care:"), "4201");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", dateAsString(date));
              request.setParameter("w11", "");
              request.setParameter("w13", "");
            }

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
                List<Obs> obsList = os.getObservationsByPersonAndConcept(results.getPatient(), cs.getConcept("4200"));
                Assert.assertEquals(obsList.size(),1);
                Assert.assertEquals("RECOVERED",obsList.get(0).getValueCoded().getDisplayString());
                Assert.assertEquals(dateAsString(date),dateAsString(obsList.get(0).getObsDatetime()));
			}

        }.run();
    }

    @Test
    public void exitFromCare_shouldProcessDeathWithValidDateAndReasonAndCauseOfDeath() throws Exception{

        final Date date = new Date();
        new RegressionTestHelper(){

            @Override
            public String getFormName() {
                return "exitFromCareForm";
            }

            @Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
			}

            @Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
              request.addParameter(widgets.get("Date:"), dateAsString(date));
			  request.addParameter(widgets.get("Location:"), "2");
			  request.addParameter(widgets.get("Provider:"), "502");
              request.setParameter(widgets.get("Exit From Care:"), "4202");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", dateAsString(date));
              request.setParameter("w11", "4301");
              request.setParameter("w13", "");
            }

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
                List<Obs> obsList = os.getObservationsByPersonAndConcept(results.getPatient(), cs.getConcept("4200"));
                List<Obs> obsDeathList = os.getObservationsByPersonAndConcept(results.getPatient(), cs.getConcept("4300"));
                Assert.assertEquals(obsList.size(),1);
                Assert.assertEquals(obsDeathList.size(),1);
                Assert.assertEquals("PATIENT DIED",obsList.get(0).getValueCoded().getDisplayString());
                Assert.assertEquals(dateAsString(date),dateAsString(obsList.get(0).getObsDatetime()));
                Assert.assertEquals("STROKE",obsDeathList.get(0).getValueCoded().getDisplayString());
                Assert.assertEquals(null,obsList.get(0).getValueText());

			}

        }.run();
    }

    @Test
       public void exitFromCare_shouldProcessDeathWithValidDateReasonAndNonCodedCauseOfDeath() throws Exception{

           final Date date = new Date();
           new RegressionTestHelper(){

               @Override
               public String getFormName() {
                   return "exitFromCareForm";
               }

               @Override
               public String[] widgetLabels() {
                   return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
               }

               @Override
               public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                 request.addParameter(widgets.get("Date:"), dateAsString(date));
                 request.addParameter(widgets.get("Location:"), "2");
                 request.addParameter(widgets.get("Provider:"), "502");
                 request.setParameter(widgets.get("Exit From Care:"), "4202");
               // the exit date is set by using widget id, since there is no label
                 request.setParameter("w7", dateAsString(date));
                 request.setParameter("w11", "4302");
                 request.setParameter("w13", "Died from cancer");
               }

               @Override
               public void testResults(SubmissionResults results) {
                   results.assertNoErrors();
                   List<Obs> obsList = os.getObservationsByPersonAndConcept(results.getPatient(), cs.getConcept("4200"));
                   List<Obs> obsDeathList = os.getObservationsByPersonAndConcept(results.getPatient(), cs.getConcept("4300"));
                   Assert.assertEquals(obsList.size(),1);
                   Assert.assertEquals(obsDeathList.size(),1);
                   Assert.assertEquals("PATIENT DIED",obsList.get(0).getValueCoded().getDisplayString());
                   Assert.assertEquals(dateAsString(date),dateAsString(obsList.get(0).getObsDatetime()));
                   Assert.assertEquals("OTHER NON-CODED",obsDeathList.get(0).getValueCoded().getDisplayString());
                   Assert.assertEquals("Died from cancer",obsDeathList.get(0).getValueText());

               }

           }.run();
       }

    @Test
       public void exitFromCare_shouldNotSubmitIfReasonIsPatientDiedAndCauseOfDeathNull() throws Exception{

           final Date date = new Date();
           new RegressionTestHelper(){

               @Override
               public String getFormName() {
                   return "exitFromCareForm";
               }

               @Override
               public String[] widgetLabels() {
                   return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
               }

               @Override
               public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                 request.addParameter(widgets.get("Date:"), dateAsString(date));
                 request.addParameter(widgets.get("Location:"), "2");
                 request.addParameter(widgets.get("Provider:"), "502");
                 request.setParameter(widgets.get("Exit From Care:"), "4202");
               // the exit date is set by using widget id, since there is no label
                 request.setParameter("w7", dateAsString(date));
                 request.setParameter("w11", "");
                 request.setParameter("w13", "");
               }

               @Override
               public void testResults(SubmissionResults results) {
                   results.assertErrors(1);
                   List<FormSubmissionError> errors = results.getValidationErrors();
                    for(FormSubmissionError error : errors){
                    Assert.assertEquals("w12", error.getId().trim());
                    Assert.assertEquals("htmlformentry.error.required", error.getError().trim());
                }
               }

           }.run();
       }

     @Test
       public void exitFromCare_shouldNotSubmitIfOtherNonCodedIsCauseAndOtherReasonTextNull() throws Exception{

           final Date date = new Date();
           new RegressionTestHelper(){

               @Override
               public String getFormName() {
                   return "exitFromCareForm";
               }

               @Override
               public String[] widgetLabels() {
                   return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
               }

               @Override
               public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                 request.addParameter(widgets.get("Date:"), dateAsString(date));
                 request.addParameter(widgets.get("Location:"), "2");
                 request.addParameter(widgets.get("Provider:"), "502");
                 request.setParameter(widgets.get("Exit From Care:"), "4202");
               // the exit date is set by using widget id, since there is no label
                 request.setParameter("w7", dateAsString(date));
                 request.setParameter("w11", "4302");
                 request.setParameter("w13", "");
               }

               @Override
               public void testResults(SubmissionResults results) {
                   results.assertErrors(1);
                   List<FormSubmissionError> errors = results.getValidationErrors();
                    for(FormSubmissionError error : errors){
                    Assert.assertEquals("w14", error.getId().trim());
                    Assert.assertEquals("htmlformentry.error.required", error.getError().trim());
                }
               }

           }.run();
       }

    @Test
    public void exitFromCare_shouldNotAllowToEditAndSubmitIfDateIsNull() throws Exception{

        final Date date = new Date();
        new RegressionTestHelper(){

            @Override
            public String getFormName() {
                return "exitFromCareForm";
            }

            @Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
			}

            @Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
              request.addParameter(widgets.get("Date:"), dateAsString(date));
			  request.addParameter(widgets.get("Location:"), "2");
			  request.addParameter(widgets.get("Provider:"), "502");
              request.setParameter(widgets.get("Exit From Care:"), "4201");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", dateAsString(date));
              request.setParameter("w11", "");
              request.setParameter("w13", "");
            }

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
			}

            @Override
            public boolean doEditPatient(){
                return true;
            }

            @Override
            public String[] widgetLabelsForEdit(){
                return new String[] { "Exit From Care:"};
            }

            @Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.setParameter(widgets.get("Exit From Care:"), "4201");
                request.setParameter("w13", "");
                // the exit date is set by using widget id, since there is no label
                request.setParameter("w7", "");
            }

            @Override
			public void testEditedResults(SubmissionResults results){
                results.assertErrors(1);
                List<FormSubmissionError> errors = results.getValidationErrors();
                for(FormSubmissionError error : errors){
                    Assert.assertEquals("w8", error.getId().trim());
                    Assert.assertEquals("htmlformentry.error.required", error.getError().trim());
                }
            }

        }.run();
    }

    @Test
    public void exitFromCare_shouldNotAllowToEditAndSubmitIfReasonIsNull() throws Exception{

        final Date date = new Date();
        new RegressionTestHelper(){

            @Override
            public String getFormName() {
                return "exitFromCareForm";
            }

            @Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
			}

            @Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
              request.addParameter(widgets.get("Date:"), dateAsString(date));
			  request.addParameter(widgets.get("Location:"), "2");
			  request.addParameter(widgets.get("Provider:"), "502");
              request.setParameter(widgets.get("Exit From Care:"), "4201");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", dateAsString(date));
              request.setParameter("w11", "");
              request.setParameter("w13", "");
            }

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
			}

            @Override
            public boolean doEditPatient(){
                return true;
            }

            @Override
            public String[] widgetLabelsForEdit(){
                return new String[] { "Exit From Care:"};
            }

            @Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.setParameter(widgets.get("Exit From Care:"), "");
                request.setParameter("w13", "");
            // a new Date() is used to edit and change the exit date
                request.setParameter("w7", dateAsString(new Date(112,5,1)));
            }

            @Override
			public void testEditedResults(SubmissionResults results){
                results.assertErrors(1);
                List<FormSubmissionError> errors = results.getValidationErrors();
                for(FormSubmissionError error : errors){
                    Assert.assertEquals("w10", error.getId().trim());
                    Assert.assertEquals("htmlformentry.error.required" , error.getError().trim());
                }
            }

        }.run();
    }

    @Test
    public void exitFromCare_shouldAllowToSubmitIfBothDateAndReasonNotFilledInitially() throws Exception{

        final Date date = new Date();
        new RegressionTestHelper(){

            @Override
            public String getFormName() {
                return "exitFromCareForm";
            }

            @Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Exit From Care:"};
			}

            @Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
              request.addParameter(widgets.get("Date:"), dateAsString(date));
			  request.addParameter(widgets.get("Location:"), "2");
			  request.addParameter(widgets.get("Provider:"), "502");
              request.setParameter(widgets.get("Exit From Care:"), "");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", "");
              request.setParameter("w11", "");
              request.setParameter("w13", "");
            }

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
                List<Obs> obsList = os.getObservationsByPersonAndConcept(results.getPatient(), cs.getConcept("4200"));
                Assert.assertEquals(obsList.size(),0);
			}

        }.run();
    }

}

