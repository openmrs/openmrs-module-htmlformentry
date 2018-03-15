package org.openmrs.module.htmlformentry.handler;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.LocationTag;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.w3c.dom.Node;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Collection;

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
        if (session.getContext().getMode() != FormEntryContext.Mode.VIEW && element.hasWhenValueThen()) {
            if (element.getId() == null) {
                throw new IllegalStateException("<obs> must have an id attribute to define when-then actions");
            }
            out.println("<script type=\"text/javascript\">");
            out.println("jQuery(function() { htmlForm.setupWhenThen('" + element.getId() + "', "
                    + simplifyWhenThen(element.getWhenValueThenDisplaySection()) + ", "
                    + simplifyWhenThen(element.getWhenValueThenJavascript()) + ", "
                    + simplifyWhenThen(element.getWhenValueElseJavascript())
                    + "); });");
            out.println("</script>");
        }
    }

    private String simplifyWhenThen(Map<Object, String> whenThen) {
        Map<Object, String> simplified = new LinkedHashMap<Object, String>();
        if (whenThen.size() == 0) {
            return "null";
        }
        for (Map.Entry<Object, String> entry : whenThen.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Concept) {
                key = ((Concept) key).getConceptId();
            }
            simplified.put(key, entry.getValue());
        }
        return toJson(simplified);
    }

    @Override
    public TagAnalysis validate(Node node) {
        Concept concept = null;
        TagAnalysis analysis = new TagAnalysis();
        List<Concept> concepts = new ArrayList<Concept>();
        String conceptId = HtmlFormEntryUtil.getNodeAttribute(node, "conceptId", null);
        String conceptIds = HtmlFormEntryUtil.getNodeAttribute(node, "conceptIds", null);
        String answerConceptId = HtmlFormEntryUtil.getNodeAttribute(node, "answerConceptId", null);
        String answerConceptIds = HtmlFormEntryUtil.getNodeAttribute(node, "answerConceptIds", null);
	    
        if (conceptId == null && conceptIds == null) {
            analysis.addError(Context.getMessageSourceService().getMessage("htmlformentry.error.invalidConceptIdsAttribute",
                new Object[] { "[concept id]", "[concept ids]" }, null));
        } else if (conceptId != null && conceptIds != null) {
            analysis.addError(Context.getMessageSourceService().getMessage("htmlformentry.error.invalidConceptIdsAttribute",
                new Object[] { conceptId, conceptIds }, null));
        }

        if (analysis.getErrors().size() == 0 && conceptId != null) {
            concept = HtmlFormEntryUtil.getConcept(conceptId);
            if (concept == null) {
                analysis.addError(Context.getMessageSourceService().getMessage("htmlformentry.error.invalidConcept",
                    new Object[] { conceptId }, null));
            } else if(answerConceptId != null || answerConceptIds != null) {
	    	    ArrayList<Integer> validAnswerConceptIDs = new ArrayList<Integer>();
                Collection<ConceptAnswer> conceptAnswers = concept.getAnswers();
                if(conceptAnswers != null) {
                    for (ConceptAnswer ca : conceptAnswers) {
                        validAnswerConceptIDs.add(ca.getAnswerConcept().getConceptId());
                    }
                }
                //assuming only one of answerConceptIds and answerConceptId will be present at a time
                if(answerConceptIds != null) {
                    for (StringTokenizer st = new StringTokenizer(answerConceptIds, ","); st.hasMoreTokens();) {
                        String answerCId = st.nextToken();
                        if(!validAnswerConceptIDs.contains(HtmlFormEntryUtil.getConcept(answerCId).getConceptId())) {
                            analysis.addWarning(Context.getMessageSourceService().getMessage(
                                    "htmlformentry.warning.invalidAnswerConcept",
                                    new Object[] { String.valueOf(answerCId) }, null));
                        }
                    }
                } else if(answerConceptId != null && !validAnswerConceptIDs.contains(HtmlFormEntryUtil.getConcept(answerConceptId).getConceptId())) {
                    analysis.addWarning(Context.getMessageSourceService().getMessage(
                            "htmlformentry.warning.invalidAnswerConcept",
                            new Object[] { String.valueOf(answerConceptId) }, null));
                }
            }
        } else if (analysis.getErrors().size() == 0 && conceptIds != null) {
            for (StringTokenizer st = new StringTokenizer(conceptIds, ","); st.hasMoreTokens();) {
                String conceptIdString = st.nextToken().trim();
                Concept c = HtmlFormEntryUtil.getConcept(conceptIdString);
                if (c == null) {
                    analysis.addError(Context.getMessageSourceService().getMessage("htmlformentry.error.invalidConcept",
                        new Object[] { conceptIdString }, null));
                } else if(answerConceptId != null) {
                    Integer answerConceptIdInteger = HtmlFormEntryUtil.getConcept(answerConceptId).getConceptId();
                    Collection<ConceptAnswer> cas = c.getAnswers();
                    ArrayList<Integer> answerIds = new ArrayList<Integer>();
                    if(cas != null) {
                        for (ConceptAnswer ca : cas) {
                            answerIds.add(ca.getAnswerConcept().getConceptId());
                        }
                    }
                    if(!answerIds.contains(answerConceptIdInteger)) {
                        analysis.addWarning(Context.getMessageSourceService().getMessage(
                                "htmlformentry.warning.invalidAnswerConcept",
                                new Object[] { String.valueOf(conceptIdString) }, null));
                    }
                }
                concepts.add(c);
            }
        }

        Node parentNode = node.getParentNode();
        List<Concept> setMembers = concept == null ? concepts : Arrays.asList(concept);
        while (parentNode.getParentNode() != null) {
            String groupingConceptId = HtmlFormEntryUtil.getNodeAttribute(parentNode, "groupingConceptId", null);
            if (groupingConceptId != null) {
                Concept groupingConcept = HtmlFormEntryUtil.getConcept(groupingConceptId);
                if (groupingConcept != null) {
                    for (Concept setMember : setMembers) {
                        if (!groupingConcept.getSetMembers().contains(setMember)) {
                            analysis.addWarning(Context.getMessageSourceService().getMessage(
                                "htmlformentry.warning.invalidMember",
                                new Object[] { String.valueOf(setMember.getConceptId()), groupingConceptId }, null));
                        }
                    }
                }
                break;
            }
            parentNode = parentNode.getParentNode();
        }
        return analysis;
    }

}
