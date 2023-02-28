package org.openmrs.module.htmlformentry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Obs;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Data object that represents a single component of an ObsGroup
 */
public class ObsGroupComponent {
	
	private Concept question;
	
	private Concept answer;
	
	private Drug answerDrug;
	
	private Boolean partOfSet = false;
	
	private Boolean lastInSet = false;
	
	/** Logger for this class and subclasses */
	protected final static Log log = LogFactory.getLog(ObsGroupComponent.class);
	
	public ObsGroupComponent() {
	}
	
	public ObsGroupComponent(Concept question, Concept answer) {
		this.question = question;
		this.answer = answer;
	}
	
	public ObsGroupComponent(Concept question, Concept answer, Drug answerDrug) {
		this.question = question;
		this.answer = answer;
		this.answerDrug = answerDrug;
	}
	
	public ObsGroupComponent(Concept question, Concept answer, Drug answerDrug, Boolean partOfSet, Boolean lastInSet) {
		this.question = question;
		this.answer = answer;
		this.answerDrug = answerDrug;
		this.partOfSet = partOfSet;
		this.lastInSet = lastInSet;
	}
	
	/** Gets the concept that represents the component's question */
	public Concept getQuestion() {
		return question;
	}
	
	/** Sets the concept that represents the component's question */
	public void setQuestion(Concept question) {
		this.question = question;
	}
	
	/** Gets the concept that represents the component's answer */
	public Concept getAnswer() {
		return answer;
	}
	
	/** Sets the concept that represents the component's answer */
	public void setAnswer(Concept answer) {
		this.answer = answer;
	}
	
	public Drug getAnswerDrug() {
		return answerDrug;
	}
	
	public void setAnswerDrug(Drug answerDrug) {
		this.answerDrug = answerDrug;
	}
	
	@Deprecated
	public static boolean supports(List<ObsGroupComponent> questionsAndAnswers, Obs parentObs, Set<Obs> group) {
		for (Obs obs : group) {
			boolean match = false;
			for (ObsGroupComponent test : questionsAndAnswers) {
				boolean questionMatches = test.getQuestion().getConceptId().equals(obs.getConcept().getConceptId());
				boolean answerMatches = test.getAnswer() == null || (obs.getValueCoded() != null
				        && test.getAnswer().getConceptId().equals(obs.getValueCoded().getConceptId()));
				
				if (questionMatches && !answerMatches) {
					match = false;
					break;
				}
				
				if (questionMatches && answerMatches) {
					match = true;
				}
			}
			if (!match) {
				return false;
			}
		}
		return true;
	}
	
	public static int supportingRank(List<ObsGroupComponent> obsGroupComponents, Set<Obs> obsSet) {
		int rank = 0;
		
		// iterate through all obs in the set
		for (Obs obs : obsSet) {
			Set<Integer> obsGroupComponentMatchLog = new HashSet<Integer>();
			
			// iterate though all form obs elements for obs group we are testing for a match against
			for (ObsGroupComponent obsGroupComponent : obsGroupComponents) {
				Concept groupComponentQuestion = obsGroupComponent.getQuestion();
				if (groupComponentQuestion == null) {
					// The correct error should be thrown with useful contextual information from ObsSubmissionElement:174
					// https://github.com/openmrs/openmrs-module-htmlformentry/blob/e6188717db16fc681ac4ec5d1610f251f214c372/api/src/main/java/org/openmrs/module/htmlformentry/element/ObsSubmissionElement.java#L174
					continue;
				}
				
				boolean questionMatches = groupComponentQuestion.getConceptId().equals(obs.getConcept().getConceptId());
				boolean answerMatches = false;
				
				if (obsGroupComponent.getAnswerDrug() == null) {
					answerMatches = (obsGroupComponent.getAnswer() == null || // TODO: why do we consider a match if answer is null?
					        (obs.getValueCoded() != null && obsGroupComponent.getAnswer().getConceptId()
					                .equals(obs.getValueCoded().getConceptId())));
				} else {
					answerMatches = (obs.getValueDrug() != null
					        && obsGroupComponent.getAnswerDrug().getDrugId().equals(obs.getValueDrug().getDrugId()));
				}
				
				// we've found a form obs element where the question matches an existing obs, but the answer does *not* match
				if (questionMatches && !answerMatches) {
					// confirm that we haven't previously found another match for this question
					if (!obsGroupComponentMatchLog.contains(obsGroupComponent.getQuestion().getConceptId())) {
						// TODO what does this clause do?
						if (((obsGroupComponent.getAnswer() != null)
						        && (obs.getValueCoded() == null || obs.getValueCoded().getConceptId() == null))
						        || ((obsGroupComponent.getAnswerDrug() != null)
						                && (obs.getValueDrug() == null || obs.getValueDrug().getDrugId() == null))) {
							return 0;
						} else {
							// are there multiple obs group components for this question?
							if (obsGroupComponent.isPartOfSet()) {
								// this is the last member belonging to the set, (and no matches were found previously), return an insurmountable ranking
								if (obsGroupComponent.isLastInSet()) {
									return -1000;
								}
							} else {
								//  not part of set and not a match, return an insurmountable ranking
								return -1000;
							}
						}
					}
				} else if (questionMatches && answerMatches) {
					if ((obsGroupComponent.getAnswer() != null) || (obsGroupComponent.getAnswerDrug() != null)) {
						// add extra weight to this matching...
						rank++;
					}
					obsGroupComponentMatchLog.add(obsGroupComponent.getQuestion().getConceptId());
					rank++;
				}
			}
		}
		return rank;
	}
	
	public static List<ObsGroupComponent> findQuestionsAndAnswersForGroup(String parentGroupingConceptId,
	        Pair<Concept, Concept> hiddenObs, Node node) {
		List<ObsGroupComponent> ret = new ArrayList<ObsGroupComponent>();
		if (hiddenObs != null) { // consider the hidden obs when making a match
			ret.add(new ObsGroupComponent(hiddenObs.getKey(), hiddenObs.getValue(), null, false, false));
		}
		findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId, node, ret);
		return ret;
	}
	
	private static void findQuestionsAndAnswersForGroupHelper(String parentGroupingConceptId, Node node,
	        List<ObsGroupComponent> obsGroupComponents) {
		
		if ("obs".equals(node.getNodeName())) {
			Concept question = null;
			List<Concept> questions = null;
			Concept answer = null;
			Drug answerDrug = null;
			List<Concept> answersList = null;
			NamedNodeMap attrs = node.getAttributes();
			try {
				String questionsStr = attrs.getNamedItem("conceptIds").getNodeValue();
				if (questionsStr != null && !"".equals(questionsStr)) {
					for (StringTokenizer st = new StringTokenizer(questionsStr, ","); st.hasMoreTokens();) {
						String s = st.nextToken().trim();
						if (questions == null)
							questions = new ArrayList<Concept>();
						questions.add(HtmlFormEntryUtil.getConcept(s));
					}
				}
			}
			catch (Exception ex) {
				//pass
			}
			try {
				question = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("conceptId").getNodeValue());
			}
			catch (Exception ex) {
				//pass
			}
			try {
				answer = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("answerConceptId").getNodeValue());
			}
			catch (Exception ex) {
				// this is fine
			}
			try {
				Node answerDrugId = attrs.getNamedItem("answerDrugId");
				if (answerDrugId != null) {
					answerDrug = HtmlFormEntryUtil.getDrug(answerDrugId.getNodeValue());
				}
			}
			catch (Exception ex) {
				// this is fine
			}
			//check for answerConceptIds (plural)
			if (answer == null) {
				Node n = attrs.getNamedItem("answerConceptIds");
				if (n != null) {
					String answersIds = n.getNodeValue();
					if (answersIds != null && !answersIds.equals("")) {
						//initialize list
						answersList = new ArrayList<Concept>();
						for (StringTokenizer st = new StringTokenizer(answersIds, ","); st.hasMoreTokens();) {
							try {
								answersList.add(HtmlFormEntryUtil.getConcept(st.nextToken().trim()));
							}
							catch (Exception ex) {
								//just ignore invalid conceptId if encountered in answersList
							}
						}
					}
				}
			}
			
			//determine whether or not the obs group parent of this obs is the obsGroup obs that we're looking at.
			boolean thisObsInThisGroup = false;
			Node pTmp = node.getParentNode();
			
			while (pTmp.getParentNode() != null) {
				Map<String, String> attributes = new HashMap<String, String>();
				NamedNodeMap map = pTmp.getAttributes();
				if (map != null)
					for (int i = 0; i < map.getLength(); ++i) {
						Node attribute = map.item(i);
						attributes.put(attribute.getNodeName(), attribute.getNodeValue());
					}
				if (attributes.containsKey("groupingConceptId")) {
					if (attributes.get("groupingConceptId").equals(parentGroupingConceptId))
						thisObsInThisGroup = true;
					break;
					
				}
				pTmp = pTmp.getParentNode();
			}
			
			if (thisObsInThisGroup) {
				if (answersList != null && answersList.size() > 0) {
					Iterator<Concept> i = answersList.iterator();
					while (i.hasNext()) {
						Concept c = i.next();
						// add all the answers as separate obs group components, flagging them all as part of a set, and flagging the last one as the last one in the set
						obsGroupComponents.add(new ObsGroupComponent(question, c, null, true, !i.hasNext()));
					}
				} else if (questions != null && questions.size() > 0) {
					Iterator<Concept> i = questions.iterator();
					while (i.hasNext()) {
						Concept c = i.next();
						// add all the questions as separate obs group components, flagging them all as part of a set, and flagging the last one as the last one in the set
						obsGroupComponents.add(new ObsGroupComponent(c, answer, null, true, !i.hasNext()));
					}
				} else {
					addToObsGroupComponentList(obsGroupComponents, question, answer, answerDrug);
				}
			}
		} else if ("obsgroup".equals(node.getNodeName())) {
			try {
				NamedNodeMap attrs = node.getAttributes();
				attrs.getNamedItem("groupingConceptId").getNodeValue();
				obsGroupComponents.add(new ObsGroupComponent(
				        HtmlFormEntryUtil.getConcept(attrs.getNamedItem("groupingConceptId").getNodeValue()), null));
			}
			catch (Exception ex) {
				throw new RuntimeException("Unable to get groupingConcept out of obsgroup tag.");
			}
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); ++i) {
			findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId, nl.item(i), obsGroupComponents);
		}
	}
	
	// see: https://issues.openmrs.org/browse/HTML-806
	private static void addToObsGroupComponentList(List<ObsGroupComponent> list, Concept question, Concept answer,
	        Drug answerDrug) {
		boolean isSet = false;
		// if there are any existing components with the same question, make sure they are flagged as part of set, but *not* the last one in the set
		for (ObsGroupComponent component : list) {
			if (component.getQuestion().equals(question)) {
				component.setPartOfSet(true);
				component.setLastInSet(false);
				isSet = true;
			}
		}
		// if existing components with the same question were found, flag this new component as part of a set, *and* the last element in the set
		list.add(new ObsGroupComponent(question, answer, answerDrug, isSet, isSet));
	}
	
	/**
	 * returns the obsgroup hierarchy path of an obsgroup Obs, including itself
	 *
	 * @param o
	 * @return
	 */
	public static String getObsGroupPath(Obs o) {
		StringBuilder st = new StringBuilder("/" + o.getConcept().getConceptId());
		while (o.getObsGroup() != null) {
			o = o.getObsGroup();
			st.insert(0, "/" + o.getConcept().getConceptId());
		}
		return st.toString();
	}
	
	/**
	 * returns the obsgroup hierarchy path of an obsgroup node in the xml, including itself
	 *
	 * @param node
	 * @return
	 */
	public static String getObsGroupPath(Node node) {
		StringBuilder st = new StringBuilder();
		while (!node.getNodeName().equals("htmlform")) {
			if (node.getNodeName().equals("obsgroup")) {
				try {
					String conceptIdString = node.getAttributes().getNamedItem("groupingConceptId").getNodeValue();
					Concept c = HtmlFormEntryUtil.getConcept(conceptIdString);
					st.insert(0, "/" + c.getConceptId());
				}
				catch (Exception ex) {
					throw new RuntimeException("obsgroup tag encountered without groupingConceptId attribute");
				}
			}
			node = node.getParentNode();
		}
		return st.toString();
	}
	
	public Boolean isPartOfSet() {
		return partOfSet;
	}
	
	public void setPartOfSet(Boolean partOfSet) {
		this.partOfSet = partOfSet;
	}
	
	public Boolean isLastInSet() {
		return lastInSet;
	}
	
	public void setLastInSet(Boolean lastInSet) {
		this.lastInSet = lastInSet;
	}
}
