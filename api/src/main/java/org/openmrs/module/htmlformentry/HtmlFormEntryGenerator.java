package org.openmrs.module.htmlformentry;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.logic.LogicService;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.openmrs.module.htmlformentry.handler.IteratingTagHandler;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.openmrs.module.htmlformentry.matching.ObsGroupEntity;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides methods to take a {@code <htmlform>...</htmlform>} xml block and turns it into HTML to
 * be displayed as a form in a web browser. It can apply the {@code <macros>...</macros>} section,
 * and replace tags like {@code <obs/>}.
 */
public class HtmlFormEntryGenerator implements TagHandler {
	
	/**
	 * Takes an XML string, finds the {@code <macros></macros>} section in it, and applies those
	 * substitutions
	 * <p>
	 * For example the following input:
	 * 
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
	 * 
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
	 * Takes an XML string, finds the {@code <translations></translations>} section in it, and
	 * applies those substitutions
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
		for (int i = 0; i < codeNodeList.getLength(); i++) {
			Node codeNode = codeNodeList.item(i);
			if (codeNode.getNodeName().equalsIgnoreCase("code")) {
				String codeName = HtmlFormEntryUtil.getNodeAttribute(codeNode, "name", null);
				if (codeName == null) {
					throw new IllegalArgumentException("All translation elements must contain a valid code name");
				}
				NodeList variantNodeList = codeNode.getChildNodes();
				for (int j = 0; j < variantNodeList.getLength(); ++j) {
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
     * Takes an xml string, searches for 'comments'   in the string using RegEx and filters out
     * the comments from the input string
     *
     * @param xml input string
     * @return the xml string after filtering out comments
     * @throws Exception
     * @should  return correct xml after filtering out comments
     */
    public String stripComments(String xml) throws Exception {

        String regex = "<!\\s*--.*?--\\s*>";    // this is the regEx for html comment tag <!-- .* -->
        Pattern pattern = Pattern.compile(regex,
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(xml);
        xml = matcher.replaceAll("");

        return xml;
    }
	
	/**
	 * Takes an XML string, finds each {@code <repeat></repeat>} section in it, and applies those
	 * substitutions
	 * <p>
	 * 
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
			String template = xmlToReplace.substring(xmlToReplace.indexOf("<template>") + 10,
			    xmlToReplace.indexOf("</template>"));
			StringBuilder replacement = new StringBuilder();
			for (Map<String, String> replacements : renderMapIter.next()) {
				String curr = template;
				for (String key : replacements.keySet()) {
					curr = curr.replace("{" + key + "}", replacements.get(key));
				}
				replacement.append(curr);
			}
			xml = xml.substring(0, startIndex) + replacement + xml.substring(endIndex);
		}
		
		return xml;
	}
	
	private void loadRenderElementsForEachRepeatElement(Node node, List<List<Map<String, String>>> renderMaps)
	    throws Exception {
		
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node n = list.item(i);
			if (n.getNodeName().equalsIgnoreCase("repeat")) {
				Node templateNode = HtmlFormEntryUtil.findChild(n, "template");
				if (templateNode == null) {
					throw new IllegalArgumentException("All <repeat> elements must contain a child <template> element.");
				}
				List<Map<String, String>> l = new ArrayList<Map<String, String>>();
				NodeList repeatNodes = n.getChildNodes();
				for (int j = 0; j < repeatNodes.getLength(); j++) {
					Node renderNode = repeatNodes.item(j);
					if (renderNode.getNodeName().equalsIgnoreCase("render")) {
						l.add(HtmlFormEntryUtil.getNodeAttributes(renderNode));
					}
				}
				renderMaps.add(l);
			} else {
				loadRenderElementsForEachRepeatElement(n, renderMaps);
			}
		}
	}
	
	public String applyUnmatchedTags(FormEntrySession session, String xml) throws Exception {
		List<ObsGroupEntity> obsGroupEntities = session.getContext().getUnmatchedObsGroupEntities();
		
		for (ObsGroupEntity obsGroupEntity : obsGroupEntities) {
			StringWriter out = new StringWriter();
			applyTagsHelper(session, new PrintWriter(out), null, obsGroupEntity.getNode(), null);
			xml = xml.replaceAll("<unmatched id=\"" + obsGroupEntity.getId() + "\" />", out.toString());			
		}
		
		return xml;
	}
	
	/**
	 * Applies all the HTML Form Entry tags in a specific XML file (excluding
	 * {@code <macro>, <translations>, and <repeat>)}, by calling the appropriate tag handler (see
	 * {@see org.openmrs.module.htmlformentry.handler}) for each tag
	 * <p>
	 * 
	 * @param session the current form entry session context
	 * @param xml the xml string to process
	 * @return the xml string (which should now be html) after tag processing
	 * @throws Exception
	 */
	public String applyTags(FormEntrySession session, String xml) throws Exception {
		Document doc = HtmlFormEntryUtil.stringToDocument(xml);
		Node content = HtmlFormEntryUtil.findChild(doc, "htmlform");
		StringWriter out = new StringWriter();
		applyTagsHelper(session, new PrintWriter(out), null, content, null);
		return out.toString();
	}
	
	private void applyTagsHelper(FormEntrySession session, PrintWriter out, Node parent, Node node,
	                             Map<String, TagHandler> tagHandlerCache) {
		if (tagHandlerCache == null)
			tagHandlerCache = new HashMap<String, TagHandler>();
		TagHandler handler = null;
		// Find the handler for this node
		{
			String name = node.getNodeName();
			if (name != null) {
				if (tagHandlerCache.containsKey(name)) {
					// we've looked this up before (though it could be null)
					handler = tagHandlerCache.get(name);
				} else {
					handler = HtmlFormEntryUtil.getService().getHandlerByTagName(name);
					tagHandlerCache.put(name, handler);
				}
			}
		}
		
		if (handler == null)
			handler = this; // do default actions
		
		try {
			boolean handleContents = handler.doStartTag(session, out, parent, node);
			
			// Unless the handler told us to skip them, then iterate over any children
			if (handleContents) {
				if (handler != null && handler instanceof IteratingTagHandler) {
					// recurse as many times as the tag wants
					IteratingTagHandler iteratingHandler = (IteratingTagHandler) handler;
					while (iteratingHandler.shouldRunAgain(session, out, parent, node)) {
						NodeList list = node.getChildNodes();
						for (int i = 0; i < list.getLength(); ++i) {
							applyTagsHelper(session, out, node, list.item(i), tagHandlerCache);
						}
					}
					
				} else { // recurse to contents once
					NodeList list = node.getChildNodes();
					for (int i = 0; i < list.getLength(); ++i) {
						applyTagsHelper(session, out, node, list.item(i), tagHandlerCache);
					}
				}
			}
			
			handler.doEndTag(session, out, parent, node);
		} catch (BadFormDesignException e) {
	        out.print("<div class=\"error\">" + handler + " reported an error in the design of the form. Consult your administrator.<br/><pre>");
	        e.printStackTrace(out);
	        out.print("</pre></div>");
        }

	}
	
	/**
	 * Provides default getAttributeDescriptors handling (returns null)
	 */
	@Override
	public List<AttributeDescriptor> getAttributeDescriptors() {
		return null;
	}
	
	/**
	 * Provides default start tag handling for tags with no custom handler
	 * <p>
	 * Default behavior is simply to leave the tag unprocessed. That is, any basic HTML tags are
	 * left as is.
	 * 
	 * @should close br tags
	 */
	@Override
	public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
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
            // added so that a single <br/> tag isn't rendered as two line breaks: see HTML-342
			if ("br".equalsIgnoreCase(node.getNodeName())) {
				out.print("/>");
			}
			else {
				out.print(">");
			}
		}
		return true;
	}
	
	/**
	 * Provides default end tag handling for tags with no custom handler
	 * <p>
	 * Default behavior is simply to leave the tag unprocessed. That is, any basic HTML tags are
	 * left as is.
	 * 
	 * @should skip br tags
	 */
	@Override
	public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) {
		if (node.getNodeType() == Node.TEXT_NODE) {
			// do nothing
		} else if (node.getNodeType() == Node.COMMENT_NODE) {
			// do nothing
		} else {
            // added so that a single <br/> tag isn't rendered as two line breaks: see HTML-342
			if (!"br".equalsIgnoreCase(node.getNodeName())) {
				out.print("</" + node.getNodeName() + ">");
			}
		}
	}
	
	/**
	 * Takes an XML string, finds the {@code <includeIf></includeIf>} section in it, make a test
	 * against the logicTest/velocityTest to include/exclude the content within
	 * <p>
	 * For example the following input:
	 * 
	 * <pre>
	 * {@code
	 * <htmlform>
	 *      <includeIf logicTest="FEMALE">
	 * 		<obs conceptId="123" labelText="Pregnant?"/>
	 * 	</includeIf>
	 * </htmlform>
	 * }
	 * </pre>
	 * 
	 * Would include the following only if the logicTest="FEMALE" success
	 * 
	 * <pre>
	 * {@code
	 * <htmlform>
	 * 		<obs conceptId="123" labelText="Pregnant?"/>	
	 * </htmlform>
	 * }
	 * </pre>
	 * 
	 * @param xml the xml string to process for includeIf tag
	 * @return the xml string with after includeIf substitution
	 * @throws BadFormDesignException
	 * @should return correct xml after apply include tag
	 * @should return correct xml when the expression contains greater than character
	 */
	public String applyIncludes(FormEntrySession session, String xml) throws BadFormDesignException {
		return processIncludeOrExcludeTag(session, xml, true);
	}
	
	/**
	 * Removes htmlform tag and wraps the form in the div tag.
	 * 
	 * @param xml
	 * @return xml
	 * @should remove htmlform tag and wrap form in div
	 */
	public String wrapInDiv(String xml) {
		xml = xml.trim();
		xml = xml.replaceAll("(?s)<htmlform>(.*)</htmlform>", "<div class=\"htmlform\">$1</div>");
		return xml;
	}
	
	/***
	 * this is an opposite of includeif see applyIncludes
	 * 
	 * @param session
	 * @param xml
	 * @return the xml after applied ExcludeIf tag
	 * @throws BadFormDesignException
	 * @should return correct xml after apply excludeIf tag
	 * @should return correct xml when the expression contains greater than character
	 */
	public String applyExcludes(FormEntrySession session, String xml) throws BadFormDesignException {
		return processIncludeOrExcludeTag(session, xml, false);
	}
	
	/***
	 * Given a include/exclude string. fetch the test expression
	 * 
	 * @param teststr
	 * @return a substring of a test expression
	 * @throws BadFormDesignException
	 * @should extract the correct expression from teststr
	 */
	protected static String getTestStr(String teststr) throws BadFormDesignException {
		if (StringUtils.isBlank(teststr))
			throw new BadFormDesignException("Can't extract the test expression from " + teststr);
		
		//get the text inside the quotes, i.e the expression
		String[] actualExpression = StringUtils.substringsBetween(teststr, "\"", "\"");
		
		if (actualExpression == null || actualExpression.length != 1 || StringUtils.isBlank(actualExpression[0])) {
			throw new BadFormDesignException("Can't extract the test expression from " + teststr);//throw bad design exception here            		
		}
		
		return actualExpression[0];
	}
	
	/***
	 * given a test string, parse the string to return a boolean value for logicTest result or
	 * Velocity result
	 * 
	 * @param session
	 * @param includeStr for ex. = "logicTest='GENDER = F' >"
	 * @return a boolean value if this patient is a female
	 * @throws BadFormDesignException
	 * @should return a correct boolean value for logic test string
	 */
	protected static boolean processIncludeLogic(FormEntrySession session, String includeStr) throws BadFormDesignException {
		
		int logicTestIndex = includeStr.indexOf("logicTest");
		String testStr = "";
		boolean result;
		
		if (logicTestIndex >= 0) {//constains a logicTest
		
			testStr = getTestStr(includeStr.substring(logicTestIndex));
			
			LogicService ls = Context.getLogicService();
			LogicCriteria logicCriteria = null;
			try {
				logicCriteria = ls.parse(testStr);
			}
			catch (Exception ex) {
				throw new BadFormDesignException(ex.getMessage());
			}
			
			if (logicCriteria != null) {
				if ("testing-html-form-entry".equals(session.getPatient().getUuid()))
					result = false;
				else {
					try {
						result = ls.eval(session.getPatient(), logicCriteria).toBoolean();
					}
					catch (Exception ex) {
						throw new BadFormDesignException(ex.getMessage());
					}
				}
			} else {
				throw new BadFormDesignException("The " + testStr + "is not a valid logic expression");//throw a bad form design
				
			}
		} else {
			int velocityTestIndex = includeStr.indexOf("velocityTest");
			if (velocityTestIndex != -1) {
				
				testStr = getTestStr(includeStr.substring(velocityTestIndex));
				
				//("#if($patient.getPatientIdentifier(5))true #else false #end"));
				testStr = "#if (" + testStr + ") true #else false #end";
				result = session.evaluateVelocityExpression(testStr).trim().equals("true");
			} else {
				throw new BadFormDesignException("The " + testStr + "is not a valid velocity expression");//throw a bad form design
			}
		}
		return result;
	}
	
	public static StringBuilder removeFirstIncludeIfOrExcludeIf(StringBuilder sb, boolean keepcontent) {
		int startIndex = 0;
		int endIndex = 0;
		
		if (keepcontent) {
			//remove this pair of includeif  and keep the content within
			startIndex = sb.indexOf("<includeIf");
			if (startIndex == -1) {
				startIndex = sb.indexOf("<excludeIf");
			}
			
			int startTemp = startIndex+10;
			int indexOfOpeningQuote = sb.substring(startTemp).indexOf("\"");
			int indexOfClosingQuote = sb.substring(startTemp).indexOf("\"", indexOfOpeningQuote+1);
			endIndex = sb.substring(startTemp).indexOf(">", indexOfClosingQuote+1);
			sb.replace(startIndex, startTemp+endIndex+1, "");// should remove the <includeIf ...>
			
			startIndex = sb.indexOf("</includeIf>");
			if (startIndex == -1)
				startIndex = sb.indexOf("</excludeIf>");
			endIndex = startIndex + 12;
			sb.replace(startIndex, endIndex, "");
		} else {
			//remove this part of xml
			startIndex = sb.indexOf("<includeIf");
			endIndex = sb.indexOf("</includeIf>") + 12;
			if (startIndex == -1) {
				startIndex = sb.indexOf("<excludeIf");
				endIndex = sb.indexOf("</excludeIf>") + 12;
			}
			sb.replace(startIndex, endIndex, "");
			
		}
		
		return sb;
	}
	
	/** 
	 * Utility method that process and replaces the includeIf/excludeIf tags
	 * 
	 * @param session
	 * @param xml
	 * @param isInclude
	 * @return the new processed xml
	 * @throws BadFormDesignException
	 */
	private String processIncludeOrExcludeTag(FormEntrySession session, String xml, boolean isInclude) throws BadFormDesignException {
		StringBuilder sb = new StringBuilder(xml);
		String tagName = (isInclude) ? "<includeIf" :"<excludeIf";
		while (xml.contains(tagName)) {
			int startIndex = sb.indexOf(tagName) + 10;
			int indexOfOpeningQuote = sb.substring(startIndex).indexOf("\"");
			//get test up to closing quote
			int endIndex = sb.substring(startIndex).indexOf("\"", indexOfOpeningQuote+1);
			String includeStr = sb.substring(startIndex, startIndex + endIndex + 1);
			boolean result = HtmlFormEntryGenerator.processIncludeLogic(session, includeStr);

			HtmlFormEntryGenerator.removeFirstIncludeIfOrExcludeIf(sb, (isInclude) ? result : !result);
			
			xml = sb.toString();
		}
		
		return xml;
	}
	
}
