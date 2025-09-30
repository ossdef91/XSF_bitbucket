/*
 *
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file defines an "Application" for REST service purposes.
 * This particular application implements the services
 * for the configuration web app.
 */
package com.dassaultsystemes.xsoftware.scmdaemon.filesys;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;

public class FileSysConfigApplication extends Application {
    /*
     * This identifies the classes that implement services for this Application.
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(FileSysResource.class);
        return s;
    }
}
