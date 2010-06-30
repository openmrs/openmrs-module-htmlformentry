package org.openmrs.module.htmlformentry;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.handler.IteratingTagHandler;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.openmrs.module.htmlformentry.schema.RptGroup;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides methods to take a {@code <htmlform>...</htmlform>} xml block and turns it into HTML to be displayed as a form in
 * a web browser. It can apply the {@code <macros>...</macros>} section, and replace tags like {@code <obs/>}.
 * 
 */
public class HtmlFormEntryGenerator implements TagHandler {
        
    /**
     * Takes an XML string, finds the {@code <macros></macros>} section in it, and applies those substitutions
     * <p>
     * For example the following input:
     * <pre>
     * {@code
     * <htmlform>
     *     <macros>
     *          count=1, 2, 3
     *     </macros>
     *     You can count like $count
     * </htmlform>
     * }
     * </pre>
     * <p>
     * Would produce the following output:
     * <pre>
     * {@code
     * <htmlform>
     *     You can count like 1, 2, 3
     * </htmlform>
     * }
     * </pre>
     * 
     * @param xml the xml string to process for macros
     * @return the xml string with after macro substitution
     * @throws Exception 
     */
    public String applyMacros(String xml) throws Exception {
        Document doc = HtmlFormEntryUtil.stringToDocument(xml);
        Node content = HtmlFormEntryUtil.findChild(doc, "htmlform");
        Node macrosNode = HtmlFormEntryUtil.findChild(content, "macros");

        // if there are no macros defined, we just return the original xml unchanged
        if (macrosNode == null)
            return xml;
               
        // otherwise get its contents
        Properties macros = new Properties();
        String macrosText = macrosNode.getTextContent();
        if (macrosText != null) {
            macros.load(new ByteArrayInputStream(macrosText.getBytes()));
            //macros.load(new StringReader(macrosText));
        }
        
        // now remove the macros node
        content.removeChild(macrosNode);
        
        // switch back to String mode from the document so we can use string utilities to substitute
        xml = HtmlFormEntryUtil.documentToString(doc);

        // substitute any macros we found
        for (Object temp : macros.keySet()) {
            String key = (String) temp;
            String value = macros.getProperty(key, "");
            xml = xml.replace("$" + key, value);
        }

        return xml;
    }
    
    /**
     * Takes an XML string, finds the {@code <translations></translations>} section in it, and applies those substitutions
     * 
     * <pre>
     * {@code
     * <htmlform>
     *     <translations defaultLocale="en">
     *       <code name="night_sweats">
     *         <variant locale="en" value="night sweats"/>
     *         <variant locale="fr" value="sueurs nocturnes"/>
     * 		  </code>
     *     </translations>
     * </htmlform>
     * }
     * </pre>
     * 
     * @param xml the xml string to process for translations
     * @return the xml string after translation substitutions have been made
     * @throws Exception 
     */
    public String applyTranslations(String xml, FormEntryContext context) throws Exception {
        Document doc = HtmlFormEntryUtil.stringToDocument(xml);
        Node content = HtmlFormEntryUtil.findChild(doc, "htmlform");
        Node transNode = HtmlFormEntryUtil.findChild(content, "translations");

        // if there are no translations defined, we just return the original xml unchanged
        if (transNode == null) {
            return xml;
        }
        
        String defaultLocaleStr = HtmlFormEntryUtil.getNodeAttribute(transNode, "defaultLocale", "en");

        NodeList codeNodeList = transNode.getChildNodes();
        for (int i=0; i<codeNodeList.getLength(); i++) {
        	Node codeNode = codeNodeList.item(i);
        	if (codeNode.getNodeName().equalsIgnoreCase("code")) {
        		String codeName = HtmlFormEntryUtil.getNodeAttribute(codeNode, "name", null);
	        	if (codeName == null) {
	        		throw new IllegalArgumentException("All translation elements must contain a valid code name");
	        	}
	            NodeList variantNodeList = codeNode.getChildNodes();
	            for (int j=0; j<variantNodeList.getLength(); ++j) {
	            	Node variantNode = variantNodeList.item(j);
	            	if (variantNode.getNodeName().equalsIgnoreCase("variant")) {
		            	String localeStr = HtmlFormEntryUtil.getNodeAttribute(variantNode, "locale", defaultLocaleStr);
		            	String valueStr = HtmlFormEntryUtil.getNodeAttribute(variantNode, "value", null);
		            	if (valueStr == null) {
		            		throw new IllegalArgumentException("All variants must specify a value");
		            	}
		            	context.getTranslator().addTranslation(localeStr, codeName, valueStr);
	            	}
	            }
        	}
        }
        
        // now remove the macros node
        content.removeChild(transNode);
        
        // switch back to String mode from the document so we can use string utilities to substitute
        xml = HtmlFormEntryUtil.documentToString(doc);
        return xml;
    }
    
    /**
     * Takes an XML string, finds each {@code <repeat></repeat>} section in it, and applies those substitutions
     * <p>
     * <pre>
     * {@code
     * <htmlform>
     *   <repeat>
     *     <template>
     *       <obsgroup groupingConceptId="1608">
     *         <tr>
     *           <td><obs conceptId="1611" answerConceptId="{conceptId}" answerLabel="{answerLabel}" /></td>
     *           <td><obs conceptId="1499"/></td>
     *           <td><obs conceptId="1500"/></td>
     *           <td><obs conceptId="1568" answerConceptIds="1746,843,1743" answerLabels="gueri,echec,abandonne"/></td>
     *         </tr>
     *       </obsgroup>
     *     </template>
     *     <render conceptId="2125" answerLabel="Traitement initial: 2 HRZE/4 HR"/>
     *     <render conceptId="2125" answerLabel="Traitement initial: 2 HRE/6HE (MSPP)"/>
     *     <render conceptId="2126" answerLabel="Retraitement: 2 SHREZ + 1 HREX + 5 HRE"/>
     *     <render conceptId="2124" answerLabel="Traitement des enfants de &lt; ans: 2 HRZ/4 HR"/>
     *   </repeat>
     * </htmlform>
     * }
     * </pre>
     * 
     * @param xml the xml string to process for repeat sections
     * @return the xml string after repeat substitutions have been made
     * @throws Exception 
     */
    public String applyTemplates(String xml) throws Exception {
        Document doc = HtmlFormEntryUtil.stringToDocument(xml);
        Node content = HtmlFormEntryUtil.findChild(doc, "htmlform");
        
        // We are doing this as follows since I can't seem to get the XML node cloning to work right.
        // We can refactor later as needed if we can get it to work properly, or replace the xml library
        // First we need to parse the document to get the node attributes for repeating elements
        List<List<Map<String, String>>> renderMaps = new ArrayList<List<Map<String, String>>>();
        loadRenderElementsForEachRepeatElement(content, renderMaps);
        
        // Now we are just going to use String replacements to explode the repeat tags properly
        Iterator<List<Map<String, String>>> renderMapIter = renderMaps.iterator();
        while (xml.contains("<repeat>")) {
        	int startIndex = xml.indexOf("<repeat>");
        	int endIndex = xml.indexOf("</repeat>") + 9;
        	String xmlToReplace = xml.substring(startIndex, endIndex);
        	String template = xmlToReplace.substring(xmlToReplace.indexOf("<template>")+10, xmlToReplace.indexOf("</template>"));
        	StringBuilder replacement = new StringBuilder();
        	for (Map<String, String> replacements : renderMapIter.next()) {
        		String curr = template;
        		for (String key : replacements.keySet()) {
        			curr = curr.replace("{"+key+"}", replacements.get(key));
        		}
        		replacement.append(curr);
        	}
        	xml = xml.substring(0, startIndex) + replacement + xml.substring(endIndex);
        }
        
        return xml;
    }
    
    private void loadRenderElementsForEachRepeatElement(Node node, List<List<Map<String, String>>> renderMaps) throws Exception {
    	
        NodeList list = node.getChildNodes();
        for (int i=0; i<list.getLength(); i++) {
        	Node n = list.item(i);
        	if (n.getNodeName().equalsIgnoreCase("repeat")) {
        		Node templateNode = HtmlFormEntryUtil.findChild(n, "template");
	        	if (templateNode == null) {
	        		throw new IllegalArgumentException("All <repeat> elements must contain a child <template> element.");
	        	}
	        	List<Map<String, String>> l = new ArrayList<Map<String, String>>();
	        	NodeList repeatNodes = n.getChildNodes();
	        	for (int j=0; j<repeatNodes.getLength(); j++) {
	        		Node renderNode = repeatNodes.item(j);
	        		if (renderNode.getNodeName().equalsIgnoreCase("render")) {
	        			l.add(HtmlFormEntryUtil.getNodeAttributes(renderNode));
	        		}
	        	}
	        	renderMaps.add(l);
        	}
        	else {
        		loadRenderElementsForEachRepeatElement(n, renderMaps);
        	}
        }
    }
    
    /** 
     * Applies all the HTML Form Entry tags in a specific XML file 
     * (excluding {@code <macro>, <translations>, and <repeat>)}, by calling the appropriate
     * tag handler (see {@see org.openmrs.module.htmlformentry.handler}) for each tag
     * <p>
     * @param session the current form entry session context
     * @param xml the xml string to process
     * @return the xml string (which should now be html) after tag processing
     * @throws Exception
     */
    public String applyTags(FormEntrySession session, String xml) throws Exception {
        Document doc = HtmlFormEntryUtil.stringToDocument(xml);
        StringWriter out = new StringWriter();
        applyTagsHelper(session, new PrintWriter(out), null, doc);
        return out.toString();
    }

    private void applyTagsHelper(FormEntrySession session, PrintWriter out, Node parent, Node node) {
        TagHandler handler = null;
        // Find the handler for this node
        {
            String name = node.getNodeName();
            if (name != null) {
                handler = HtmlFormEntryUtil.getService().getHandlerByTagName(name);
            }
        }
        
        if (handler == null)
            handler = this; // do default actions

        boolean handleContents = handler.doStartTag(session, out, parent, node);
       
        // Unless the handler told us to skip them, then iterate over any children
        if (handleContents) {
            if (handler != null && handler instanceof IteratingTagHandler) {
                // recurse as many times as the tag wants
                IteratingTagHandler iteratingHandler = (IteratingTagHandler) handler;
                while (iteratingHandler.shouldRunAgain(session, out, parent, node)) {
                    NodeList list = node.getChildNodes();
                    for (int i = 0; i < list.getLength(); ++i) {
                        applyTagsHelper(session, out, node, list.item(i));
                    }
                }
                
            } else { // recurse to contents once
                NodeList list = node.getChildNodes();
                for (int i = 0; i < list.getLength(); ++i) {
                    applyTagsHelper(session, out, node, list.item(i));
                }
            }
        }
        
        handler.doEndTag(session, out, parent, node);
    }

    /** Provides default start tag handling for tags with no custom handler
     * <p>
     * Default behavior is simply to leave the tag unprocessed. That is, any basic HTML
     * tags are left as is.
     */
    public boolean doStartTag(FormEntrySession session, PrintWriter out,
            Node parent, Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            out.print(node.getNodeValue());
        } else if (node.getNodeType() == Node.COMMENT_NODE) {
            // do nothing
        } else {
            out.print("<");
            out.print(node.getNodeName());
            NamedNodeMap attrs = node.getAttributes();
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); ++i) {
                    Node attr = attrs.item(i);
                    out.print(" ");
                    out.print(attr.getNodeName());
                    out.print("=\"");
                    out.print(attr.getNodeValue());
                    out.print("\"");
                }
            }
            out.print(">");
        }
        return true;
    }

    /** Provides default end tag handling for tags with no custom handler
     * <p>
     * Default behavior is simply to leave the tag unprocessed. That is, any basic HTML
     * tags are left as is.
     */
    public void doEndTag(FormEntrySession session, PrintWriter out,
            Node parent, Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            // do nothing
        } else if (node.getNodeType() == Node.COMMENT_NODE) {
            // do nothing
        } else {
            out.print("</" + node.getNodeName() + ">");
        }
    }


	/***
	 * * the function to fetch required data for later form edit display there
	 * are 2 cases: 1)load for form display. i.e encounter = null; mode != Edit
	 * 2)load for edit. i.e. encounter != null; mode == Edit in mode of edit we
	 * have to load the existing obs before we can parse the form, we will need
	 * 1) how many times each newrepeat repeated based on database record 2) xml
	 * fragment in each newrepeat then we paste this fragment in front of this
	 * 
	 * @param xml
	 * @return the xml contains the newrepeat part
	 * @throws Exception
	 */
	public void applyNewRepeat(FormEntrySession session, String xml)
			throws Exception {
		FormEntryContext context = session.getContext();
		int startIndex = 0;
		int endIndex = 0;

		StringBuilder sb = new StringBuilder(xml);

		/* here we get the rpt xml fragment */
		while (true) {              
			startIndex = sb.indexOf("<multiple");
			if (startIndex == -1)
				break;
			startIndex += sb.substring(startIndex).indexOf(">")+1;
			
			
			endIndex = sb.indexOf("</multiple>");
			String xmlfragment = sb.substring(startIndex, endIndex);
			
			xmlfragment = "<span>" + xmlfragment;
			xmlfragment = xmlfragment + "</span>";

			RptGroup rptgroup = new RptGroup();
			this.FillChildObs(xmlfragment, rptgroup);
		
			this.FillIsInTD(xmlfragment, rptgroup);

			rptgroup.setXmlfragment(xmlfragment);
			
			rptgroup.setActionsize(this.CountObsActions(xmlfragment));
			
			context.getExistingRptGroups().add(rptgroup);

			sb = new StringBuilder(sb.substring(endIndex + 11));
		}
		
		/* now we are in enter/edit  submit mode */
		if(session.getContext().getRequest()!=null&&
				session.getContext().getRequest().getParameter("kCount1")!=null){
			//&&(context.getMode() == Mode.EDIT)){
	
			for(int i = 0; i< context.getExistingRptGroups().size();++i){
	       		RptGroup rg = context.getExistingRptGroups().get(i);
	       		String paraName = "kCount"+(i+1);       		
	       		int rpttime = Integer.parseInt((context.getRequest().getParameter(paraName)));
	       		rg.setRepeattime(rpttime);
			}
		}
		
		/* else we are in edit/view loading mode */ 
		else if (context.getMode() == Mode.VIEW || (context.getMode() == Mode.EDIT)) {

			Set<Obs> allObs = session.getEncounter().getAllObs();
			List<Obs> sortedObs = new ArrayList<Obs>();

			int t = CountObs(xml);
			sortedObs = HtmlFormEntryUtil.SortObs(allObs);

			/* remove first t obs, where t stands for total number of obs appear
			 * in the htmlform
			 */
			for (int i = 0; i < t; ++i) {
				sortedObs.remove(0);
			}

			// TODO: now it can't handle when 1 repeat set is a prefix of another
			// i.e. for sequence a,b,c <mutilple> a,b</mutilple> will also
			// match, which is not right
			for (RptGroup rpt : context.getExistingRptGroups()) {
				/* count for the original record */
				if (rpt.getObsNum() == 0)
					continue;
				rpt.setRepeattime(0);//start from 0
				boolean matchFlag = true;
				while (matchFlag == true) {
					for (int i = 0; i < rpt.getObsNum(); ++i) {
						if (sortedObs.size() < i + 1
								|| sortedObs.get(i).getConcept().getConceptId()
										.compareTo(rpt.getChildrenobs().get(i)) != 0) {
							matchFlag = false;
							break;
						} else {
							// else it is a match,
							if (sortedObs.get(i).hasGroupMembers()) {
								// if it is a obsgroup match
								// we would skip the group content
								// and assume there is a match
								// TODO:this is because group members are saving
								// as a Set,see:org.openmrs.obs
								// so there is noway we can match their order
								// except either do a permutation or give an
								// assumption that once the group conceptid match, the
								// group will match
								
								// skip all the member
								int tmp = sortedObs.get(i).getGroupMembers()
										.size();
								for (int j = 0; j < tmp; ++j) {
									sortedObs.remove(i + 1);
								}
								// note the group content in rpt also has been
								// skipped in this.FillChildObs
							}
						}
					}
					if (matchFlag == true) {
						for (int i = 0; i < rpt.getObsNum(); ++i) {
							sortedObs.remove(0);
						}
						// Found 1 match, add to the rpt repeattime
						rpt.setRepeattime(rpt.getRepeattime() + 1);
					} else {
						break;
					}
				}
			}
		}
	}
	
	/***
	 * to see if the multiple was in a <tr><td></td></tr> pair
	 * @param xmlfragment
	 * @param rptgroup
	 * @throws Exception 
	 */
	private void FillIsInTD(String xmlfragment, RptGroup rptgroup) throws Exception {
		xmlfragment = xmlfragment.replaceAll("\r\n", "");
		
		xmlfragment = xmlfragment.trim();
		Document doc = HtmlFormEntryUtil.stringToDocument(xmlfragment);
		NodeList nList = doc.getChildNodes();
		/* if the first child is <tr> then this multiple is not likely 
		 * in a td
		 */
		for ( int i = 0; i< nList.getLength(); ++i){
			if(!nList.item(0).getChildNodes().item(i).getNodeName().equals("#text")){
				rptgroup.setIntd(!("tr").equals(nList.item(0).getChildNodes().item(i).getNodeName()));
				return;
			}
		}
		rptgroup.setIntd(true);
	}

	/****
	 * find out how many obs exsit in this xml that will finally go to the db:
	 *  1)count <obs tag 
	 *  2)count <obsgroup tag if it contains obs tag
	 *  update: if <obs is in a repeat then ignore
	 * @param xml
	 * @return the number of obs
	 * @throws Exception
	 */
	private int CountObs(String xml) throws Exception {
		
		Document doc = HtmlFormEntryUtil.stringToDocument(xml);
		NodeList nList = doc.getChildNodes();

		/* the stack to store children list */
		Stack<NodeList> stack = new Stack<NodeList>();

		stack.push(nList);
		int obsCount = 0;
		while (!stack.empty()) {
			nList = stack.pop();
			for (int i = 0; i < nList.getLength(); ++i) {
				Node node = nList.item(i);
				
				/*skip all obs in a multiple tag */
				if(!"multiple".equals(node.getNodeName())){
					stack.push(node.getChildNodes());
				}
				String nodeName = node.getNodeName();
				if ("obs".equals(nodeName)) {
					++obsCount;
				}
				/* obsgroup tag will count only if it contains obs tag */
				else if ("obsgroup".equals(nodeName)) {
					Stack<NodeList> subStack = new Stack<NodeList>();
					subStack.add(node.getChildNodes());
					while(!subStack.isEmpty()){
						NodeList subNList = subStack.pop();
						for (int j = 0; j < subNList.getLength(); ++j) {
							subStack.add(subNList.item(j).getChildNodes());
							if ("obs".equals(subNList.item(j).getNodeName())) {
								++obsCount;// increase for this obsgroup tag
								subStack.clear();
								break;
							}
						}
					}
				}
			}
		}
		return obsCount;
	}

	/***
	 * parse the xml fragment and store the parsed information in rptgroup
	 * 
	 * @param xml
	 *            the xmlfragment with a rptgroupt
	 * @param rptgroup
	 *            the rptgroup to store a repeat info
	 * @return
	 * @throws Exception
	 */
	private void FillChildObs(String xml, RptGroup rptgroup) throws Exception {

		Document doc = HtmlFormEntryUtil.stringToDocument(xml);
		NodeList nList = doc.getChildNodes();
		Queue<NodeList> stack = new LinkedList<NodeList>();
		stack.add(nList);
		while (!stack.isEmpty()) {
			nList = stack.poll();
			for (int i = 0; i < nList.getLength(); ++i) {
				Node node = nList.item(i);
				String nodeName = node.getNodeName();
				if ("obs".equals(nodeName)) {
					for (int j = 0; j < node.getAttributes().getLength(); ++j) {
						if ("conceptId".equals(node.getAttributes().item(j)
								.getNodeName())) {
							int conceptId = Integer.parseInt(node
									.getAttributes().item(j).getNodeValue());
							rptgroup.getChildrenobs().add(conceptId);
							break;
						}
					}
				} else if ("obsgroup".equals(nodeName)) {
					for (int j = 0; j < node.getAttributes().getLength(); ++j) {
						if ("groupingConceptId".equals(node.getAttributes()
								.item(j).getNodeName())) {
							int conceptId = Integer.parseInt(node
									.getAttributes().item(j).getNodeValue());
							rptgroup.getChildrenobs().add(conceptId);
							break;
						}
					}
				}

				/* we will skip the content of obsgroup */
				if (!"obsgroup".equals(nodeName)) {
					stack.add(node.getChildNodes());
				}
			}
		}
	}

	/**
	 * here we deal with the remaining obs in view/edit model to ensure the
	 * order into the database
	 * 
	 * @param session
	 * @param xml
	 */
	public String applyNewRepeatEnd(FormEntrySession session, String xml)throws Exception {
	
		FormEntryContext context = session.getContext();
		
		context.ResetNewrepeatSeqVal();
		context.ResetCtrlInNewrepeatSeqVal();
		context.ResetNewrepeatTimesSeqVal();

		for (RptGroup rg : context.getExistingRptGroups()) {
			StringBuilder sb = new StringBuilder();
			String addtionalxml = rg.getXmlfragment();
			
			for (int i = 1; i < rg.getRepeattime()+1; ++i) {
				/* output 1 set of repeat here */
				
				/*this should ensure the activegroup is updated*/
				context.beginNewRepeatGroup();
			
				String replaceStr = session.getHtmlGenerator().applyTags(session, addtionalxml);
				
				/*if all obs in this group have null value, then we roll back n in the actions
				 * we only need to see this in edit/enter submit
				 * */
				//TODO:write a function to deal with this if
				if(session.getContext().getRequest()!=null && session.getContext().getRequest().getParameter("kCount1")!=null){
					if(session.getContext().getActiveRptGroup().getIsallobsnulllist().get(session.getContext().getNewrepeatTimesSeqVal()-1).booleanValue()==true){
						session.getSubmissionController().rollbackLastNActions(session.getContext().getActiveRptGroup().getActionsize());
					}
				}
				
				int endOfFirstTag = replaceStr.indexOf('>');
				int startOfLastTag = replaceStr.lastIndexOf('<');
				if (endOfFirstTag < 0 || startOfLastTag < 0 || endOfFirstTag > startOfLastTag)
						replaceStr = "";
				
				replaceStr = replaceStr.substring(endOfFirstTag + 1,startOfLastTag);
					
				sb.append("<span id=\"newRepeat" + context.getNewrepeatSeqVal() + "_"+i
						+ "\" class=\"newRepeat" + context.getNewrepeatSeqVal()
						+ "\" style=\"display:block \" ><table style=\"display:inline\"> \n");
					
				sb.append(replaceStr);
					
				//the remove button
				sb.append("</table>");					
				
				if(context.getMode() == Mode.EDIT){
					sb.append("<input type=\"button\" id=\"removeRowButton"
							+ "\" value=\"X\" size=\"1\"  onclick=\"removeParentWithClass(this,'newRepeat"
							+ context.getNewrepeatSeqVal() + "');\" />\n");
				}
				sb.append("</span>");
	
								
				context.getnewrepeatTimesNextSeqVal();
				context.ResetCtrlInNewrepeatSeqVal();
			}
			xml = xml.replace("<#reservenewrepeat" + context.getNewrepeatSeqVal(),sb.toString());
			context.endNewRepeatGroup();
		}

		return xml;
	}
	
	
	/****
	 * find out how many obs exsit in this xml:
	 *  1)count <obs tag 
	 *  2)count <obsgroup tag if it contains obs tag
	 *  update: if <obs is in a repeat then ignore
	 * @param xml
	 * @return the number of obs
	 * @throws Exception
	 */
	private int CountObsActions(String xml) throws Exception {
		
		Document doc = HtmlFormEntryUtil.stringToDocument(xml);
		NodeList nList = doc.getChildNodes();

		/* the stack to store children list */
		Stack<NodeList> stack = new Stack<NodeList>();

		stack.push(nList);
		int obsCount = 0;
		while (!stack.empty()) {
			nList = stack.pop();
			for (int i = 0; i < nList.getLength(); ++i) {
				Node node = nList.item(i);
				
				/*skip all obs in a multiple tag */
				if(!"multiple".equals(node.getNodeName())){
					stack.push(node.getChildNodes());
				}
				String nodeName = node.getNodeName();
				if ("obs".equals(nodeName)) {
					++obsCount;
				}
				/* obsgroup tag will count only if it contains obs tag */
				else if ("obsgroup".equals(nodeName)) {
					Stack<NodeList> subStack = new Stack<NodeList>();
					subStack.add(node.getChildNodes());
					while(!subStack.isEmpty()){
						NodeList subNList = subStack.pop();
						for (int j = 0; j < subNList.getLength(); ++j) {
							subStack.add(subNList.item(j).getChildNodes());
							if ("obs".equals(subNList.item(j).getNodeName())) {
								++obsCount;// increase for this obsgroup tag
								++obsCount;//1 obsgroup tag will produce 2 actions
								subStack.clear();
								break;
							}
						}
					}
				}
			}
		}
		return obsCount;
	}

}
