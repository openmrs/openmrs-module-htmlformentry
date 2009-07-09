package org.openmrs.module.htmlformentry.web;

import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;

public class DWRHtmlFormEntryService {
    
    public boolean checkIfLoggedIn() {
        return Context.isAuthenticated();
    }
    
    public boolean authenticate(String user, String pass) {
        try {
            Context.authenticate(user, pass);
            return true;
        } catch (ContextAuthenticationException ex) {
            return false;
        }
    }

}
