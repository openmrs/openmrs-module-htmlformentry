package org.openmrs.module.htmlformentry.matching;

import java.util.List;

import org.openmrs.Concept;
import org.openmrs.module.htmlformentry.ObsGroupComponent;
import org.w3c.dom.Node;

public class ObsGroupEntity {

	private int id;
	private List<ObsGroupComponent> questionsAndAnswers;
	private String xmlObsGroupConcept;
	private String path;
	private Concept groupingConcept;
	private Node node;

	public List<ObsGroupComponent> getQuestionsAndAnswers() {
		return questionsAndAnswers;
	}
	public void setQuestionsAndAnswers(List<ObsGroupComponent> questionsAndAnswers) {
		this.questionsAndAnswers = questionsAndAnswers;
	}
	public String getXmlObsGroupConcept() {
		return xmlObsGroupConcept;
	}
	public void setXmlObsGroupConcept(String xmlObsGroupConcept) {
		this.xmlObsGroupConcept = xmlObsGroupConcept;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Concept getGroupingConcept() {
		return groupingConcept;
	}
	public void setGroupingConcept(Concept groupingConcept) {
		this.groupingConcept = groupingConcept;
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

}
