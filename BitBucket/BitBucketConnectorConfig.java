/*
 *
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file implements the "configuration" data for this connector
 * This is in the form of a JAXB implementation with fields
 * for each configuration value.
 */

package com.dassaultsystemes.xsoftware.scmdaemon.bitbucket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorFileUtils;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorUtils;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.AutoReleasable;


// Define this as a root element in the XML, and define the property order.
// Here we currently have only have:
// exposedPath: The root path of the repositories, and hence the area of disk
//              that is "exposed" by this connector.
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "exposedPath" })
public class BitBucketConnectorConfig {

    private static final Logger logger = Logger.getLogger(BitBucketConnectorConfig.class.getName());
    private ServletContext context;

    private String exposedPath = "";

    @XmlElement(name = "exposedPath")
    public String getExposedPath() {
        return exposedPath;
    }
    
    public void setExposedPath(String exposedPath) {
         this.exposedPath = exposedPath;
    }

    // This is used to implement locking of the configuration file.
    private ReentrantReadWriteLock cfgLock = new ReentrantReadWriteLock();

    public BitBucketConnectorConfig() {
        // Empty constructors are used for JAX-RS. Do not remove them.
    }

    public BitBucketConnectorConfig(ServletContext context) {
        this.context = context;
    }

    public static BitBucketConnectorConfig fromContext(ServletContext context) {
        return ConnectorUtils.singletonFromContext(context, 
            BitBucketConnectorConfig.class, () -> new BitBucketConnectorConfig(context));
    }

    public ServletContext getServletContext() {
        return context;
    }
    
    /*
     * Obtain a read lock on the configuration file.
     */
    public AutoReleasable readLock() {
        logger.fine("read lock {");
        cfgLock.readLock().lock();
        return () -> {
            readUnlock();
        };
    }

    /*
     * Release the read lock.
     */
    public void readUnlock() {
        logger.fine("read unlock }");
        cfgLock.readLock().unlock();
    }
    
    /*
     * Obtain a write lock on the configuration file
     */
    public AutoReleasable writeLock() {
        logger.fine("write lock {");
        cfgLock.writeLock().lock();
        return () -> {
            writeUnlock();
        };
    }
    
    /*
     * Release the write lock
     */
    public void writeUnlock() {
        logger.fine("write unlock }");
        cfgLock.writeLock().unlock();
    }

    /*
     * For test purposes, this allows us to reset all
     * configuration values.
     */
    public void testReset() {
        exposedPath = "";
    }

    /*
     * Read the configuration data from the default file.
     */
    public String readConfiguration() {
        return readConfiguration(!ConnectorUtils.allowXML() ? "" : getConfigFile());
    }

    /*
     * This is used in internal test mode to setup a default configuration
     */
    private void getTestConfiguration() {
        this.setExposedPath(Paths.get(System.getenv("ADL_ODT_TMP"), "ExposedPath").toString());
    }
    
    /*
     * Read configuration data from a specific file.
     */
    public String readConfiguration(String configFile) {
        logger.info("context: " + context);
        try (AutoReleasable wlock = writeLock()) {
            File file = new File(configFile);
            logger.fine("configFile: " + configFile);

            // For Dassault internal development only:
            // This test configuration is used for setup during regression testing
            if ( !ConnectorUtils.allowXML() ) {
                getTestConfiguration();
                return null;
            }

            if (!file.exists()) {
                String msg = Utils.getStr("ErrorNoConfigFile", getConfigFile());
                logger.info(msg);
                //return msg;
            }

            if (file.isDirectory()) {
                String msg = Utils.getStr("ErrorConfigIsDir", getConfigFile());
                logger.info(msg);
                //return msg;
            }

            try {
                logger.fine("reading start {");
                logger.fine("reading from configFile: " + configFile);

                JAXBContext jaxbContext = JAXBContext.newInstance(BitBucketConnectorConfig.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                
                // ConnectorConfig is intended to be a singleton, but the
                // cast below calls the constructor, and so we need to make sure
                // to reset each variable here.
                BitBucketConnectorConfig data = 
                        (BitBucketConnectorConfig) jaxbUnmarshaller.unmarshal(file);
                exposedPath = data.exposedPath;

                logger.fine("exposedPath: "+exposedPath);
                
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.toString(), ex );
                return ex.toString();
            } finally {
                logger.fine("reading end }");
            }
        }
        return null;
    }

    /*
     * Write the configuration data to the file.
     */
    private void writeXML() throws JAXBException, IOException {
        checkWriteLock();
        
        if ( !ConnectorUtils.allowXML() )
            return;
        logger.fine("configFile: " + getConfigFile());
        
        JAXBContext jaxbContext = JAXBContext.newInstance(BitBucketConnectorConfig.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // Output is pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        FileOutputStream file = ConnectorFileUtils.createProtectedFile(getConfigFile());
        try {
            jaxbMarshaller.marshal(this, file);
        } finally {
            file.close();
        }
    }

    /*
     * Check this thread has the write lock.
     */
    private void checkWriteLock() {
        if (!cfgLock.writeLock().isHeldByCurrentThread()) {
            logger.severe("No write lock");
            throw new IllegalStateException("Write lock must be held");
        }
    }

    // MT-unsafe
    /*
     * Update the connector configuration by storing new
     * configuration data to the XML file.
     */
    public String updateConnector(String path) throws JAXBException, IOException  {
        logger.fine("path:" + path);
        checkWriteLock();

        exposedPath = path;
        writeXML();
        return "";
    }

    /*
     * This returns the path to the configuration file for this connector.
     */
    private static String getConfigFile() {
        // Configuration file is within the server base directory.
        String serverBaseDir = System.getProperty("catalina.base");
        String configFile = 
            Paths.get(serverBaseDir, "WEB-INF", "3DSCM", "bitbucket_connector.xml")
                  .toString();
        return configFile;
    }

}
