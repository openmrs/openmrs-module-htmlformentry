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
package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.action.RepeatControllerAction;
import org.openmrs.module.htmlformentry.widget.HiddenFieldWidget;
import org.w3c.dom.Node;


/**
 *
 */
public class DynamicRepeatTagHandler extends RepeatControllerAction implements TagHandler, FormSubmissionControllerAction {

	HiddenFieldWidget numberOfRepeatsWidget;

	/**
	 * @see org.openmrs.module.htmlformentry.handler.TagHandler#getAttributeDescriptors()
	 */
	@Override
	public List<AttributeDescriptor> getAttributeDescriptors() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.openmrs.module.htmlformentry.handler.TagHandler#doStartTag(org.openmrs.module.htmlformentry.FormEntrySession, java.io.PrintWriter, org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	    throws BadFormDesignException {
//this.repeatingActions.clear();
		numberOfRepeatsWidget = new HiddenFieldWidget();
		numberOfRepeatsWidget.setInitialValue("0");
		session.getContext().registerWidget(numberOfRepeatsWidget);
		
		
		session.getContext().beginDynamicRepeat();
		out.println("<div class=\"dynamic-repeat-container\">");
		out.println(numberOfRepeatsWidget.generateHtml(session.getContext()));
		out.println("<div class=\"dynamic-repeat-template tempe\">");
		
		
		session.getSubmissionController().startRepeat(this);
		return true; // yes, do the children
	}

	/**
	 * @see org.openmrs.module.htmlformentry.handler.TagHandler#doEndTag(org.openmrs.module.htmlformentry.FormEntrySession, java.io.PrintWriter, org.w3c.dom.Node, org.w3c.dom.Node)
	 */
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
		out.println("<input type=\"button\" class=\"dynamicRepeat\" value=\"Add\" onClick=\"duplicateTemplate($j(this).parent());\"/></div> <!-- End of Dynamic Repeat --></div> <!-- End of Dynamic Repeat Container -->");
		//out.println("<div class=\"dynamic-repeat-button\"><input type=\"button\" value=\"Add\" onClick=\"duplicateTempateEtc();\"/></div>");
		session.getContext().endDynamicRepeat();
		session.getSubmissionController().endRepeat();
	}

	/**
	 * @see org.openmrs.module.htmlformentry.action.RepeatControllerAction#getNumberOfIterations(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected int getNumberOfIterations(FormEntryContext context, HttpServletRequest submission) {
    	return Integer.valueOf(numberOfRepeatsWidget.getValue(context, submission).toString());
	}

}