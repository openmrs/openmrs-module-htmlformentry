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

import junit.framework.Assert;

import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

public class HtmlFormValidatorTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see {@link HtmlFormValidator#validate(Object,Errors)}
	 */
	@Test
	@Verifies(value = "should allow xml containing encounter type tag for a form with no encounter type", method = "validate(Object,Errors)")
	public void validate_shouldAllowXmlContainingEncounterTypeTagForAFormWithNoEncounterType() throws Exception {
		String xml = "<htmlform>Date: <encounterDate/>Location: <encounterLocation/>Provider: <encounterProvider role=\"Provider\"/>Encounter Type: <encounterType /><submit/></htmlform>";
		HtmlForm htmlForm = new HtmlForm();
		htmlForm.setName("testForm");
		Form form = Context.getFormService().getForm(1);
		Assert.assertNull(form.getEncounterType());
		htmlForm.setForm(form);
		htmlForm.setXmlData(xml);
		Errors errors = new BindException(htmlForm, "htmlForm");
		new HtmlFormValidator().validate(htmlForm, errors);
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see {@link HtmlFormValidator#validate(Object,Errors)}
	 */
	@Test
	@Verifies(value = "should reject xml containing encounter type tag for a form with an encounter type", method = "validate(Object,Errors)")
	public void validate_shouldRejectXmlContainingEncounterTypeTagForAFormWithAnEncounterType() throws Exception {
		String xml = "<htmlform>Date: <encounterDate/>Location: <encounterLocation/>Provider: <encounterProvider role=\"Provider\"/>Encounter Type: <encounterType /><submit/></htmlform>";
		HtmlForm htmlForm = new HtmlForm();
		htmlForm.setName("testForm");
		htmlForm.setXmlData(xml);
		
		Form form = Context.getFormService().getForm(1);
		//set the encounter type on the form for testing purposes
		EncounterType encType = Context.getEncounterService().getEncounterType(1);
		Assert.assertNotNull(encType);
		form.setEncounterType(encType);
		htmlForm.setForm(form);
		
		Errors errors = new BindException(htmlForm, "htmlForm");
		new HtmlFormValidator().validate(htmlForm, errors);
		Assert.assertTrue(errors.hasFieldErrors("xmlData"));
	}
}
