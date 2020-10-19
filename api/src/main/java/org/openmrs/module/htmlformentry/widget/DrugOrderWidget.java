package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.DosingInstructions;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ValidationException;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.util.OpenmrsClassLoader;

/**
 * Holds the widgets used to represent a specific drug order
 */
public class DrugOrderWidget implements Widget {
	
	protected final Log log = LogFactory.getLog(DrugOrderWidget.class);
	
	private final DrugOrderAnswer drugOrderAnswer;
	
	private final DrugOrderWidgetConfig widgetConfig;
	
	private final Map<String, Widget> widgetReplacements = new HashMap<>();
	
	private DrugOrder initialValue;
	
	private Widget drugWidget;
	
	private Widget actionWidget;
	
	private Widget careSettingWidget;
	
	private Widget dosingTypeWidget;
	
	private Widget orderTypeWidget;
	
	private Widget dosingInstructionsWidget;
	
	private Widget doseWidget;
	
	private Widget doseUnitsWidget;
	
	private Widget routeWidget;
	
	private Widget frequencyWidget;
	
	private Widget asNeededWidget;
	
	private Widget instructionsWidget;
	
	private Widget urgencyWidget;
	
	private Widget dateActivatedWidget;
	
	private Widget scheduledDateWidget;
	
	private Widget durationWidget;
	
	private Widget durationUnitsWidget;
	
	private Widget quantityWidget;
	
	private Widget quantityUnitsWidget;
	
	private Widget numRefillsWidget;
	
	private Widget voidedWidget;
	
	private Widget discontinueReasonWidget;
	
	public DrugOrderWidget(FormEntryContext context, DrugOrderAnswer drugOrderAnswer, DrugOrderWidgetConfig widgetConfig) {
		this.drugOrderAnswer = drugOrderAnswer;
		this.widgetConfig = widgetConfig;
		configureDrugWidget(context);
		configureActionWidget(context);
		configureCareSettingWidget(context);
		configureDosingTypeWidget(context);
		configureOrderTypeWidget(context);
		configureDosingInstructionsWidget(context);
		configureDoseWidget(context);
		configureDoseUnitsWidget(context);
		configureRouteWidget(context);
		configureFrequencyWidget(context);
		configureAsNeededWidget(context);
		configureInstructionsWidget(context);
		configureUrgencyWidget(context);
		configureDateActivatedWidget(context);
		configureScheduledDateWidget(context);
		configureDurationWidget(context);
		configureDurationUnitsWidget(context);
		configureQuantityWidget(context);
		configureQuantityUnitsWidget(context);
		configureNumRefillsWidget(context);
		configureVoidedWidget(context);
		configureDiscontinueReasonWidget(context);
	}
	
	protected void configureDrugWidget(FormEntryContext context) {
		HiddenFieldWidget w = new HiddenFieldWidget();
		w.addAttribute("class", "order-property drug");
		w.setInitialValue(drugOrderAnswer.getDrug().getId().toString());
		w.setLabel(drugOrderAnswer.getDisplayName());
		drugWidget = w;
		registerWidget(context, w, new ErrorWidget(), "drug");
	}
	
	protected Drug getDrugWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return HtmlFormEntryUtil.getDrug((String) drugWidget.getValue(context, request));
	}
	
	protected void configureActionWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("action");
		DropdownWidget w = new DropdownWidget();
		w.addOption(new Option("", "", true));
		for (Order.Action a : Order.Action.values()) {
			w.addOption(new Option(a.name(), a.name(), false));
		}
		w.setInitialValue(config.get("value"));
		actionWidget = w;
		registerWidget(context, w, new ErrorWidget(), "action");
	}
	
	protected Order.Action getActionWidgetValue(FormEntryContext context, HttpServletRequest request) {
		String val = (String) actionWidget.getValue(context, request);
		return (StringUtils.isBlank(val) ? null : Order.Action.valueOf(val.toString()));
	}
	
	protected void configureCareSettingWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("careSetting");
		List<CareSetting> careSettings = Context.getOrderService().getCareSettings(false);
		MetadataDropdownWidget<CareSetting> w = new MetadataDropdownWidget<>(careSettings, "");
		w.setInitialValue(HtmlFormEntryUtil.getCareSetting(config.get("value")));
		careSettingWidget = w;
		registerWidget(context, w, new ErrorWidget(), "careSetting");
	}
	
	protected CareSetting getCareSettingWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return HtmlFormEntryUtil.getCareSetting((String) careSettingWidget.getValue(context, request));
	}
	
	protected void configureDosingTypeWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("dosingType");
		DropdownWidget w = new DropdownWidget();
		w.addOption(new Option("", "", true));
		Class[] arr = { SimpleDosingInstructions.class, FreeTextDosingInstructions.class };
		for (Class c : arr) {
			w.addOption(new Option(c.getSimpleName(), c.getName(), false));
		}
		w.setInitialValue(config.get("value"));
		dosingTypeWidget = w;
		registerWidget(context, w, new ErrorWidget(), "dosingType");
	}
	
	protected Class<? extends DosingInstructions> getDosingTypeWidgetValue(FormEntryContext ctx, HttpServletRequest req) {
		String val = (String) dosingTypeWidget.getValue(ctx, req);
		if (StringUtils.isBlank(val)) {
			return null;
		}
		try {
			return (Class<? extends DosingInstructions>) OpenmrsClassLoader.getInstance().loadClass(val.toString());
		}
		catch (Exception e) {
			throw new ValidationException("Dosing Type of " + val + " is not found");
		}
	}
	
	protected void configureOrderTypeWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("orderType");
		List<OrderType> orderTypes = HtmlFormEntryUtil.getDrugOrderTypes();
		MetadataDropdownWidget<OrderType> w = new MetadataDropdownWidget<>(orderTypes, null);
		String defaultVal = config.get("value");
		if (defaultVal != null) {
			w.setInitialMetadataValue(HtmlFormEntryUtil.getOrderType(defaultVal));
		} else {
			w.setInitialMetadataValue(HtmlFormEntryUtil.getDrugOrderType());
		}
		orderTypeWidget = w;
		registerWidget(context, w, new ErrorWidget(), "orderType");
	}
	
	protected OrderType getOrderTypeWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return HtmlFormEntryUtil.getOrderType((String) orderTypeWidget.getValue(context, request));
	}
	
	protected void configureDosingInstructionsWidget(FormEntryContext context) {
		dosingInstructionsWidget = configureTextWidget(context, "dosingInstructions");
	}
	
	protected String getDosingInstructionsWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return (String) dosingInstructionsWidget.getValue(context, request);
	}
	
	protected void configureDoseWidget(FormEntryContext context) {
		doseWidget = configureNumericWidget(context, "dose", true);
	}
	
	protected Double getDoseWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return (Double) doseWidget.getValue(context, request);
	}
	
	protected void configureDoseUnitsWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("doseUnits");
		List<Concept> concepts = Context.getOrderService().getDrugDosingUnits();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		doseUnitsWidget = w;
		registerWidget(context, w, new ErrorWidget(), "doseUnits");
	}
	
	protected Concept getDoseUnitsWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((ConceptDropdownWidget) doseUnitsWidget).getConceptValue(context, request);
	}
	
	protected void configureRouteWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("route");
		List<Concept> concepts = Context.getOrderService().getDrugRoutes();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		routeWidget = w;
		registerWidget(context, w, new ErrorWidget(), "route");
	}
	
	protected Concept getRouteWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((ConceptDropdownWidget) routeWidget).getConceptValue(context, request);
	}
	
	protected void configureFrequencyWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("frequency");
		List<OrderFrequency> frequencies = Context.getOrderService().getOrderFrequencies(false);
		MetadataDropdownWidget<OrderFrequency> w = new MetadataDropdownWidget<>(frequencies, "");
		w.setInitialMetadataValue(HtmlFormEntryUtil.getOrderFrequency(config.get("value")));
		frequencyWidget = w;
		registerWidget(context, w, new ErrorWidget(), "frequency");
	}
	
	protected OrderFrequency getFrequencyWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((MetadataDropdownWidget<OrderFrequency>) frequencyWidget).getMetadataValue(context, request);
	}
	
	protected void configureAsNeededWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("asNeeded");
		String checkboxValue = config.getOrDefault("value", "true");
		CheckboxWidget w = new CheckboxWidget(config.get("label"), checkboxValue);
		asNeededWidget = w;
		registerWidget(context, w, new ErrorWidget(), "asNeeded");
	}
	
	protected boolean getAsNeededWidgetValue(FormEntryContext context, HttpServletRequest request) {
		Object val = asNeededWidget.getValue(context, request);
		return (val != null && !val.equals(""));
	}
	
	protected void configureInstructionsWidget(FormEntryContext context) {
		instructionsWidget = configureTextWidget(context, "instructions");
	}
	
	protected String getInstructionsWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return (String) instructionsWidget.getValue(context, request);
	}
	
	protected void configureUrgencyWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("urgency");
		DropdownWidget w = new DropdownWidget();
		w.addOption(new Option("", "", true));
		for (Order.Urgency u : Order.Urgency.values()) {
			w.addOption(new Option(u.name(), u.name(), false));
		}
		w.setInitialValue(config.get("value"));
		urgencyWidget = w;
		registerWidget(context, w, new ErrorWidget(), "urgency");
	}
	
	protected Order.Urgency getUrgencyWidgetValue(FormEntryContext context, HttpServletRequest request) {
		String val = (String) urgencyWidget.getValue(context, request);
		return (StringUtils.isBlank(val) ? null : Order.Urgency.valueOf(val));
	}
	
	protected void configureDateActivatedWidget(FormEntryContext context) {
		DateWidget w = new DateWidget();
		dateActivatedWidget = w;
		registerWidget(context, w, new ErrorWidget(), "dateActivated");
	}
	
	protected Date getDateActivatedWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((DateWidget) dateActivatedWidget).getValue(context, request);
	}
	
	protected void configureScheduledDateWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("scheduledDate");
		DateWidget w = new DateWidget();
		scheduledDateWidget = w;
		registerWidget(context, w, new ErrorWidget(), "scheduledDate");
	}
	
	protected Date getScheduledDateWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((DateWidget) scheduledDateWidget).getValue(context, request);
	}
	
	protected void configureDurationWidget(FormEntryContext context) {
		durationWidget = configureNumericWidget(context, "duration", false);
	}
	
	protected Integer getDurationWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return (Integer) durationWidget.getValue(context, request);
	}
	
	protected void configureDurationUnitsWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("durationUnits");
		List<Concept> concepts = Context.getOrderService().getDurationUnits();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		durationUnitsWidget = w;
		registerWidget(context, w, new ErrorWidget(), "durationUnits");
	}
	
	protected Concept getDurationUnitsWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((ConceptDropdownWidget) durationUnitsWidget).getConceptValue(context, request);
	}
	
	protected void configureQuantityWidget(FormEntryContext context) {
		quantityWidget = configureNumericWidget(context, "quantity", true);
	}
	
	protected Double getQuantityWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return (Double) quantityWidget.getValue(context, request);
	}
	
	protected void configureQuantityUnitsWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("quantityUnits");
		List<Concept> concepts = Context.getOrderService().getDrugDispensingUnits();
		ConceptDropdownWidget w = new ConceptDropdownWidget(concepts, "");
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		quantityUnitsWidget = w;
		registerWidget(context, w, new ErrorWidget(), "quantityUnits");
	}
	
	protected Concept getQuantityUnitsWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((ConceptDropdownWidget) quantityUnitsWidget).getConceptValue(context, request);
	}
	
	protected void configureNumRefillsWidget(FormEntryContext context) {
		String p = "numRefills";
		numRefillsWidget = configureNumericWidget(context, p, false);
	}
	
	protected Integer getNumRefillsWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return (Integer) numRefillsWidget.getValue(context, request);
	}
	
	protected void configureVoidedWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("voided");
		String checkboxValue = config.getOrDefault("value", "true");
		CheckboxWidget w = new CheckboxWidget(config.get("label"), checkboxValue);
		voidedWidget = w;
		registerWidget(context, w, new ErrorWidget(), "voided");
	}
	
	protected boolean getVoidedWidgetValue(FormEntryContext context, HttpServletRequest request) {
		Object val = voidedWidget.getValue(context, request);
		return (val != null && !val.equals(""));
	}
	
	protected void configureDiscontinueReasonWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getTemplateConfig("discontinueReason");
		List<ObsFieldAnswer> reasons = widgetConfig.getDrugOrderField().getDiscontinuedReasonAnswers();
		ConceptDropdownWidget w = new ConceptDropdownWidget(reasons);
		w.setInitialConceptValue(HtmlFormEntryUtil.getConcept(config.get("value")));
		discontinueReasonWidget = w;
		registerWidget(context, w, new ErrorWidget(), "discontinueReason");
	}
	
	protected Concept getDiscontinueReasonWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((ConceptDropdownWidget) discontinueReasonWidget).getConceptValue(context, request);
	}
	
	protected String registerWidget(FormEntryContext context, Widget widget, ErrorWidget errorWidget, String propertyName) {
		String widgetId = context.registerWidget(widget);
		context.registerErrorWidget(widget, errorWidget);
		widgetReplacements.put(propertyName, widget);
		return widgetId;
	}
	
	protected Widget configureTextWidget(FormEntryContext context, String property) {
		Map<String, String> config = widgetConfig.getTemplateConfig(property);
		TextFieldWidget w = new TextFieldWidget();
		w.setInitialValue(config.get("value"));
		if (config.get("textArea") != null) {
			w.setTextArea(Boolean.parseBoolean(config.get("textArea")));
		}
		if (config.get("textAreaRows") != null) {
			w.setTextAreaRows(Integer.parseInt(config.get("textAreaRows")));
		}
		if (config.get("textAreaColumns") != null) {
			w.setTextAreaColumns(Integer.parseInt(config.get("textAreaColumns")));
		}
		if (config.get("textFieldSize") != null) {
			w.setTextFieldSize(Integer.parseInt(config.get("textFieldSize")));
		}
		if (config.get("textFieldMaxLength") != null) {
			w.setTextFieldSize(Integer.parseInt(config.get("textFieldMaxLength")));
		}
		if (config.get("placeholder") != null) {
			w.setPlaceholder(config.get("placeholder"));
		}
		registerWidget(context, w, new ErrorWidget(), property);
		return w;
	}
	
	protected Widget configureNumericWidget(FormEntryContext context, String property, boolean allowDecimal) {
		Map<String, String> config = widgetConfig.getTemplateConfig(property);
		NumberFieldWidget w = new NumberFieldWidget(0d, null, allowDecimal);
		String defaultVal = config.get("value");
		if (defaultVal != null) {
			if (allowDecimal) {
				w.setInitialValue(Double.parseDouble(defaultVal));
			} else {
				w.setInitialValue(Integer.parseInt(defaultVal));
			}
		}
		registerWidget(context, w, new ErrorWidget(), property);
		return w;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		String ret = getWidgetConfig().getTemplateContent();
		for (String property : widgetReplacements.keySet()) {
			Widget w = widgetReplacements.get(property);
			Map<String, String> c = widgetConfig.getTemplateConfig(property);
			if (c != null) {
				String key = c.toString();
				ret = ret.replace(key, w.generateHtml(context));
			}
		}
		return ret;
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = ((DrugOrder) initialValue);
		// TODO: Populate initial values for various widgets using properties of initialValue if non-null
	}
	
	public DrugOrder getInitialValue() {
		return initialValue;
	}
	
	@Override
	public DrugOrderWidgetValue getValue(FormEntryContext context, HttpServletRequest request) {
		DrugOrderWidgetValue ret = new DrugOrderWidgetValue();
		ret.setPreviousDrugOrder(initialValue);
		Order.Action action = getActionWidgetValue(context, request);
		if (action != null) {
			DrugOrder drugOrder = new DrugOrder();
			drugOrder.setDrug(drugOrderAnswer.getDrug());
			drugOrder.setPreviousOrder(initialValue);
			drugOrder.setAction(action);
			drugOrder.setCareSetting(getCareSettingWidgetValue(context, request));
			drugOrder.setDosingType(getDosingTypeWidgetValue(context, request));
			drugOrder.setOrderType(getOrderTypeWidgetValue(context, request));
			drugOrder.setDosingInstructions(getDosingInstructionsWidgetValue(context, request));
			drugOrder.setDose(getDoseWidgetValue(context, request));
			drugOrder.setDoseUnits(getDoseUnitsWidgetValue(context, request));
			drugOrder.setRoute(getRouteWidgetValue(context, request));
			drugOrder.setFrequency(getFrequencyWidgetValue(context, request));
			drugOrder.setAsNeeded(getAsNeededWidgetValue(context, request));
			drugOrder.setInstructions(getInstructionsWidgetValue(context, request));
			drugOrder.setUrgency(getUrgencyWidgetValue(context, request));
			drugOrder.setDateActivated(getDateActivatedWidgetValue(context, request));
			drugOrder.setScheduledDate(getScheduledDateWidgetValue(context, request));
			drugOrder.setDuration(getDurationWidgetValue(context, request));
			drugOrder.setDurationUnits(getDurationUnitsWidgetValue(context, request));
			drugOrder.setQuantity(getQuantityWidgetValue(context, request));
			drugOrder.setQuantityUnits(getQuantityUnitsWidgetValue(context, request));
			drugOrder.setNumRefills(getNumRefillsWidgetValue(context, request));
			if (action == Order.Action.DISCONTINUE) {
				drugOrder.setOrderReason(getDiscontinueReasonWidgetValue(context, request));
			}
			ret.setNewDrugOrder(drugOrder);
		}
		ret.setVoidPreviousOrder(getVoidedWidgetValue(context, request));
		return ret;
	}
	
	public DrugOrderWidgetConfig getWidgetConfig() {
		return widgetConfig;
	}
	
	public Map<String, Widget> getWidgetReplacements() {
		return widgetReplacements;
	}
	
	public Widget getDrugWidget() {
		return drugWidget;
	}
	
	public Widget getActionWidget() {
		return actionWidget;
	}
	
	public Widget getCareSettingWidget() {
		return careSettingWidget;
	}
	
	public Widget getDosingTypeWidget() {
		return dosingTypeWidget;
	}
	
	public Widget getOrderTypeWidget() {
		return orderTypeWidget;
	}
	
	public Widget getDosingInstructionsWidget() {
		return dosingInstructionsWidget;
	}
	
	public Widget getDoseWidget() {
		return doseWidget;
	}
	
	public Widget getDoseUnitsWidget() {
		return doseUnitsWidget;
	}
	
	public Widget getRouteWidget() {
		return routeWidget;
	}
	
	public Widget getFrequencyWidget() {
		return frequencyWidget;
	}
	
	public Widget getAsNeededWidget() {
		return asNeededWidget;
	}
	
	public Widget getInstructionsWidget() {
		return instructionsWidget;
	}
	
	public Widget getUrgencyWidget() {
		return urgencyWidget;
	}
	
	public Widget getDateActivatedWidget() {
		return dateActivatedWidget;
	}
	
	public Widget getScheduledDateWidget() {
		return scheduledDateWidget;
	}
	
	public Widget getDurationWidget() {
		return durationWidget;
	}
	
	public Widget getDurationUnitsWidget() {
		return durationUnitsWidget;
	}
	
	public Widget getQuantityWidget() {
		return quantityWidget;
	}
	
	public Widget getQuantityUnitsWidget() {
		return quantityUnitsWidget;
	}
	
	public Widget getNumRefillsWidget() {
		return numRefillsWidget;
	}
	
	public Widget getVoidedWidget() {
		return voidedWidget;
	}
	
	public Widget getDiscontinueReasonWidget() {
		return discontinueReasonWidget;
	}
}
