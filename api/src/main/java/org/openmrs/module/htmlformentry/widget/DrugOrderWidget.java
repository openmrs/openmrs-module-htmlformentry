package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.DosingInstructions;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ValidationException;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
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
		
		StopWatch sw = new StopWatch();
		sw.start();
		log.trace("In DrugOrderWidget.constructor");
		
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
		
		sw.stop();
		log.trace("DrugOrderWidget.constructor: " + sw.toString());
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		StopWatch sw = new StopWatch();
		sw.start();
		log.trace("In DrugOrderWidget.generateHtml");
		String ret = getWidgetConfig().getTemplateContent();
		for (String property : widgetReplacements.keySet()) {
			Widget w = widgetReplacements.get(property);
			Map<String, String> c = widgetConfig.getAttributes(property);
			if (c != null) {
				String key = c.toString();
				StringBuilder replacement = new StringBuilder();
				String label = translate("htmlformentry.drugOrder." + property);
				replacement.append("<div class=\"order-field " + property + "\">");
				replacement.append("<div class=\"order-field-label " + property + "\">").append(label).append("</div>");
				replacement.append("<div class=\"order-field-widget " + property + "\">");
				replacement.append(w.generateHtml(context));
				replacement.append("</div>");
				replacement.append("</div>");
				ret = ret.replace(key, replacement.toString());
			}
		}
		sw.stop();
		log.trace("DrugOrdersWidget.generateHtml: " + sw.toString());
		return ret;
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		DrugOrder d = (DrugOrder) initialValue;
		this.initialValue = d;
		if (d != null) {
			setInitialValue(drugWidget, d.getDrug());
			setInitialValue(actionWidget, d.getAction());
			setInitialValue(careSettingWidget, d.getCareSetting());
			setInitialValue(dosingTypeWidget, d.getDosingType());
			setInitialValue(orderTypeWidget, d.getOrderType());
			setInitialValue(dosingInstructionsWidget, d.getDosingInstructions());
			setInitialValue(doseWidget, d.getDose());
			setInitialValue(doseUnitsWidget, d.getDoseUnits());
			setInitialValue(routeWidget, d.getRoute());
			setInitialValue(frequencyWidget, d.getFrequency());
			setInitialValue(asNeededWidget, d.getAsNeeded());
			setInitialValue(instructionsWidget, d.getInstructions());
			setInitialValue(urgencyWidget, d.getUrgency());
			setInitialValue(dateActivatedWidget, d.getDateActivated());
			setInitialValue(scheduledDateWidget, d.getScheduledDate());
			setInitialValue(durationWidget, d.getDuration());
			setInitialValue(durationUnitsWidget, d.getDurationUnits());
			setInitialValue(quantityWidget, d.getQuantity());
			setInitialValue(quantityUnitsWidget, d.getQuantityUnits());
			setInitialValue(numRefillsWidget, d.getNumRefills());
			setInitialValue(voidedWidget, d.getVoided());
			setInitialValue(discontinueReasonWidget, d.getOrderReason());
		}
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
	
	protected void configureDrugWidget(FormEntryContext context) {
		HiddenFieldWidget w = new HiddenFieldWidget();
		w.setInitialValue(drugOrderAnswer.getDrug().getId().toString());
		drugWidget = w;
		registerWidget(context, w, new ErrorWidget(), "drug");
	}
	
	protected Drug getDrugWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return HtmlFormEntryUtil.getDrug((String) drugWidget.getValue(context, request));
	}
	
	protected void configureActionWidget(FormEntryContext context) {
		String property = "action";
		actionWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, actionWidget, new ErrorWidget(), property);
	}
	
	protected Order.Action getActionWidgetValue(FormEntryContext context, HttpServletRequest request) {
		String val = (String) actionWidget.getValue(context, request);
		return (StringUtils.isBlank(val) ? null : Order.Action.valueOf(val));
	}
	
	protected void configureCareSettingWidget(FormEntryContext context) {
		String property = "careSetting";
		careSettingWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, careSettingWidget, new ErrorWidget(), property);
	}
	
	protected CareSetting getCareSettingWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return HtmlFormEntryUtil.getCareSetting((String) careSettingWidget.getValue(context, request));
	}
	
	protected void configureDosingTypeWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getAttributes("dosingType");
		RadioButtonsWidget w = new RadioButtonsWidget();
		Option simpleOption = new Option();
		simpleOption.setValue(SimpleDosingInstructions.class.getName());
		simpleOption.setLabel(translate("htmlformentry.drugOrder.dosingType.simple"));
		w.addOption(simpleOption);
		Option freeTextOption = new Option();
		freeTextOption.setValue(FreeTextDosingInstructions.class.getName());
		freeTextOption.setLabel(translate("htmlformentry.drugOrder.dosingType.freetext"));
		w.addOption(freeTextOption);
		if (context.getMode() != FormEntryContext.Mode.VIEW) {
			w.setInitialValue(config.get("value"));
		}
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
		String property = "orderType";
		orderTypeWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, orderTypeWidget, new ErrorWidget(), property);
	}
	
	protected OrderType getOrderTypeWidgetValue(FormEntryContext context, HttpServletRequest request) {
		OrderType orderType = HtmlFormEntryUtil.getOrderType((String) orderTypeWidget.getValue(context, request));
		if (orderType == null) {
			orderType = HtmlFormEntryUtil.getDrugOrderType();
		}
		return orderType;
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
		String property = "doseUnits";
		doseUnitsWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, doseUnitsWidget, new ErrorWidget(), property);
	}
	
	protected Concept getDoseUnitsWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((ConceptDropdownWidget) doseUnitsWidget).getConceptValue(context, request);
	}
	
	protected void configureRouteWidget(FormEntryContext context) {
		String property = "route";
		routeWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, routeWidget, new ErrorWidget(), property);
	}
	
	protected Concept getRouteWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((ConceptDropdownWidget) routeWidget).getConceptValue(context, request);
	}
	
	protected void configureFrequencyWidget(FormEntryContext context) {
		String property = "frequency";
		frequencyWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, frequencyWidget, new ErrorWidget(), property);
	}
	
	protected OrderFrequency getFrequencyWidgetValue(FormEntryContext context, HttpServletRequest request) {
		return ((MetadataDropdownWidget<OrderFrequency>) frequencyWidget).getMetadataValue(context, request);
	}
	
	protected void configureAsNeededWidget(FormEntryContext context) {
		Map<String, String> config = widgetConfig.getAttributes("asNeeded");
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
		String property = "urgency";
		urgencyWidget = configureOptionWidget(context, property, "radio");
		registerWidget(context, urgencyWidget, new ErrorWidget(), property);
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
		Map<String, String> config = widgetConfig.getAttributes("scheduledDate");
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
		String property = "durationUnits";
		durationUnitsWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, durationUnitsWidget, new ErrorWidget(), property);
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
		String property = "quantityUnits";
		quantityUnitsWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, quantityUnitsWidget, new ErrorWidget(), property);
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
		Map<String, String> config = widgetConfig.getAttributes("voided");
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
		String property = "discontinueReason";
		discontinueReasonWidget = configureOptionWidget(context, property, "dropdown");
		registerWidget(context, discontinueReasonWidget, new ErrorWidget(), property);
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
		Map<String, String> config = widgetConfig.getAttributes(property);
		TextFieldWidget w = new TextFieldWidget();
		if (context.getMode() != FormEntryContext.Mode.VIEW) {
			w.setInitialValue(config.get("value"));
		}
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
		Map<String, String> config = widgetConfig.getAttributes(property);
		NumberFieldWidget w = new NumberFieldWidget(0d, null, allowDecimal);
		if (context.getMode() != FormEntryContext.Mode.VIEW) {
			String defaultVal = config.get("value");
			if (defaultVal != null) {
				if (allowDecimal) {
					w.setInitialValue(Double.parseDouble(defaultVal));
				} else {
					w.setInitialValue(Integer.parseInt(defaultVal));
				}
			}
		}
		registerWidget(context, w, new ErrorWidget(), property);
		return w;
	}
	
	protected Widget configureOptionWidget(FormEntryContext context, String property, String defaultType) {
		Map<String, String> attrs = widgetConfig.getOrderPropertyAttributes().getOrDefault(property, new HashMap<>());
		String style = attrs.getOrDefault("style", defaultType);
		SingleOptionWidget w = ("radio".equalsIgnoreCase(style) ? new RadioButtonsWidget() : new DropdownWidget());
		List<Option> options = widgetConfig.getOrderPropertyOptions(property);
		if (options != null) {
			// If only one option is configured, do not add an empty option, and select by default
			if (options.size() == 1) {
				Option singleOption = options.get(0);
				singleOption.setSelected(true);
				w.addOption(singleOption);
			} else {
				w.addOption(new Option("", "", false));
				for (Option o : widgetConfig.getOrderPropertyOptions(property)) {
					w.addOption(o);
				}
			}
		}
		return w;
	}
	
	protected void setInitialValue(Widget widget, Object value) {
		if (value == null) {
			widget.setInitialValue(null);
		} else {
			if (value instanceof Date || value instanceof String) {
				widget.setInitialValue(value);
			} else if (value instanceof Number) {
				if (widget instanceof NumberFieldWidget) {
					widget.setInitialValue(value);
				} else {
					widget.setInitialValue(value.toString());
				}
			} else if (value instanceof OpenmrsObject) {
				widget.setInitialValue(((OpenmrsObject) value).getId().toString());
			} else if (value.getClass().isEnum()) {
				widget.setInitialValue(((Enum) value).name());
			} else if (value instanceof Class) {
				widget.setInitialValue(((Class) value).getName());
			} else if (value instanceof Boolean && widget instanceof CheckboxWidget) {
				widget.setInitialValue((Boolean) value ? "true" : null);
			} else {
				widget.setInitialValue(value.toString());
			}
		}
	}
	
	protected String translate(String code) {
		return Context.getMessageSourceService().getMessage(code);
	}
	
	public DrugOrder getInitialValue() {
		return initialValue;
	}
	
	public DrugOrderAnswer getDrugOrderAnswer() {
		return drugOrderAnswer;
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
