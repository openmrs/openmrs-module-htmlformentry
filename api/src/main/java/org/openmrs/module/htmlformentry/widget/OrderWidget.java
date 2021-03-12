package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.handler.OrderTagHandler;
import org.openmrs.module.htmlformentry.schema.OrderField;
import org.openmrs.module.htmlformentry.tag.TagUtil;
import org.openmrs.module.htmlformentry.util.JsonObject;
import org.openmrs.util.OpenmrsUtil;

public class OrderWidget implements Widget {
	
	private static final Log log = LogFactory.getLog(OrderWidget.class);
	
	private OrderWidgetConfig widgetConfig;
	
	private final Map<String, Widget> widgets = new LinkedHashMap<>();
	
	private Map<Concept, List<Order>> initialValue;
	
	public OrderWidget(FormEntryContext context, OrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
		configureOptionWidget(context, "concept", "dropdown");
		configureOptionWidget(context, "action", "dropdown");
		registerWidget(context, new HiddenFieldWidget(), new ErrorWidget(), "previousOrder");
		configureOptionWidget(context, "careSetting", "dropdown");
		configureOptionWidget(context, "orderReason", "dropdown");
		configureTextWidget(context, "orderReasonNonCoded");
		configureTextWidget(context, "instructions");
		configureOptionWidget(context, "urgency", "radio");
		registerWidget(context, new DateWidget(), new ErrorWidget(), "dateActivated");
		registerWidget(context, new DateWidget(), new ErrorWidget(), "scheduledDate");
		configureOptionWidget(context, "discontinueReason", "dropdown");
		configureTextWidget(context, "discontinueReasonNonCoded");
		
		if (isDrugOrder()) {
			configureOptionWidget(context, "drug", "dropdown");
			configureTextWidget(context, "drugNonCoded");
			configureOptionWidget(context, "dosingType", "radio");
			configureTextWidget(context, "dosingInstructions");
			configureNumericWidget(context, "dose", true);
			configureOptionWidget(context, "doseUnits", "dropdown");
			configureOptionWidget(context, "route", "dropdown");
			configureOptionWidget(context, "frequency", "dropdown");
			configureCheckboxWidget(context, "asNeeded");
			configureNumericWidget(context, "duration", false);
			configureOptionWidget(context, "durationUnits", "dropdown");
			configureNumericWidget(context, "quantity", true);
			configureOptionWidget(context, "quantityUnits", "dropdown");
			configureNumericWidget(context, "numRefills", false);
		}
	}
	
	@Override
	public void setInitialValue(Object v) {
		List<Order> l = (List<Order>) v;
		initialValue = new HashMap<>();
		for (Order order : l) {
			Concept concept = order.getConcept();
			List<Order> orderList = initialValue.get(concept);
			if (orderList == null) {
				orderList = new ArrayList<>();
				initialValue.put(concept, orderList);
			}
			orderList.add(order);
		}
	}
	
	public List<Order> getInitialValueForConcept(Concept concept) {
		List<Order> ret = null;
		if (initialValue != null) {
			ret = initialValue.get(concept);
		}
		return (ret == null ? new ArrayList<>() : ret);
	}
	
	/**
	 * Configuration object that is passed into Javascript widgets and used to configure their
	 * capabilities
	 */
	public JsonObject constructJavascriptConfig(FormEntryContext context) {
		log.trace("OrderWidget - constructing javascript config");
		String fieldName = context.getFieldName(this);
		
		Integer patId = context.getExistingPatient().getPatientId();
		Integer encId = context.getExistingEncounter() == null ? null : context.getExistingEncounter().getEncounterId();
		
		// By default, set the default order date to the current encounter date, if editing an existing encounter, or today
		Date defaultDate = new Date();
		if (context.getExistingEncounter() != null) {
			defaultDate = context.getExistingEncounter().getEncounterDatetime();
		}
		
		JsonObject jsonConfig = new JsonObject();
		jsonConfig.addString("fieldName", fieldName);
		jsonConfig.addString("defaultDate", new SimpleDateFormat("yyyy-MM-dd").format(defaultDate));
		jsonConfig.addString("patientId", patId.toString());
		jsonConfig.addString("encounterId", encId == null ? "" : encId.toString());
		jsonConfig.addString("mode", context.getMode().name());
		jsonConfig.addString("hasTemplate", Boolean.toString(StringUtils.isNotBlank(widgetConfig.getTemplateContent())));
		
		// Add all of the attributes configured on the top level order tag, for use as needed by the widget
		log.trace("OrderWidget - adding and translating tag attributes");
		JsonObject tagAttributes = jsonConfig.addObject("tagAttributes");
		if (widgetConfig.getAttributes() != null) {
			for (String att : widgetConfig.getAttributes().keySet()) {
				String attVal = widgetConfig.getAttributes().get(att);
				if (att.endsWith("Label") && attVal != null) {
					attVal = translate(attVal);
				}
				tagAttributes.addString(att, attVal);
			}
		}
		
		jsonConfig.put("orderPropertyAttributes", widgetConfig.getOrderPropertyAttributes());
		
		// In order to re-configure date widgets, add some additional configuration
		DateWidget w = (DateWidget) widgets.getOrDefault("dateActivated", new DateWidget());
		JsonObject dateConfig = jsonConfig.addObject("dateWidgetConfig");
		dateConfig.addString("dateFormat", w.jsDateFormat());
		dateConfig.addString("yearsRange", w.getYearsRange());
		dateConfig.addString("locale", w.getLocaleForJquery());
		
		JsonObject jsonWidgets = jsonConfig.addObject("widgets");
		for (String key : widgets.keySet()) {
			jsonWidgets.addString(key, context.getFieldName(widgets.get(key)));
		}
		
		// Add any translations needed by the default views
		log.trace("OrderWidget - adding all of the translations");
		String prefix = "htmlformentry.orders.";
		JsonObject translations = jsonConfig.addObject("translations");
		String[] messageCodes = { "encounterDateChangeWarning", "delete", "editDeleteWarning", "editOrder", "deleteOrder",
		        "previousOrder", "orderReason", "starting", "until", "for", "discontinueReason", "asNeeded", "quantity",
		        "refills", "active", "cancelAction", "existingOrdersViewTitle", "existingOrdersEditTitle", "newOrdersTitle",
		        "noOrders" };
		for (String messageCode : messageCodes) {
			translations.addTranslation(prefix, messageCode);
		}
		
		List<JsonObject> historyArray = jsonConfig.getObjectArray("history");
		List<JsonObject> conceptArray = jsonConfig.getObjectArray("concepts");
		
		// Add a section for each concept configured in the tag
		log.trace("OrderWidget - add concepts and drugs");
		for (Concept c : widgetConfig.getConceptsAndDrugsConfigured().keySet()) {
			
			Option conceptOption = widgetConfig.getOption("concept", c.getId().toString());
			
			// For each rendered widget, add configuration of that widget into json for javascript
			JsonObject jsonConcept = new JsonObject();
			conceptArray.add(jsonConcept);
			jsonConcept.addString("conceptId", c.getId().toString());
			jsonConcept.addString("conceptLabel", conceptOption.getLabel());
			
			List<JsonObject> jsonConceptDrugs = jsonConcept.getObjectArray("drugs");
			for (Drug d : widgetConfig.getConceptsAndDrugsConfigured().get(c)) {
				Option drugOption = widgetConfig.getOption("drug", d.getId().toString());
				JsonObject jsonDrug = new JsonObject();
				jsonDrug.addString("drugId", d.getDrugId().toString());
				jsonDrug.addString("drugLabel", drugOption.getLabel());
				jsonDrug.addString("strength", d.getStrength());
				String dosageForm = d.getDosageForm() == null ? "" : d.getDosageForm().getConceptId().toString();
				jsonDrug.addString("dosageForm", dosageForm);
				jsonConceptDrugs.add(jsonDrug);
			}
			
			if (initialValue != null) {
				for (Order o : getInitialValueForConcept(c)) {
					Order pd = o.getPreviousOrder();
					JsonObject jho = new JsonObject();
					jho.addString("orderId", o.getOrderId().toString());
					jho.addString("encounterId", o.getEncounter().getEncounterId().toString());
					jho.addString("previousOrderId", pd == null ? "" : pd.getOrderId().toString());
					jho.addString("orderClass", o.getClass().getName());
					jho.addString("isDrugOrder", Boolean.toString(HtmlFormEntryUtil.isADrugOrderType(o.getOrderType())));
					addToJsonObject(jho, "action", o.getAction());
					JsonObject conceptObj = jho.addObject("concept");
					conceptObj.addString("value", c.getId().toString());
					conceptObj.addString("display", conceptOption.getLabel());
					addToJsonObject(jho, "careSetting", o.getCareSetting());
					addToJsonObject(jho, "orderType", o.getOrderType());
					addToJsonObject(jho, "instructions", o.getInstructions());
					addToJsonObject(jho, "urgency", o.getUrgency());
					addToJsonObject(jho, "dateActivated", o.getDateActivated());
					addToJsonObject(jho, "scheduledDate", o.getScheduledDate());
					addToJsonObject(jho, "effectiveStartDate", o.getEffectiveStartDate());
					addToJsonObject(jho, "autoExpireDate", o.getAutoExpireDate());
					addToJsonObject(jho, "dateStopped", o.getDateStopped());
					addToJsonObject(jho, "effectiveStopDate", o.getEffectiveStopDate());
					
					if (o instanceof DrugOrder) {
						DrugOrder d = (DrugOrder) o;
						addToJsonObject(jho, "drug", d.getDrug());
						addToJsonObject(jho, "drugNonCoded", d.getDrugNonCoded());
						addToJsonObject(jho, "dosingType", d.getDosingType());
						addToJsonObject(jho, "dosingInstructions", d.getDosingInstructions());
						addToJsonObject(jho, "dose", d.getDose());
						addToJsonObject(jho, "doseUnits", d.getDoseUnits());
						addToJsonObject(jho, "route", d.getRoute());
						addToJsonObject(jho, "frequency", d.getFrequency());
						addToJsonObject(jho, "asNeeded", d.getAsNeeded());
						addToJsonObject(jho, "duration", d.getDuration());
						addToJsonObject(jho, "durationUnits", d.getDurationUnits());
						addToJsonObject(jho, "quantity", d.getQuantity());
						addToJsonObject(jho, "quantityUnits", d.getQuantityUnits());
						addToJsonObject(jho, "numRefills", d.getNumRefills());
					}
					
					if (o.getAction() == Order.Action.DISCONTINUE) {
						addToJsonObject(jho, "orderReason", "");
						addToJsonObject(jho, "orderReasonNonCoded", "");
						addToJsonObject(jho, "discontinueReason", o.getOrderReason());
						addToJsonObject(jho, "discontinueReasonNonCoded", o.getOrderReasonNonCoded());
					} else {
						addToJsonObject(jho, "orderReason", o.getOrderReason());
						addToJsonObject(jho, "orderReasonNonCoded", o.getOrderReasonNonCoded());
						addToJsonObject(jho, "discontinueReason", "");
						addToJsonObject(jho, "discontinueReasonNonCoded", "");
					}
					historyArray.add(jho);
				}
			}
		}
		return jsonConfig;
	}
	
	public void addToJsonObject(JsonObject jho, String property, Object propertyValue) {
		JsonObject o = jho.addObject(property);
		o.addString("value", getValueForProperty(propertyValue));
		o.addString("display", getLabelForProperty(property, propertyValue));
	}
	
	public String getValueForProperty(Object propertyValue) {
		String val = "";
		if (propertyValue != null) {
			if (propertyValue instanceof OpenmrsObject) {
				val = ((OpenmrsObject) propertyValue).getId().toString();
			} else if (propertyValue instanceof Date) {
				Date dateVal = (Date) propertyValue;
				val = new SimpleDateFormat("yyyy-MM-dd").format(dateVal);
			} else if (propertyValue instanceof Class) {
				Class classValue = (Class) propertyValue;
				val = classValue.getName();
			} else if (propertyValue instanceof Enum) {
				Enum enumVal = (Enum) propertyValue;
				val = enumVal.name();
			} else {
				val = propertyValue.toString();
			}
		}
		return val;
	}
	
	public String getLabelForProperty(String property, Object propertyValue) {
		if (propertyValue != null) {
			try {
				Widget w = widgets.get(property);
				if (w != null) {
					if (w instanceof SingleOptionWidget) {
						String valueToLookup = getValueForProperty(propertyValue);
						for (Option o : ((SingleOptionWidget) w).getOptions()) {
							if (OpenmrsUtil.nullSafeEquals(valueToLookup, o.getValue())) {
								return o.getLabel();
							}
						}
					}
				}
				if (propertyValue instanceof Date) {
					DateWidget dw = (DateWidget) widgets.getOrDefault("dateActivated", new DateWidget());
					return dw.getDateFormatForDisplay().format((Date) propertyValue);
				}
				if (propertyValue instanceof Concept) {
					return ((Concept) propertyValue).getDisplayString();
				} else if (propertyValue instanceof OpenmrsMetadata) {
					return HtmlFormEntryUtil.format((OpenmrsMetadata) propertyValue);
				}
			}
			catch (Exception e) {
				log.warn("An error occurred trying to get label for property " + property, e);
			}
			return propertyValue.toString();
		}
		return "";
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		
		log.trace("OrderWidget - generating html");
		CapturingPrintWriter writer = new CapturingPrintWriter();
		String fieldName = context.getFieldName(this);
		
		// Wrap the entire widget in a div
		writer.println("<div id=\"" + fieldName + "\" class=\"orderwidget-element\">");
		
		// Add a section that will contain the selected orders
		writer.println("<div id=\"" + fieldName + "_orders\" class=\"orderwidget-order-section\"></div>");
		
		// Add a section that can contain a selector
		writer.println("<div id=\"" + fieldName + "_header\" class=\"orderwidget-selector-section\"></div>");
		
		// Add sections to serve as templates for edit mode and view mode for a given order
		
		// Build the templates from the configured widgets and properties
		String editTemplateContent = getWidgetConfig().getTemplateContent();
		StringBuilder defaultEditContent = new StringBuilder();
		String viewTemplateContent = getWidgetConfig().getTemplateContent();
		StringBuilder defaultViewContent = new StringBuilder();
		
		for (String property : widgets.keySet()) {
			Widget w = widgets.get(property);
			Map<String, String> c = widgetConfig.getAttributes(property);
			if (c != null) {
				String key = c.toString();
				log.trace("OrderWidget - generating html for: " + property);
				
				// We always generate a view template, as it is used both in view and edit/enter modes
				String viewHtml = generateHtmlForWidget(property, null, c, context);
				if (StringUtils.isBlank(viewTemplateContent)) {
					defaultViewContent.append(viewHtml);
				} else {
					viewTemplateContent = viewTemplateContent.replace(key, viewHtml);
				}
				
				// We only generate the edit template for ENTER/EDIT modes
				if (context.getMode() != FormEntryContext.Mode.VIEW) {
					String widgetHtml = generateHtmlForWidget(property, w, c, context);
					// If no template was supplied, or if the template does not contain this widget, add to the default section
					if (StringUtils.isBlank(editTemplateContent) || !editTemplateContent.contains(key)) {
						defaultEditContent.append(widgetHtml);
					}
					// Otherwise, replace the widget configuration with the widget html in the template
					else {
						editTemplateContent = editTemplateContent.replace(key, widgetHtml);
					}
				}
			}
		}
		
		// Render edit template
		if (context.getMode() != FormEntryContext.Mode.VIEW) {
			writer.println(
			    "<div id=\"" + fieldName + "_template\" class=\"orderwidget-order-form\" style=\"display:none;\">");
			if (StringUtils.isNotBlank(editTemplateContent)) {
				writer.println(editTemplateContent);
			}
			// If a template was configured, then hide the non-template fields by default, otherwise show them
			String nonTemplateStyle = StringUtils.isBlank(editTemplateContent) ? "" : "display:none;";
			writer.println("<div class=\"non-template-field\" style=\"" + nonTemplateStyle + "\">");
			writer.println(defaultEditContent.toString());
			writer.println("</div>");
			writer.println("</div>");
		}
		
		// Render view template
		writer.println(
		    "<div id=\"" + fieldName + "_view_template\" class=\"orderwidget-order-history-item\" style=\"display:none;\">");
		writer.println(StringUtils.isBlank(viewTemplateContent) ? defaultViewContent : viewTemplateContent);
		writer.println("</div>");
		
		// Add javascript function to initialize widget as appropriate
		String defaultLoadFn = "orderWidget.initialize";
		String onLoadFn = widgetConfig.getAttributes().getOrDefault("onLoadFunction", defaultLoadFn);
		writer.println("<script type=\"text/javascript\">");
		writer.println("jQuery(function() { " + onLoadFn + "(");
		writer.println(constructJavascriptConfig(context).toJson());
		writer.println(")});");
		writer.println("</script>");
		
		writer.println("</div>");
		
		return writer.getContent();
	}
	
	/**
	 * @return the html to render for each found property widget
	 */
	public String generateHtmlForWidget(String property, Widget w, Map<String, String> attrs, FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		String labelCode = attrs.getOrDefault(OrderTagHandler.LABEL_ATTRIBUTE, "htmlformentry.orders." + property);
		String label = translate(labelCode);
		ret.append("<div class=\"order-field order-").append(property).append("\">");
		ret.append("<div class=\"order-field-label order-").append(property).append("\">");
		ret.append(label);
		ret.append("</div>");
		ret.append("<div class=\"order-field-widget order-").append(property);
		if (w instanceof RadioButtonsWidget) {
			ret.append(" order-field-radio-group");
		}
		ret.append("\">");
		if (w != null) {
			ret.append(w.generateHtml(context));
			ErrorWidget ew = context.getErrorWidget(w);
			if (ew != null) {
				ret.append(ew.generateHtml(context));
			}
		}
		ret.append("</div>");
		ret.append("</div>");
		return ret.toString();
	}
	
	public String getLastSubmissionJavascript(FormEntryContext c, HttpServletRequest r) {
		CapturingPrintWriter writer = new CapturingPrintWriter();
		writer.println("orderWidget.resetWidget(");
		JsonObject json = new JsonObject();
		json.put("config", constructJavascriptConfig(c));
		Set<String> fieldSuffixes = getFieldSuffixes(c, r);
		for (String fs : fieldSuffixes) {
			Order.Action action = parseValue(getValue(c, r, fs, "action"), Order.Action.class);
			if (action != null) {
				JsonObject o = json.addObjectToArray("values");
				o.addString("fieldSuffix", fs);
				o.addString("action", action.name());
				for (Widget w : widgets.values()) {
					String fieldName = c.getFieldName(w) + fs;
					JsonObject field = o.addObjectToArray("fields");
					field.addString("name", fieldName);
					field.addString("value", r.getParameter(fieldName));
				}
			}
		}
		writer.println(json.toJson());
		writer.println(");");
		return writer.getContent();
	}
	
	@Override
	public List<OrderWidgetValue> getValue(FormEntryContext c, HttpServletRequest r) {
		List<OrderWidgetValue> ret = new ArrayList<>();
		Set<String> fieldSuffixes = getFieldSuffixes(c, r);
		for (String fs : fieldSuffixes) {
			Order.Action action = parseValue(getValue(c, r, fs, "action"), Order.Action.class);
			if (action != null) {
				OrderType orderType = widgetConfig.getOrderField().getOrderType();
				log.trace("User requested to place a " + action + " " + orderType.getJavaClass().getSimpleName());
				Order previousOrder = parseValue(getValue(c, r, fs, "previousOrder"), Order.class);
				OrderWidgetValue v = new OrderWidgetValue();
				v.setFieldSuffix(fs);
				v.setPreviousOrder(previousOrder);
				try {
					Order newOrder = (Order) orderType.getJavaClass().newInstance();
					newOrder.setPreviousOrder(previousOrder);
					newOrder.setAction(action);
					newOrder.setOrderType(widgetConfig.getOrderField().getOrderType());
					newOrder.setConcept(parseValue(getValue(c, r, fs, "concept"), Concept.class));
					newOrder.setCareSetting(parseValue(getValue(c, r, fs, "careSetting"), CareSetting.class));
					newOrder.setInstructions(getValue(c, r, fs, "instructions"));
					newOrder.setUrgency(parseValue(getValue(c, r, fs, "urgency"), Order.Urgency.class));
					newOrder.setDateActivated(getDateActivatedValue());
					newOrder.setScheduledDate(parseValue(getValue(c, r, fs, "scheduledDate"), Date.class));
					if (action == Order.Action.DISCONTINUE) {
						newOrder.setOrderReason(parseValue(getValue(c, r, fs, "discontinueReason"), Concept.class));
						newOrder.setOrderReasonNonCoded(getValue(c, r, fs, "discontinueReasonNonCoded"));
					} else {
						newOrder.setOrderReason(parseValue(getValue(c, r, fs, "orderReason"), Concept.class));
						newOrder.setOrderReasonNonCoded(getValue(c, r, fs, "orderReasonNonCoded"));
					}
					if (newOrder instanceof DrugOrder) {
						DrugOrder newDrugOrder = (DrugOrder) newOrder;
						newDrugOrder.setDrug(parseValue(getValue(c, r, fs, "drug"), Drug.class));
						newDrugOrder.setDrugNonCoded(getValue(c, r, fs, "drugNonCoded"));
						newDrugOrder.setDosingType(parseValue(getValue(c, r, fs, "dosingType"), Class.class));
						newDrugOrder.setDosingInstructions(getValue(c, r, fs, "dosingInstructions"));
						newDrugOrder.setDose(parseValue(getValue(c, r, fs, "dose"), Double.class));
						newDrugOrder.setDoseUnits(parseValue(getValue(c, r, fs, "doseUnits"), Concept.class));
						newDrugOrder.setRoute(parseValue(getValue(c, r, fs, "route"), Concept.class));
						newDrugOrder.setFrequency(parseValue(getValue(c, r, fs, "frequency"), OrderFrequency.class));
						newDrugOrder.setAsNeeded(parseValue(getValue(c, r, fs, "asNeeded"), Boolean.class, false));
						newDrugOrder.setDuration(parseValue(getValue(c, r, fs, "duration"), Integer.class));
						if (newDrugOrder.getDuration() != null) {
							newDrugOrder.setDurationUnits(parseValue(getValue(c, r, fs, "durationUnits"), Concept.class));
						}
						newDrugOrder.setQuantity(parseValue(getValue(c, r, fs, "quantity"), Double.class));
						if (newDrugOrder.getQuantity() != null) {
							newDrugOrder.setQuantityUnits(parseValue(getValue(c, r, fs, "quantityUnits"), Concept.class));
						}
						newDrugOrder.setNumRefills(parseValue(getValue(c, r, fs, "numRefills"), Integer.class));
					}
					v.setNewOrder(newOrder);
					ret.add(v);
				}
				catch (Exception e) {
					throw new IllegalStateException("Unable to construct a new order", e);
				}
			}
		}
		return ret;
	}
	
	public Set<String> getFieldSuffixes(FormEntryContext c, HttpServletRequest r) {
		Set<String> ret = new TreeSet<>();
		// All submitted orders will have a non-null action submitted
		String widgetFieldName = c.getFieldName(widgets.get("action"));
		for (Object paramKey : r.getParameterMap().keySet()) {
			String paramName = paramKey.toString();
			if (paramName.startsWith(widgetFieldName + "_")) {
				ret.add(StringUtils.substringAfter(paramName, widgetFieldName));
			}
		}
		return ret;
	}
	
	public String getFormField(FormEntryContext context, String suffix, String property) {
		try {
			Widget w = widgets.get(property);
			String requestParam = context.getFieldName(w) + suffix;
			return requestParam;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to get form field instance " + suffix + " property " + property, e);
		}
	}
	
	public String getFormErrorField(FormEntryContext context, String suffix, String property) {
		try {
			Widget w = widgets.get(property);
			ErrorWidget ew = context.getErrorWidget(w);
			String requestParam = context.getFieldName(ew) + suffix;
			return requestParam;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to get form field for " + suffix + " property " + property, e);
		}
	}
	
	protected String getValue(FormEntryContext context, HttpServletRequest req, String fieldSuffix, String property) {
		try {
			return req.getParameter(getFormField(context, fieldSuffix, property));
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to get value from request", e);
		}
	}
	
	/**
	 * We do not allow for explicitly setting date activated on the form. If the tag is configured to
	 * use entryDate, set this, otherwise leave as null to enable the default behavior, which associates
	 * the date with the encounter date.
	 */
	protected Date getDateActivatedValue() {
		String property = "dateActivated";
		Date val = null;
		Map<String, String> attrs = widgetConfig.getAttributes(property);
		String defaultVal = attrs.get("value");
		if (StringUtils.isNotBlank(defaultVal)) {
			if ("entryDate".equals(defaultVal)) {
				val = new Date();
			} else {
				throw new IllegalArgumentException("Unknown value for dateActivated: " + defaultVal);
			}
		}
		return val;
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
		return TagUtil.parseValue(value, type);
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
	
	protected boolean isDrugOrder() {
		return HtmlFormEntryUtil.isADrugOrderType(widgetConfig.getOrderField().getOrderType());
	}
	
	protected String translate(String code) {
		return Context.getMessageSourceService().getMessage(code);
	}
	
	public OrderField getOrderField() {
		return widgetConfig.getOrderField();
	}
	
	public OrderWidgetConfig getWidgetConfig() {
		return widgetConfig;
	}
	
	public void setWidgetConfig(OrderWidgetConfig widgetConfig) {
		this.widgetConfig = widgetConfig;
	}
	
	public Map<String, Widget> getWidgets() {
		return widgets;
	}
}
