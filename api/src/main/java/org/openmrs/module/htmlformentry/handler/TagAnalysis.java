package org.openmrs.module.htmlformentry.handler;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a list of errors and warnings obtained from analysing a {@link Node} object
 */
public class TagAnalysis {
    
    private List<String> errors = new ArrayList<String>();
    
    private List<String> warnings = new ArrayList<String>();
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
}
