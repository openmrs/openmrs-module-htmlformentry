package org.openmrs.module.htmlformentry.widget;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.springframework.util.StringUtils;

public class UserWidget implements Widget {

    private User user;
    private List<User> options;
    
    public UserWidget() { }

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

    public Object getValue(FormEntryContext context, HttpServletRequest request) {
        String val = request.getParameter(context.getFieldName(this));
        if (StringUtils.hasText(val))
            return HtmlFormEntryUtil.convertToType(val, User.class);
        return null;
    }

    public void setInitialValue(Object initialValue) {
        user = (User) initialValue;
    }

    public void setOptions(List<User> options) {
        this.options = options;
    }

}
