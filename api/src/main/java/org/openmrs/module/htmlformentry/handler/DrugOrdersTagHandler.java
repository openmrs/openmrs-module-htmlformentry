package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryGenerator;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.DrugOrdersSubmissionElement;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetConfig;
import org.openmrs.module.htmlformentry.widget.DrugOrdersWidget;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles the {@code <drugOrders>} tag.
 */
public class DrugOrdersTagHandler extends AbstractTagHandler {
	
	private static Log log = LogFactory.getLog(DrugOrdersTagHandler.class);
	
	public static final String ORDER_TEMPLATE_TAG = "orderTemplate";
	
	public static final String ORDER_PROPERTY_TAG = "orderProperty";
	
	public static final String ORDER_PROPERTY_OPTION_TAG = "option";
	
	public static final String NAME_ATTRIBUTE = "name";
	
	public static final String DRUG_OPTIONS_TAG = "drugOptions";
	
	public static final String DRUG_OPTION_TAG = "drugOption";
	
	public static final String DISCONTINUE_REASONS_TAG = "discontinueReasons";
	
	public static final String DISCONTINUE_REASON_OPTION_TAG = "discontinueReasonOption";
	
	public static final String DRUG_ATTRIBUTE = "drug";
	
	public static final String CONCEPT_ATTRIBUTE = "concept";
	
	public static final String LABEL_ATTRIBUTE = "label";
	
	public static final String VALUE_ATTRIBUTE = "value";
	
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
		
		StopWatch sw = new StopWatch();
		sw.start();
		log.trace("In DrugOrdersTagHandler.doStartTag");
		
		FormEntryContext context = session.getContext();
		DrugOrderField drugOrderField = new DrugOrderField();
		DrugOrderWidgetConfig widgetConfig = new DrugOrderWidgetConfig();
		widgetConfig.setDrugOrderField(drugOrderField);
		widgetConfig.setAttributes(getAttributes(node));
		
		// <drugOrders>
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equalsIgnoreCase(ORDER_TEMPLATE_TAG)) {
				// <orderTemplate>
				widgetConfig.setTemplateAttributes(getAttributes(childNode));
				CapturingPrintWriter writer = new CapturingPrintWriter();
				processTemplateNode(session, widgetConfig, node, childNode, writer);
				widgetConfig.setTemplateContent(writer.getContent());
				// </orderTemplate>
			} else if (childNode.getNodeName().equalsIgnoreCase(DRUG_OPTIONS_TAG)) {
				// <drugOptions>
				widgetConfig.setDrugOrderAttributes(getAttributes(childNode));
				NodeList drugOptionNodes = childNode.getChildNodes();
				if (drugOptionNodes != null) {
					for (int j = 0; j < drugOptionNodes.getLength(); j++) {
						Node drugOptionNode = drugOptionNodes.item(j);
						if (drugOptionNode.getNodeName().equalsIgnoreCase(DRUG_OPTION_TAG)) {
							// <drugOption drug="" label="">
							Map<String, String> attrs = getAttributes(drugOptionNode);
							Drug drug = HtmlFormEntryUtil.getDrug(attrs.get(DRUG_ATTRIBUTE));
							String label = attrs.get(LABEL_ATTRIBUTE);
							DrugOrderAnswer doa = new DrugOrderAnswer(drug, label);
							drugOrderField.addDrugOrderAnswer(doa);
							// </drugOption>
						}
					}
				}
				// </drugOptions>
			}
			// By default, just output node as written for whatever html formatting is in the form
			else {
				generator.doStartTag(session, out, childNode.getParentNode(), childNode);
			}
		}
		
		// Ensure all property options are configured appropriately, including with default values configured
		OrderService os = Context.getOrderService();
		widgetConfig.configureEnumPropertyOptions("action", Order.Action.values());
		widgetConfig.configureMetadataPropertyOptions("careSetting", os.getCareSettings(false));
		widgetConfig.configureMetadataPropertyOptions("orderType", os.getOrderTypes(false));
		widgetConfig.configureConceptPropertyOptions("doseUnits", os.getDrugDosingUnits());
		widgetConfig.configureConceptPropertyOptions("route", os.getDrugRoutes());
		widgetConfig.configureMetadataPropertyOptions("frequency", os.getOrderFrequencies(false));
		widgetConfig.configureEnumPropertyOptions("urgency", Order.Urgency.values());
		widgetConfig.configureConceptPropertyOptions("durationUnits", os.getDurationUnits());
		widgetConfig.configureConceptPropertyOptions("quantityUnits", os.getDrugDispensingUnits());
		widgetConfig.configureConceptPropertyOptions("discontinueReason", new ArrayList<>());
		widgetConfig.toggleDefaultOptions();
		
		DrugOrdersWidget drugOrdersWidget = new DrugOrdersWidget(context, drugOrderField, widgetConfig);
		DrugOrdersSubmissionElement element = new DrugOrdersSubmissionElement(context, drugOrdersWidget);
		session.getSubmissionController().addAction(element);
		out.print(element.generateHtml(context));
		
		sw.stop();
		log.trace("DrugOrdersTagHandler.doStartTag: " + sw.toString());
		
		return false; // skip contents/children
	}
	
	/**
	 * Provides a means to recurse through the nodes in <orderTemplate> and either process normally
	 * using the HtmlFormEntryGenerator, or render order property widgets
	 */
	protected void processTemplateNode(FormEntrySession session, DrugOrderWidgetConfig c, Node pn, Node n, PrintWriter w)
	        throws BadFormDesignException {
		if (n.getNodeName().equalsIgnoreCase(ORDER_PROPERTY_TAG)) {
			processOrderPropertyTag(session, c, pn, n, w);
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
					processTemplateNode(session, c, n, childNode, w);
				}
			}
			if (!isOrderTemplateTag) {
				generator.doEndTag(session, w, pn, n);
			}
		}
	}
	
	protected void processOrderPropertyTag(FormEntrySession session, DrugOrderWidgetConfig c, Node pn, Node n, PrintWriter w)
	        throws BadFormDesignException {
		Map<String, String> attributes = new TreeMap<>(getAttributes(n));
		String name = attributes.get(NAME_ATTRIBUTE);
		if (StringUtils.isBlank(name)) {
			throw new BadFormDesignException(NAME_ATTRIBUTE + " is required for " + ORDER_PROPERTY_TAG + " tag");
		}
		c.setOrderPropertyAttributes(name, attributes);
		w.print(attributes.toString()); // This writes to the template, which is used for replacement later on
		
		// For properties with discrete options, determine which of these options to include
		
		// First, determine if there are any explicit options configured as child nodes
		NodeList childNodes = n.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equalsIgnoreCase(ORDER_PROPERTY_OPTION_TAG)) {
				Map<String, String> optionAttributes = getAttributes(childNode);
				String value = optionAttributes.get("value");
				if (StringUtils.isBlank(value)) {
					String msg = ORDER_PROPERTY_OPTION_TAG + " must have a " + VALUE_ATTRIBUTE + " attribute";
					throw new BadFormDesignException(msg);
				}
				c.addOrderPropertyOption(name, value, optionAttributes.get(LABEL_ATTRIBUTE));
			}
		}
	}
	
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
	}
}
