package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Concept;
import org.openmrs.LocationTag;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the {@code <obs>} tag
 */
public class ObsTagHandler extends AbstractTagHandler {
	
	@Override
	protected List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("conceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("conceptIds", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("answerConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("answerConceptIds", Concept.class));
        attributeDescriptors.add(new AttributeDescriptor(HtmlFormEntryConstants.ANSWER_LOCATION_TAGS, LocationTag.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}


    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        FormEntryContext context = session.getContext();
        ObsSubmissionElement element = new ObsSubmissionElement(context, getAttributes(node));
        session.getSubmissionController().addAction(element);
        out.print(element.generateHtml(context));

        context.pushToStack(element);
        return true;
    }

    @Override
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        Object popped = session.getContext().popFromStack();
        if (!(popped instanceof ObsSubmissionElement)) {
            throw new IllegalStateException("Popped an element from the stack but it wasn't an ObsSubmissionElement!");
        }

        ObsSubmissionElement element = (ObsSubmissionElement) popped;
        Map<Object, String> whenThen = element.getWhenValueThenDisplaySection();
        if (whenThen.size() > 0) {
            if (element.getId() == null) {
                throw new IllegalStateException("<obs> must have an id attribute to define when-then actions");
            }
            Map<Object, String> simplified = new LinkedHashMap<Object, String>();
            for (Map.Entry<Object, String> entry : whenThen.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof Concept) {
                    key = ((Concept) key).getConceptId();
                }
                simplified.put(key, entry.getValue());
            }
            out.println("<script type=\"text/javascript\">");
            out.println("jQuery(function() { htmlForm.setupWhenThenDisplay('" + element.getId() + "', " + toJson(simplified) + "); });");
            out.println("</script>");
        }
    }

}
