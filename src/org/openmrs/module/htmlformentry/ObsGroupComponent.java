package org.openmrs.module.htmlformentry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

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
    
    public ObsGroupComponent() {
    }

    public ObsGroupComponent(Concept question, Concept answer) {
        this.question = question;
        this.answer = answer;
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
    
    public static boolean supports(List<ObsGroupComponent> questionsAndAnswers, Obs parentObs, Set<Obs> group) {
//      for (ObsGroupComponent ogc: questionsAndAnswers){
//          System.out.println(ogc.getQuestion() + " " + ogc.getAnswer());
//      }
      for (Obs obs : group) {
          boolean match = false;
          for (ObsGroupComponent test : questionsAndAnswers) {
              boolean questionMatches = test.getQuestion().getConceptId().equals(obs.getConcept().getConceptId());
              boolean answerMatches = test.getAnswer() == null ||(obs.getValueCoded() != null && test.getAnswer().getConceptId().equals(obs.getValueCoded().getConceptId()));
//            System.out.println("First comparison, questionMatches evaluated "+ questionMatches+ " based on test"  + test.getQuestion().getConceptId() + " and obs.concept" + obs.getConcept().getConceptId() + " AND answer concept evaluated " + answerMatches  );
              if (questionMatches && answerMatches) {
                  match = true;
                  break;
              }
          }
          if (!match){
//            System.out.println("returning false for " + parentObs);
              return false;
          }    
      }
//      System.out.println("returning true for " + parentObs);
      return true;
  }
  

  
  public static List<ObsGroupComponent> findQuestionsAndAnswersForGroup(String parentGroupingConceptId, Node node) {
      List<ObsGroupComponent> ret = new ArrayList<ObsGroupComponent>();
      findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId,node, ret);
      return ret;
  }
  

  private static void findQuestionsAndAnswersForGroupHelper(String parentGroupingConceptId, Node node, List<ObsGroupComponent> ret) {
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
          while(pTmp.getParentNode() != null){
                        
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
                  if (answersList != null && answersList.size() > 0)
                      for (Concept c : answersList)
                          ret.add(new ObsGroupComponent(question, c));
                  else if (questions != null && questions.size() > 0)
                      for (Concept c: questions)
                          ret.add(new ObsGroupComponent(c, answer));
                  else 
                      ret.add(new ObsGroupComponent(question, answer));     
          } 
      } else {
              if ("obsgroup".equals(node.getNodeName())){
                  try {
                      NamedNodeMap attrs = node.getAttributes();
                      attrs.getNamedItem("groupingConceptId").getNodeValue();
                      ret.add(new ObsGroupComponent(HtmlFormEntryUtil.getConcept(attrs.getNamedItem("groupingConceptId").getNodeValue()), null));
                  } catch (Exception ex){
                      throw new RuntimeException("Unable to get groupingConcept out of obsgroup tag.");
                  }    
              }
               NodeList nl = node.getChildNodes();
               for (int i = 0; i < nl.getLength(); ++i) {
                   findQuestionsAndAnswersForGroupHelper(parentGroupingConceptId, nl.item(i), ret);
               }
      }     
      
  }
}
