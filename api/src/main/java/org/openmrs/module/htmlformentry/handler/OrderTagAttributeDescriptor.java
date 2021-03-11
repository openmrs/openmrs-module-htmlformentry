
package org.openmrs.module.htmlformentry.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.OpenmrsObject;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.CareSettingAnswer;
import org.openmrs.module.htmlformentry.schema.ConceptOption;
import org.openmrs.module.htmlformentry.schema.ConceptOptionGroup;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.OrderField;
import org.openmrs.module.htmlformentry.schema.OrderFrequencyAnswer;
import org.openmrs.module.htmlformentry.substitution.Substituter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is a subclass of AttributeDescriptor that allows for resolving openmrs object dependencies
 * and substituting them out for Metadata Sharing
 */
public class OrderTagAttributeDescriptor extends AttributeDescriptor {
	
	public OrderTagAttributeDescriptor() {
		super("value", OpenmrsObject.class);
	}
	
	public HtmlForm performSubstitutions(HtmlForm htmlForm, Substituter substituter, Map<OpenmrsObject, OpenmrsObject> m) {
		try {
			String xml = htmlForm.getXmlData();
			Document doc = HtmlFormEntryUtil.stringToDocument(xml);
			Node content = HtmlFormEntryUtil.findChild(doc, "htmlform");
			handleNode(content, null, substituter, m);
			String updatedXml = HtmlFormEntryUtil.documentToString(doc);
			htmlForm.setXmlData(updatedXml);
			return htmlForm;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to perform drug order tag substitutions", e);
		}
	}
	
	private void handleNode(Node node, Class<? extends OpenmrsObject> currentPropertyType, Substituter substituter,
	        Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		if (node != null) {
			String name = node.getNodeName();
			if (name.equalsIgnoreCase("order")) {
				updateOrderNode(node, substituter, substitutionMap);
			} else if (name.equalsIgnoreCase("orderProperty")) {
				String property = HtmlFormEntryUtil.getNodeAttribute(node, "name", "");
				currentPropertyType = OrderTagHandler.PROPERTIES.get(property);
				if (currentPropertyType != null) {
					updateValueNode(node, currentPropertyType, substituter, substitutionMap);
				}
			} else if (name.equalsIgnoreCase("option")) {
				if (currentPropertyType != null) {
					updateValueNode(node, currentPropertyType, substituter, substitutionMap);
				}
			} else if (name.equalsIgnoreCase("optionGroup")) {
				updateOptionGroupNode(node, substituter, substitutionMap);
			}
			if (node.getChildNodes() != null) {
				NodeList children = node.getChildNodes();
				for (int i = 0; i < children.getLength(); ++i) {
					handleNode(children.item(i), currentPropertyType, substituter, substitutionMap);
				}
			}
		}
	}
	
	private void updateOrderNode(Node node, Substituter substituter, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		if (node != null) {
			Node orderTypeNode = node.getAttributes().getNamedItem("orderType");
			if (orderTypeNode != null) {
				String propertyValue = orderTypeNode.getTextContent();
				if (StringUtils.isNotBlank(propertyValue)) {
					String replacementVal = substituter.substitute(propertyValue, OrderType.class, substitutionMap);
					orderTypeNode.setTextContent(replacementVal);
				}
			}
		}
	}
	
	private void updateValueNode(Node node, Class<? extends OpenmrsObject> type, Substituter substituter,
	        Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		if (node != null) {
			Node valueNode = node.getAttributes().getNamedItem("value");
			if (valueNode != null) {
				String propertyValue = valueNode.getTextContent();
				if (StringUtils.isNotBlank(propertyValue)) {
					String replacementVal = substituter.substitute(propertyValue, type, substitutionMap);
					valueNode.setTextContent(replacementVal);
				}
			}
		}
	}
	
	private void updateOptionGroupNode(Node node, Substituter substituter,
	        Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		if (node != null) {
			Node conceptNode = node.getAttributes().getNamedItem("concept");
			if (conceptNode != null) {
				String propertyValue = conceptNode.getTextContent();
				if (StringUtils.isNotBlank(propertyValue)) {
					String replacementVal = substituter.substitute(propertyValue, Concept.class, substitutionMap);
					conceptNode.setTextContent(replacementVal);
				}
			}
		}
	}
	
	public Map<Class<? extends OpenmrsObject>, Set<OpenmrsObject>> getDependencies(HtmlForm htmlForm) {
		Map<Class<? extends OpenmrsObject>, Set<OpenmrsObject>> ret = new HashMap<>();
		try {
			HtmlFormSchema schema = HtmlFormEntryUtil.getHtmlFormSchema(htmlForm, FormEntryContext.Mode.ENTER);
			for (HtmlFormField field : schema.getAllFields()) {
				if (field instanceof OrderField) {
					OrderField f = (OrderField) field;
					addDependency(ret, OrderType.class, f.getOrderType());
					for (List<ConceptOptionGroup> setList : f.getConceptOptionGroups().values()) {
						for (ConceptOptionGroup optionSet : setList) {
							addDependency(ret, Concept.class, optionSet.getConcept());
						}
					}
					if (f.getConceptOptions() != null) {
						for (ConceptOption a : f.getConceptOptions()) {
							addDependency(ret, Concept.class, a.getConcept());
						}
					}
					if (f.getDrugOrderAnswers() != null) {
						for (DrugOrderAnswer a : f.getDrugOrderAnswers()) {
							addDependency(ret, Drug.class, a.getDrug());
						}
					}
					if (f.getCareSettingAnswers() != null) {
						for (CareSettingAnswer a : f.getCareSettingAnswers()) {
							addDependency(ret, CareSetting.class, a.getCareSetting());
						}
					}
					if (f.getDoseUnitAnswers() != null) {
						for (ConceptOption a : f.getDoseUnitAnswers()) {
							addDependency(ret, Concept.class, a.getConcept());
						}
					}
					if (f.getRouteAnswers() != null) {
						for (ConceptOption a : f.getRouteAnswers()) {
							addDependency(ret, Concept.class, a.getConcept());
						}
					}
					if (f.getFrequencyAnswers() != null) {
						for (OrderFrequencyAnswer a : f.getFrequencyAnswers()) {
							addDependency(ret, OrderFrequency.class, a.getOrderFrequency());
						}
					}
					if (f.getDurationUnitAnswers() != null) {
						for (ConceptOption a : f.getDurationUnitAnswers()) {
							addDependency(ret, Concept.class, a.getConcept());
						}
					}
					if (f.getQuantityUnitAnswers() != null) {
						for (ConceptOption a : f.getQuantityUnitAnswers()) {
							addDependency(ret, Concept.class, a.getConcept());
						}
					}
					if (f.getOrderReasonAnswers() != null) {
						for (ConceptOption a : f.getOrderReasonAnswers()) {
							addDependency(ret, Concept.class, a.getConcept());
						}
					}
					if (f.getDiscontinueReasonAnswers() != null) {
						for (ConceptOption a : f.getDiscontinueReasonAnswers()) {
							addDependency(ret, Concept.class, a.getConcept());
						}
					}
					if (f.getConceptOptionGroups() != null) {
						for (List<ConceptOptionGroup> groups : f.getConceptOptionGroups().values()) {
							for (ConceptOptionGroup group : groups) {
								addDependency(ret, Concept.class, group.getConcept());
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to get dependencies for form", e);
		}
		return ret;
	}
	
	private void addDependency(Map<Class<? extends OpenmrsObject>, Set<OpenmrsObject>> m,
	        Class<? extends OpenmrsObject> type, OpenmrsObject obj) {
		Set<OpenmrsObject> s = m.computeIfAbsent(type, k -> new HashSet<>());
		s.add(obj);
	}
}
