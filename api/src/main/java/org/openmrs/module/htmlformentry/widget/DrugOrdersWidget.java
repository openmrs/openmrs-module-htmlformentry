package org.openmrs.module.htmlformentry.widget;

import static org.openmrs.module.htmlformentry.handler.DrugOrdersTagHandler.FORMAT_ATTRIBUTE;
import static org.openmrs.module.htmlformentry.handler.DrugOrdersTagHandler.ON_SELECT;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.OpenmrsObject;
import org.openmrs.OrderFrequency;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.CapturingPrintWriter;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;

public class DrugOrdersWidget implements Widget {
	
	private Map<String, String> templateAttributes;
	
	private String templateContent;
	
	private Map<String, Map<String, String>> templateWidgets;
	
	private Map<String, String> drugOrderAttributes;
	
	private List<Map<String, String>> drugOrderOptions;
	
	private Map<String, String> discontinueReasonAttributes;
	
	private List<Map<String, String>> discontinueReasonOptions;
	
	private DrugOrderField drugOrderField;
	
	private Map<String, Widget> fieldWidgets;
	
	private List<DrugOrder> initialValue;
	
	public DrugOrdersWidget() {
	}
	
	@Override
	public void setInitialValue(Object initialValue) {
		this.initialValue = (List<DrugOrder>) initialValue;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		CapturingPrintWriter writer = new CapturingPrintWriter();
		String fieldName = context.getFieldName(this);
		
		// Wrap the entire widget in a div
		startTag(writer, "div", "drugOrdersSection", fieldName, null);
		writer.println();
		
		// Add a hidden input for the field name to submit the json data back to the server to represent the changes
		String inputId = fieldName;
		writer.println("<input type=\"hidden\" id=\"" + inputId + "\" name=\"" + inputId + "\"/>");
		writer.println();
		
		// If the format is onselect, hide all of the drug sections (unless already in the encounter) and show select widget
		boolean onSelect = ON_SELECT.equals(getDrugOrderAttributes().getOrDefault(FORMAT_ATTRIBUTE, ""));
		startTag(writer, "span", "drugSelectorSection", fieldName, (onSelect ? "" : "display:none"));
		writer.println();
		
		// Add a drug selector to the section.  This will only be visible if the section is visible
		startTag(writer, "select", "drugSelector", fieldName, null);
		writer.println();
		for (DrugOrderAnswer a : drugOrderField.getDrugOrderAnswers()) {
			Integer id = a.getDrug().getId();
			writer.print("<option value=\"" + id + "\"" + ">");
			writer.print(a.getDisplayName());
			writer.println("</option>");
		}
		writer.println("</select>");
		
		writer.println("</span>");
		
		// Add a section for each drug configured in the tag.  Hide these sections if appropriate
		
		for (DrugOrderAnswer a : drugOrderField.getDrugOrderAnswers()) {
			DrugOrder initialValueForDrug = getInitialValueForDrug(a.getDrug());
			
			// All elements for a given drug will have an id prefix like "fieldName_drugId"
			String idPrefix = fieldName + "_" + a.getDrug().getId();
			String sectionStyle = (onSelect && initialValueForDrug == null ? "display:none" : "");
			
			startTag(writer, "div", "drugOrderSection", idPrefix, sectionStyle);
			writer.println();
			renderDrugOrderTemplate(context, writer, idPrefix, a, initialValueForDrug);
			writer.println();
			writer.println("</div>");
			writer.println("<script type=\"text/javascript\">");
			writer.println("jQuery(function() { htmlForm.initializeDrugOrderWidget('" + idPrefix + "')});");
			writer.println("</script>");
		}
		
		writer.println("</div>");
		
		return writer.getContent();
	}
	
	protected void renderDrugOrderTemplate(FormEntryContext context, PrintWriter writer, String idPrefix,
	        DrugOrderAnswer drugOrderAnswer, DrugOrder drugOrder) {
		
		String template = getTemplateContent();
		drugOrder = (drugOrder == null ? new DrugOrder() : drugOrder);
		
		for (String widgetKey : getTemplateWidgets().keySet()) {
			
			Map<String, String> attributes = getTemplateWidgets().get(widgetKey);
			String propertyName = attributes.get("name");
			String fieldVal = attributes.get("value");
			
			String renderBefore = "";
			Widget w = null;
			ErrorWidget errorWidget = new ErrorWidget();
			String renderAfter = "";
			
			if ("drug".equalsIgnoreCase(propertyName)) {
				HiddenFieldWidget hiddenFieldWidget = new HiddenFieldWidget();
				hiddenFieldWidget.addAttribute("class", "order-property drug");
				hiddenFieldWidget.setInitialValue(drugOrderAnswer.getDrug().getId().toString());
				w = hiddenFieldWidget;
				if ("true".equalsIgnoreCase(attributes.get("showLabel"))) {
					renderAfter = drugOrderAnswer.getDisplayName();
				}
			} else if ("dosingType".equalsIgnoreCase(propertyName)) {
				DropdownWidget dosingTypeWidget = new DropdownWidget();
				dosingTypeWidget.addOption(new Option("", "", true));
				Class[] dis = { SimpleDosingInstructions.class, FreeTextDosingInstructions.class };
				for (Class c : dis) {
					dosingTypeWidget.addOption(new Option(c.getSimpleName(), c.getName(), false));
				}
				if (drugOrder.getDosingType() != null) {
					dosingTypeWidget.setInitialValue(drugOrder.getDosingType().getName());
				} else if (fieldVal != null) {
					dosingTypeWidget.setInitialValue(fieldVal);
				}
				w = dosingTypeWidget;
			} else if ("careSetting".equalsIgnoreCase(propertyName)) {
				List<CareSetting> careSettings = Context.getOrderService().getCareSettings(false);
				MetadataDropdownWidget careSettingWidget = new MetadataDropdownWidget(careSettings, "");
				if (drugOrder.getCareSetting() != null) {
					careSettingWidget.setInitialValue(drugOrder.getCareSetting().getId().toString());
				} else if (fieldVal != null) {
					CareSetting careSetting = HtmlFormEntryUtil.getCareSetting(fieldVal);
					careSettingWidget.setInitialValue(careSetting.getId().toString());
				}
				w = careSettingWidget;
			} else if ("dose".equalsIgnoreCase(propertyName)) {
				NumberFieldWidget numberFieldWidget = new NumberFieldWidget(0d, null, true);
				if (drugOrder.getDose() != null) {
					numberFieldWidget.setInitialValue(drugOrder.getDose());
				} else if (fieldVal != null) {
					numberFieldWidget.setInitialValue(Double.parseDouble(fieldVal));
				}
				w = numberFieldWidget;
			} else if ("doseUnits".equalsIgnoreCase(propertyName)) {
				List<Concept> concepts = Context.getOrderService().getDrugDosingUnits();
				ConceptDropdownWidget conceptWidget = new ConceptDropdownWidget(concepts, "");
				if (drugOrder.getDoseUnits() != null) {
					conceptWidget.setInitialValue(drugOrder.getDoseUnits());
				} else if (fieldVal != null) {
					Concept c = HtmlFormEntryUtil.getConcept(fieldVal);
					conceptWidget.setInitialValue(c.getId());
				}
				w = conceptWidget;
			} else if ("route".equalsIgnoreCase(propertyName)) {
				List<Concept> concepts = Context.getOrderService().getDrugRoutes();
				ConceptDropdownWidget conceptWidget = new ConceptDropdownWidget(concepts, "");
				if (drugOrder.getRoute() != null) {
					conceptWidget.setInitialValue(drugOrder.getRoute());
				} else if (fieldVal != null) {
					Concept c = HtmlFormEntryUtil.getConcept(fieldVal);
					conceptWidget.setInitialValue(c.getId());
				}
				w = conceptWidget;
			} else if ("frequency".equalsIgnoreCase(propertyName)) {
				List<OrderFrequency> frequencies = Context.getOrderService().getOrderFrequencies(false);
				MetadataDropdownWidget metadataWidget = new MetadataDropdownWidget(frequencies,
				        translate("DrugOrder.frequency"));
				if (drugOrder.getFrequency() != null) {
					metadataWidget.setInitialValue(drugOrder.getFrequency());
				} else if (fieldVal != null) {
					OrderFrequency orderFrequency = HtmlFormEntryUtil.getOrderFrequency(fieldVal);
					metadataWidget.setInitialValue(orderFrequency.getId());
				}
				w = metadataWidget;
			} else if ("scheduledDate".equalsIgnoreCase(propertyName)) {
				DateWidget dateWidget = new DateWidget();
				if (drugOrder.getEffectiveStartDate() != null) {
					dateWidget.setInitialValue(drugOrder.getEffectiveStartDate());
				}
				w = dateWidget;
			} else if ("duration".equalsIgnoreCase(propertyName)) {
				NumberFieldWidget numberFieldWidget = new NumberFieldWidget(0d, null, false);
				if (drugOrder.getDuration() != null) {
					numberFieldWidget.setInitialValue(drugOrder.getDuration());
				} else if (fieldVal != null) {
					numberFieldWidget.setInitialValue(Integer.parseInt(fieldVal));
				}
				w = numberFieldWidget;
			} else if ("durationUnits".equalsIgnoreCase(propertyName)) {
				List<Concept> concepts = Context.getOrderService().getDurationUnits();
				ConceptDropdownWidget conceptWidget = new ConceptDropdownWidget(concepts, "");
				if (drugOrder.getDurationUnits() != null) {
					conceptWidget.setInitialValue(drugOrder.getDurationUnits());
				} else if (fieldVal != null) {
					Concept c = HtmlFormEntryUtil.getConcept(fieldVal);
					conceptWidget.setInitialValue(c.getId());
				}
				w = conceptWidget;
			} else if ("quantity".equalsIgnoreCase(propertyName)) {
				NumberFieldWidget numberFieldWidget = new NumberFieldWidget(0d, null, true);
				if (drugOrder.getQuantity() != null) {
					numberFieldWidget.setInitialValue(drugOrder.getQuantity());
				} else if (fieldVal != null) {
					numberFieldWidget.setInitialValue(Double.parseDouble(fieldVal));
				}
				w = numberFieldWidget;
			} else if ("quantityUnits".equalsIgnoreCase(propertyName)) {
				List<Concept> concepts = Context.getOrderService().getDrugDispensingUnits();
				ConceptDropdownWidget conceptWidget = new ConceptDropdownWidget(concepts, translate("DrugOrder.units"));
				if (drugOrder.getQuantityUnits() != null) {
					conceptWidget.setInitialValue(drugOrder.getQuantityUnits());
				} else if (fieldVal != null) {
					Concept c = HtmlFormEntryUtil.getConcept(fieldVal);
					conceptWidget.setInitialValue(c.getId());
				}
				w = conceptWidget;
			} else if ("numRefills".equalsIgnoreCase(propertyName)) {
				NumberFieldWidget numberFieldWidget = new NumberFieldWidget(0d, null, false);
				if (drugOrder.getNumRefills() != null) {
					numberFieldWidget.setInitialValue(drugOrder.getNumRefills());
				} else if (fieldVal != null) {
					numberFieldWidget.setInitialValue(Integer.parseInt(fieldVal));
				}
				w = numberFieldWidget;
			}
			
			if (w == null) {
				throw new IllegalStateException("Unable to handle Drug Order property widget for: " + propertyName);
			}
			
			registerWidget(context, w, propertyName);
			context.registerErrorWidget(w, errorWidget);
			
			String replacementHtml = renderBefore + w.generateHtml(context) + renderAfter;
			template = template.replace(widgetKey, replacementHtml);
		}


		writer.print(template);
	}
	
	protected String attPair(String key, String value) {
		return " " + key + "=\"" + value + "\" ";
	}
	
	protected String registerWidget(FormEntryContext context, Widget widget, String propertyName) {
		String widgetId = context.registerWidget(widget);
		getFieldWidgets().put(propertyName, widget);
		return widgetId;
	}
	
	protected void startTag(CapturingPrintWriter writer, String tagName, String classId, String elementPrefix,
	        String cssStyle) {
		writer.print("<" + tagName);
		writer.print(" id=\"" + elementPrefix + classId + "\"");
		writer.print(" class=\"" + classId + "\"");
		if (StringUtils.isNotBlank(cssStyle)) {
			writer.print(" style=\"" + cssStyle + "\"");
		}
		writer.print(">");
	}
	
	public DrugOrder getInitialValueForDrug(Drug drug) {
		if (initialValue != null) {
			for (DrugOrder drugOrder : initialValue) {
				if (drugOrder.getDrug().equals(drug)) {
					return drugOrder;
				}
			}
		}
		return null;
	}
	
	@Override
	public Object getValue(FormEntryContext context, HttpServletRequest request) {
		String value = request.getParameter(context.getFieldName(this));
		return fromJsonObject(value);
	}
	
	public static List<Map<String, Object>> toJsonObject(List<DrugOrder> drugOrders) {
		List<Map<String, Object>> l = new ArrayList<>();
		for (DrugOrder drugOrder : drugOrders) {
			Map<String, Object> m = new LinkedHashMap<>();
			addJsonProperty(m, "orderId", drugOrder.getId());
			addJsonProperty(m, "uuid", drugOrder.getUuid());
			addJsonProperty(m, "patient", drugOrder.getPatient());
			addJsonProperty(m, "encounter", drugOrder.getEncounter());
			addJsonProperty(m, "orderReason", drugOrder.getOrderReason());
			addJsonProperty(m, "action", drugOrder.getAction().name());
			addJsonProperty(m, "previousOrder", drugOrder.getPreviousOrder());
			addJsonProperty(m, "drug", drugOrder.getDrug());
			addJsonProperty(m, "dosingType", drugOrder.getDosingType().getSimpleName());
			addJsonProperty(m, "careSetting", drugOrder.getCareSetting());
			addJsonProperty(m, "orderType", drugOrder.getOrderType());
			addJsonProperty(m, "orderer", drugOrder.getOrderer());
			addJsonProperty(m, "dosingInstructions", drugOrder.getDosingInstructions());
			addJsonProperty(m, "dose", drugOrder.getDose());
			addJsonProperty(m, "doseUnits", drugOrder.getDoseUnits());
			addJsonProperty(m, "route", drugOrder.getRoute());
			addJsonProperty(m, "frequency", drugOrder.getFrequency());
			addJsonProperty(m, "asNeeded", drugOrder.getAsNeeded());
			addJsonProperty(m, "asNeededCondition", drugOrder.getAsNeededCondition());
			addJsonProperty(m, "dateActivated", drugOrder.getDateActivated());
			addJsonProperty(m, "urgency", drugOrder.getUrgency().name());
			addJsonProperty(m, "scheduledDate", drugOrder.getScheduledDate());
			addJsonProperty(m, "effectiveStartDate", drugOrder.getEffectiveStartDate());
			addJsonProperty(m, "autoExpireDate", drugOrder.getAutoExpireDate());
			addJsonProperty(m, "effectiveStopDate", drugOrder.getEffectiveStopDate());
			addJsonProperty(m, "dateStopped", drugOrder.getDateStopped());
			addJsonProperty(m, "duration", drugOrder.getDuration());
			addJsonProperty(m, "durationUnits", drugOrder.getDurationUnits());
			addJsonProperty(m, "quantity", drugOrder.getQuantity());
			addJsonProperty(m, "quantityUnits", drugOrder.getQuantityUnits());
			addJsonProperty(m, "instructions", drugOrder.getInstructions());
			addJsonProperty(m, "numRefills", drugOrder.getNumRefills());
			l.add(m);
		}
		return l;
	}
	
	protected static void addJsonProperty(Map<String, Object> m, String property, Object value) {
		if (value != null) {
			if (value instanceof OpenmrsObject) {
				value = ((OpenmrsObject) value).getId();
			} else if (value instanceof Date) {
				value = (new SimpleDateFormat("yyyy-MM-dd").format((Date) value));
			}
		}
		m.put(property, value);
	}
	
	protected static DrugOrder fromJsonObject(String json) {
		DrugOrder drugOrder = null;
		// TODO: Implement this
		
		return drugOrder;
	}
	
	protected String translate(String code) {
		return Context.getMessageSourceService().getMessage(code);
	}
	
	public Map<String, String> getTemplateAttributes() {
		return templateAttributes;
	}
	
	public void setTemplateAttributes(Map<String, String> templateAttributes) {
		this.templateAttributes = templateAttributes;
	}
	
	public String getTemplateContent() {
		return templateContent;
	}
	
	public void setTemplateContent(String templateContent) {
		this.templateContent = templateContent;
	}
	
	public Map<String, Map<String, String>> getTemplateWidgets() {
		if (templateWidgets == null) {
			templateWidgets = new LinkedHashMap<>();
		}
		return templateWidgets;
	}
	
	public void addTemplateWidget(String key, Map<String, String> attributes) {
		getTemplateWidgets().put(key, attributes);
	}
	
	public Map<String, String> getDrugOrderAttributes() {
		if (drugOrderAttributes == null) {
			drugOrderAttributes = new LinkedHashMap<>();
		}
		return drugOrderAttributes;
	}
	
	public void setDrugOrderAttributes(Map<String, String> drugOrderAttributes) {
		this.drugOrderAttributes = drugOrderAttributes;
	}
	
	public List<Map<String, String>> getDrugOrderOptions() {
		if (drugOrderOptions == null) {
			drugOrderOptions = new ArrayList<>();
		}
		return drugOrderOptions;
	}
	
	public void addDrugOrderOption(Map<String, String> drugOrderOption) {
		getDrugOrderOptions().add(drugOrderOption);
	}
	
	public Map<String, String> getDiscontinueReasonAttributes() {
		return discontinueReasonAttributes;
	}
	
	public void setDiscontinueReasonAttributes(Map<String, String> discontinueReasonAttributes) {
		this.discontinueReasonAttributes = discontinueReasonAttributes;
	}
	
	public List<Map<String, String>> getDiscontinueReasonOptions() {
		if (discontinueReasonOptions == null) {
			discontinueReasonOptions = new ArrayList<>();
		}
		return discontinueReasonOptions;
	}
	
	public void addDiscontinueReasonOption(Map<String, String> discontinueReasonOption) {
		getDiscontinueReasonOptions().add(discontinueReasonOption);
	}
	
	public DrugOrderField getDrugOrderField() {
		return drugOrderField;
	}
	
	public void setDrugOrderField(DrugOrderField drugOrderField) {
		this.drugOrderField = drugOrderField;
	}
	
	public Map<String, Widget> getFieldWidgets() {
		if (fieldWidgets == null) {
			fieldWidgets = new LinkedHashMap<>();
		}
		return fieldWidgets;
	}
}
