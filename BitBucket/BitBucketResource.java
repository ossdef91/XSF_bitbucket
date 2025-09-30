/*
 *
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file contains backend services for the configuration page
 * One service is called to fetch the main configuration page and
 * another called on "submit" of the form to save the configuration
 * data and return the result page.
 */

package com.dassaultsystemes.xsoftware.scmdaemon.filesys;

import java.util.logging.Logger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.AutoReleasable;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

public class FileSysResource {
    private static final Logger logger = Logger.getLogger(FileSysResource.class.getName());

    private static final String ACTION = "Action";
    private static final String ERROR = "Error";
    private static final String NEXTSTEP = "NextStep";
    private static final String TEXT_HTML_UTF8 = MediaType.TEXT_HTML + "; charset=UTF-8";
    
    /*
     * Build a 200 Response from a string "entity"
     */
    private Response makeResponse(String entity) {
        return Response.ok().entity(entity).build();
    }
    
    /*
     * This is called from the configuration panel on submission,
     *
     * @param request The full request
     * @param exposedPath The submitted path from the user
     * @param action The form "action" value.
     * @return Response
     */
    @Path("/config/submit")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_HTML_UTF8)
    public Response configSubmit(@Context HttpServletRequest request, 
            @FormParam("exposedPath") String exposedPath, 
            @FormParam("action") String action) {

        logger.info("exposedPath: "+exposedPath);
        ServletContext context = request.getServletContext();
        FileSysConnectorConfig config = 
            FileSysConnectorConfig.fromContext(request.getServletContext());

        String msg="";
        String displayAction = "";
        try {
            // The only expected action is the main form submission.
            switch (action) {
            case "ConnectorConfig":
                displayAction = Utils.getStr("ConnectorConfig");
                try (AutoReleasable wlock = config.writeLock()) {
                    // Save the new value in the configuration
                    msg = config.updateConnector(exposedPath);
                    if (msg.isEmpty()) {
                        String[] msgs = { ACTION, Utils.getStr("ConnectorConfig"), NEXTSTEP,
                                Utils.getStr("ConfigurationComplete") };
                        return makeResponse(HtmlForm.formatResult(context, true, 
                                msgs, "location.replace(document.referrer)"));
                    } else {
                        String[] msgs = { ACTION, displayAction, ERROR, msg };
                        return makeResponse(HtmlForm.formatResult(context, false, msgs));
                    }
                }

            default:
                String[] msgs = { ACTION, action, ERROR, 
                         Utils.getStr("InvalidAction", action) };
                return makeResponse(HtmlForm.formatResult(context, false, msgs));
            }
        } catch (Exception ex) {
            return makeResponse(HtmlForm.formatResultOnThrow(context, displayAction, ex));
        }
    }

    /*
     * Called for generation of the main configuration page.
     *
     * @param request Main request
     * @return String
     */
    @Path("/config/start")
    @GET
    @Produces(TEXT_HTML_UTF8)
    public String configStart(@Context HttpServletRequest request) {
        return HtmlForm.MainConfig(request.getServletContext());
    }

}
