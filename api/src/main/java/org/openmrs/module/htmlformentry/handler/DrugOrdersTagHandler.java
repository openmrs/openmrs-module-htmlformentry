package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryGenerator;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.DrugOrdersSubmissionElement;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.widget.DrugOrdersWidget;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles the {@code <drugOrders>} tag.
 */
public class DrugOrdersTagHandler extends AbstractTagHandler {
	
	public static final String ORDER_TEMPLATE_TAG = "orderTemplate";
	
	public static final String ORDER_PROPERTY_TAG = "orderProperty";
	
	public static final String NAME_ATTRIBUTE = "name";
	
	public static final String DRUG_OPTIONS_TAG = "drugOptions";
	
	public static final String DRUG_OPTION_TAG = "drugOption";
	
	public static final String DISCONTINUE_REASONS_TAG = "discontinueReasons";
	
	public static final String DISCONTINUE_REASON_OPTION_TAG = "discontinueReasonOption";
	
	public static final String FORMAT_ATTRIBUTE = "format";
	
	public static final String ON_SELECT = "onselect";
	
	public static final String DRUG_ATTRIBUTE = "drug";
	
	public static final String CONCEPT_ATTRIBUTE = "concept";
	
	public static final String LABEL_ATTRIBUTE = "label";
	
	private HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator();
	
	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();
		attributeDescriptors.add(new AttributeDescriptor("drugs", Drug.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinueReasons", Concept.class));
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node p, Node node) throws BadFormDesignException {
		
		FormEntryContext context = session.getContext();
		DrugOrdersWidget drugOrdersWidget = new DrugOrdersWidget();
		DrugOrderField f = new DrugOrderField();
		drugOrdersWidget.setDrugOrderField(f);
		
		// <drugOrders>
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equalsIgnoreCase(ORDER_TEMPLATE_TAG)) {
				// <orderTemplate>
				drugOrdersWidget.setTemplateAttributes(getAttributes(childNode));
				CapturingPrintWriter writer = new CapturingPrintWriter();
				processTemplateNode(session, drugOrdersWidget, node, childNode, writer);
				drugOrdersWidget.setTemplateContent(writer.getContent());
				// </orderTemplate>
			} else if (childNode.getNodeName().equalsIgnoreCase(DRUG_OPTIONS_TAG)) {
				// <drugOptions>
				drugOrdersWidget.setDrugOrderAttributes(getAttributes(childNode));
				NodeList drugOptionNodes = childNode.getChildNodes();
				if (drugOptionNodes != null) {
					for (int j = 0; j < drugOptionNodes.getLength(); j++) {
						Node drugOptionNode = drugOptionNodes.item(j);
						if (drugOptionNode.getNodeName().equalsIgnoreCase(DRUG_OPTION_TAG)) {
							// <drugOption drug="" label="">
							Map<String, String> attrs = getAttributes(drugOptionNode);
							drugOrdersWidget.addDrugOrderOption(attrs);
							Drug drug = HtmlFormEntryUtil.getDrug(attrs.get(DRUG_ATTRIBUTE));
							String label = attrs.get(LABEL_ATTRIBUTE);
							f.addDrugOrderAnswer(new DrugOrderAnswer(drug, label));
							// </drugOption>
						}
					}
				}
				// </drugOptions>
			} else if (childNode.getNodeName().equalsIgnoreCase(DISCONTINUE_REASONS_TAG)) {
				// <discontinueReasons>
				drugOrdersWidget.setDiscontinueReasonAttributes(getAttributes(childNode));
				NodeList drNodes = childNode.getChildNodes();
				if (drNodes != null) {
					for (int j = 0; j < drNodes.getLength(); j++) {
						Node drNode = drNodes.item(j);
						if (drNode.getNodeName().equalsIgnoreCase(DISCONTINUE_REASON_OPTION_TAG)) {
							// <discontinueReasonOption concept="" label="">
							Map<String, String> attrs = getAttributes(drNodes.item(j));
							drugOrdersWidget.addDiscontinueReasonOption(attrs);
							Concept reason = HtmlFormEntryUtil.getConcept(attrs.get(CONCEPT_ATTRIBUTE));
							String label = attrs.get(LABEL_ATTRIBUTE);
							f.addDiscontinuedReasonAnswer(new ObsFieldAnswer(label, reason));
							// </discontinueReasonOption>
						}
					}
				}
				// <discontinueReasons>
			}
			// By default, just output node as written for whatever html formatting is in the form
			else {
				generator.doStartTag(session, out, childNode.getParentNode(), childNode);
			}
		}
		
		DrugOrdersSubmissionElement element = new DrugOrdersSubmissionElement(context, drugOrdersWidget);
		session.getSubmissionController().addAction(element);
		out.print(element.generateHtml(context));
		return false; // skip contents/children
	}
	
	/**
	 * Provides a means to recurse through the nodes in <orderTemplate> and either process normally
	 * using the HtmlFormEntryGenerator, or render order property widgets
	 */
	protected void processTemplateNode(FormEntrySession session, DrugOrdersWidget widget, Node pn, Node n, PrintWriter w)
	        throws BadFormDesignException {
		if (n.getNodeName().equalsIgnoreCase(ORDER_PROPERTY_TAG)) {
			Map<String, String> attributes = new TreeMap<>(getAttributes(n));
			String name = attributes.get(NAME_ATTRIBUTE);
			if (StringUtils.isBlank(name)) {
				throw new BadFormDesignException(NAME_ATTRIBUTE + " is required for " + ORDER_PROPERTY_TAG + " tag");
			}
			widget.addTemplateWidget(name, attributes);
			w.print(attributes.toString());
		} else {
			boolean isOrderTemplateTag = (n.getNodeName().equalsIgnoreCase(ORDER_TEMPLATE_TAG));
			boolean handleContents = isOrderTemplateTag;
			if (!isOrderTemplateTag) {
				handleContents = generator.doStartTag(session, w, pn, n);
			}
			if (handleContents) {
				NodeList childNodes = n.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node childNode = childNodes.item(i);
					processTemplateNode(session, widget, n, childNode, w);
				}
			}
			if (!isOrderTemplateTag) {
				generator.doEndTag(session, w, pn, n);
			}
		}
	}
	
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
	}
}
