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
package com.dassaultsystemes.xsoftware.scmdaemon.bitbucket;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;

public class BitBucketConfigApplication extends Application {
    /*
     * This identifies the classes that implement services for this Application.
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(BitBucketResource.class);
        return s;
    }
}
