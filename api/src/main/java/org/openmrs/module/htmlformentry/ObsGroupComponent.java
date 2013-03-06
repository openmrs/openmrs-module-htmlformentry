package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 
 * Data object that represents a single component of an ObsGroup
 */
public class ObsGroupComponent {

	private Concept question;
	private Concept answer;
	private int remainingInSet = 0;

	/** Logger for this class and subclasses */
	protected final static Log log = LogFactory.getLog(ObsGroupComponent.class);

	public ObsGroupComponent() {
	}

	public ObsGroupComponent(Concept question, Concept answer) {
		this.question = question;
		this.answer = answer;
	}

	public ObsGroupComponent(Concept question, Concept answer, int remainingInSet) {
		this.question = question;
		this.answer = answer;
		this.remainingInSet = remainingInSet;
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

	@Deprecated
	public static boolean supports(List<ObsGroupComponent> questionsAndAnswers, Obs parentObs, Set<Obs> group) {
		for (Obs obs : group) {
			boolean match = false;
			for (ObsGroupComponent test : questionsAndAnswers) {
				boolean questionMatches = test.getQuestion().getConceptId().equals(obs.getConcept().getConceptId());
				boolean answerMatches = test.getAnswer() == null ||(obs.getValueCoded() != null && test.getAnswer().getConceptId().equals(obs.getValueCoded().getConceptId()));

				if (questionMatches && !answerMatches) {
					match = false;
					break;
				}

				if (questionMatches && answerMatches) {
					match = true;
				}
			}
			if (!match){
				return false;
			}    
		}
		return true;
	}

	public static int supportingRank(List<ObsGroupComponent> obsGroupComponents, Obs parentObs, Set<Obs> obsSet) {
		int rank = 0;

		for (Obs obs : obsSet) {
			Set<Integer> obsGroupComponentMatchLog = new HashSet<Integer>();

			for (ObsGroupComponent obsGroupComponent : obsGroupComponents) {
				boolean questionMatches = obsGroupComponent.getQuestion().getConceptId().equals(obs.getConcept().getConceptId());
				boolean answerMatches = obsGroupComponent.getAnswer() == null ||(obs.getValueCoded() != null && obsGroupComponent.getAnswer().getConceptId().equals(obs.getValueCoded().getConceptId()));

				if (questionMatches && !answerMatches) {					
					if (!obsGroupComponentMatchLog.contains(obsGroupComponent.getQuestion().getConceptId())) {
						if (obs.getValueCoded() == null || obs.getValueCoded().getConceptId() == null) {
							return 0;
						} else {
							if (obsGroupComponent.isPartOfSet()) {
								if (obsGroupComponent.getRemainingInSet() == 1) {
									return -1000;
								}
							} else {
								// If this ever happens, this is NOT a match and we can stop checking and return an insurmountable ranking
								return -1000;
							}
						}
					}
				} else if (questionMatches && answerMatches) {
					if (obsGroupComponent.getAnswer() != null) {
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

	public static List<ObsGroupComponent> findQuestionsAndAnswersForGroup(String parentGroupingConceptId, Node node) {
		List<ObsGroupComponent> ret = new ArrayList<ObsGroupComponent>();
		findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId,node, ret);
		return ret;
	}


	private static void findQuestionsAndAnswersForGroupHelper(String parentGroupingConceptId, Node node, List<ObsGroupComponent> obsGroupComponents) {

		if ("obs".equals(node.getNodeName())) {
			Concept question = null;
			List<Concept> questions = null;
			Concept answer = null;
			List<Concept> answersList = null;
			NamedNodeMap attrs = node.getAttributes();
			try {
				String questionsStr = attrs.getNamedItem("conceptIds").getNodeValue();
				if (questionsStr != null && !"".equals(questionsStr)){
					for (StringTokenizer st = new StringTokenizer(questionsStr, ","); st.hasMoreTokens(); ) {
						String s = st.nextToken().trim();
						if (questions == null)
							questions = new ArrayList<Concept>();
						questions.add(HtmlFormEntryUtil.getConcept(s));
					}
				}
			} catch (Exception ex){
				//pass
			}
			try {
				question = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("conceptId").getNodeValue());
			} catch (Exception ex) {
				//pass
			}
			try {
				answer = HtmlFormEntryUtil.getConcept(attrs.getNamedItem("answerConceptId").getNodeValue());
			} catch (Exception ex) {
				// this is fine
			}
			//check for answerConceptIds (plural)
			if (answer == null){
				Node n = attrs.getNamedItem("answerConceptIds");
				if (n != null){
					String answersIds = n.getNodeValue();
					if (answersIds != null && !answersIds.equals("")){
						//initialize list
						answersList = new ArrayList<Concept>();
						for (StringTokenizer st = new StringTokenizer(answersIds, ","); st.hasMoreTokens(); ) {
							try {
								answersList.add(HtmlFormEntryUtil.getConcept(st.nextToken().trim()));
							} catch (Exception ex){
								//just ignore invalid conceptId if encountered in answersList
							}
						} 
					}
				}
			}

			//deterimine whether or not the obs group parent of this obs is the obsGroup obs that we're looking at.
			boolean thisObsInThisGroup = false;
			Node pTmp = node.getParentNode();

			while(pTmp.getParentNode() != null) {
				Map<String, String> attributes = new HashMap<String, String>();        
				NamedNodeMap map = pTmp.getAttributes();
				if (map != null)
					for (int i = 0; i < map.getLength(); ++i) {
						Node attribute = map.item(i);
						attributes.put(attribute.getNodeName(), attribute.getNodeValue());
					}
				if (attributes.containsKey("groupingConceptId")){
					if (attributes.get("groupingConceptId").equals(parentGroupingConceptId))
						thisObsInThisGroup = true;
					break;

				} 
				pTmp = pTmp.getParentNode();
			}

			if (thisObsInThisGroup){
				if (answersList != null && answersList.size() > 0) {
					int setCounter = 0;
					for (Concept c : answersList) {
						obsGroupComponents.add(new ObsGroupComponent(question, c, answersList.size() - (setCounter++)));
					}
				} else if (questions != null && questions.size() > 0) {
					int setCounter = 0;
					for (Concept c: questions) {
						obsGroupComponents.add(new ObsGroupComponent(c, answer, questions.size() - (setCounter++)));
					}
				} else {
					obsGroupComponents.add(new ObsGroupComponent(question, answer));
				}	     
			} 
		} else if ("obsgroup".equals(node.getNodeName())){
			try {
				NamedNodeMap attrs = node.getAttributes();
				attrs.getNamedItem("groupingConceptId").getNodeValue();
				obsGroupComponents.add(new ObsGroupComponent(HtmlFormEntryUtil.getConcept(attrs.getNamedItem("groupingConceptId").getNodeValue()), null));
			} catch (Exception ex){
				throw new RuntimeException("Unable to get groupingConcept out of obsgroup tag.");
			}    
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); ++i) {
			findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId, nl.item(i), obsGroupComponents);
		}
	}

	/**
	 * 
	 * returns the obsgroup hierarchy path of an obsgroup Obs, including itself
	 * 
	 * @param o
	 * @return
	 */
	public static String getObsGroupPath(Obs o){
		StringBuilder st = new StringBuilder("/" + o.getConcept().getConceptId());
		while (o.getObsGroup() != null){
			o = o.getObsGroup();
			st.insert(0, "/" + o.getConcept().getConceptId());
		}
		return st.toString();
	}

	/**
	 * 
	 * returns the obsgroup hierarchy path of an obsgroup node in the xml, including itself
	 * 
	 * @param node
	 * @return
	 */
	public static String getObsGroupPath(Node node){
		StringBuilder st = new StringBuilder();
		while (!node.getNodeName().equals("htmlform")){
			if (node.getNodeName().equals("obsgroup")){
				try {
					String conceptIdString = node.getAttributes().getNamedItem("groupingConceptId").getNodeValue();
					Concept c = HtmlFormEntryUtil.getConcept(conceptIdString);
					st.insert(0, "/" + c.getConceptId());
				} catch (Exception ex){
					throw new RuntimeException("obsgroup tag encountered without groupingConceptId attribute");
				}
			}
			node = node.getParentNode();        
		}
		return st.toString();
	}

	public boolean isPartOfSet() {
		return remainingInSet > 0;
	}

	public int getRemainingInSet() {
		return remainingInSet;
	}

	public void setRemainingInSet(int remainingInSet) {
		this.remainingInSet = remainingInSet;
	}

}
