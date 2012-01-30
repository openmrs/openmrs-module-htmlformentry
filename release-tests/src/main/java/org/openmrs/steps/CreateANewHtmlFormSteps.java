package org.openmrs.steps;

import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.openmrs.Steps;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.openmrs.Finders.selectbox;
import static org.openmrs.Finders.textarea;
import static org.openqa.selenium.lift.Finders.div;
import static org.openqa.selenium.lift.Finders.textbox;
import static org.openqa.selenium.lift.Matchers.attribute;
import static org.openqa.selenium.lift.Matchers.text;

/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p/>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p/>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
public class CreateANewHtmlFormSteps extends Steps {

    public CreateANewHtmlFormSteps(WebDriver driver) {
          super(driver);
    }

    @When("I enter $name, $description, $version, $encounterType")
    public void enterHtmlFormDetails(String name, String description, String version, String encounterType){
        type(name, into(textbox().with(attribute("name", equalTo("form.name")))));
        type(description, into(textarea().with(attribute("name", equalTo("form.description")))));
        type(version, into(textbox().with(attribute("name", equalTo("form.version")))));
        type(encounterType, into(selectbox().with(attribute("name", equalTo("form.encounterType")))));
    }
    
    @Then("I should see $successMessage")
    public void htmlformShouldBeSaved(String successMessage){
        assertPresenceOf(div().with(text(containsString(successMessage))));
    }
}
