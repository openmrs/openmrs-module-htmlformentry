package org.openmrs.module.htmlformentry.handler;

import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.logic.LogicService;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.w3c.dom.Node;

public abstract class LogicConditionalIncludeTagHandler extends SimpleConditionalIncludeTagHandler {

    public boolean shouldIncludeContentHelper(FormEntrySession session, Node parent, Node node) throws BadFormDesignException {

        String velocityTestStr = getAttribute(node, "velocityTest", null);
        String logicTestStr = getAttribute(node, "logicTest", null);

        if (velocityTestStr != null) {
            return processVelocityIncludeLogic(session, velocityTestStr);
        }
        else if (logicTestStr != null) {
            return processLogicIncludeLogic(session, logicTestStr);
        }
        else {
            throw new BadFormDesignException("Include/exclude tag must have either a velocityTest or logicTest attribute");
        }

    }

    /**
     * given a test string, parse the string to return a boolean value for logicTest result
     *
     * @param session
     * @param testStr for ex. = "logicTest='GENDER = F' >"
     * @return a boolean value if this patient is a female
     * @throws org.openmrs.module.htmlformentry.BadFormDesignException
     * @should return a correct boolean value for logic test string
     */
    protected boolean processLogicIncludeLogic(FormEntrySession session, String testStr) throws BadFormDesignException {

        LogicService ls = Context.getLogicService();
        LogicCriteria logicCriteria = null;
        try {
            logicCriteria = ls.parse(testStr);
        } catch (Exception ex) {
            throw new BadFormDesignException(ex.getMessage());
        }

        if (logicCriteria != null) {
            if ("testing-html-form-entry".equals(session.getPatient().getUuid()))
                return false;
            else {
                try {
                    return ls.eval(session.getPatient().getPatientId(), logicCriteria).toBoolean();
                } catch (Exception ex) {
                    throw new BadFormDesignException(ex.getMessage());
                }
            }
        } else {
            throw new BadFormDesignException("The " + testStr + "is not a valid logic expression");//throw a bad form desigm
        }
    }

    /**
     * given a test string, parse the string to return a boolean value for Velocity result
     *
     * @param session
     * @param testStr for ex. = "velocityTest='#if($patient.getPatientIdentifier(5))true #else false #end")' >"
     * @return a boolean value if this patient is a female
     * @throws org.openmrs.module.htmlformentry.BadFormDesignException
     * @should return a correct boolean value for logic test string
     */
    protected boolean processVelocityIncludeLogic(FormEntrySession session, String testStr) throws BadFormDesignException {

        //("#if($patient.getPatientIdentifier(5))true #else false #end"));
        testStr = "#if (" + testStr + ") true #else false #end";
        return session.evaluateVelocityExpression(testStr).trim().equals("true");

    }

}
