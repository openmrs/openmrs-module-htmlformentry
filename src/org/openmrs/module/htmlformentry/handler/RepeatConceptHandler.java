package org.openmrs.module.htmlformentry.handler;


/**
 * Currently unimplemented
 */
public class RepeatConceptHandler { /* extends RepeatTagHandler {

    private List<Concept> items;
    private String varToSet;
    
    @Override
    protected void setupBefore(FormEntrySession session, Map<String, String> attributes) {
        ConceptService cs = Context.getConceptService();
        String conceptIds = attributes.get("conceptIds");
        varToSet = attributes.get("var");
        try {
            items = new ArrayList<Concept>();
            for (StringTokenizer st = new StringTokenizer(conceptIds, ", "); st.hasMoreTokens(); ) {
                items.add(cs.getConcept(Integer.valueOf(st.nextToken())));
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Currently only know how to handle a repeat with a conceptIds argument, which is a comma-separated list of concept ids");
        }
    }

    @Override
    protected RepeatControllerAction getRepeatAction(FormEntrySession session, Map<String, String> attributes) {
        // TODO Auto-generated method stub
        return null;
    }
    */

}
