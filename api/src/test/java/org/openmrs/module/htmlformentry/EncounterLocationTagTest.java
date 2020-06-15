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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.element.EncounterDetailSubmissionElement;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class EncounterLocationTagTest extends BaseModuleContextSensitiveTest {
	
	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/encounterLocationTest.xml");
		
	}
	
	@Test
	public void getAllVisitsAndChildLocations_shouldReturnAllVisistsAndTheirChildLocations() {
		Set<Location> traversedLocations = new HashSet<Location>();
		Set<Location> allVisitLocations = new HashSet<Location>();
		LocationTag visitLocationTag = HtmlFormEntryUtil.getLocationTag("Visit Location");
		List<Location> visitLocations = Context.getLocationService().getLocationsByTag(visitLocationTag);
		allVisitLocations.addAll(visitLocations);
		assertThat(
		    EncounterDetailSubmissionElement.getAllVisitsAndChildLocations(allVisitLocations, traversedLocations).size(),
		    is(10));
		
	}
	
}
