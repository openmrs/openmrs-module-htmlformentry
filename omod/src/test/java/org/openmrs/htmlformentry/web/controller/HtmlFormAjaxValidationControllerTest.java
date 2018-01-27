package org.openmrs.htmlformentry.web.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.web.controller.HtmlFormAjaxValidationController;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;

public class HtmlFormAjaxValidationControllerTest extends BaseModuleContextSensitiveTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private HtmlFormAjaxValidationController controller;

    @Before
    public void setUp() throws Exception {

        controller = new HtmlFormAjaxValidationController();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testFormWithoutDuplicateEncounter() throws Exception {
        controller.duplicateForm(new ModelMap(), request, response, new Integer(5), new Integer(5), "2011-04-04", "yyyy-MM-dd");
        Assert.assertEquals(response.getContentAsString(), "false");
    }

    @Test
    public void testFormWithDuplicateEncounter() throws Exception {
        controller.duplicateForm(new ModelMap(), request, response, new Integer(1), new Integer(2), "2008-08-19", "yyyy-MM-dd");
        Assert.assertEquals(response.getContentAsString(), "false");
    }
}
