/*
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

package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.w3c.dom.Node;

/**
 * Works like <ifNotMode mode="ENTER" include="true" />
 */
public class IfModeTagHandler extends SimpleConditionalIncludeTagHandler {

	@Override
	public boolean shouldIncludeContent(FormEntrySession session, Node parent, Node node) {
		String mode = HtmlFormEntryUtil.getNodeAttribute(node, "mode", null);
		String include = HtmlFormEntryUtil.getNodeAttribute(node, "include", "true");

		if (mode == null) {
			throw new RuntimeException("ifMode tag requires the mode attribute");
		}

		boolean modeIsCurrent = mode.equalsIgnoreCase(session.getContext().getMode().toString());

		return include.equals("true") ? modeIsCurrent : !modeIsCurrent;
	}
}