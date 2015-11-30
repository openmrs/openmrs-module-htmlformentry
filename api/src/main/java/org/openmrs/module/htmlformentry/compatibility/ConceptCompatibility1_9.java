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
package org.openmrs.module.htmlformentry.compatibility;

import org.openmrs.ConceptNumeric;
import org.openmrs.annotation.OpenmrsProfile;
import org.springframework.stereotype.Component;

@Component("htmlformentry.ConceptCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "1.9.9 - 1.12.*")
public class ConceptCompatibility1_9 implements ConceptCompatibility {

	@Override
	public Boolean isAllowDecimal(ConceptNumeric cn) {
		return cn.isPrecise();
	}
}