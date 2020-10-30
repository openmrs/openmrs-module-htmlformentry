package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryGenerator;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidget;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetConfig;
import org.openmrs.module.htmlformentry.widget.Option;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles the {@code <drugOrder>} tag. The drugOrder tag has the following general structure:
 * <drugOrder drugOrderAttribute="drugOrderAttributeValue"> <orderTemplate> Arbitrary html can be
 * placed within this, which is what is rendered for the Order Form orderProperty tags can be
 * interspersed throughout to control with the various widgets are for each of the drug order
 * properties that need to be selected. These orderProperty tags can be nested in divs that choose
 * to hide them, and javascript and css can further control their layout.
 * <orderProperty name="drugOrderPropertyName" value="drugOrderPropertyDefaultValue" otherAtt=
 * "otherAttVal"> <option value="IF_CODED_CAN_LIMIT_TO_LOOKUP" label"Optional Label for this
 * option"/> </orderProperty> </orderTemplate> </drugOrder>
 */
public class DrugOrderTagHandler extends AbstractTagHandler {
	
	private static final Log log = LogFactory.getLog(DrugOrderTagHandler.class);
	
	public static final String ORDER_TEMPLATE_TAG = "orderTemplate";
	
	public static final String ORDER_PROPERTY_TAG = "orderProperty";
	
	public static final String ORDER_PROPERTY_OPTION_TAG = "option";
	
	public static final String NAME_ATTRIBUTE = "name";
	
	public static final String LABEL_ATTRIBUTE = "label";
	
	public static final String VALUE_ATTRIBUTE = "value";
	
	private final HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator();
	
	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<>();
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	/**
	 * Process a <drugOrders> tag, and nested tags
	 */
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node p, Node node) throws BadFormDesignException {
		
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
			}
			// By default, just output node as written for whatever html formatting is in the form
			else {
				generator.doStartTag(session, out, childNode.getParentNode(), childNode);
			}
		}
		
		DrugOrderWidget drugOrdersWidget = new DrugOrderWidget(context, widgetConfig);
		DrugOrderSubmissionElement element = new DrugOrderSubmissionElement(context, drugOrdersWidget);
		session.getSubmissionController().addAction(element);
		out.print(element.generateHtml(context));
		
		return false; // skip contents/children
	}
	
	/**
	 * Provides a means to recurse through the nodes in <orderTemplate> and either process normally
	 * using the HtmlFormEntryGenerator, or render order property widgets
	 */
	protected void processTemplateNode(FormEntrySession session, DrugOrderWidgetConfig c, Node pn, Node n, PrintWriter w)
	        throws BadFormDesignException {
		if (n.getNodeName().equalsIgnoreCase(ORDER_PROPERTY_TAG)) {
			// <orderProperty>
			processOrderPropertyTag(session, c, pn, n, w);
			// </orderProperty>
		} else {
			// Process normally, recursing through child nodes to identify more <orderProperty> tags
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
	
	/**
	 * Processes an <orderProperty> tag These tags may have attributes to control their behavior (for
	 * numeric and text widgets, or to toggle input types) These tags may also have nested
	 * <option value="" label=""/> tags for single option property configuration
	 */
	protected void processOrderPropertyTag(FormEntrySession session, DrugOrderWidgetConfig c, Node pn, Node n, PrintWriter w)
	        throws BadFormDesignException {
		Map<String, String> attributes = new TreeMap<>(getAttributes(n));
		String name = attributes.get(NAME_ATTRIBUTE);
		if (StringUtils.isBlank(name)) {
			throw new BadFormDesignException(NAME_ATTRIBUTE + " is required for " + ORDER_PROPERTY_TAG + " tag");
		}
		c.setOrderPropertyAttributes(name, attributes);
		w.print(attributes.toString()); // This writes to the template, which is used for replacement later on
		
		// Determine if there are any explicit options configured for this property
		Map<String, String> optionVals = new LinkedHashMap<>();
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
				optionVals.put(value, optionAttributes.get(LABEL_ATTRIBUTE));
			}
		}
		
		if ("drug".equalsIgnoreCase(name)) {
			c.getDrugOrderField().setDrugOrderAnswers(getDrugOrderAnswers(optionVals));
		} else {
			List<Option> options = getOptions(name, optionVals, attributes.get(VALUE_ATTRIBUTE));
			if (options != null) {
				c.addOrderPropertyOptions(name, options);
			}
		}
	}
	
	/**
	 * Convenience method to get the available value options for a given property and configuration
	 */
	private List<Option> getOptions(String property, Map<String, String> optionVals, String defaultValue)
	        throws BadFormDesignException {
		if ("action".equalsIgnoreCase(property)) {
			return getEnumOptions(property, optionVals, Order.Action.values(), defaultValue);
		}
		if ("careSetting".equalsIgnoreCase(property)) {
			return getCareSettingOptions(optionVals, defaultValue);
		}
		if ("orderType".equalsIgnoreCase(property)) {
			return getOrderTypeOptions(optionVals, defaultValue);
		}
		if ("doseUnits".equalsIgnoreCase(property)) {
			return getConceptOptions(property, optionVals, defaultValue);
		}
		if ("route".equalsIgnoreCase(property)) {
			return getConceptOptions(property, optionVals, defaultValue);
		}
		if ("frequency".equalsIgnoreCase(property)) {
			return getOrderFrequencyOptions(optionVals, defaultValue);
		}
		if ("urgency".equalsIgnoreCase(property)) {
			defaultValue = (StringUtils.isBlank(defaultValue) ? Order.Urgency.ROUTINE.name() : defaultValue);
			return getEnumOptions(property, optionVals, Order.Urgency.values(), defaultValue);
		}
		if ("durationUnits".equalsIgnoreCase(property)) {
			return getConceptOptions(property, optionVals, defaultValue);
		}
		if ("quantityUnits".equalsIgnoreCase(property)) {
			return getConceptOptions(property, optionVals, defaultValue);
		}
		if ("discontinueReason".equalsIgnoreCase(property)) {
			return getConceptOptions(property, optionVals, defaultValue);
		}
		return null;
	}
	
	/**
	 * Convenience method to get the available drugs for use by the tag
	 */
	protected List<DrugOrderAnswer> getDrugOrderAnswers(Map<String, String> optionVals) throws BadFormDesignException {
		List<DrugOrderAnswer> ret = new ArrayList<>();
		if (optionVals.isEmpty()) {
			for (Drug d : Context.getConceptService().getAllDrugs(false)) {
				ret.add(new DrugOrderAnswer(d, d.getDisplayName()));
			}
		} else {
			for (String val : optionVals.keySet()) {
				Drug d = HtmlFormEntryUtil.getDrug(val);
				if (d == null) {
					throw new BadFormDesignException("Unable to find Drug option value: " + val);
				}
				String label = optionVals.get(val);
				if (StringUtils.isBlank(label)) {
					label = d.getDisplayName();
				}
				ret.add(new DrugOrderAnswer(d, label));
			}
		}
		return ret;
	}
	
	/**
	 * Convenience method to get the available value options for a given enum property
	 */
	protected List<Option> getEnumOptions(String property, Map<String, String> options, Enum[] vals, String selected) {
		List<Option> l = new ArrayList<>();
		for (Enum e : vals) {
			if (options.isEmpty() || options.containsKey(e.name())) {
				String labelCode = options.get(e.name());
				if (StringUtils.isBlank(labelCode)) {
					labelCode = "htmlformentry.drugOrder." + property + "." + e.name().toLowerCase();
				}
				String label = HtmlFormEntryUtil.translate(labelCode);
				boolean isSelected = selected != null && e.name().equalsIgnoreCase(selected);
				l.add(new Option(label, e.name(), isSelected));
			}
		}
		return l;
	}
	
	/**
	 * Convenience method to get the available value options for the Care Setting property
	 */
	protected List<Option> getCareSettingOptions(Map<String, String> optionVals, String defaultValue)
	        throws BadFormDesignException {
		CareSetting defaultCareSetting = null;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultCareSetting = HtmlFormEntryUtil.getCareSetting(defaultValue);
			if (defaultCareSetting == null) {
				throw new BadFormDesignException("Unable to find care setting default value: " + defaultValue);
			}
		}
		Map<CareSetting, String> careSettingList = new LinkedHashMap<>();
		if (optionVals.isEmpty()) {
			for (CareSetting cs : getOrderService().getCareSettings(false)) {
				careSettingList.put(cs, cs.getName());
			}
		} else {
			for (String val : optionVals.keySet()) {
				CareSetting cs = HtmlFormEntryUtil.getCareSetting(val);
				if (cs == null) {
					throw new BadFormDesignException("Unable to find care setting option value: " + defaultValue);
				}
				careSettingList.put(cs, getLabel(optionVals.get(val), cs.getName()));
			}
		}
		return getMetadataOptions(careSettingList, defaultCareSetting);
	}
	
	/**
	 * Convenience method to get the available value options for the Order Type property
	 */
	protected List<Option> getOrderTypeOptions(Map<String, String> optionVals, String defaultValue)
	        throws BadFormDesignException {
		OrderType defaultOrderType;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultOrderType = HtmlFormEntryUtil.getOrderType(defaultValue);
			if (defaultOrderType == null) {
				throw new BadFormDesignException("Unable to find order type default value: " + defaultValue);
			}
		} else {
			defaultOrderType = HtmlFormEntryUtil.getDrugOrderType();
		}
		Map<OrderType, String> orderTypeList = new LinkedHashMap<>();
		if (optionVals.isEmpty()) {
			for (OrderType ot : getOrderService().getOrderTypes(false)) {
				orderTypeList.put(ot, ot.getName());
			}
		} else {
			for (String val : optionVals.keySet()) {
				OrderType cs = HtmlFormEntryUtil.getOrderType(val);
				if (cs == null) {
					throw new BadFormDesignException("Unable to find order type option value: " + defaultValue);
				}
				orderTypeList.put(cs, getLabel(optionVals.get(val), cs.getName()));
			}
		}
		return getMetadataOptions(orderTypeList, defaultOrderType);
	}
	
	/**
	 * Convenience method to get the available value options for the frequency property
	 */
	protected List<Option> getOrderFrequencyOptions(Map<String, String> optionVals, String defaultValue)
	        throws BadFormDesignException {
		OrderFrequency defaultOrderFrequency = null;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultOrderFrequency = HtmlFormEntryUtil.getOrderFrequency(defaultValue);
			if (defaultOrderFrequency == null) {
				throw new BadFormDesignException("Unable to find frequency default value: " + defaultValue);
			}
		}
		Map<OrderFrequency, String> frequencyList = new LinkedHashMap<>();
		if (optionVals.isEmpty()) {
			for (OrderFrequency f : getOrderService().getOrderFrequencies(false)) {
				frequencyList.put(f, f.getName());
			}
		} else {
			for (String val : optionVals.keySet()) {
				OrderFrequency cs = HtmlFormEntryUtil.getOrderFrequency(val);
				if (cs == null) {
					throw new BadFormDesignException("Unable to find frequency option value: " + defaultValue);
				}
				frequencyList.put(cs, getLabel(optionVals.get(val), cs.getName()));
			}
		}
		return getMetadataOptions(frequencyList, defaultOrderFrequency);
	}
	
	/**
	 * Convenience method to configure options from a list of metadata
	 */
	protected List<Option> getMetadataOptions(Map<? extends OpenmrsMetadata, String> vals, OpenmrsMetadata selected) {
		List<Option> l = new ArrayList<>();
		for (OpenmrsMetadata m : vals.keySet()) {
			String val = m.getId().toString();
			String label = vals.get(m);
			boolean isSelected = selected != null && val.equalsIgnoreCase(selected.getId().toString());
			l.add(new Option(label, val, isSelected));
		}
		return l;
	}
	
	/**
	 * Convenience method to get the available value options for a given concept property
	 */
	protected List<Option> getConceptOptions(String property, Map<String, String> optionVals, String defaultValue)
	        throws BadFormDesignException {
		Concept defaultConcept = null;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultConcept = HtmlFormEntryUtil.getConcept(defaultValue);
			if (defaultConcept == null) {
				throw new BadFormDesignException("Unable to find Concept default value: " + defaultValue);
			}
		}
		
		List<Concept> fullList = new ArrayList<>();
		if ("doseUnits".equalsIgnoreCase(property)) {
			fullList = getOrderService().getDrugDosingUnits();
		} else if ("route".equalsIgnoreCase(property)) {
			fullList = getOrderService().getDrugRoutes();
		} else if ("durationUnits".equalsIgnoreCase(property)) {
			fullList = getOrderService().getDurationUnits();
		} else if ("quantityUnits".equalsIgnoreCase(property)) {
			fullList = getOrderService().getDrugDispensingUnits();
		}
		
		Map<Concept, String> configuredList = new LinkedHashMap<>();
		if (optionVals == null || optionVals.isEmpty()) {
			for (Concept c : fullList) {
				configuredList.put(c, c.getDisplayString());
			}
		} else {
			for (String val : optionVals.keySet()) {
				Concept c = HtmlFormEntryUtil.getConcept(val);
				if (c == null) {
					throw new BadFormDesignException("Unable to find concept option value: " + defaultValue);
				}
				if (!fullList.isEmpty() && !fullList.contains(c)) {
					throw new BadFormDesignException(val + " does not refer to a valid concept for " + property);
				}
				configuredList.put(c, getLabel(optionVals.get(val), c.getDisplayString()));
			}
		}
		
		List<Option> ret = new ArrayList<>();
		for (Concept c : configuredList.keySet()) {
			String val = c.getId().toString();
			String label = configuredList.get(c);
			boolean selected = defaultConcept != null && defaultConcept.getId().equals(c.getId());
			ret.add(new Option(label, val, selected));
		}
		return ret;
	}
	
	private String getLabel(String configuredLabel, String defaultLabel) {
		if (StringUtils.isNotBlank(configuredLabel)) {
			return HtmlFormEntryUtil.translate(configuredLabel);
		}
		return defaultLabel;
	}
	
	private OrderService getOrderService() {
		return Context.getOrderService();
	}
	
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
	}
}
