package org.openmrs.module.htmlformentry.widget;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.springframework.util.StringUtils;

/**
 * A widget that allows for the selection of a User.  Implemented using a drop-down selection list.
 */
public class UserWidget implements Widget {

    private User user;
    private List<User> options;
    
    public UserWidget() { }

    @Override
    public String generateHtml(FormEntryContext context) {
        if (context.getMode() == Mode.VIEW) {
            if (user != null)
                return WidgetFactory.displayValue(user.getPersonName().toString());
            else
                return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"" + context.getFieldName(this) + "\">");
        // TODO translate
        sb.append("\n<option value=\"\">");
        sb.append(Context.getMessageSourceService().getMessage("general.choose") + "...");
        sb.append("</option>");
        List<User> userList;
        if (options != null) {
            userList = options;
        } else {
            userList = Context.getUserService().getAllUsers();
        }
        for (User u : userList) {
            sb.append("\n<option");
            if (user != null && user.equals(u))
                sb.append(" selected=\"true\"");
            sb.append(" value=\"" + u.getUserId() + "\">").append(u.getPersonName()).append("</option>");
        }
        sb.append("</select>");
        return sb.toString();
    }

    @Override
    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        String val = request.getParameter(context.getFieldName(this));
        if (StringUtils.hasText(val))
            return HtmlFormEntryUtil.convertToType(val, User.class);
        return null;
    }

    @Override
    public void setInitialValue(Object initialValue) {
        user = (User) initialValue;
    }

    /**
     * Sets the Users to use as options for this widget
     * 
     * @param options
     */
    public void setOptions(List<User> options) {
        this.options = options;
    }

}
