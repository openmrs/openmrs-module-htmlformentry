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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.List;
import java.util.Map;


public class ExitFromCareTagTest extends BaseModuleContextSensitiveTest {

    protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";

	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";


    @Before
    public void loadData() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
	}

    @Test
    public void exitFromCare_shouldExitFromCareWithValidDateAndReason() throws Exception{

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
              request.setParameter(widgets.get("Exit From Care:"), "5");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", dateAsString(date));
            }

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
            // TODO: have to add a way to verify the exit from care obs creation
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
              request.setParameter(widgets.get("Exit From Care:"), "5");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", dateAsString(date));
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
                request.setParameter(widgets.get("Exit From Care:"), "4");
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
              request.setParameter(widgets.get("Exit From Care:"), "5");
            // the exit date is set by using widget id, since there is no label
              request.setParameter("w7", dateAsString(date));
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
}
