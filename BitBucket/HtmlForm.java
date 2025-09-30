/*
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file contains the code to generate the 
 * main HTML form page and a result form after any action has
 * been performed.
 */

package com.dassaultsystemes.xsoftware.scmdaemon.filesys;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.ServletContext;

import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.AutoReleasable;

public class HtmlForm {

    
    private static final String CSSFILE = "scmfilesys.css";
    private static final String CONNECTORCONFIG = "ConnectorConfig";
    private static final String POST = "post";
    private static final String SUBMIT = "submit";
    private static final String ACTION = "action";
    private static final String TEXT = "text";
    private static final String BUTTON = "button";

    public static final String REQUIREJS = "/webapps/VENrequirejs/js/require.js";
    public static final String JSMODULE = "DS/scmfilesys/scmfilesys";
    public static final String WEBAPPS_SCMFILESYS = "/webapps/scmfilesys"; 
    public static final String WEBAPPS_SCMDAEMON_INDEX_JSP = "/webapps/scmdaemon/index.jsp";

    /*
     * This is the main function which generates the Form HTML code
     */
    public static String MainConfig(ServletContext context) {
        String html;
        String newLine = "<br>";
        
        Html Htm = new Html(context.getContextPath(), WEBAPPS_SCMFILESYS);
        
        // Run this before generating any html, to have clear error results
        // if the command throws
        FileSysConnectorConfig config = FileSysConnectorConfig.fromContext(context);
        try (AutoReleasable lock = config.readLock()) {

            html = Htm.pageStart( "" );
            html += "<body class=Upload>\n";
            html += String.format("<DIV class='PAGETITLE'>%s</DIV>\n", 
                    Utils.getStr("Connector"));
            html += Htm.linkCSS(CSSFILE);
            html += Htm.includeScript(REQUIREJS);
            html += Htm.loadScriptModule(JSMODULE);

            // Connector configuration
            html += Htm.dialogSeparator(Utils.getStr(CONNECTORCONFIG));
            html += Htm.formStart(CONNECTORCONFIG, POST, SUBMIT, 
                    "return scmfilesys.Validate2(this)");
            html += Htm.hiddenInput(ACTION,  CONNECTORCONFIG);
            html += Htm.tableStartLeft(true, false, "");
            html += Htm.inputField(TEXT, Utils.getStr("ExposedPath"), 
                    "exposedPath", 
                    config.getExposedPath(), 
                    120);
            html += connectorControlBar(context, Htm);
            html += Htm.tableEnd();
            html += Htm.formEnd();

            html += Htm.tableEnd();
            html += Htm.formEnd();
            html += newLine;
            html += Htm.hLink(context.getContextPath() + WEBAPPS_SCMDAEMON_INDEX_JSP, 
                    Utils.getStr("SCMAdapter"));
            html += Htm.pageEnd();
        }
        return html;
    }
    
    /*
     * This generates the control bar at the bottom of the page
     */
    static private String connectorControlBar(ServletContext context, Html html) {
        
        Html Htm = new Html(context.getContextPath(), WEBAPPS_SCMFILESYS);

        List<String> myButtons = Arrays.asList(
            Htm.myButton(SUBMIT, "", Utils.getStr("Apply")),
            Htm.myButton(BUTTON, "document.getElementById(\"ConnectorConfig\").reset();",  
                    Utils.getStr("Reset")),
            Htm.myButton(BUTTON, 
                    String.format("window.open(\"%s\")", 
                    html.resourcePath("assets/doc/FileSysConnector.htm")), 
                    Utils.getStr("Help")));
        return Htm.controlBar(2, myButtons);
    }

    /*
     * This generates a standard result page in the case of an error being thrown.
     * Always returns to the form.
     *
     * @param context ServletContext
     * @param action Action being performed (for error message)
     * @param e The thrown error. Message is reported.
     */
    public static String formatResultOnThrow(ServletContext context, String action, Throwable e) {
        Html html = new Html(context.getContextPath(), HtmlForm.WEBAPPS_SCMFILESYS);
        String result = html.resultHeader(false);
        result += "<TR>\n";
        result += "<TH>" + Utils.getStr("Action") + "</TH>";
        result += "<TD>" + action + "</TD>";
        result += "</TR>\n";
        result += "<TR>\n";
        result += "<TH>" + Utils.getStr("Error") + "</TH>";
        result += "<TD>" + e.toString() + "</TD>";
        result += "</TR>\n";
        result += html.resultFooter("window.history.back()");
        return result;
    }

    /*
     * Format a result page on success or specific identified failure
     * @param context ServletContext
     * @param success Whether the operation succeeded
     * @param msgs List of message strings to show
     * @param destination Page to jump to after result is acknowledged.
     */
    public static String formatResult (ServletContext context, boolean success, 
                String[] msgs, String destination) {
        Html html = new Html(context.getContextPath(), HtmlForm.WEBAPPS_SCMFILESYS);
        String result = html.resultHeader(success);
        for(int i = 0; i < msgs.length; ++i) {
            String title = msgs[i];
            String value = msgs[++i];
            result += "<TR>\n";
            result += "<TH>" + Utils.getStr(title) + "</TH>";
            result += "<TD>" + value + "</TD>";
            result += "</TR>\n";
        }

        result += html.resultFooter(destination);
        return result;
    }
    /*
     * As above, but default to going back to calling page as the destination.
     */
    public static String formatResult (ServletContext context, boolean success, String[] msgs) {
        return formatResult(context, success, msgs, "window.history.back()");
    }

}
