/*
 *
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file is called on server start-up and initializes
 * the "configuration" data. It also registers this Connector, so
 * that a link to its configuraion page can appear in the main Adapter
 * configuration page, and so that this Connector is available when
 * craeting a "Connector" (Repository) in 3DExperience.
 */

package com.dassaultsystemes.xsoftware.scmdaemon.filesys;

import java.util.logging.Logger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorDescriptor;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorUtils;

@WebListener
public class FileSysContextListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(FileSysContextListener.class.getName());

    // The parameter in the web.xml file that identifies our services path,
    // which will be /resource/v1/modeler/FileSys
    public static final String PARAM_SERVICE_PATH = "SCMFileSysServicePath"; 
    // The parameter in the web.xml file that identifies our configuration page
    // url. webapps/scmdesignsync/config/start
    public static final String PARAM_CONFIG_PATH = "SCMFileSysConfigPath"; 
    // The parameter in the web.xml file that identifies a glob match for
    // paths our UI uses. These are the paths that will be authenticated.
    public static final String PARAM_CONFIG_AUTH_PATH_MATCH = "SCMFileSysConfigAuthPathMatch";
    // This is the Type displayed when creating a Connector. This MUST
    // currently match the leaf of the SERVICE_PATH
    public static final String SCMNAME = "FileSys";
    // This is a key used for storing the information for this connector.
    public static final String SCMTYPE = "scmfilesys";

    // This is the class that implements our service methods.
    public static final Class<?> SERVICE_IMPL_CLASS = 
            com.dassaultsystemes.xsoftware.scmdaemon.filesys.V1SCMServices.class;

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {}

    @Override
    public void contextInitialized(ServletContextEvent event) {
        logger.fine("FileSys connector starting");

        ServletContext context = event.getServletContext();

        // Extract the context parameters from the web.xml file so we can register
        // this connector.
        String servicePath = context.getInitParameter(PARAM_SERVICE_PATH);
        String configPath = context.getInitParameter(PARAM_CONFIG_PATH);
        String configAuthPathMatch = context.getInitParameter(PARAM_CONFIG_AUTH_PATH_MATCH);
        // If the values are missing for some reason, set them to defaults here.
        // This should never happen in practice, butis used by Dassault Systems
        // for test purposes.
        if (servicePath==null || configPath==null) {
            configPath = "webapps/scmfilesys/config/start";
            servicePath = "/resources/v1/modeler/FileSys";
        }

        logger.fine("filesys resourcePath: " + servicePath);
        logger.fine("filesys configPath: " + configPath);

        ConnectorDescriptor descriptor = ConnectorDescriptor.create();
        descriptor.setScmType(SCMTYPE)
                  .setConnectorName(SCMNAME)
                  .setServiceClass(SERVICE_IMPL_CLASS)
                  .setServicePath(servicePath)
                  .setConfigPath(configPath)
                  .setConfigAuthPathMatch(configAuthPathMatch);
        ConnectorUtils.registerConnector(context, descriptor);


        FileSysConnectorConfig configData = FileSysConnectorConfig.fromContext(context);

        String errMsg = configData.readConfiguration();
        if (errMsg != null) {
            System.out.println(errMsg);
            logger.severe(errMsg);
            return;
        }

        System.out.println("FileSys connector started");
        
    }
}
