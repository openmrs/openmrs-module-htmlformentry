package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.HibernateUtil;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.util.JsonObject;

public class DrugOrdersWidget implements Widget {
	
	private static final Log log = LogFactory.getLog(DrugOrdersWidget.class);
	
	private DrugOrderWidgetConfig widgetConfig;
	
	private final Map<String, Widget> widgets = new LinkedHashMap<>();
	
	private Map<Drug, List<DrugOrder>> initialValue;
	
	public DrugOrdersWidget(FormEntryContext context, DrugOrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
		registerWidget(context, new HiddenFieldWidget(), new ErrorWidget(), "drug");
		configureOptionWidget(context, "action", "dropdown");
		registerWidget(context, new HiddenFieldWidget(), new ErrorWidget(), "previousOrder");
		configureOptionWidget(context, "careSetting", "dropdown");
		configureDosingTypeWidget(context);
		configureOptionWidget(context, "orderType", "dropdown");
		configureTextWidget(context, "dosingInstructions");
		configureNumericWidget(context, "dose", true);
		configureOptionWidget(context, "doseUnits", "dropdown");
		configureOptionWidget(context, "route", "dropdown");
		configureOptionWidget(context, "frequency", "dropdown");
		configureCheckboxWidget(context, "asNeeded");
		configureTextWidget(context, "instructions");
		configureOptionWidget(context, "urgency", "radio");
		registerWidget(context, new DateWidget(), new ErrorWidget(), "dateActivated");
		registerWidget(context, new DateWidget(), new ErrorWidget(), "scheduledDate");
		configureNumericWidget(context, "duration", false);
		configureOptionWidget(context, "durationUnits", "dropdown");
		configureNumericWidget(context, "quantity", true);
		configureOptionWidget(context, "quantityUnits", "dropdown");
		configureNumericWidget(context, "numRefills", false);
		configureCheckboxWidget(context, "voided");
		configureOptionWidget(context, "discontinueReason", "dropdown");
	}
	
	@Override
	public void setInitialValue(Object v) {
		initialValue = (Map<Drug, List<DrugOrder>>) v;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		
		StopWatch sw = new StopWatch();
		sw.start();
		log.trace("In DrugOrdersWidget.generateHtml");
		
		CapturingPrintWriter writer = new CapturingPrintWriter();
		String fieldName = context.getFieldName(this);
		
		// Wrap the entire widget in a div
		writer.println("<div id=\"" + fieldName + "\" class=\"drugorders-element\">");
		
		// Add a section to the orders section
		writer.println("<div id=\"" + fieldName + "_header\" class=\"drugorders-header-section\"></div>");
		
		// Add a section that will contain the selected drug orders
		writer.println("<div id=\"" + fieldName + "_orders\" class=\"drugorders-order-section\"></div>");
		
		// Add a section that contains the order form template to use for entering orders
		writer.println("<div id=\"" + fieldName + "_template\" class=\"drugorders-order-form\" style=\"display:none;\">");
		String templateContent = getWidgetConfig().getTemplateContent();
		for (String property : widgets.keySet()) {
			Widget w = widgets.get(property);
			Map<String, String> c = widgetConfig.getAttributes(property);
			if (c != null) {
				String key = c.toString();
				StringBuilder replacement = new StringBuilder();
				String label = translate("htmlformentry.drugOrder." + property);
				replacement.append("<div class=\"order-field ").append(property).append("\">");
				replacement.append("<div class=\"order-field-label ").append(property).append("\">");
				replacement.append(label);
				replacement.append("</div>");
				replacement.append("<div class=\"order-field-widget ").append(property).append("\">");
				replacement.append(w.generateHtml(context));
				replacement.append("</div>");
				replacement.append("</div>");
				templateContent = templateContent.replace(key, replacement.toString());
			}
		}
		writer.println(templateContent);
		writer.println("</div>");
		
		// Establish a json config that initializes the javascript-based widget
		Integer patId = context.getExistingPatient().getPatientId();
		Integer encId = context.getExistingEncounter() == null ? null : context.getExistingEncounter().getEncounterId();
		
		JsonObject jsonConfig = new JsonObject();
		jsonConfig.addString("fieldName", fieldName);
		jsonConfig.addString("today", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		jsonConfig.addString("patientId", patId.toString());
		jsonConfig.addString("encounterId", encId == null ? "" : encId.toString());
		jsonConfig.addString("mode", context.getMode().name());
		if (widgetConfig.getAttributes() != null) {
			for (String att : widgetConfig.getAttributes().keySet()) {
				jsonConfig.addString(att, widgetConfig.getAttributes().get(att));
			}
		}
		
		// In order to re-configure date widgets, add some additional configuration
		DateWidget w = (DateWidget) widgets.getOrDefault("dateActivated", new DateWidget());
		JsonObject dateConfig = jsonConfig.addObject("dateWidgetConfig");
		dateConfig.addString("dateFormat", w.jsDateFormat());
		dateConfig.addString("yearsRange", w.getYearsRange());
		dateConfig.addString("locale", w.getLocaleForJquery());
		
		JsonObject jsonDrugWidgets = jsonConfig.addObject("widgets");
		for (String key : widgets.keySet()) {
			jsonDrugWidgets.addString(key, context.getFieldName(widgets.get(key)));
		}
		
		// Add any translations needed by the default views
		String prefix = "htmlformentry.drugOrder.";
		JsonObject translations = jsonConfig.addObject("translations");
		translations.addTranslation(prefix, "asNeeded");
		translations.addTranslation(prefix, "previousOrder");
		translations.addTranslation(prefix, "present");
		translations.addTranslation(prefix, "noOrders");
		translations.addTranslation(prefix, "chooseDrug");
		
		DrugOrderField field = widgetConfig.getDrugOrderField();
		
		// Add a section for each drug configured in the tag.  Hide these sections if appropriate
		for (DrugOrderAnswer doa : field.getDrugOrderAnswers()) {
			Drug drug = doa.getDrug();
			String drugLabel = doa.getDisplayName();
			
			// For each rendered drugOrderWidget, add configuration of that widget into json for javascript
			JsonObject jsonDrug = jsonConfig.addObjectToArray("drugs");
			jsonDrug.addString("drugId", drug.getId().toString());
			jsonDrug.addString("drugLabel", drugLabel);
			List<JsonObject> history = jsonDrug.getObjectArray("history");
			
			if (initialValue != null) {
				for (DrugOrder d : initialValue.getOrDefault(drug, new ArrayList<>())) {
					Order pd = d.getPreviousOrder();
					JsonObject jho = new JsonObject();
					jho.addString("orderId", d.getOrderId().toString());
					jho.addString("encounterId", d.getEncounter().getEncounterId().toString());
					jho.addString("previousOrderId", pd == null ? "" : pd.getOrderId().toString());
					jho.addIdAndLabel("action", "value", "display", d.getAction());
					jho.addIdAndLabel("drug", "value", "display", d.getDrug());
					jho.addIdAndLabel("careSetting", "value", "display", d.getCareSetting());
					jho.addIdAndLabel("dosingType", "value", "display", d.getDosingType());
					jho.addIdAndLabel("orderType", "value", "display", d.getOrderType());
					jho.addIdAndLabel("dosingInstructions", "value", "display", d.getDosingInstructions());
					jho.addIdAndLabel("dose", "value", "display", d.getDose());
					jho.addIdAndLabel("doseUnits", "value", "display", d.getDoseUnits());
					jho.addIdAndLabel("route", "value", "display", d.getRoute());
					jho.addIdAndLabel("frequency", "value", "display", d.getFrequency());
					jho.addIdAndLabel("asNeeded", "value", "display", d.getAsNeeded());
					jho.addIdAndLabel("instructions", "value", "display", d.getInstructions());
					jho.addIdAndLabel("urgency", "value", "display", d.getUrgency());
					jho.addIdAndLabel("dateActivated", "value", "display", d.getDateActivated());
					jho.addIdAndLabel("scheduledDate", "value", "display", d.getScheduledDate());
					jho.addIdAndLabel("effectiveStartDate", "value", "display", d.getEffectiveStartDate());
					jho.addIdAndLabel("duration", "value", "display", d.getDuration());
					jho.addIdAndLabel("durationUnits", "value", "display", d.getDurationUnits());
					jho.addIdAndLabel("autoExpireDate", "value", "display", d.getAutoExpireDate());
					jho.addIdAndLabel("dateStopped", "value", "display", d.getDateStopped());
					jho.addIdAndLabel("effectiveStopDate", "value", "display", d.getEffectiveStopDate());
					jho.addIdAndLabel("quantity", "value", "display", d.getQuantity());
					jho.addIdAndLabel("quantityUnits", "value", "display", d.getQuantityUnits());
					jho.addIdAndLabel("numRefills", "value", "display", d.getNumRefills());
					jho.addIdAndLabel("orderReason", "value", "display", d.getOrderReason());
					history.add(jho);
				}
			}
		}
		
		// Add javascript function to initialize widget as appropriate
		String defaultLoadFn = "htmlForm.initializeDrugOrdersWidgets";
		String onLoadFn = widgetConfig.getAttributes().getOrDefault("onLoadFunction", defaultLoadFn);
		writer.println("<script type=\"text/javascript\">");
		writer.println("jQuery(function() { " + onLoadFn + "(");
		writer.println(jsonConfig.toJson());
		writer.println(")});");
		writer.println("</script>");
		
		writer.println("</div>");
		
		sw.stop();
		log.trace("DrugOrdersWidget.generateHtml: " + sw.toString());
		
		return writer.getContent();
	}
	
	@Override
	public List<DrugOrderWidgetValue> getValue(FormEntryContext c, HttpServletRequest r) {
		List<DrugOrderWidgetValue> ret = new ArrayList<>();
		OrderType defOrderType = HtmlFormEntryUtil.getDrugOrderType();
		for (DrugOrderAnswer doa : getDrugOrderField().getDrugOrderAnswers()) {
			Drug d = doa.getDrug();
			Order.Action action = parseValue(getValue(c, r, d, "action"), Order.Action.class);
			DrugOrder previousOrder = parseValue(getValue(c, r, d, "previousOrder"), DrugOrder.class);
			boolean voidPrevious = parseValue(getValue(c, r, d, "voided"), Boolean.class, false);
			if (action != null || voidPrevious) {
				DrugOrderWidgetValue v = new DrugOrderWidgetValue();
				v.setPreviousDrugOrder(previousOrder);
				if (voidPrevious) {
					v.setVoidPreviousOrder(true);
					log.trace("User requested to void previous order for: " + d.getDisplayName());
				}
				if (action != null) {
					log.trace("User requested to place a " + action + "drug order for: " + d.getDisplayName());
					DrugOrder drugOrder = new DrugOrder();
					drugOrder.setDrug(d);
					drugOrder.setPreviousOrder(previousOrder);
					drugOrder.setAction(action);
					drugOrder.setCareSetting(parseValue(getValue(c, r, d, "careSetting"), CareSetting.class));
					drugOrder.setDosingType(parseValue(getValue(c, r, d, "dosingType"), Class.class));
					drugOrder.setOrderType(parseValue(getValue(c, r, d, "orderType"), OrderType.class, defOrderType));
					drugOrder.setDosingInstructions(getValue(c, r, d, "dosingInstructions"));
					drugOrder.setDose(parseValue(getValue(c, r, d, "dose"), Double.class));
					drugOrder.setDoseUnits(parseValue(getValue(c, r, d, "doseUnits"), Concept.class));
					drugOrder.setRoute(parseValue(getValue(c, r, d, "route"), Concept.class));
					drugOrder.setFrequency(parseValue(getValue(c, r, d, "frequency"), OrderFrequency.class));
					drugOrder.setAsNeeded(parseValue(getValue(c, r, d, "asNeeded"), Boolean.class, false));
					drugOrder.setInstructions(getValue(c, r, d, "instructions"));
					drugOrder.setUrgency(parseValue(getValue(c, r, d, "urgency"), Order.Urgency.class));
					drugOrder.setDateActivated(parseValue(getValue(c, r, d, "dateActivated"), Date.class));
					drugOrder.setScheduledDate(parseValue(getValue(c, r, d, "scheduledDate"), Date.class));
					drugOrder.setDuration(parseValue(getValue(c, r, d, "duration"), Integer.class));
					drugOrder.setDurationUnits(parseValue(getValue(c, r, d, "durationUnits"), Concept.class));
					drugOrder.setQuantity(parseValue(getValue(c, r, d, "quantity"), Double.class));
					drugOrder.setQuantityUnits(parseValue(getValue(c, r, d, "quantityUnits"), Concept.class));
					drugOrder.setNumRefills(parseValue(getValue(c, r, d, "numRefills"), Integer.class));
					if (action == Order.Action.DISCONTINUE) {
						drugOrder.setOrderReason(parseValue(getValue(c, r, d, "discontinueReason"), Concept.class));
					}
					v.setNewDrugOrder(drugOrder);
				}
				ret.add(v);
			}
		}
		return ret;
	}
	
	protected String getValue(FormEntryContext context, HttpServletRequest req, Drug drug, String property) {
		try {
			Widget w = widgets.get(property);
			String requestParam = context.getFieldName(w) + "_" + drug.getId();
			String value = req.getParameter(requestParam);
			return value;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to get value from request", e);
		}
	}
	
	/**
	 * This method intends to ensure that if a value is configured, that it is parsed correctly and set
	 * If it is configured but does not return a valid object, it leads to an error
	 */
	protected <T> T parseValue(String value, Class<T> type) {
		return parseValue(value, type, null);
	}
	
	/**
	 * This method intends to ensure that if a value is configured, that it is parsed correctly and set
	 * If it is configured but does not return a valid object, it leads to an error
	 */
	protected <T> T parseValue(String value, Class<T> type, T defaultValue) {
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}
		try {
			Object ret = null;
			if (type == String.class) {
				ret = (T) value;
			} else if (type == Boolean.class) {
				ret = StringUtils.isNotBlank(value);
			} else if (type == Concept.class) {
				ret = HtmlFormEntryUtil.getConcept(value);
			} else if (type == Drug.class) {
				ret = HtmlFormEntryUtil.getDrug(value);
			} else if (type == OrderFrequency.class) {
				ret = HtmlFormEntryUtil.getOrderFrequency(value);
			} else if (type == OrderType.class) {
				ret = HtmlFormEntryUtil.getOrderType(value);
			} else if (type == CareSetting.class) {
				ret = HtmlFormEntryUtil.getCareSetting(value);
			} else if (type == Date.class) {
				ret = new SimpleDateFormat("yyyy-MM-dd").parse(value);
			} else if (type == Double.class) {
				ret = Double.valueOf(value);
			} else if (type == Integer.class) {
				ret = Integer.valueOf(value);
			} else if (type == Order.Action.class) {
				ret = Order.Action.valueOf(value);
			} else if (type == Order.Urgency.class) {
				ret = Order.Urgency.valueOf(value);
			} else if (type == Class.class) {
				ret = Context.loadClass(value);
			} else if (type == DrugOrder.class) {
				Integer orderId = Integer.parseInt(value);
				Order order = Context.getOrderService().getOrder(orderId);
				ret = HibernateUtil.getRealObjectFromProxy(order);
			}
			if (ret == null) {
				throw new IllegalArgumentException("Unable to get " + type + " with value: " + value);
			}
			return (T) ret;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to get value from request", e);
		}
	}
	
	protected String registerWidget(FormEntryContext context, Widget widget, ErrorWidget errorWidget, String propertyName) {
		String widgetId = context.registerWidget(widget);
		context.registerErrorWidget(widget, errorWidget);
		widgets.put(propertyName, widget);
		return widgetId;
	}
	
	protected CheckboxWidget configureCheckboxWidget(FormEntryContext context, String property) {
		Map<String, String> config = widgetConfig.getAttributes(property);
		String checkboxValue = config.getOrDefault("value", "true");
		CheckboxWidget w = new CheckboxWidget(config.get("label"), checkboxValue);
		registerWidget(context, w, new ErrorWidget(), property);
		return w;
	}
	
	protected TextFieldWidget configureTextWidget(FormEntryContext context, String property) {
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
	
	protected NumberFieldWidget configureNumericWidget(FormEntryContext context, String property, boolean allowDecimal) {
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
	
	protected SingleOptionWidget configureOptionWidget(FormEntryContext context, String property, String defaultType) {
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
				if (w instanceof DropdownWidget) {
					w.addOption(new Option("", "", false));
				}
				for (Option o : widgetConfig.getOrderPropertyOptions(property)) {
					w.addOption(o);
				}
			}
		}
		registerWidget(context, w, new ErrorWidget(), property);
		return w;
	}
	
	protected RadioButtonsWidget configureDosingTypeWidget(FormEntryContext context) {
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
		registerWidget(context, w, new ErrorWidget(), "dosingType");
		return w;
	}
	
	protected String translate(String code) {
		return Context.getMessageSourceService().getMessage(code);
	}
	
	public DrugOrderField getDrugOrderField() {
		return widgetConfig.getDrugOrderField();
	}
	
	public DrugOrderWidgetConfig getWidgetConfig() {
		return widgetConfig;
	}
	
	public void setWidgetConfig(DrugOrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
	}
}
