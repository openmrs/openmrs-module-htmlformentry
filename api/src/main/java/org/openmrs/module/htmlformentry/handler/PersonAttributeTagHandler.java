package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.LocationTag;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.element.PersonAttributeSubmissionElement;
import org.w3c.dom.Node;

/**
 * Handles the {@code <personAttribute>} tag, which allows viewing, entering, and editing a person
 * attribute of type String, Concept, or Location.
 *
 * <p>Supported attributes:
 * <ul>
 *   <li>{@code attributeType} (required) – UUID of the {@link PersonAttributeType}</li>
 *   <li>{@code answerConceptIds} (required for Concept-format types) – comma-separated list of
 *       concept IDs or UUIDs shown in the dropdown</li>
 *   <li>{@code tags} (optional, Location-format types only) – comma-separated location tag names
 *       used to filter the location dropdown</li>
 * </ul>
 */
public class PersonAttributeTagHandler extends AbstractTagHandler {

	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> descriptors = new ArrayList<AttributeDescriptor>();
		descriptors.add(new AttributeDescriptor("attributeType", PersonAttributeType.class));
		descriptors.add(new AttributeDescriptor("answerConceptIds", Concept.class));
		descriptors.add(new AttributeDescriptor("tags", LocationTag.class));
		return Collections.unmodifiableList(descriptors);
	}

	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	        throws BadFormDesignException {
		FormEntryContext context = session.getContext();
		PersonAttributeSubmissionElement element = new PersonAttributeSubmissionElement(context, getAttributes(node));
		session.getSubmissionController().addAction(element);
		out.print(element.generateHtml(context));
		return false; // no body to process
	}

	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node)
	        throws BadFormDesignException {
		// nothing needed
	}
}
