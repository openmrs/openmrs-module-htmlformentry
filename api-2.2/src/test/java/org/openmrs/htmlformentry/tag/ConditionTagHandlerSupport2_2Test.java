package org.openmrs.htmlformentry.tag;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.ModuleUtil;
import org.openmrs.module.htmlformentry.handler.ConditionTagHandlerSupport2_2;

public class ConditionTagHandlerSupport2_2Test {

	@Test
	public void shouldBeLoadedOnPlatformVersions2_2AndAbove() {
		OpenmrsProfile profile = ConditionTagHandlerSupport2_2.class.getAnnotation(OpenmrsProfile.class);
		
		Assert.assertFalse(ModuleUtil.matchRequiredVersions("2.1.3", profile.openmrsPlatformVersion()));
		Assert.assertTrue(ModuleUtil.matchRequiredVersions("2.2.0", profile.openmrsPlatformVersion()));
		Assert.assertTrue(ModuleUtil.matchRequiredVersions("2.3.0", profile.openmrsPlatformVersion()));
	}
}
