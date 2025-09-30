/*
 *
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file defines an "Application" for REST service purposes.
 * This particular application implements the main REST services
 * for the connector.
 */
package com.dassaultsystemes.xsoftware.scmdaemon.filesys;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;

import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorUtils;

public class FileSysApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(ConnectorUtils.getV1DispatcherClass());
        return s;
    }
}
