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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.ModuleUtil;
import org.openmrs.module.htmlformentry.handler.EncounterProviderAndRoleTagHandler;

/**
 * Contains the logic that is run every time HTML Form Entry module
 * is either started or shutdown
 */
public class HtmlFormEntryActivator extends BaseModuleActivator {

	private static final String HTMLFORMENTRY_19_EXT_ID = "htmlformentry19ext";

	private Log log = LogFactory.getLog(this.getClass());

	@Override
	public void started() {
		Module htmlformentryExt = ModuleFactory.getModuleById(HTMLFORMENTRY_19_EXT_ID);
		if(htmlformentryExt != null){
			log.error("Functionality of htmlformentry19ext module has been moved to htmlformentry module since 3.3.1. Stopping htmlformentry1.9");
			try {
				ModuleFactory.stopModule(ModuleFactory.getModuleById(HTMLFORMENTRY_19_EXT_ID));
			} catch( APIException e ){
				/**
				 * method {@link HTMLFormEntryExtensions19Activator#stopped()} throws API exception
				 * because service HtmlFormEntryService is not loaded before context refresh.
				 * There is no need to do anything about it, because 'started' hasn't been invoked yet
				 * catching this error prevents logging stack trace.
				 */
			}
		}
		log.info("Started HTML Form Entry Module");
	}

	@Override
	public void stopped() {
		log.info("Stopped HTML Form Entry Module");
	}
}