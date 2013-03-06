package org.openmrs.stories;

import java.util.List;

import org.openmrs.Steps;
import org.openmrs.Story;
import org.openmrs.steps.AdminSteps;
import org.openmrs.steps.CreateANewHtmlFormSteps;
import org.openmrs.steps.LoginSteps;

import static java.util.Arrays.asList;

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
public class CreateANewHtmlForm extends Story {

    @Override
    public List<Steps> includeSteps() {
        return asList(new LoginSteps(driver), new AdminSteps(driver), new CreateANewHtmlFormSteps(driver));
    }
}
