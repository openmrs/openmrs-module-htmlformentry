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
package org.openmrs.module.htmlformentry.velocity;

import org.apache.velocity.VelocityContext;
import org.openmrs.module.htmlformentry.FormEntrySession;


/**
 * Modules may implement this interface if they want to add additional content to the {@link VelocityContext}
 * used in the lookup, includeIf, and excludeIf tags.
 */
public interface VelocityContextContentProvider {

	/**
     * Called by {@link FormEntrySession} after instantiating and populating a velocity context
     * 
     * @param session
     * @param velocityContext
     */
    void populateContext(FormEntrySession session, VelocityContext velocityContext);
	
}