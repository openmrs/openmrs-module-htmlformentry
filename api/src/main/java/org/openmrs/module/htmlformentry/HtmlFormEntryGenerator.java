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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.openmrs.module.htmlformentry.handler.IteratingTagHandler;
import org.openmrs.module.htmlformentry.handler.TagHandler;
import org.openmrs.module.htmlformentry.matching.ObsGroupEntity;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides methods to take a {@code <htmlform>...</htmlform>} xml block and turns it into HTML to
 * be displayed as a form in a web browser. It can apply the {@code <macros>...</macros>} section,
 * and replace tags like {@code <obs/>}.
 */
public class HtmlFormEntryGenerator implements TagHandler {

    /**
     * @see #applyMacros(FormEntrySession, String)
     * This method simply delegates to the applyMacros(FormEntrySession, String) method with a null FormEntry session.
     * This is retained for backwards-compatibility, with the limitation that it does not support velocity expressions
     * as macro values
     */
    public String applyMacros(String xml) throws Exception {
        return applyMacros(null, xml);
    }

    /**
     * Takes an XML string, finds the {@code <macros></macros>} section in it, and applies those
     * substitutions
     * <p/>
     * For example the following input:
     * <p/>
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
     * <p/>
     * Would produce the following output:
     * <p/>
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
    public String applyMacros(FormEntrySession session, String xml) throws Exception {
        Document doc = HtmlFormEntryUtil.stringToDocument(xml);
        Node content = HtmlFormEntryUtil.findChild(doc, "htmlform");
        Node macrosNode = HtmlFormEntryUtil.findChild(content, "macros");

        // if there are no macros defined, we just return the original xml unchanged
        if (macrosNode == null) {
            return xml;
        }

        // One way to define macros is simply as the text content of the macros node.  This is left for backwards compatibility
        Properties macros = new Properties();
        String macrosText = macrosNode.getTextContent();
        if (macrosText != null) {
            macros.load(new ByteArrayInputStream(macrosText.getBytes()));
        }

        // Another way to define macros is as child tags to the macros node.
        NodeList children = macrosNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ("macro".equals(node.getNodeName())) {
                String key = HtmlFormEntryUtil.getNodeAttribute(node, "key", "");
                if (StringUtils.isBlank(key)) {
                    throw new IllegalArgumentException("Macros must define a 'key' attribute");
                }
                String value = HtmlFormEntryUtil.getNodeAttribute(node, "value", "");
                if (StringUtils.isBlank(value)) {
                    String expression = HtmlFormEntryUtil.getNodeAttribute(node, "expression", "");
                    if (StringUtils.isBlank(expression)) {
                        throw new IllegalArgumentException("Macros must define either a 'value' or 'expression' attribute");
                    }
                    if (session != null) {
                        value = session.evaluateVelocityExpression("$!{" + expression + "}");
                    }
                    else {
                        value = expression;
                    }
                }
                macros.put(key, value);
            }
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
     * <p/>
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
     * @should return correct xml after filtering out comments
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
     * Takes an xml string, searches for special characters in the string and replaces them
     * with the appropriate ascii value https://issues.openmrs.org/browse/HTML-325
     *
     * @param xml input string
     * @return the xml after replacing special characters with their ascii code
     * @throws Exception
     * @should return correct xml after replacing special characters with their ascii code
     */
    public String substituteCharacterCodesWithAsciiCodes(String xml) throws Exception {
        HashMap<String, String> encodings = new HashMap<String, String>();
        encodings.put("&nbsp;", "&#160;");
        for (String key : encodings.keySet()) {
            Pattern pattern = Pattern.compile(key);
            Matcher matcher = pattern.matcher(xml);
            xml = matcher.replaceAll(encodings.get(key));
        }
        return xml;
    }

    /**
     * Replaces &&, < and > within form with their encoded values within velocity and logic expressions
     * (provides backwards compatibility after refactoring includeIf and excludeIf)
     *
     * @param xml
     * @return
     * @throws Exception
     */
    public String convertSpecialCharactersWithinLogicAndVelocityTests(String xml) throws Exception {

        Pattern lessThan = Pattern.compile("<");
        Pattern greaterThan = Pattern.compile(">");
        Pattern doubleAmpersand = Pattern.compile("&&");

        Matcher velocityMatcher = Pattern.compile("velocityTest=\"[^\"]*\"").matcher(xml);
        StringBuffer afterVelocityChanges = new StringBuffer();

        while (velocityMatcher.find()) {
            String str = velocityMatcher.group();
            str = doubleAmpersand.matcher(str).replaceAll("&amp;&amp;");
            str = lessThan.matcher(str).replaceAll("&lt;");
            str = greaterThan.matcher(str).replaceAll("&gt;");
            velocityMatcher.appendReplacement(afterVelocityChanges, Matcher.quoteReplacement(str));
        }
        velocityMatcher.appendTail(afterVelocityChanges);

        Matcher logicMatcher = Pattern.compile("logicTest=\"[^\"]*\"").matcher(afterVelocityChanges);
        StringBuffer afterLogicChanges = new StringBuffer();

        while (logicMatcher.find()) {
            String str = logicMatcher.group();
            str = doubleAmpersand.matcher(str).replaceAll("&amp;&amp;");
            str = lessThan.matcher(str).replaceAll("&lt;");
            str = greaterThan.matcher(str).replaceAll("&gt;");
            logicMatcher.appendReplacement(afterLogicChanges, Matcher.quoteReplacement(str));
        }

        logicMatcher.appendTail(afterLogicChanges);
        return afterLogicChanges.toString();
    }


    /**
     * Calls the two underlying methods for handling the "<repeat>" tag
     *
     * @param xml
     * @return
     * @throws Exception
     */
    public String applyRepeats(String xml) throws Exception {
        xml = applyRepeatTemplateTags(xml);
        xml = applyRepeatWithTags(xml);
        return xml;
    }

    /**
     * Handles the original version of the <repeat>:
     *
     * Takes an XML string, finds each {@code <repeat></repeat>} section in it, and applies those
     * substitutions
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
     **/
    private String applyRepeatTemplateTags(String xml) throws Exception {
        Document doc = HtmlFormEntryUtil.stringToDocument(xml);
        Node content = HtmlFormEntryUtil.findChild(doc, "htmlform");

        // We are doing this as follows since I can't seem to get the XML node cloning to work right.
        // We can refactor later as needed if we can get it to work properly, or replace the xml library
        // First we need to parse the document to get the node attributes for repeating elements
        List<List<Map<String, String>>> renderMaps = new ArrayList<List<Map<String, String>>>();

        loadRenderElementsForEachRepeatElement(content, renderMaps);

        // Now we are just going to use String replacements to explode the repeat tags properly
        Iterator<List<Map<String, String>>> renderMapIter = renderMaps.iterator();
        int idLabelpairIndex = 0;
        while (xml.contains("<repeat>")) {
            int startIndex = xml.indexOf("<repeat>");
            int endIndex = xml.indexOf("</repeat>", startIndex) + 9;
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
            if (n.getNodeName().equalsIgnoreCase("repeat") && !n.hasAttributes()) {
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

    /**
     * Handle the new, less-verbose version of the <repeat> tag:
     *
    * </pre>
    * <p/>
    * Takes an XML string, finds each {@code <repeat with=""></repeat>} sections in it, and applies those
    * substitutions
    * <pre>
    * { @code
    * <repeat with="['664','No Complaints'], ['832','Weight Loss']">
    *    <obs conceptId="1069" answerConceptId="{0}" answerLabel="{1}" style="checkbox" /><br/>
    * </repeat>
    * }
    * this will be replaced with,
    * { @code
    * <obs conceptId="1069" answerConceptId="644" answerLabel="No Complaints" style="checkbox" /><br/>
    * <obs conceptId="1069" answerConceptId="832" answerLabel="Weight Loss" style="checkbox" /><br/>
    * }
    *
    * </pre>
    *
    * @param xml the xml string to process for repeat sections
    * @return the xml string after repeat substitutions have been made
    * @throws Exception
    */
    private String applyRepeatWithTags(String xml) throws Exception {

        while (xml.contains("<repeat with=")) {

            int startIndex = xml.indexOf("<repeat with=");
            int endIndex = xml.indexOf("</repeat>", startIndex) + 9;

            String xmlToReplace = xml.substring(startIndex, endIndex);

            int substitutionSetsStartIndex = xmlToReplace.indexOf("with=") + 6;
            int substitutionSetsEndIndex =  xmlToReplace.indexOf("]\">") + 1;
            List<List<String>> substitutionSets = getSubstitutionSets(xmlToReplace.substring(substitutionSetsStartIndex, substitutionSetsEndIndex));

            int templateStartIndex = xmlToReplace.indexOf("]\">") + 3;
            int templateEndIndex = xmlToReplace.indexOf("</repeat>");
            String template = xmlToReplace.substring(templateStartIndex, templateEndIndex);

            StringBuilder sb = new StringBuilder();

            String current = template;
            for (List<String> substitutionSet : substitutionSets) {

                int i = 0;

                for (String substitution : substitutionSet) {
                    current = current.replace("{" + i + "}", substitution);
                    i++;
                }

                sb.append(current);
                current = template;
            }

            xml = xml.substring(0, startIndex) + sb + xml.substring(endIndex);
         }

        return xml;
    }

    /**
     * Gets a string like [664,'No Complaints'], [832,'Weight Loss'] and splits it into separate string entries
     * to be used in the repeated html elements
     * @param val = the string to process and get the entires
     * @return List of entries
     */
    private List<List<String>> getSubstitutionSets(String val) {

        List<List<String>> substitutionSet = new ArrayList<List<String>>();

        // first, strip off the leading and trailing brackets
        val = val.replaceFirst("\\s*\\[\\s*", "");
        val = val.replaceFirst("\\s*\\]\\s*$", "");

        // split on " ] , [ "
        for (String subVal : val.split("\\s*\\]\\s*\\,\\s*\\[\\s*")) {

            List<String> set= new ArrayList<String>();

            // trim off the leading quote and trailing quote
            subVal = subVal.replaceFirst("\\s*\\'", "");
            subVal = subVal.replaceFirst("\\s*'\\s*$","");

            // split on " ',' "
            for (String str : subVal.split("\\s*\\'\\s*\\,\\s*\\'\\s*")) {
                set.add(str);
            }

            substitutionSet.add(set);
        }

        return substitutionSet;
    }

    public String applyUnmatchedTags(FormEntrySession session, String xml) throws Exception {
        List<ObsGroupEntity> obsGroupEntities = session.getContext().getUnmatchedObsGroupEntities();

        for (ObsGroupEntity obsGroupEntity : obsGroupEntities) {
            StringWriter out = new StringWriter();
            applyTagsHelper(session, new PrintWriter(out), null, obsGroupEntity.getNode(), null);
            xml = xml.replaceAll("<unmatched id=\"" + obsGroupEntity.getId() + "\" />", Matcher.quoteReplacement(out.toString()));
        }

        return xml;
    }

    /**
     * Applies all the HTML Form Entry tags in a specific XML file (excluding
     * {@code <macro>, <translations>, and <repeat>)}, by calling the appropriate tag handler (see
     * {@see org.openmrs.module.htmlformentry.handler}) for each tag
     * <p/>
     *
     * @param session the current form entry session context
     * @param xml     the xml string to process
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
     * <p/>
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
            } else {
                out.print(">");
            }
        }
        return true;
    }

    /**
     * Provides default end tag handling for tags with no custom handler
     * <p/>
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


    /**
     * Takes an XML string, finds the {@code <restrictByRole></restrictByRole>} section in it, verifies whether
     * the corresponding 'role' is included/excluded to view the content within
     * For example the following input:
     * <p/>
     * <pre>
     * {@code
     * <htmlform>
     * <restrictByRole include="Provider">
     * <obs conceptId="123" labelText="Education"/>
     * </includeIf>
     * </htmlform>
     * }
     * </pre>
     * <p/>
     * Would include the following only if the user is not assigned with "Provider" role
     * <p/>
     * <pre>
     * {@code
     * <htmlform>
     * <obs conceptId="123" labelText="Education"/>
     * </htmlform>
     * }
     * </pre>
     *
     * @param xml the xml string to process for restrictByRole tag
     * @return the xml after applied restrictByRole tag
     * @throws BadFormDesignException
     * @should return correct xml after apply restrictByRole tag
     */
    public String applyRoleRestrictions(String xml) throws BadFormDesignException {

        StringBuilder sb = new StringBuilder(xml);

        while (xml.contains("<restrictByRole")) {
            int startIndex = sb.indexOf("<restrictByRole") + 15;
            int endIndex = sb.substring(startIndex).indexOf(">");
            String includeStr = sb.substring(startIndex, startIndex + endIndex + 1);

            boolean result = HtmlFormEntryGenerator.processRoleRestrictionLogic(includeStr);
            HtmlFormEntryGenerator.removeFirstTagOccurrence(sb, "restrictByRole", result);

            xml = sb.toString();
        }
        return xml;
    }

    /**
     * given a test string, parse the string to return a boolean value for comparison of user role
     * into a specified role
     *
     * @param includeStr for ex. = "include= "Provider">" or ex. = "exclude= "Provider">"
     * @return a boolean value if this user has the "Provider" role assigned
     * @throws BadFormDesignException
     * @should return a correct boolean value for user role test string
     */
    protected static boolean processRoleRestrictionLogic(String includeStr) throws BadFormDesignException {

        int includeOrExcludeTestIndex = includeStr.indexOf("include");
        String testStr = "";
        String[] testRoles = null;
        boolean includeresult = false;
        boolean excluderesult = true;

        if (includeOrExcludeTestIndex >= 0) { // contains list of roles in 'include'
            testStr = HtmlFormEntryUtil.getTestStr(includeStr.substring(includeOrExcludeTestIndex));
            testRoles = testStr.split(",");
            User currentUser = Context.getUserContext().getAuthenticatedUser();
            try {
                Set<Role> assignedRoles = currentUser.getAllRoles();
                for (Role role : assignedRoles) {
                    for (String testRole : testRoles) {
                        if (role.getRole().equals(testRole)) {
                            includeresult = true;
                        }
                    }
                }
            } catch (Exception ex) {
                throw new BadFormDesignException("The " + testStr + "contains an invalid user Role");
            }

            return includeresult;
        } else {
            includeOrExcludeTestIndex = includeStr.indexOf("exclude");
            if (includeOrExcludeTestIndex != -1) { // contains list of roles in 'exclude'
                testStr = HtmlFormEntryUtil.getTestStr(includeStr.substring(includeOrExcludeTestIndex)) ;
                testRoles = testStr.split(",");
                User currentUser = Context.getUserContext().getAuthenticatedUser();
                try {
                    Set<Role> assignedRoles = currentUser.getAllRoles();
                    for (Role role : assignedRoles) {
                        for (String testRole : testRoles) {
                            if (role.getRole().equals(testRole)) {
                                excluderesult = false;
                            }
                        }
                    }
                } catch (Exception ex) {
                    throw new BadFormDesignException("The " + testStr + "contains an invalid user Role");
                }
            }
            return excluderesult;
        }
    }

    public static StringBuilder removeFirstTagOccurrence(StringBuilder sb, String tagName, boolean keepcontent) {
        int startIndex = 0;
        int endIndex = 0;
        int tagLenth = tagName.length();

        if (keepcontent) {
            //remove this pair of tag occurrences and keep the content within
            startIndex = sb.indexOf("<" + tagName + "");
            int startTemp = startIndex + 10;
            int indexOfOpeningQuote = sb.substring(startTemp).indexOf("\"");
            int indexOfClosingQuote = sb.substring(startTemp).indexOf("\"", indexOfOpeningQuote + 1);
            endIndex = sb.substring(startTemp).indexOf(">", indexOfClosingQuote + 1);
            //endIndex = sb.substring(startIndex).indexOf(">");
            sb.replace(startIndex, startTemp + endIndex + 1, "");// should remove the <tagName ...>

            startIndex = sb.indexOf("</" + tagName + ">");
            endIndex = startIndex + tagLenth + 3;
            sb.replace(startIndex, endIndex, ""); //should remove the </tagName>
        } else {
            //remove this part of xml
            startIndex = sb.indexOf("<" + tagName + "");
            endIndex = sb.indexOf("</" + tagName + ">") + tagLenth + 3;
            sb.replace(startIndex, endIndex, "");

        }

        return sb;
    }


    /**
     * Deprecated methods
     */

    /**
     * Old name of the "applyRepeats" tag
     */
    @Deprecated
    public String applyTemplates(String xml) throws Exception {
        return applyRepeats(xml);
    }

}
