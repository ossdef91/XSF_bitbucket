/*
 *
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file implements utility routines
 */

package com.dassaultsystemes.xsoftware.scmdaemon.bitbucket;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.StringManager;

public class Utils {

    // This is the name of our resource bundle.
    private static final String resourceBundle = 
        "com.dassaultsystemes.xsoftware.scmdaemon.bitbucket.resources.bitbucket";

    /*
     * This gets a formated string from our resource bundle.
     * This is a wrapper around our general string management
     * to pass in the bundle names, saving all the callers having to do that.
     *
     * @param key    Resource key
     * @param s_args Strings applied to format the string
     * @return String
     */
    public static String getStr(String key, String... s_args) {
        return StringManager.getStr(resourceBundle, key, s_args);
    }

}
