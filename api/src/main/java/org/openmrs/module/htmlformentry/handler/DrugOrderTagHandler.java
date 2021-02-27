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
import org.openmrs.module.htmlformentry.element.DrugOrderSubmissionElement;
import org.openmrs.module.htmlformentry.schema.CareSettingAnswer;
import org.openmrs.module.htmlformentry.schema.ConceptOptionGroup;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.schema.OrderFrequencyAnswer;
import org.openmrs.module.htmlformentry.schema.OrderTypeAnswer;
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
	
	public static final String DRUG_NAMES_ATTRIBUTE = "drugNames"; // Supports legacy attribute, csv drug lookups
	
	public static final String DRUG_LABELS_ATTRIBUTE = "drugLabels"; // Supports legacy attribute, csv labels for lookups
	
	public static final String DISCONTINUE_REASON_QUESTION_ATTRIBUTE = "discontinuedReasonConceptId"; // Legacy
	
	public static final String DISCONTINUE_REASON_ANSWERS_ATTRIBUTE = "discontinueReasonAnswers"; // Legacy
	
	public static final String DISCONTINUE_REASON_ANSWER_LABELS_ATTRIBUTE = "discontinueReasonAnswerLabels"; // Legacy
	
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
		attributeDescriptors.add(new AttributeDescriptor("drugNames", Drug.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinuedReasonConceptId", Concept.class));
		attributeDescriptors.add(new AttributeDescriptor("discontinueReasonAnswers", Concept.class));
		attributeDescriptors.add(new DrugOrderTagAttributeDescriptor());
		return Collections.unmodifiableList(attributeDescriptors);
	}
	
	/**
	 * Process a <drugOrders> tag, and nested tags
	 */
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node p, Node node) throws BadFormDesignException {
		
		log.trace("DrugOrderTagHandler - started");
		FormEntryContext context = session.getContext();
		DrugOrderField drugOrderField = new DrugOrderField();
		DrugOrderWidgetConfig widgetConfig = new DrugOrderWidgetConfig();
		widgetConfig.setDrugOrderField(drugOrderField);
		widgetConfig.setAttributes(getAttributes(node));

		log.trace("DrugOrderTagHandler - processing legacy drug order attributes");
		processLegacyDrugOrderAttributes(widgetConfig);
		
		// <drugOrder>
		log.trace("DrugOrderTagHandler - loading drug order tag elements and attributes");
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
		log.trace("DrugOrderTagHandler - processing and validating options");
		processConceptOptions(widgetConfig, "concept");
		processDrugOptions(widgetConfig);
		ensureConceptAndDrugOptionsAreConsistent(widgetConfig);
		processEnumOptions(widgetConfig, "action", Order.Action.values(), null);
		processCareSettingOptions(widgetConfig);
		processOrderTypeOptions(widgetConfig);
		processConceptOptions(widgetConfig, "orderReason");
		processConceptOptions(widgetConfig, "doseUnits");
		processConceptOptions(widgetConfig, "route");
		processOrderFrequencyOptions(widgetConfig);
		processDosingTypeOptions(widgetConfig);
		processEnumOptions(widgetConfig, "urgency", Order.Urgency.values(), Order.Urgency.ROUTINE);
		processConceptOptions(widgetConfig, "durationUnits");
		processConceptOptions(widgetConfig, "quantityUnits");
		processConceptOptions(widgetConfig, "discontinueReason");

		log.trace("DrugOrderTagHandler - constructing drug order widget");
		DrugOrderWidget drugOrderWidget = new DrugOrderWidget(context, widgetConfig);

		log.trace("DrugOrderTagHandler - constructing drug order submission element");
		DrugOrderSubmissionElement element = new DrugOrderSubmissionElement(context, drugOrderWidget);
		session.getSubmissionController().addAction(element);

		log.trace("DrugOrderTagHandler - generating html");
		out.print(element.generateHtml(context));
		
		log.trace("DrugOrderTagHandler - completed");

		return false; // skip contents/children
	}
	
	/**
	 * The purpose of this method is to support the legacy attributes for configuring the drug order tag
	 * 
	 * @param widgetConfig
	 */
	protected void processLegacyDrugOrderAttributes(DrugOrderWidgetConfig widgetConfig) throws BadFormDesignException {
		
		// Configure drugs via the drugNames and drugLabels attributes
		String drugNameConfig = widgetConfig.getAttribute(DRUG_NAMES_ATTRIBUTE);
		String drugLabelConfig = widgetConfig.getAttribute(DRUG_LABELS_ATTRIBUTE);
		String[] drugsNames = StringUtils.isBlank(drugNameConfig) ? null : drugNameConfig.split(",");
		String[] drugLabels = StringUtils.isBlank(drugLabelConfig) ? null : drugLabelConfig.split(",");
		if (drugsNames == null) {
			if (drugLabels != null) {
				throw new BadFormDesignException("You must specify drugNames if you specify drugLabels");
			}
		} else {
			if (drugLabels != null && drugLabels.length != drugsNames.length) {
				throw new BadFormDesignException("If drugLabels are configured, they must be configured for each drugName");
			}
			for (int i = 0; i < drugsNames.length; i++) {
				String drug = drugsNames[i];
				String label = (drugLabels == null ? "" : drugLabels[i]);
				widgetConfig.addOrderPropertyOption("drug", new Option(label, drug, false));
			}
		}
		
		// Configure discontinueReasons via attributes
		String discontinuedQuestionConfig = widgetConfig.getAttribute(DISCONTINUE_REASON_QUESTION_ATTRIBUTE);
		if (StringUtils.isNotBlank(discontinuedQuestionConfig)) {
			Concept q = HtmlFormEntryUtil.getConcept(discontinuedQuestionConfig);
			if (q == null) {
				throw new BadFormDesignException("Unable to find concept: " + discontinuedQuestionConfig);
			}
			if (q.getAnswers() == null || q.getAnswers().isEmpty()) {
				throw new BadFormDesignException(DISCONTINUE_REASON_QUESTION_ATTRIBUTE + " does not have any answers");
			}
			widgetConfig.getDrugOrderField().setDiscontinuedReasonQuestion(q);
		}
		
		String answerConfig = widgetConfig.getAttribute(DISCONTINUE_REASON_ANSWERS_ATTRIBUTE);
		String labelConfig = widgetConfig.getAttribute(DISCONTINUE_REASON_ANSWER_LABELS_ATTRIBUTE);
		String[] discontinuedAnswers = StringUtils.isBlank(answerConfig) ? null : answerConfig.split(",");
		String[] discontinuedLabels = StringUtils.isBlank(labelConfig) ? null : labelConfig.split(",");
		if (discontinuedAnswers == null) {
			if (discontinuedLabels != null) {
				String msg = "You must specify discontinueReasonAnswers if you specify discontinueReasonAnswerLabels";
				throw new BadFormDesignException(msg);
			}
			if (widgetConfig.getDrugOrderField().getDiscontinuedReasonQuestion() != null) {
				for (ConceptAnswer ca : widgetConfig.getDrugOrderField().getDiscontinuedReasonQuestion().getAnswers()) {
					String caLabel = ca.getAnswerConcept().getDisplayString();
					String caName = ca.getAnswerConcept().getId().toString();
					widgetConfig.addOrderPropertyOption("discontinueReason", new Option(caLabel, caName, false));
				}
			}
		} else {
			if (discontinuedLabels != null && discontinuedLabels.length != discontinuedAnswers.length) {
				String msg = "discontinueReasonAnswerLabels must be configured for each discontinueReasonAnswer";
				throw new BadFormDesignException(msg);
			}
			for (int i = 0; i < discontinuedAnswers.length; i++) {
				String answer = discontinuedAnswers[i];
				String label = (discontinuedLabels == null ? "" : discontinuedLabels[i]);
				widgetConfig.addOrderPropertyOption("discontinueReason", new Option(label, answer, false));
			}
		}
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
					c.getDrugOrderField().addConceptOptionGroup(name, optionGroup);
				} else {
					throw new BadFormDesignException(ORDER_PROPERTY_OPTION_GROUP_TAG + " is only for concept properties");
				}
			}
		}
		c.setOrderPropertyOptions(name, optionVals);
	}
	
	/**
	 * Processes concept and drug options by populating default labels if not supplied, validating
	 * inputs, and populating the DrugOrderField
	 */
	protected void processDrugOptions(DrugOrderWidgetConfig config) throws BadFormDesignException {
		List<Option> drugOptions = config.getOrderPropertyOptions("drug");
		for (Option option : drugOptions) {
			Drug d = HtmlFormEntryUtil.getDrug(option.getValue());
			if (d == null) {
				throw new BadFormDesignException("Unable to find Drug option value: " + option.getValue());
			}
			option.setValue(d.getDrugId().toString());
			option.setLabel(getLabel(option.getLabel(), d.getDisplayName()));
			config.getDrugOrderField().addDrugOrderAnswer(new DrugOrderAnswer(d, option.getLabel()));
		}
		config.setOrderPropertyOptions("drug", drugOptions);
	}
	
	/**
	 * Processes dosing type options
	 */
	protected void processDosingTypeOptions(DrugOrderWidgetConfig config) throws BadFormDesignException {
		String property = "dosingType";
		List<Option> options = config.getOrderPropertyOptions(property);
		Map<Class<? extends DosingInstructions>, String> m = new LinkedHashMap<>();
		m.put(SimpleDosingInstructions.class, "htmlformentry.drugOrder.dosingType.simple");
		m.put(FreeTextDosingInstructions.class, "htmlformentry.drugOrder.dosingType.freetext");
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
	 * populating the DrugOrderField
	 */
	protected void processEnumOptions(DrugOrderWidgetConfig config, String property, Enum[] vals, Enum defVal) {
		List<Option> options = config.getOrderPropertyOptions(property);
		String msgPrefix = "htmlformentry.drugOrder." + property + ".";
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
	 * populating the DrugOrderField
	 */
	protected void processCareSettingOptions(DrugOrderWidgetConfig config) throws BadFormDesignException {
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
				config.getDrugOrderField().addCareSettingAnswer(new CareSettingAnswer(cs, option.getLabel()));
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
	 * populating the DrugOrderField
	 */
	protected void processOrderTypeOptions(DrugOrderWidgetConfig config) throws BadFormDesignException {
		String property = "orderType";
		List<Option> options = config.getOrderPropertyOptions(property);
		String defaultValue = config.getAttributes(property).get(VALUE_ATTRIBUTE);
		OrderType defaultOrderType;
		if (StringUtils.isNotBlank(defaultValue)) {
			defaultOrderType = HtmlFormEntryUtil.getOrderType(defaultValue);
			if (defaultOrderType == null) {
				throw new BadFormDesignException("Unable to find order type default value: " + defaultValue);
			}
		} else {
			defaultOrderType = HtmlFormEntryUtil.getDrugOrderType();
		}
		if (options.isEmpty()) {
			for (OrderType ot : HtmlFormEntryUtil.getDrugOrderTypes()) {
				options.add(new Option(ot.getName(), ot.getId().toString(), false));
			}
		} else {
			for (Option option : options) {
				OrderType ot = HtmlFormEntryUtil.getOrderType(option.getValue());
				if (ot == null) {
					throw new BadFormDesignException("Unable to find order type option value: " + defaultValue);
				}
				option.setValue(ot.getOrderTypeId().toString());
				option.setLabel(getLabel(option.getLabel(), ot.getName()));
				config.getDrugOrderField().addOrderTypeAnswer(new OrderTypeAnswer(ot, option.getLabel()));
			}
		}
		if (defaultOrderType != null) {
			for (Option option : options) {
				option.setSelected(option.getValue().equalsIgnoreCase(defaultOrderType.getId().toString()));
			}
		}
		config.setOrderPropertyOptions(property, options);
	}
	
	/**
	 * Processes option by populating default labels if not supplied, validating value inputs, and
	 * populating the DrugOrderField
	 */
	protected void processOrderFrequencyOptions(DrugOrderWidgetConfig config) throws BadFormDesignException {
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
				config.getDrugOrderField().addOrderFrequencyAnswer(new OrderFrequencyAnswer(freq, option.getLabel()));
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
	protected void processConceptOptions(DrugOrderWidgetConfig config, String prop) throws BadFormDesignException {
		List<Option> options = config.getOrderPropertyOptions(prop);
		
		Set<String> valuesAlreadyAdded = new HashSet<>();
		for (Option o : options) {
			valuesAlreadyAdded.add(o.getValue());
		}
		
		List<Concept> fullList = getFullConceptListForProperty(prop);
		
		List<ConceptOptionGroup> optGroups = config.getDrugOrderField().getConceptOptionGroups().get(prop);
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
				
				ObsFieldAnswer a = new ObsFieldAnswer(option.getLabel(), c);
				if ("concept".equalsIgnoreCase(prop)) {
					config.getDrugOrderField().addConceptOption(a);
				} else if ("doseUnits".equalsIgnoreCase(prop)) {
					config.getDrugOrderField().addDoseUnitAnswer(a);
				} else if ("route".equalsIgnoreCase(prop)) {
					config.getDrugOrderField().addRouteAnswer(a);
				} else if ("durationUnits".equalsIgnoreCase(prop)) {
					config.getDrugOrderField().addDurationUnitAnswer(a);
				} else if ("quantityUnits".equalsIgnoreCase(prop)) {
					config.getDrugOrderField().addQuantityUnitAnswer(a);
				} else if ("discontinueReason".equalsIgnoreCase(prop)) {
					config.getDrugOrderField().addDiscontinuedReasonAnswer(a);
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
	
	protected void ensureConceptAndDrugOptionsAreConsistent(DrugOrderWidgetConfig config) {
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
				}
			}
			// If concepts are configured, all associated drugs should be configured
			for (Option conceptOption : concepts) {
				Concept c = allConceptsInFormulary.get(conceptOption.getValue());
				if (conceptsToDrugs.get(c) != null) {
					for (Drug d : conceptsToDrugs.get(c)) {
						if (!existingDrugs.contains(d.getDrugId().toString())) {
							drugs.add(new Option(d.getDisplayName(), d.getId().toString()));
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
