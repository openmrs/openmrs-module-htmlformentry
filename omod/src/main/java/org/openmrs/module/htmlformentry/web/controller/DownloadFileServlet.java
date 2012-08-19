package org.openmrs.module.htmlformentry.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.obs.ComplexData;
import org.openmrs.web.WebConstants;
import org.openmrs.web.servlet.ComplexObsServlet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;

import static org.openmrs.obs.handler.AbstractHandler.getComplexDataFile;

/**
 * Created with IntelliJ IDEA.
 * @author Jibesh
 * Date: 2/7/12
 * Time: 4:55 PM
 * The ComplexObsServlet can be easily tweaked to implement the work done by this
 */
@Controller
public class DownloadFileServlet extends HttpServlet {

    public static final long serialVersionUID = 1234432L;

    private static final Log log = LogFactory.getLog(ComplexObsServlet.class);

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param obsId The obsevation Id of the Complex observation that needs to be downloaded     *
     *
     */
    @RequestMapping(value="/module/htmlformentry/downloadfile.form", method= RequestMethod.GET)
    protected void doGet(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "obsId")String obsId) throws ServletException, IOException {
        String view=request.getParameter("view");
        HttpSession session = request.getSession();

        if (obsId == null || obsId.length() == 0) {
            session.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "error.null");
            return;
        }

         // if (!Context.hasPrivilege(PrivilegeConstants.VIEW_OBS)) {
      //   session.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Privilege required: " + PrivilegeConstants.VIEW_OBS);
        if (!Context.hasPrivilege("Get Observations")) {
            session.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "Privilege required: Get Observations");
                session.setAttribute(WebConstants.OPENMRS_LOGIN_REDIRECT_HTTPSESSION_ATTR, request.getRequestURI() + "?"
                    + request.getQueryString());
            response.sendRedirect(request.getContextPath() + "/login.htm");
            return;
        }


            Obs complexObs = Context.getObsService().getComplexObs(Integer.valueOf(obsId), view);
            ComplexData cd = complexObs.getComplexData();
            Object data = cd.getData();


            File file = getComplexDataFile(complexObs);
            String[] names = complexObs.getValueComplex().split("\\|");
            String originalFilename = names[0];

            response.setContentType("application/octet-stream");
            if(file.canRead()) {
            response.setHeader("Content-Disposition","attachment;filename=\"" +originalFilename+ "\"");
            FileInputStream fileIn = null;
            try {
                fileIn = new FileInputStream(file);
                ServletOutputStream out = response.getOutputStream();

                byte[] outputByte = new byte[Integer.parseInt(String.valueOf(file.length()))];
                while(fileIn.read(outputByte, 0, Integer.parseInt(String.valueOf(file.length()))) != -1)
                {
                    out.write(outputByte, 0, Integer.parseInt(String.valueOf(file.length())));
                }
                fileIn.close();
                out.flush();
                out.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            }
        else {
                throw new ServletException("Couldn't serialize complex obs data for obsId=" + obsId + " of type "
                        + data.getClass());
            }


    }
}