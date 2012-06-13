package org.openmrs.module.htmlformentry.widget;

import javax.servlet.http.HttpServletRequest;
import org.openmrs.module.htmlformentry.FormEntryContext;
import java.util.HashMap;

/**
 * A widget that implements a upload button to upload images/files, like {@code <input type="file"/>},
 * or as a {@code <obs conceptId="1234">}. The attribute conceptId mentioned in
 * the obs tag has to be the id of a concept with datatype 'complex'.
 * @author Jibesh
 */
public class UploadWidget extends Gadget {

    public String initialValue;
    UploadWidget upldWidget;
    HashMap fileNames = new HashMap();
    static int key=0;

    @Override
    public void setInitialValue(Object initialValue) {
        this.initialValue = (String) initialValue;
    }

    @Override
    public void registerWidgets(FormEntryContext context) {
     context.registerWidget(upldWidget);
    }

    @Override
    public String generateHtml(FormEntryContext context) {

        String id = context.getFieldName(this);
        StringBuilder sb = new StringBuilder();
        sb.append("<input type=\"file\"  id=\"" + id +"\" name=\"" + id + "\"");
        sb.append("/>");

        if (context.getMode() == FormEntryContext.Mode.VIEW) {
            String toPrint = "";
            if (initialValue != null) {
                toPrint = initialValue;
                return WidgetFactory.displayComplexValue(toPrint); // toPrint passed is actually the ObsId
            }
            else {
                toPrint = "____";
                return WidgetFactory.displayEmptyValue(toPrint);
            }
        }else if(context.getMode() == FormEntryContext.Mode.EDIT){
            String toPrint = "";

            if (initialValue != null) {
                toPrint = initialValue;
                String complexValueHtml= WidgetFactory.displayComplexValue(toPrint);
                return "<p>"+complexValueHtml+"</p>"+sb.toString();
            }
            else {
                toPrint = "______";
                return WidgetFactory.displayEmptyValue(toPrint);
            }

        }
       return sb.toString();
    }

    /**
     *
     * @param context
     * @param request
     * @return The name of the file uploaded. Eg. Ball.jpg and is stored in the
     * Obs table as a string in the column value_complex
     */

    @Override
    public String getValue(FormEntryContext context, HttpServletRequest request) {

        fileNames.putAll((HashMap) request.getAttribute(context.getFieldName(this)));
        String returnValue=null;
        returnValue= (String) fileNames.get(key);
        key++;
        if(key==fileNames.size())
            key=0;
        return returnValue;
    }

}
