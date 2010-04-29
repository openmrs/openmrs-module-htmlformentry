package org.openmrs.module.htmlformentry.web;

import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;

/**
 * DWR method for the HTMLFormEntry module. The methods in here are used to access service methods via javascript.
 */
public class DWRHtmlFormEntryService {
    
	/**
	 * Check if user is authenticated
	 * 
	 * @return true/false whether user is authenticated
	 */
    public boolean checkIfLoggedIn() {
        return Context.isAuthenticated();
    }
    
    /**
     * Tries to authenticate the given user
     * 
     * @param user user to authenticate
     * @param pass the user's password
     * @return true/false whether authentication was successful
     */
    public boolean authenticate(String user, String pass) {
        try {
            Context.authenticate(user, pass);
            return true;
        } catch (ContextAuthenticationException ex) {
            return false;
        }
    }

}
