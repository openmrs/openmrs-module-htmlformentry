package org.openmrs.module.htmlformentry.handler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.DosingInstructions;
import org.openmrs.Drug;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryGenerator;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.element.OrderSubmissionElement;
import org.openmrs.module.htmlformentry.schema.CareSettingAnswer;
import org.openmrs.module.htmlformentry.schema.ConceptOption;
import org.openmrs.module.htmlformentry.schema.ConceptOptionGroup;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.OrderField;
import org.openmrs.module.htmlformentry.schema.OrderFrequencyAnswer;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.OrderWidget;
import org.openmrs.module.htmlformentry.widget.OrderWidgetConfig;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles the {@code <order>} tag. The order tag has the following general structure:
 * <order orderAttribute="orderAttributeValue"> <orderTemplate> arbitrary html with
 * <orderProperty propertyAttribute="propertyAttributeValue"></orderProperty> interspersed to
 * control which properties are displayed / editable and with what configuration </orderTemplate>
 * </order>
 */
public class OrderTagHandler extends AbstractTagHandler {
	
	private static final Log log = LogFactory.getLog(OrderTagHandler.class);
	
	public static final String ORDER_TEMPLATE_TAG = "orderTemplate";
	
	public static final String ORDER_PROPERTY_TAG = "orderProperty";
	
	public static final String ORDER_PROPERTY_OPTION_TAG = "option";
	
	public static final String ORDER_PROPERTY_OPTION_GROUP_TAG = "optionGroup";
	
	public static final String NAME_ATTRIBUTE = "name";
	
	public static final String LABEL_ATTRIBUTE = "label";
	
	public static final String VALUE_ATTRIBUTE = "value";
	
	public static final Map<String, Class<? extends OpenmrsObject>> PROPERTIES = new HashMap<>();
	
	static {
		PROPERTIES.put("concept", Concept.class);
		PROPERTIES.put("drug", Drug.class);
		PROPERTIES.put("careSetting", CareSetting.class);
		PROPERTIES.put("orderType", OrderType.class);
		PROPERTIES.put("doseUnits", Concept.class);
		PROPERTIES.put("route", Concept.class);
		PROPERTIES.put("frequency", OrderFrequency.class);
		PROPERTIES.put("durationUnits", Concept.class);
		PROPERTIES.put("quantityUnits", Concept.class);
		PROPERTIES.put("orderReason", Concept.class);
		PROPERTIES.put("discontinueReason", Concept.class);
	}
	
	private final HtmlFormEntryGenerator generator = new HtmlFormEntryGenerator();
	
	@Override
	public List<AttributeDescriptor> createAttributeDescriptors() {
		List<AttributeDescriptor> attributeDescriptors = new ArrayList<>();
		attributeDescriptors.add(new OrderTagAttributeDescriptor());
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	/**
	 * Process a <order> tag, and nested tags
	 */
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node p, Node node) throws BadFormDesignException {
		
		log.trace("OrderTagHandler - started");
		FormEntryContext context = session.getContext();
		OrderField orderField = new OrderField();
		OrderWidgetConfig widgetConfig = new OrderWidgetConfig();
		widgetConfig.setOrderField(orderField);
		widgetConfig.setAttributes(getAttributes(node));
		
		// <order>
		log.trace("OrderTagHandler - loading order tag elements and attributes");
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
		
		// For each property, ensure all options are validated and defaults are configured
		log.trace("OrderTagHandler - processing and validating options");
		
		OrderType orderType = processOrderType(widgetConfig);
		
		// Options valid for all orders
		processConceptOptions(widgetConfig, "concept");
		processEnumOptions(widgetConfig, "action", Order.Action.values(), null);
		processCareSettingOptions(widgetConfig);
		processEnumOptions(widgetConfig, "urgency", Order.Urgency.values(), Order.Urgency.ROUTINE);
		processConceptOptions(widgetConfig, "orderReason");
		processConceptOptions(widgetConfig, "discontinueReason");
		
		// Options valid for drug orders
		if (HtmlFormEntryUtil.isADrugOrderType(orderType)) {
			processDrugOptions(widgetConfig);
			ensureConceptAndDrugOptionsAreConsistent(widgetConfig);
			processConceptOptions(widgetConfig, "doseUnits");
			processConceptOptions(widgetConfig, "route");
			processOrderFrequencyOptions(widgetConfig);
			processDosingTypeOptions(widgetConfig);
			processConceptOptions(widgetConfig, "durationUnits");
			processConceptOptions(widgetConfig, "quantityUnits");
		}
		
		log.trace("OrderTagHandler - constructing order widget");
		OrderWidget orderWidget = new OrderWidget(context, widgetConfig);
		
		log.trace("OrderTagHandler - constructing order submission element");
		OrderSubmissionElement element = new OrderSubmissionElement(context, orderWidget);
		session.getSubmissionController().addAction(element);
		
		log.trace("OrderTagHandler - generating html");
		out.print(element.generateHtml(context));
		
		log.trace("OrderTagHandler - completed");
		
		return false; // skip contents/children
	}
	
	/**
	 * Order Type is retrieved from an attribute on the tag. This is required.
	 */
	protected OrderType processOrderType(OrderWidgetConfig widgetConfig) throws BadFormDesignException {
		String configuredOrderType = widgetConfig.getAttribute("orderType");
		if (StringUtils.isBlank(configuredOrderType)) {
			throw new BadFormDesignException("The orderType attribute is not configured on the order tag");
		}
		OrderType orderType = HtmlFormEntryUtil.getOrderType(configuredOrderType);
		if (orderType == null) {
			throw new BadFormDesignException("The orderType configured on the order tag is invalid: " + orderType);
		}
		widgetConfig.getOrderField().setOrderType(orderType);
		return orderType;
	}
	
	/**
	 * Provides a means to recurse through the nodes in <orderTemplate> and either process normally
	 * using the HtmlFormEntryGenerator, or render order property widgets
	 */
	protected void processTemplateNode(FormEntrySession session, OrderWidgetConfig c, Node pn, Node n, PrintWriter w)
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
	protected void processOrderPropertyTag(FormEntrySession session, OrderWidgetConfig c, Node pn, Node n, PrintWriter w)
	        throws BadFormDesignException {
		Map<String, String> attributes = new TreeMap<>(getAttributes(n));
		String name = attributes.get(NAME_ATTRIBUTE);
		if (StringUtils.isBlank(name)) {
			throw new BadFormDesignException(NAME_ATTRIBUTE + " is required for " + ORDER_PROPERTY_TAG + " tag");
		}
		c.setOrderPropertyAttributes(name, attributes);
		w.print(attributes.toString()); // This writes to the template, which is used for replacement later on
		
		// Determine if there are any explicit options or option groups configured for this property
		List<Option> optionVals = new ArrayList<>();
		NodeList childNodes = n.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equalsIgnoreCase(ORDER_PROPERTY_OPTION_TAG)) {
				Map<String, String> optionAttributes = getAttributes(childNode);
				String value = optionAttributes.get(VALUE_ATTRIBUTE);
				if (StringUtils.isBlank(value)) {
					String msg = ORDER_PROPERTY_OPTION_TAG + " must have a " + VALUE_ATTRIBUTE + " attribute";
					throw new BadFormDesignException(msg);
				}
				optionVals.add(new Option(optionAttributes.get(LABEL_ATTRIBUTE), value, false));
			} else if (childNode.getNodeName().equalsIgnoreCase(ORDER_PROPERTY_OPTION_GROUP_TAG)) {
				if (PROPERTIES.get(name) == Concept.class) {
					Map<String, String> optionAttributes = getAttributes(childNode);
					ConceptOptionGroup optionGroup = ConceptOptionGroup.newInstance(optionAttributes);
					c.getOrderField().addConceptOptionGroup(name, optionGroup);
				} else {
					throw new BadFormDesignException(ORDER_PROPERTY_OPTION_GROUP_TAG + " is only for concept properties");
				}
			}
		}
		c.setOrderPropertyOptions(name, optionVals);
	}
	
	/**
	 * Processes concept and drug options by populating default labels if not supplied, validating
	 * inputs, and populating the OrderField
	 */
	protected void processDrugOptions(OrderWidgetConfig config) throws BadFormDesignException {
		List<Option> drugOptions = config.getOrderPropertyOptions("drug");
		for (Option option : drugOptions) {
			Drug d = HtmlFormEntryUtil.getDrug(option.getValue());
			if (d == null) {
				throw new BadFormDesignException("Unable to find Drug option value: " + option.getValue());
			}
			option.setValue(d.getDrugId().toString());
			option.setLabel(getLabel(option.getLabel(), d.getDisplayName()));
			config.getOrderField().addDrugOrderAnswer(new DrugOrderAnswer(d, option.getLabel()));
		}
		config.setOrderPropertyOptions("drug", drugOptions);
	}
	
	/**
	 * Processes dosing type options
	 */
	protected void processDosingTypeOptions(OrderWidgetConfig config) throws BadFormDesignException {
		String property = "dosingType";
		List<Option> options = config.getOrderPropertyOptions(property);
		Map<Class<? extends DosingInstructions>, String> m = new LinkedHashMap<>();
		m.put(SimpleDosingInstructions.class, "htmlformentry.orders.dosingType.simple");
		m.put(FreeTextDosingInstructions.class, "htmlformentry.orders.dosingType.freetext");
		if (options.isEmpty()) {
			for (Map.Entry<Class<? extends DosingInstructions>, String> e : m.entrySet()) {
				options.add(new Option(e.getValue(), e.getKey().getName(), false));
			}
		} else {
			for (Option option : options) {
				Class<? extends DosingInstructions> dosingType = HtmlFormEntryUtil.getDosingType(option.getValue());
				if (dosingType == null) {
					throw new BadFormDesignException("Unable to find dosing type value: " + option.getValue());
				}
				option.setValue(dosingType.getName());
				option.setLabel(getLabel(option.getLabel(), m.get(dosingType)));
			}
		}
		String selectedVal = config.getAttributes(property).get(VALUE_ATTRIBUTE);
		if (StringUtils.isBlank(selectedVal)) {
			selectedVal = SimpleDosingInstructions.class.getName();
		}
		for (Option option : options) {
			option.setLabel(HtmlFormEntryUtil.translate(option.getLabel()));
			option.setSelected(option.getValue().equalsIgnoreCase(selectedVal));
		}
		config.setOrderPropertyOptions(property, options);
	}
	
	/**
	 * Processes option by populating default labels if not supplied, validating value inputs, and
	 * populating the OrderField
	 */
	protected void processEnumOptions(OrderWidgetConfig config, String property, Enum[] vals, Enum defVal) {
		List<Option> options = config.getOrderPropertyOptions(property);
		String msgPrefix = "htmlformentry.orders." + property + ".";
		if (options.isEmpty()) {
			for (Enum e : vals) {
				String label = HtmlFormEntryUtil.translate(msgPrefix + e.name().toLowerCase());
				options.add(new Option(label, e.name(), false));
			}
		} else {
			for (Option option : options) {
				String defaultLabel = HtmlFormEntryUtil.translate(msgPrefix + option.getValue().toLowerCase());
				String label = getLabel(option.getLabel(), defaultLabel);
				option.setLabel(label);
			}
		}
		String selectedVal = config.getAttributes(property).get(VALUE_ATTRIBUTE);
		if (StringUtils.isBlank(selectedVal) && defVal != null) {
			selectedVal = defVal.name();
		}
		for (Option option : options) {
			option.setSelected(option.getValue().equalsIgnoreCase(selectedVal));
		}
		config.setOrderPropertyOptions(property, options);
	}
	
	/**
	 * Processes option by populating default labels if not supplied, validating value inputs, and
	 * populating the OrderField
	 */
	protected void processCareSettingOptions(OrderWidgetConfig config) throws BadFormDesignException {
		String property = "careSetting";
		List<Option> options = config.getOrderPropertyOptions(property);
		String defaultValue = config.getAttributes(property).get(VALUE_ATTRIBUTE);
		CareSetting defaultCareSetting = null;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultCareSetting = HtmlFormEntryUtil.getCareSetting(defaultValue);
			if (defaultCareSetting == null) {
				throw new BadFormDesignException("Unable to find care setting default value: " + defaultValue);
			}
		}
		if (options.isEmpty()) {
			for (CareSetting cs : getOrderService().getCareSettings(false)) {
				options.add(new Option(cs.getName(), cs.getId().toString(), false));
			}
		} else {
			for (Option option : options) {
				CareSetting cs = HtmlFormEntryUtil.getCareSetting(option.getValue());
				if (cs == null) {
					throw new BadFormDesignException("Unable to find care setting option value: " + defaultValue);
				}
				option.setValue(cs.getCareSettingId().toString());
				option.setLabel(getLabel(option.getLabel(), cs.getName()));
				config.getOrderField().addCareSettingAnswer(new CareSettingAnswer(cs, option.getLabel()));
			}
		}
		if (defaultCareSetting != null) {
			for (Option option : options) {
				option.setSelected(option.getValue().equalsIgnoreCase(defaultCareSetting.getId().toString()));
			}
		}
		config.setOrderPropertyOptions(property, options);
	}
	
	/**
	 * Processes option by populating default labels if not supplied, validating value inputs, and
	 * populating the OrderField
	 */
	protected void processOrderFrequencyOptions(OrderWidgetConfig config) throws BadFormDesignException {
		String property = "frequency";
		List<Option> options = config.getOrderPropertyOptions(property);
		String defaultValue = config.getAttributes(property).get(VALUE_ATTRIBUTE);
		OrderFrequency defaultOrderFrequency = null;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultOrderFrequency = HtmlFormEntryUtil.getOrderFrequency(defaultValue);
			if (defaultOrderFrequency == null) {
				throw new BadFormDesignException("Unable to find frequency default value: " + defaultValue);
			}
		}
		if (options.isEmpty()) {
			for (OrderFrequency f : getOrderService().getOrderFrequencies(false)) {
				options.add(new Option(f.getConcept().getDisplayString(), f.getId().toString(), false));
			}
		} else {
			for (Option option : options) {
				OrderFrequency freq = HtmlFormEntryUtil.getOrderFrequency(option.getValue());
				if (freq == null) {
					throw new BadFormDesignException("Unable to find frequency option value: " + defaultValue);
				}
				option.setValue(freq.getOrderFrequencyId().toString());
				option.setLabel(getLabel(option.getLabel(), freq.getConcept().getDisplayString()));
				config.getOrderField().addOrderFrequencyAnswer(new OrderFrequencyAnswer(freq, option.getLabel()));
			}
		}
		if (defaultOrderFrequency != null) {
			for (Option option : options) {
				option.setSelected(option.getValue().equalsIgnoreCase(defaultOrderFrequency.getId().toString()));
			}
		}
		config.setOrderPropertyOptions(property, options);
	}
	
	/**
	 * Convenience method to get the available value options for a given concept property
	 */
	protected void processConceptOptions(OrderWidgetConfig config, String prop) throws BadFormDesignException {
		List<Option> options = config.getOrderPropertyOptions(prop);
		
		Set<String> valuesAlreadyAdded = new HashSet<>();
		for (Option o : options) {
			valuesAlreadyAdded.add(o.getValue());
		}
		
		List<Concept> fullList = getFullConceptListForProperty(prop);
		
		List<ConceptOptionGroup> optGroups = config.getOrderField().getConceptOptionGroups().get(prop);
		if (optGroups != null) {
			for (ConceptOptionGroup optGroup : optGroups) {
				List<Option> optionsInSet = new ArrayList<>();
				
				// If an option group is configured without a concept, add all concepts still available from full list
				List<Concept> conceptsForOptionGroup = getConceptsForOptionGroup(optGroup);
				if (conceptsForOptionGroup == null || conceptsForOptionGroup.isEmpty()) {
					conceptsForOptionGroup = fullList;
				}
				for (Concept c : conceptsForOptionGroup) {
					String value = c.getConceptId().toString();
					if (!valuesAlreadyAdded.contains(value)) {
						String label = c.getDisplayString();
						ConceptNameType type = HtmlFormEntryUtil.getConceptNameType(optGroup.getLabelNameType());
						ConceptNameTag tag = HtmlFormEntryUtil.getConceptNameTag(optGroup.getLabelNameTag());
						if (type != null || tag != null) {
							ConceptName cn = c.getName(Context.getLocale(), type, tag);
							if (cn != null) {
								label = cn.getName();
							}
						} else if (StringUtils.isNotBlank(optGroup.getLabelMessagePrefix())) {
							String messageCode = optGroup.getLabelMessagePrefix() + c.getUuid();
							String translated = HtmlFormEntryUtil.translate(messageCode);
							if (StringUtils.isNotBlank(translated) && !translated.equals(messageCode)) {
								label = translated;
							}
						}
						StringBuilder css = new StringBuilder();
						if (optGroup.getGroupClass() != null) {
							css.append(optGroup.getOptionClass());
						}
						if (BooleanUtils.isTrue(c.getRetired()) && optGroup.getRetiredOptionClass() != null) {
							css.append(css.length() > 0 ? " " : "").append(optGroup.getRetiredOptionClass());
						}
						Option option = new Option();
						option.setValue(value);
						option.setLabel(label);
						option.setGroupLabel(optGroup.getGroupLabel());
						option.setGroupCssClass(optGroup.getGroupClass());
						option.setCssClass(css.toString());
						optionsInSet.add(option);
						valuesAlreadyAdded.add(value);
						config.getConceptsAndDrugsConfigured().computeIfAbsent(c, k -> new ArrayList<>());
					}
				}
				if (BooleanUtils.isTrue(optGroup.getSortAlphabetically())) {
					optionsInSet.sort(Comparator.comparing(option -> option.getLabel().toLowerCase()));
				}
				options.addAll(optionsInSet);
			}
		}
		
		String defaultValue = config.getAttributes(prop).get(VALUE_ATTRIBUTE);
		Concept defaultConcept = null;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultConcept = HtmlFormEntryUtil.getConcept(defaultValue);
			if (defaultConcept == null) {
				throw new BadFormDesignException("Unable to find Concept default value: " + defaultValue);
			}
		}
		
		if (options.isEmpty()) {
			for (Concept c : fullList) {
				options.add(new Option(c.getDisplayString(), c.getId().toString(), false));
			}
		} else {
			for (Option option : options) {
				Concept c = HtmlFormEntryUtil.getConcept(option.getValue());
				if (c == null) {
					throw new BadFormDesignException("Unable to find concept option value: " + defaultValue);
				}
				if (!fullList.isEmpty() && !fullList.contains(c) && !prop.equalsIgnoreCase("concept")) {
					throw new BadFormDesignException(option.getValue() + " does not refer to a valid concept for " + prop);
				}
				option.setValue(c.getConceptId().toString());
				option.setLabel(getLabel(option.getLabel(), c.getDisplayString()));
				
				ConceptOption conceptOption = new ConceptOption(option.getLabel(), c);
				if ("concept".equalsIgnoreCase(prop)) {
					config.getOrderField().addConceptOption(conceptOption);
				} else if ("doseUnits".equalsIgnoreCase(prop)) {
					config.getOrderField().addDoseUnitAnswer(conceptOption);
				} else if ("route".equalsIgnoreCase(prop)) {
					config.getOrderField().addRouteAnswer(conceptOption);
				} else if ("durationUnits".equalsIgnoreCase(prop)) {
					config.getOrderField().addDurationUnitAnswer(conceptOption);
				} else if ("quantityUnits".equalsIgnoreCase(prop)) {
					config.getOrderField().addQuantityUnitAnswer(conceptOption);
				} else if ("discontinueReason".equalsIgnoreCase(prop)) {
					config.getOrderField().addDiscontinueReasonAnswer(conceptOption);
				}
			}
		}
		if (defaultConcept != null) {
			for (Option option : options) {
				option.setSelected(option.getValue().equalsIgnoreCase(defaultConcept.getId().toString()));
			}
		}
		config.setOrderPropertyOptions(prop, options);
	}
	
	protected void ensureConceptAndDrugOptionsAreConsistent(OrderWidgetConfig config) {
		List<Option> concepts = config.getOrderPropertyOptions("concept");
		List<Option> drugs = config.getOrderPropertyOptions("drug");
		
		boolean conceptsExplicitlyDefined = !concepts.isEmpty();
		boolean drugsExplicitlyDefined = !drugs.isEmpty();
		
		Set<String> existingConcepts = new HashSet<>();
		Set<String> existingDrugs = new HashSet<>();
		for (Option o : concepts) {
			existingConcepts.add(o.getValue());
		}
		for (Option o : drugs) {
			existingDrugs.add(o.getValue());
		}
		
		Map<String, Drug> allDrugsInFormulary = new HashMap<>();
		Map<String, Concept> allConceptsInFormulary = new HashMap<>();
		Map<Concept, List<Drug>> conceptsToDrugs = new HashMap<>();
		for (Drug d : Context.getConceptService().getAllDrugs(false)) {
			allDrugsInFormulary.put(d.getDrugId().toString(), d);
			Concept c = d.getConcept();
			allConceptsInFormulary.put(c.getConceptId().toString(), c);
			List<Drug> l = conceptsToDrugs.computeIfAbsent(c, k -> new ArrayList<>());
			l.add(d);
		}
		
		// If there are no drugs or concepts configured, populate based on entire formulary
		if (!conceptsExplicitlyDefined && !drugsExplicitlyDefined) {
			config.setConceptsAndDrugsConfigured(conceptsToDrugs);
			for (Concept c : conceptsToDrugs.keySet()) {
				concepts.add(new Option(c.getDisplayString(), c.getId().toString()));
				for (Drug d : conceptsToDrugs.get(c)) {
					drugs.add(new Option(d.getDisplayName(), d.getId().toString()));
				}
			}
		}
		// Otherwise, ensure drugs and concepts are additive
		else {
			// If drugs are configured, all associated concepts should be configured
			List<Option> conceptsToAddFromDrugs = new ArrayList<>();
			for (Option drugOption : drugs) {
				Drug d = allDrugsInFormulary.get(drugOption.getValue());
				if (d == null) {
					d = Context.getConceptService().getDrug(Integer.parseInt(drugOption.getValue()));
				}
				String conceptIdStr = d.getConcept().getId().toString();
				if (!existingConcepts.contains(conceptIdStr)) {
					conceptsToAddFromDrugs.add(new Option(d.getConcept().getDisplayString(), conceptIdStr));
					existingConcepts.add(conceptIdStr);
					config.getConceptsAndDrugsConfigured().computeIfAbsent(d.getConcept(), k -> new ArrayList<>()).add(d);
				}
			}
			// If concepts are configured, all associated drugs should be configured
			for (Option conceptOption : concepts) {
				Concept c = allConceptsInFormulary.get(conceptOption.getValue());
				if (conceptsToDrugs.get(c) != null) {
					for (Drug d : conceptsToDrugs.get(c)) {
						String drugId = d.getDrugId().toString();
						if (!existingDrugs.contains(drugId)) {
							drugs.add(new Option(d.getDisplayName(), drugId));
							existingDrugs.add(drugId);
							config.getConceptsAndDrugsConfigured().computeIfAbsent(c, k -> new ArrayList<>()).add(d);
						}
					}
				}
			}
			concepts.addAll(conceptsToAddFromDrugs);
		}
		
		if (!conceptsExplicitlyDefined) {
			concepts.sort(Comparator.comparing(option -> option.getLabel().toLowerCase()));
		}
	}
	
	protected List<Concept> getConceptsForOptionGroup(ConceptOptionGroup optionGroup) {
		List<Concept> ret = new ArrayList<>();
		if (optionGroup.getConcept() != null) {
			ret.addAll(Context.getConceptService().getConceptsByConceptSet(optionGroup.getConcept()));
			for (ConceptAnswer ca : optionGroup.getConcept().getAnswers()) {
				ret.add(ca.getAnswerConcept());
			}
		}
		return ret;
	}
	
	protected List<Concept> getFullConceptListForProperty(String prop) {
		List<Concept> fullList = new ArrayList<>();
		if ("doseUnits".equalsIgnoreCase(prop)) {
			fullList = getOrderService().getDrugDosingUnits();
		} else if ("route".equalsIgnoreCase(prop)) {
			fullList = getOrderService().getDrugRoutes();
		} else if ("durationUnits".equalsIgnoreCase(prop)) {
			fullList = getOrderService().getDurationUnits();
		} else if ("quantityUnits".equalsIgnoreCase(prop)) {
			fullList = getOrderService().getDrugDispensingUnits();
		}
		return fullList;
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
