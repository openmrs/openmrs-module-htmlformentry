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

import java.util.List;
import java.util.Map;

import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.layout.web.name.NameSupport;
import org.openmrs.layout.web.name.NameTemplate;
import org.openmrs.module.htmlformentry.compatibility.NameSupportCompatibility;
import org.springframework.stereotype.Component;

@Component("htmlformentry.NameSupportCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "1.9.9 - 1.12.*")
public class NameSupportCompatibility1_9 implements NameSupportCompatibility {

	@Override
	public List<List<Map<String, String>>> getLines() {
		return getDefaultLayoutTemplate().getLines();
	}

	@Override
	public String getLayoutToken() {
		return getDefaultLayoutTemplate().getLayoutToken();
	}

	@Override
	public Map<String, String> getNameMappings() {
		return getDefaultLayoutTemplate().getNameMappings();
	}
	
	private NameTemplate getDefaultLayoutTemplate() {
		return NameSupport.getInstance().getDefaultLayoutTemplate();
	}
	
	@Override
	public int getMaxTokens() {
		return getDefaultLayoutTemplate().getMaxTokens();
	}
}