/*
 *
 * Copyright (c) 2001-2021 Dassault Systemes.
 * All Rights Reserved
 *
 * Example Connector Code
 * This file contains the implementation of all the
 * services that the connector implements, plus the
 * definition of the set of supported "capabilities".
 */

package com.dassaultsystemes.xsoftware.scmdaemon.filesys;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorConstants;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.V1SCMServicesBase;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.exceptions.SCMException;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.AutoReleasable;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.FileTreeFolderBuilder;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.FileTreeResponseBuilder;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.ServiceCall;


/*
 * This class is derived from V1SCMServicesBase, which implements outline
 * versions of all the methods that need to be implemented. Therefore,
 * it is not necessary to implement here any methods that are not applicable
 * to a specific connector.
 * However, in this example we include all methods so that we provide a
 * description of the method in case it is one the connector wants to implement.
 *
 * ERROR HANDLING: Implementation methods should not throw any errors.
 * Rather, they should call a standard method to build a response on error.
 * See the use of serviceCall.buildErrorResponse(Exception e)
 */
public class V1SCMServices extends V1SCMServicesBase implements ConnectorConstants {

    // NOTE: The example connector uses the Logger system to provide tracing
    private static final Logger logger = Logger.getLogger(V1SCMServices.class.getName());

    private static final String STR_NOT_IMPLEMENTED = "Not Implemented";
    private static final String STR_SERVICE_CALL = "Service call: ";

    /*
     * Constructor.
     */
    public V1SCMServices() {

        // This variable defines the version of the services definition that this
        // connector is implementing. The version in this file indicates what the
        // sample connector was developed against. A custom connector developer should
        // check the Release Notes for each release, which will indicate the current
        // services definition version, and what changes have been made from previous
        // versions. The custom connector version should then be updated once the
        // connector code has been appropriately modified.
        setServicesDefinitionVersion("1.1.0");

        // This variable defines the set of "capabilities" that this
        // connector supports. Here we list every possible capability,
        // but a specific implementation can remove those that are not
        // supported.
        // Each capability is then implemented by one or more
        // methods, which need to be defined in this class.
        setSupportedFeatures(EnumSet.of(
            // Can a "tag" be added to a "version" of the data?
            // Method: addVersionTag
            AdapterFeatureE.AddVersionTag,

            // Can a new "branch" of data be created?
            // Method: createBranch
            AdapterFeatureE.CreateBranch,

            // Can a new "item" (i.e. data repository) be created?
            // Method: createItem
            AdapterFeatureE.CreateItem,

            // Can an individual file be downloaded?
            // Method: getFile (used for this and GetFile)
            AdapterFeatureE.DownloadFile,

            // Can a directory and its contents be downloaded?
            // Method: downloadFolderContent
            AdapterFeatureE.DownloadFolder,

            // Can a version address be "resolved" to a specific value?
            // Method: getCommitId
            AdapterFeatureE.GetCommitID,

            // Can the manifest (File Tree) of the contents be fetched?
            // Method: getVersionFileTree
            AdapterFeatureE.GetFileTree,

            // Can the set of "tags" on the data be fetched?
            // Method: getItemTags
            AdapterFeatureE.GetItemTags,

            // Can the set of "registrations" be fetched?
            // That is, the information on repositories that are "registered"
            // to be provided by this connector.
            // Method: getRegistrations
            AdapterFeatureE.GetRegistrations,

            // Can the set of accessible repositories be fetched?
            // Method: getRepositories
            AdapterFeatureE.GetRepositories,

            // Can a direct access path to the data be provided to the user?
            // Method: getRepositoryPath
            AdapterFeatureE.GetRepositoryPath,

            // Can a version address be identified as "static" or "dynamic"?
            // Method: getVersionType
            AdapterFeatureE.GetVersionType,

            // Can a "branch" of the data be locked to prevent modification
            // by another user?
            // Method: lockItemBranch
            AdapterFeatureE.LockItemBranch,

            // Can a "tag" be removed?
            // Method: removeVersionTag
            AdapterFeatureE.RemoveVersionTag,

            // Can a "tag" be moved from one "version" to another?
            // Method: replaceVersionTag
            AdapterFeatureE.ReplaceVersionTag,

            // Can the current "status" of the data repository be reported?
            // Methods: checkRepositoryRunning, getRepositoryStatus
            AdapterFeatureE.ReportRepositoryStatus,

            // Can the current status of an individual "version" of the data be reported?
            // Method: getVersionStatus
            AdapterFeatureE.ReportVersionStatus,

            // Can an individual file be fetched for display?
            // Method: getFile (used for this and DownloadFile)
            AdapterFeatureE.ShowFile,

            // Can the "history" of the data repository be fetched?
            // Method: showHistory
            AdapterFeatureE.ShowHistory,

            // Can a list of the "locked branches" be fetched?
            // Method: showLockedBranches
            AdapterFeatureE.ShowLockedBranches,

            // Can a "branch" of the data be unlocked?
            // Method: unlockItemBranch
            AdapterFeatureE.UnlockItemBranch,

            // Can a new set of data files be uploaded to the data repository?
            // Method: uploadManifest
            AdapterFeatureE.UploadManifest
        ));
        setServicesDefinitionVersion("1.1.1");
    }

    //////////////////////////////////////////////////////////////////////////
    // IMPLEMENTATION METHODS
    //
    // The following methods implement the "services" provided by the connector.
    // Each is optional. If not provided, then there are defaults in the
    // parent class that will return a "Not Implemented" error.
    // The ones listed as "Capabilities" in the ENUM at the top of this
    // class should have some implementation.
    //
    // Each service takes as argument a ServiceCall object. This object
    // provides the method with access to everything that was in the original
    // services call but also provides utility routines to access most of the
    // important information, especially:
    // - The "resolved address" which is the actual path or URL of the "repository" that the 
    //   implementation method should use, after various processes have been applied, 
    //   including access checking. See getResolvedAddress() usage.
    // - The "resolved version" which is the version of the repository addressed
    //   after various processes have been applied, 
    //   including access checking. See getResolvedVersion() usage.
    //   However, most "read" services use the version from the URL (by 
    //   calling getPathElem(VERSION) ), rather than the "resolved" version, 
    //   thus allowing the service to operate on a different version to the one 
    //   being "addressed" by the "resource".
    // - Any values passed in the service URL path. See the getPathElem() calls.
    // - Standard values passed in the service URL parameters. See the getParam() calls.
    //   Note that getParam can only be used for "standard" parameters. If the connector
    //   allows for "special" parameters that are unique to it, then they can be accessed
    //   by getting the entire servlet from the ServiceCall.
    // - Any Payload. See the getPayload() method.
    //
    // Individual method header comments describe the path elements and
    // parameters that are appropriate to that method.
    // The resolved address is available to all services that
    // operate on a specific repository so is not listed for each method.
    //////////////////////////////////////////////////////////////////////////

    /*
     * Checks that any servers are running.
     * For this connector, there are no additional servers to check,
     * so we just return the RUNNING status.
     *
     * Path elements and Parameters:
     *       For this method, there are no applicable path elements or parameters.
     *
     * Response: The Response of this method should include a JSON body with the
     * single entry "value" with a value of "RUNNING". No other responses are
     * appropriate for this method.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response checkRepositoryRunning(ServiceCall serviceCall) {
        
        logger.fine(STR_SERVICE_CALL+serviceCall);
        // Return the STATUS_RUNNING from the parent class.
        return statusResponse(STATUS_RUNNING);
    }

    /*
     *
     * Checks that any servers are running and an individual "repository"
     * is available.
     * For this connector, check that the specified directory exists.
     *
     * Path elements and Parameters:
     *       For this method, there are no applicable path elements or parameters.
     *
     * Response: The Response of this method should include a JSON body with the
     * single entry "value" with a value that is one of:
     * STATUS_RUNNING      : Indicates all is well and the repository is available
     * STATUS_NOREPOSITORY : Indicates that the repository does not exist
     * The following status value may also be returned, but is not used by
     * this connector:
     * STATUS_UNAVAILABLE  : Indicates an issue exists which means that the 
     *    repository may exist, but is not currently available. This is not
     *    used by this connector.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getRepositoryStatus(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        // Get the configuration data, which gives us our path prefix.
        FileSysConnectorConfig config = 
                FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            Path fullPath = null;
            // Get the full repository path
            // NOTE: We use the AutoReleaseable system to release the read lock
            // on the configuraiton data. This try should be around the shortest possible
            // set of code, so that the lock is held for the shortest possible time.
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            // We simply check whether the directory exists.
            if (!Files.exists(fullPath))
                return statusResponse(STATUS_NOREPOSITORY);
            return statusResponse(STATUS_RUNNING);              
        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }
    

    /*
     *
     * Checks that any servers are running and an individual "repository"
     * is available. This method should also check any "version" passed in
     * which identifies a valid "version" of the data.
     *
     * For this connector, check that the specified directory exists, but
     * as there is no versioning do nothing with the "version".
     * This is therefore identical to the "repository status" function,
     * so we just call that. We could also, possibly, have just changed the
     * "map" from service calls to methods so that one is called directly,
     * but this connector does not override that map.
     *
     * Path elements:
     *    VERSION: The repository version
     * Parameters:
     *    None.
     *
     * Response: The Response of this method should include a JSON body with the
     * single entry "value" with a value that is one of:
     * STATUS_RUNNING      : Indicates all is well and the repository is available
     * STATUS_NOREPOSITORY : Indicates that the repository does not exist
     * The following status values may also be returned, but are not used by this
     * connector:
     * STATUS_UNAVAILABLE  : Indicates an issue exists which means that the
     *    repository may exist, but is not currently available.
     * STATUS_NOVERSION    : Indicates that the repository exists, but the specified
     *    version does not exist.
     * STATUS_LOCKED       : Indicates that the repository exists, and that the
     *    version exists, but it addresses a "branch" of data which is currently
     *    locked. In this case, the Response JSON body may also contain an
     *    entry "details" with a value that is a JSON object with entries
     *    "user" and "date" indicating who owns the lock and when it was obtained.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getVersionStatus(ServiceCall serviceCall) {

        // We just call the getRepositoryStatus. If we did want something specific
        // to the "version", then we would use the version from the
        // params like this:
        // String versionAddress = getPathElem(VERSION);
        // This is the version provided in the service call URL, rather than the
        // "Version Address" extracted from the Resource object. 
        // Please see the SDK documentation for a discussion on what these different
        // versions are and when each should be used.
        return getRepositoryStatus(serviceCall);
    }
    
    /*
     *
     * This method fetches the "manifest" of folders and files for the
     * requested data repository version.
     * The response contains a hierarchical structure with elements for
     * each folder and the files/folders is contains.
     * Each folder and file have certain "properties", especially an "id" that
     * may be passed to subsequent services such as those to download data.
     * For files, there are a number of optional values that can be
     * included for each item, if they are available. These are detailed below.
     * Use the helper classes as shown in this sample implementation to ensure that
     * the result is correctly formed.
     *
     * For this sample connector, we simply scan the appropriate file system
     * directory.
     *
     * Path elements:
     *    VERSION: The repository version
     * Parameters:
     *    PATH: A relative path within the repository to filter results.
     *
     * Response: A hierarchical JSON structure of files and folders. The following
     * example shows the structure. But the helper classes/routines MUST be used.
     * {
     *   "totalItems":1,
     *   "member";[
     *     {
     *       "value":{
     *         "id":"V1.1",
     *         "attributes";["type","name","id"],
     *         "structure": {
     *           "totalItems":1,
     *           "member":[
     *             {
     *               "type":"folder",
     *               "name":"/",
     *               "id":"/",
     *               "children": {
     *                 "totalItems": 2,
     *                 "rollupFileCount":1,
     *                 "member": [
     *                   {
     *                     "type":"folder",
     *                     "name":"subDir",
     *                     "id":"/subDir",
     *                     "children":{}
     *                   },
     *                   {
     *                     "type":"file",
     *                     "name":"tmp.txt",
     *                     "id":"/tmp.txt"
     *                   }
     *                 ]
     *               }
     *             }
     *           ]
     *         }
     *       }
     *     }
     *   ]
     * }
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getVersionFileTree(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            String version = serviceCall.getPathElem(VERSION);
            // This example does not filter for the path right now.
            // String path = serviceCall.getParam(PATH);

            // In case of error, an exception can be thrown:
            //throw new SCMException(Status.BAD_REQUEST, "getVersionFileTree: Invalid parameters");

            String fullAddress = serviceCall.getResolvedAddress();
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            // This exception uses the string manager class for language mapping.
            //String msg = Utils.getStr("ErrorNoConfig", repository);
            //throw new SCMException(Status.FORBIDDEN, msg); 
            
            // One optional piece of the response is the "resolved" value of the version
            // This is returned as the top-level "id" value.
            // Here, we set it the same as the version, as an example.
            String resolvedVersion = version;

            // Note: You may want to cache the file tree, if you have some way to
            // identify when it has changed.
            
            // For this connector, we scan the file system to get the
            // directories and files to show. See the readFileTree utility routine.

            // These are the attributes we will return for each file. Additional
            // values can be provided such as "size" and "modified" (last modified
            // date stamp.)
            List<String> attributes = Arrays.asList("type", "name", "id");

            // This builder will provide our final response.
            FileTreeResponseBuilder resp = serviceCall.createFileTreeResponseBuilder();

            // Now initialize with the general information:
            // - a top-level identifier. Here we use the full path, but a
            //   system that supports versioning might want to return a value
            //   that combines the address and version.
            // - the resolved version
            // - the list of provided attributes
            resp.setFullAddress(fullAddress)
                .setResolvedVersion(resolvedVersion)
                .setAttributes(attributes);

            // Create the top-level folder, added to the response.
            // The actual name/path are set in readFileTree.
            FileTreeFolderBuilder topFolder = resp.addNewFolder();

            // We then read the folder contents, which adds the folder to the response.
            readFileTree(topFolder, fullPath, null);

            // And get the Response by building.
            return resp.build();
        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }

    /*
     *
     * Returns the contents of an individual file as a streamed Response.
     * This method is used for two services, one to "fetch" the file to
     * be displayed in a browser, and one to "download". The difference
     * is in the Content-Disposition indicator in the response.
     *
     * For this connector, all we have to do is read the file from disk.
     *
     * Path elements:
     *    VERSION: The repository version
     * Parameters:
     *    FILE: This is the "id" of a file entry from the "getVersionFileTree" service.
     *          For this connector, that is the relative path from the address
     *          of the "repository", so we can append those and then
     *          simply fetch that file.
     *    DOWNLOAD: A boolean indicating whether this is being called for
     *          a file download or fetch.
     *
     * Response: A streamed file with Content-Disposition if download is being used.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getFile(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);
        // This is how you can get the original servlet if you need it.
        // ServletContext context = serviceCall.getServletContext();

        String fileId = serviceCall.getParam(FILE);
        boolean isDownload = serviceCall.getParamBool(DOWNLOAD);

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {

            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }
            // Now we can just add the fileId to get the full file path.
            Path fullFilePath = Paths.get(fullPath.toString(), fileId);

            InputStream is = new FileInputStream(fullFilePath.toString());
            // streamOutput takes a stream and returns an approriate Response.
            String leafName = new File(fileId).getName(); // TODO Potential problem with slashes on Win
            Response resp = serviceCall.buildStreamingResponse(is, leafName, isDownload);

            return resp;
        } catch (Exception e) {
            return serviceCall.buildErrorResponse(e);
        }
    }

    /*
     *
     * Returns a "package" containing all the files under a specified "path"
     * within the specified version of the repository.
     * The package is streamed back as a "download" Content-Disposition.
     *
     * For this connector, we can simply "tar" the directory.
     *
     * Path elements:
     *    VERSION: The repository version
     * Parameters:
     *    FOLDER: This is the "id" of a folder entry from the "GetFileTree" service.
     *          For this connector, that is the relative path of the folder from the
     *          top of the repository, so we can just append them and then
     *          simply package up that directory.
     *    NAME: A file name for the returned package.
     *    FORMAT: The format to be used. Can be one of FORMAT_ZIP (default) or FORMAT_TAR
     *
     * Response: A streamed file with Content-Disposition set for download
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response downloadFolderContent(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        String folderId = serviceCall.getParam(FOLDER);
        String name = serviceCall.getParam(NAME);
        // If the name is not given, or is "/", which can happen when the whole
        // area is being downloaded, then give a default of "content".
        if (name.isEmpty() || "/".equals(name)) name = "content";
        String format = serviceCall.getParam(FORMAT, FORMAT_ZIP);

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {

            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }
            // Now we can just add the directoryId to get the full directory path.
            Path fullDirectoryPath = Paths.get(fullPath.toString(), folderId);

            // Build the command to run to create our package.
            // The commands send output to stdout, which is then streamed
            // as the return data in the response.
            String zipperCmd;
            switch (format) {
            case FORMAT_TAR:
                zipperCmd = "tar c .";
                break;
            case FORMAT_ZIP:
                zipperCmd = "zip -r - .";
                break;
            default:
                String msg = Utils.getStr("InvalidFormat", format);
                throw new SCMException(Status.FORBIDDEN, msg);
            }

            logger.fine("Executing command: " + zipperCmd);
            Process zipper = Runtime.getRuntime().exec(zipperCmd, null, 
                                               new File(fullDirectoryPath.toString()));

            logger.fine("Created archive of " + fullDirectoryPath);
            if (!FORMAT_ZIP.equals(format)) {
                try (BufferedReader archiveErr = new BufferedReader(
                        new InputStreamReader(zipper.getErrorStream()))) {
                    String errMsg;
                    if ((errMsg = getError(archiveErr)) != null) {
                        throw new SCMException(Status.NOT_FOUND, errMsg);
                    }
                }
            }

            InputStream archiveStream = zipper.getInputStream();
            String leafName = new File(name + "." + format).getName();
            Response resp = serviceCall.buildStreamingResponse(archiveStream, leafName, true);
            return resp;
        } catch (Exception e) {
            return serviceCall.buildErrorResponse(e);
        }
    }
    
    /*
     *
     * This identifies whether a "version" of the repository is
     * "static" (data within it will not change) or "dynamic" (the version
     * references something like a "branch" which could change) or "unknown".
     * It is the "version" passed in the request URL that should be checked, rather
     * than the one referenced from the "resource".
     * The purpose of this value is to identify whether a reference is being
     * made to something that should not change, and therefore the referring object
     * is in an appropriate state to be "Released" (since you don't want to release
     * something and have the data change at a later point.)
     *
     * For this connector we always return "static". This is because
     * we do not have "versions". If we returned "dynamic", which might be
     * more correct, then the referring item could never be released.
     * Ideally, there would be some kind of flag on the directory that prevents
     * future changes taking place, and this method would then check that flag.
     *
     * Path elements:
     *    VERSION: The repository version
     * Parameters:
     *    None.
     *
     * Response: A JSON with a "type" field with a value that is one of
     *           "static", "dynamic" or "undefined".
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getVersionType(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        //final String DYNAMIC = "dynamic";
        final String STATIC = "static";
        //final String UNDEFINED = "undefined";

        String versionType = STATIC;
        JsonObject result = Json.createObjectBuilder()
                    .add(TYPE, versionType)
                    .build();
        ResponseBuilder respBuilder = Response.status(200).entity(result);
        Response resp = respBuilder.build();
        return resp;
    }
    
    /*
     *
     * This "resolves" a "version" to a static value.
     * For example, for the "Git" SCM system it would resolve something like
     * a branch name to a commit ID.
     * It is the "version" passed in the request URL that should be resolved, rather
     * than the resolved version.
     *
     * For this connector we have no versioning, so this method would not
     * really be supported, and could be removed from the "capabilities" list
     * But to show how it might work, it is implemented here to just return a 
     * fixed value of "--"
     *
     * Path elements:
     *    VERSION: The repository version
     * Parameters:
     *    None.
     *
     * Response: A JSON with an "id" field with a value that is the resolved value
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getCommitId(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);
        
        // This gets the version address.
        // String version = serviceCall.getPathElem(VERSION).trim();        
        // We just return a fixed value.
        JsonObjectBuilder builder = Json.createObjectBuilder().add(ID, "--");
        return buildOK(builder.build());
    }
        

    /*
     *
     * This should return a "path" to the user that tells them how to
     * directly access the "repository". This might be a value they
     * specify to some SCM client to fetch the data. For example, a value
     * than can be specified to "git clone".
     *
     * For this connector we return the full path to the repository.
     *
     * Path elements:
     *    None.
     * Parameters:
     *    None.
     *
     * Response: A JSON with a "fullRepositoryPath" field with a value that is the path
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getRepositoryPath(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);
        
        FileSysConnectorConfig config = 
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            JsonObject result = Json.createObjectBuilder()
                .add(FULLREPOPATH, fullPath.toString())
                .build();
            return buildOK(result);
        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }

    /*
     *
     * This returns a list of "repositories" that are available to
     * the user. The intention is that this can be used as a select list
     * for a GUI to let the user select a repository.
     *
     * For this connector we will return a list of the directories
     * directly under the "exposed" root path.
     * This is not particularly useful, but is only for demonstration.
     *
     * Path elements:
     *    None.
     * Parameters:
     *       PATH 
     *            Restrict the results to paths starting with the one given
     *            This is optional, and not used in this connector.
     *
     * Response: A JSON with a "totalItems" containing a count of the results, and
     *   a "members" array with one entry for each result, each entry having a "path".
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getRepositories(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);
        
        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // This is the root path we will look at.
            String pathPrefix = null;
            try (AutoReleasable lock = config.readLock()) {
                pathPrefix = config.getExposedPath();
            }
            int pathPrefixLen = pathPrefix.length();

            // The path to filter by. Not used here.
            //String path = serviceCall.getParam(PATH);
            
            int totalItems = 0;
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(pathPrefix))) {
                for (Path entry : stream) {
                    // If a directory, add it to the array, minus the starting dir.
                    String dir = entry.toString().substring(pathPrefixLen);
                    arrayBuilder.add(Json.createObjectBuilder().add(PATH, dir));
                    ++totalItems;
                }
            }
  
            JsonObject result = Json.createObjectBuilder()
                    .add(TOTALITEMS, totalItems)
                    .add(MEMBER, arrayBuilder).build();
            return buildOK(result);
        }  catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }

    }
    
    /*
     *
     * This adds a "tag" (alias name) to a specific "version" of repository
     * data. For example, adding a commit tag in Git.
     * NOTE: This service should not "move" a tag that is present on another
     * version. Also, it is not considered an error if the tag is already
     * present on the version specified.
     *
     * For this connector, this is not applicable as we don't have
     * versions. But this example shows the basic approach.
     *
     * Path elements:
     *    VERSION: The repository version
     *    TAG: The tag to be added
     * Parameters:
     *    None.
     * Payload Values:
     *    MESSAGE: A "comment" that can be stored with the tag if the target
     *        system supports that.
     *
     * Response: An empty JSON on success, else throws an error.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response addVersionTag(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        /* Sample implementation code

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // Extract the data from the service call.
            String version = serviceCall.getPathElem(VERSION);
            String tag = serviceCall.getPathElem(TAG);
            String message = serviceCall.getPayload().getString(MESSAGE, "");

            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            // Here is where a connector would implement its specific
            // tagging operation.

            // Return a standard empty response.
            return buildOK();
        }

        */

        // For this sample, just throw "Not Implemented"
        return buildNotImpl(serviceCall);

    }

    /*
     *
     * This replaces (moves) a "tag" (alias name) to a specific "version" of repository
     * data. For example, moving a commit tag in Git from one commit to another.
     * NOTE: This service DOES allow "move" of a tag that is present on another
     * version, and this is the only difference between this and the addTag method.
     * Therefore, in practice, it is likely a common routine would be used for these.
     *
     * For this connector, this is not applicable as we don't have
     * versions. But this example shows the basic approach.
     *
     * Path elements:
     *    VERSION: The repository version
     *    TAG: The tag to be replaced
     * Parameters:
     *    None.
     * Payload Values:
     *    MESSAGE: A "comment" that can be stored with the tag if the target
     *        system supports that.
     *
     * Response: An empty JSON on success, else throws an error.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response replaceVersionTag(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        /* Sample implementation code

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // Extract the data from the parameters.
            String version = serviceCall.getPathElem(VERSION);
            String tag = serviceCall.getPathElem(TAG);
            String message = serviceCall.getPayload().getString(MESSAGE, "");

            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            // Here is where a connector would implement its specific
            // tagging operation.

            // Return a standard empty response.
            return buildOK();
        }

        */

        // For this sample, just throw "Not Implemented"
        return buildNotImpl(serviceCall);
    }

    /*
     *
     * This removes a "tag" (alias name) from a repository.
     * For example, removing a commit tag in Git.
     * NOTE: The tag is removed from whatever version it is on, so the
     * version is ignored.
     * NOTE: It is not considered an error to remove a tag that does not exist.
     *
     * For this connector, this is not applicable as we don't have
     * versions. But this example shows the basic approach.
     *
     * Path elements:
     *    VERSION: The repository version (unused)
     *    TAG: The tag to be removed
     * Parameters:
     *    None.
     *
     * Response: An empty JSON on success, else throws an error.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response removeVersionTag(ServiceCall serviceCall) {
            
        logger.fine(STR_SERVICE_CALL+serviceCall);

        /* Sample implementation code

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // Extract the data from the parameters.
            String tag = serviceCall.getPathElem(TAG);

            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            // Here is where a connector would implement its specific
            // tagging operation.

            // Return a standard empty response.
            return buildOK();

        }

        */

        // For this sample, just throw "Not Implemented"
        return buildNotImpl(serviceCall);
    }

    /*
     *
     * This method returns a list of the tags. This is either
     * the tags on an individual "version" of the repository
     * or all tags anywhere in use on the repository.
     *
     * For this connector, this is not applicable as we don't have
     * versions. But this example shows the basic approach.
     *
     * Path elements:
     *    VERSION: The repository version. May be empty.
     * Parameters:
     *       MASK: May be "tags" (version tags only) or "aliases" 
     *             (version and branch tags.)
     *       FROMADDRESS: This service can return tags for an alternative
     *              repository to that normally identified from the
     *              "resource". This allows tags to be fetched when initially
     *              associating an object with a repository, so while the resource
     *              doesn't yet address the repository we want the tags from.
     *              The parameter provides the full address of that alternative repository.
     *
     * Response: A JSON structure such as the one shown below. "value" is a list
     *     containing one entry for each tag. Each entry has: a "type" of either
     *     "tag" or "branch", the "tag" itself, the id of the "version"
     *     that tag is on, what Version address (selector) should be
     *     used to choose this tag, and then optionally a "properties" entry which can contain
     *     other properties that depend on the connector.
     * {
     *   "value": [
     *     {"type":"tag", "tag":"Gold", "id":"1.2", "selector":"Gold", 
     *       "properties":{"message":"Released version", "author":"ian"}
     *     },
     *     {"type":"branch", "tag":"B1", "id":"1", "selector":"B1:Latest",
     *       "properties":{"message":"Dev branch", "author":"ian"}
     *     },
     *  }
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getItemTags(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        /* Sample implementation code 

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // Extract the data from the service call
            String version = serviceCall.getPathElem(VERSION);
            String message = serviceCall.getParam(MASK);
            String fromAddress = serviceCall.getParam(FROMADDRESS);

            // This is the repository path, if no fromAddress is given.
            Path fullPath = fromAddress;
            if ("".equals(fullPath))
                try (AutoReleasable lock = config.readLock()) {
                    fullPath = getFullRepositoryPath(config, serviceCall);
                }

            // Here is where a connector would implement its specific
            // code to get the tags.
            // Let's assume that gives an array of items of type "Object", each of which
            // has accessor routines to get the information.
            // The result can then be built using something like the following:
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            Object[] tagsList = null;
            for (Object obj : tagsList) {
                arrayBuilder.add(Json.createObjectBuilder()
                    .add(TYPE, obj.getType())
                    .add(TAG, obj.getTag())
                    .add(ID, obj.getVersion())
                    .add(SELECTOR, obj.getSelector())
                    .add(PROPERTIES, Json.createObjectBuilder()
                        .add(USERNAME, obj.getUser())
                        .add(TIME, obj.getTime())
                        .add(MESSAGE, obj.getMessage())
                    )
                );
            }

             JsonObject result = Json.createObjectBuilder()
                     .add(VALUE, arrayBuilder)
                     .build();
             return buildOK(result);

        }

        */

        // For this sample, just throw "Not Implemented"
        return buildNotImpl(serviceCall);
    }
    
    /*
     *
     * This method returns the "history" of changes to the repository.
     * For a version control system, this means the details of the
     * changes made to each version and who made them.
     *
     * For this connector, this is not applicable as we don't have
     * versions. But this example shows the basic approach.
     *
     * Path elements:
     *    VERSION: The repository version
     *            The version the history should start from. The report
     *            starts at this version and works back up the revision chain.
     * Parameters:
     *    LASTCHANGES: The number of versions to report. If not given (value is "")
     *        then report all versions up to the "root".
     *    PATH: A file path. Optional. If given, then the report should be restricted
     *        to versions that impacted the file at this path. That is, the report
     *        is a history of that individual file.
     *    FILEID: As an alternative to a PATH, a FILEID as returned by the getVersionFileTree
     *        method can be passed in.
     *
     * Response: A JSON structure such as the one shown below. "totalItems" is a count
     *     of the number of version entries returned in the "member" array. "attributes"
     *     list the set of attributes that are returned. The example below shows typical
     *     attributes, but a connector can return others. Each entry in the "member"
     *     array then includes those attributes.
     * {
     *   "totalItems":3,
     *   "attributes":["id","tags","user","date","comment"],
     *   "member": [
     *     {"id":"1.1", "tags":["tag1"], "user":"ian",
     *             "date":1566911322000,"comment":"First checkin"},
     *     {"id":"1.2", "tags":[], "user":"ian",
     *             "date":1566911342579,"comment":"First change"},
     *     {"id":"1.3", "tags":["Gold","RelA"], "user":"ian",
     *             "date":1566911427821,"comment":"Another change"}
     *   ]
     *  }
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response showHistory(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        /* Sample implementation code

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // Extract the data from the parameters.
            String version = serviceCall.getPathElem(VERSION);
            int lastChanges = serviceCall.getParamInt(LASTCHANGES, 0);
            String pathParam = serviceCall.getParam(PATH).trim();
            String fileId = serviceCall.getParam(FILEID).trim();

            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            // Here is where a connector would implement its specific
            // code to get the history.
            // Let's assume that gives an array of items of type "Object", each of which
            // has accessor routines to get the information.
            // The result can then be built using something like the following:
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            Object[] tagsList = null;
            int totalItems = 0;
            for (Object obj : tagsList) {
                JsonArrayBuilder tagArrayBuilder = Json.createArrayBuilder();
                if (obj.getTags() != null) {
                    for(String tag: obj.getTags())
                        tagArrayBuilder.add(tag);
                }
                arrayBuilder.add(Json.createObjectBuilder()
                    .add(ID, obj.getId())
                    .add(USER, obj.getUser())
                    .add(TAGS, tagArrayBuilder)
                    .add(DATE, obj.getDate())
                    .add(COMMENT, obj.getComment())
                );
                ++totalItems;
            }

            JsonObject result = Json.createObjectBuilder()
                    .add(TOTALITEMS, totalItems)
                    .add(ATTRIBUTES, Json.createArrayBuilder()
                            .add(ID).add(TAGS).add(USER).add(DATE).add(COMMENT))
                    .add(MEMBER, arrayBuilder)
                    .build();

             return buildOK(result);

        }

        */

        // For this sample, just throw "Not Implemented"
        return buildNotImpl(serviceCall);
    }
    
    /*
     *
     * This method creates a new "branch" of development in the repository.
     * For example, in Git it would simply create a new branch tag on the commit.
     *
     * For this connector, this is not applicable as we don't have
     * versions. But this example shows the basic approach.
     *
     * Path elements:
     *    VERSION: The repository version
     *           The version the branch should be created from. Note that
     *           the branch is created from this point, not from the version
     *           of the resource.
     * Parameters:
     *    None.
     * Payload values:
     *    BRANCHNAME: The name of the new branch to be created.
     *
     * Response: A JSON structure containing  two values:
     *     1) A "branchName" value which is the value that
     *     should be used as the versionAddress for future calls. This can be
     *     different to the branch name passed in for certain revision control systems. For
     *     example, for the DesignSync SCM system, the branch might be "B1", but the 
     *     returned value "B1:Latest"
     *     2) An "existed" boolean indicating whether the branch already existed. No error
     *     should be generated in this case.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response createBranch(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        //FileSysConnectorConfig config =
        //    FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // Extract the data from the parameters.
            // This is the version. Not used at present.
            //String version = serviceCall.getPathElem(VERSION);
            String branchName = serviceCall.getPayload().getString(BRANCHNAME, "");

            // This is the repository path. Not used at present
            //Path fullPath = null;
            //try (AutoReleasable lock = config.readLock()) {
            //    fullPath = getFullRepositoryPath(config, serviceCall);
            //}

            // This is where the connector would perform the operation to
            // create the branch.

            // For this test connector, we will simlpy return a valid
            // result as if this had worked, with the returned branchName
            // being the same as that passed in, and indicating that the
            // branch did not previously exist.            
            JsonObject status = Json.createObjectBuilder()
                    .add(BRANCHNAME, branchName)
                    .add(EXISTED, false)
                    .build();

            return buildOK(status);

        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }
    
    /*
     *
     * This method creates a new "repository" for data.
     * For example, in Git it would simply create a new bare repository.
     *
     * For this connector, we simply create the directory requested under
     * the "exposed" area. 
     *
     * Path elements:
     *    None.
     * Parameters:
     *    None.
     * Payload values:
     *    REPOPATH: The "root" of the repository path (i.e. parent directory.) 
     *       For this connector, this is the path relative to the exposed area
     *    ITEMADDRESS: The name of the repository under that REPOPATH. For this connector
     *       the name of the leaf directory to be created
     *    BRANCHNAME: The name of the initial "branch" of repository data. Not used here
     *    MESSAGE: A message that can be recorded, such as a description of the
     *       new repository. Not used here.
     *    REPOSETTINGS: A JSON object that contains additional settings. These
     *       can be defined by the connector, but obviously will need negotiation with
     *       any callers of this service to provide them. Not used here.
     *
     * Response: A JSON structure containing:
     *     "repositoryPath": The path to the repository parent
     *     "itemAddress": The address for the repository itself
     *          The repositoryPath and itemAddress should be stored and used for
     *          subsequent service calls.
     *     "branchName": The value that should be used as the versionAddress for future 
     *         calls. This can be different to the branch name passed in for certain 
     *         revision control systems. For example, for the DesignSync SCM system, 
     *         the branch might be "B1", but the returned value "B1:Latest"
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response createItem(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // Extract the data from the parameters.
            String repoPath = serviceCall.getPayload().getString(REPOPATH, "");
            String itemAddress = serviceCall.getPayload().getString(ITEMADDRESS, "");
            String branchName = serviceCall.getPayload().getString(BRANCHNAME, "");
            // This is the "comment" for creating the branch. Not used at present.
            // String message = serviceCall.getPayload().getString(MESSAGE, "");
            // This gets the additional settings. Not used at present
            // JsonObject settings = serviceCall.getPayload().getJsonObject(REPOSETTINGS);

            // In this case, the path to create is the concatenation of the configured
            // Exposed Path and REPOPATH and ITEMADDRESS passed in.
            Path directory = null;
            try (AutoReleasable lock = config.readLock()) {
                directory = buildFullPath(config, Paths.get(repoPath, itemAddress));
            }

            // Here, we use the File class to just create the directory.
            // Obviously, more error handling would be appropriate.
            File file = new File(directory.toString());
            if (!file.exists()) {
                logger.fine("Create directory "+file.toString());
                file.mkdirs();
            }

            JsonObject status = Json.createObjectBuilder()
                    .add(REPOPATH, repoPath)
                    .add(ITEMADDRESS, itemAddress)
                    .add(BRANCHNAME, branchName)
                    .add(FULLREPOPATH, directory.toString())
                    .build();

            return buildOK(status);

        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }
    
    /*
     *
     * This method reports information about "branches" of the
     * repository that are "locked", meaning that a user has reserved the
     * right to create the next set of data on that branch.
     *
     * For this connector, this is not appropriate.
     * But the code below shows how this might operate.
     *
     * Path elements:
     *    VERSION: The repository version
     *           If there is none, then reports all locks on all branches,
     *           otherwise limit to the branch of the version specified.
     * Parameters:
     *    None.
     *
     * Response: A JSON structure containing:
     *     "totalItems": A count of the number of locked branches that are reported.
     *     "member": An array containing one entry for each locked branch. Each entry has:
     *          "branch": The identifier for the branch
     *          "tags": If the connector allows for "aliases" on branches, then the
     *               other aliases for this branch
     *          "user": The name of the user owning the lock
     *          "date": The date the lock was obtained
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response showLockedBranches(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        //FileSysConnectorConfig config =
        //    FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // This is how to get the version. Not used at present.
            // String version = serviceCall.getPathElem(VERSION);

            // This is the repository path. Not used at present
            // Path fullPath = null;
            //try (AutoReleasable lock = config.readLock()) {
            //    getFullRepositoryPath(config, serviceCall);
            //}

            // Here is where a connector would implement its specific
            // code to get the locks.
            // Let's assume that gives an array of items of type "Object", each of which
            // has accessor routines to get the information.
            // The result can then be built using something like the following:
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            // Object[] tagsList = null;
            int totalItems = 0;
            /*
            for (Object obj : tagsList) {
                JsonArrayBuilder tagArrayBuilder = Json.createArrayBuilder();
                if (obj.getTags() != null) {
                    for(String tag: obj.getTags())
                        tagArrayBuilder.add(tag);
                }
                arrayBuilder.add(Json.createObjectBuilder()
                    .add(BRANCH, obj.getBranch())
                    .add(USER, obj.getUser())
                    .add(TAGS, tagArrayBuilder)
                    .add(DATE, obj.getDate())
                );
                ++totalItems;
            }
            */
            JsonObject result = Json.createObjectBuilder()
                .add(TOTALITEMS, totalItems).add(MEMBER, arrayBuilder).build();

            return buildOK(result);
        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }
    
    /*
     *
     * This method "locks" a branch in the repository, meaning that a user 
     * has reserved the right to create the next set of data on that branch.
     *
     * For this connector, this is not appropriate, so we just return a 
     * "pass" status. The code below shows how this might work.
     *
     * Path elements:
     *    VERSION: The repository version
     *           The branch to be locked.
     * Parameters:
     *    None.
     * Payload values:
     *    None.
     *
     * Response: A simple 200 Response
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response lockItemBranch(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        //FileSysConnectorConfig config =
        //    FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // This is how to get the version. Not used at present.
            // String version = serviceCall.getPathElem(VERSION);
            // Here is how to get the username for the person requesting the lock.
            // String user = serviceCall.getUserName();

            // This is the repository path. Not used at present.
            // Path fullPath = null;
            //try (AutoReleasable lock = config.readLock()) {
            //    fullPath = getFullRepositoryPath(config, serviceCall);
            //}

            // Here is where connector-specific code would go to create the lock
            // for that user and version.

            // Return a simple pass.
            return buildOK();
        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }

    /*
     *
     * This method "unlocks" a branch in the repository, removing the reservation.
     *
     * For this connector, this is not appropriate, so we just return a
     * "pass" status. The code below shows how this might work.
     *
     * Path elements:
     *    VERSION: The repository version
     *        The branch to be unlocked
     * Parameters:
     *    None.
     *
     * Response: A simple 200 Response on success.
     * NOTE: If the branch is not locked, then that is not considered an error.
     * It is up to the individual connector to decide whether only the person
     * holding the lock can remove it. However, we recommend that is NOT done,
     * otherwise it becomes impossible to remove locks if a user is no longer available.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */ 
    @Override
    public Response unlockItemBranch(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        //FileSysConnectorConfig config =
        //    FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            // We are not using the version at present, but here is how to get it.
            // String version = serviceCall.getPathElem(VERSION);
            // Here is how to get the username for the person making the request,
            // in case you need to check this against the current lock holder.
            // String user = serviceCall.getUserName();

            // This is the repository path
            // Not being used here at the moment.
            // Path fullPath = null;
            //try (AutoReleasable lock = config.readLock()) {
            //    fullPath = getFullRepositoryPath(config, serviceCall);
            //}

            // Here is where connector-specific code would go to remove the lock
            // for that version.

            // Return a simple pass.
            return buildOK();
        } catch (Exception ex) {
            return serviceCall.buildErrorResponse(ex);
        }
    }

    /*
     *
     * This is the most complex of the methods to be implemented.
     * This method handles an "upload" of a "manifest of changes"
     * to the repository data. This can be any combination of:
     * - Completely new files to be "added"
     * - File whose content is to be changed
     * - Files to be "removed"
     * - Empty directories to be added
     * - Whole directories to be "removed"
     * - Move (rename) of a file or folder
     *
     * Note that it is up to the implementation to manage "combined"
     * operations where possible. In particular, the combination of a
     * "move" with a content change of the same file should be handled.
     * (This example does handle these cases. But not every corner-case
     * has been tested!)
     *
     * Path elements:
     *    VERSION: The repository version
     *           The version to which the changes are to be applied to
     *           create a new version.
     * Parameters:
     *    None.
     * Payload values:
     *    The payload is a multi-part with one part containing a JSON
     *    which describes what to do, and the rest being the files.
     *    See the implementation details below for how this works.
     *
     * Response: A simple 200 Response on success. On failure, a structure
     * which details which changes could not be applied. See the implementation
     * details for how to create this.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response uploadManifest(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

            
        FileSysConnectorConfig config =
            FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        try {
            String version = serviceCall.getPathElem(VERSION);

            // This is the repository path
            Path fullPath = null;
            try (AutoReleasable lock = config.readLock()) {
                fullPath = getFullRepositoryPath(config, serviceCall);
            }

            // Use the Upload Processor to do the work.
            UploadProcessorImpl up = new UploadProcessorImpl(serviceCall, fullPath.toString(), version);
            return up.process();

        } catch (Exception e) {
            return serviceCall.buildErrorResponse(e);
        }

    }

    /*
     *
     * This method returns information on the set of "registrations" for this
     * connector.
     * This is information on individual repositories or sets of repositories
     * that are "registered" for the connector using its Web App page.
     * For this connector, there are none, so this just shows what would be
     * be needed if there were.
     * This single implementation method is used for all services requesting
     * registration information, with details of what is requested given
     * by the "params".
     *
     * Path elements:
     *    REGISTRATION: The registration identifier
     *           If a single registration is requested, this gives the
     *           identifier of the one wanted. Else, it is empty.
     * Parameters:
     *   MASK: One of "default" or "subkeys". The "default" mask should return
     *         the basic registration information, and "subkeys" should returns
     *         any "partial" keys that could be applicable as "paths" on the
     *         objects that are addressing this connector. For example, if
     *         a "registration" were to a simple path like "/a/b/c", then it might
     *         be appropriate to return subkeys of "/a" and "/a/b".
     *
     * Response: A JSON containing a "members" array. Each entry in the array must have
     * an "id", which is a unique identifier for the registration, and a "key", which is
     * the primary string for the registration. Each can then contain any other properties
     * that are appropriate for this connector. If the mask is "subkeys" then each
     * should also have a "subkeys" entry that is an array of strings.
     *
     * @param serviceCall Structure containing the details of the service call
     * @return Response
     */
    @Override
    public Response getRegistrations(ServiceCall serviceCall) {

        logger.fine(STR_SERVICE_CALL+serviceCall);

        // Extract the options from the service
        // We are not using the registration value at the moment.
        // String registration = serviceCall.getPathElem(REGISTRATION);
        // We are not using the mask at the moment, so not supporting subkeys.
        // String mask = serviceCall.getParam(MASK);

        // Read the configuration information.
        //FileSysConnectorConfig config =
        //    FileSysConnectorConfig.fromContext(serviceCall.getServletContext());

        // We build up an array of the results.
        JsonArrayBuilder members = Json.createArrayBuilder();
        // Here we would read any "registrations" from the configuration
        // data and build an entry for each one.
        // This connector doesn't have any, so we return empty.
        // See previous use of readLock for how the "config" should be locked.

        // Build the final result from the members
        JsonObject result = Json.createObjectBuilder()
                .add(MEMBER, members)
                .build();
        return buildOK(result);
    }


    //////////////////////////////////////////////////////////////////////////
    // UTILITY ROUTINES
    //
    // Routines used by the implementation methods above.
    //////////////////////////////////////////////////////////////////////////

    /*
     * To provide the "manifest" of the "repository" for this connector
     * we simply read the file system for the directory passed in.
     * We have to build a standard structure as a return value.
     * A "FileTreeFolderBuilder" builds a directory and a "FileTreeLeafBuilder" 
     * builds a file for insertion in the tree.
     *
     * This method is called recursively.
     *
     * @param folder The FileTreeFolderBuilder we are to use for this directory
     * @param directory The directory being read
     * @param topIn The top-level Path
     */
    private void readFileTree(FileTreeFolderBuilder folder, Path directory, Path topIn) 
        throws IOException {
        String dirName = "/";
        String dirID = "/";
        Path top;
        if (topIn==null) {
            top = directory;
        } else {
            top = topIn;
            // NOTE: An "id" has to be provided for each
            // directory/file. For Directories, we use the full path
            // but switch separators to '/' to avoid processing issues.
            dirID = top.relativize(directory).toString();//directory.toString().replace('\\', '/');
            dirName = directory.getFileName().toString();
        }

        // Set the name/id for this folder
        folder.setName(dirName).setId(dirID);

        // Read the contents of this directory.
        // Note that "." and ".." entries are automatically filtered.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
            // If a directory, then recurse to build the structure for that,
            // fist adding a new folder to this one.
            if (Files.isDirectory(entry)) {
                // As an example, you could filter out objects
                // with a specific name:
                if (!".SYNC".equals(entry.getFileName().toString()))
                    readFileTree(folder.addNewFolder(), entry, top);
                } else {
                    // Otherwise it is a file, create a 'File' for that within
                    // the folder.
                    String name = entry.getFileName().toString();
                    // The second argument here is the "id" of the file, for which
                    // this connector uses the path to the file from the starting point.
                    // This has not been checked on windows! It might need
                    // the separator converting to '/'.
                    folder.addNewFile(name, top.relativize(entry).toString());
                }
            }
        }
    }

    /*
     * Utility routine to read errors from a stream
     * Used to read back errors when performing system commands.
     */
    private static String getError(BufferedReader stream) throws IOException {
        String line;

        if (stream.ready()) {
            if ((line = stream.readLine()) != null) {
                String msg = line;
                while ((line = stream.readLine()) != null) {
                    msg += line;
                }
                return msg;
            }
        }
        return null;
    }

    private Response buildNotImpl(ServiceCall serviceCall) {
        return serviceCall.buildErrorResponse(Status.NOT_IMPLEMENTED, STR_NOT_IMPLEMENTED);
    }

    /*
     * Utility function to build a standard 'OK' response with
     * an empty JSON.
     * @return Response
     */
    private static Response buildOK() {
        JsonObjectBuilder result = Json.createObjectBuilder();
        return Response.status(Response.Status.OK).entity(result.build()).build();
    }

    /*
     * Utility function to build a standard 'OK' response with
     * a JSON result passed in.
     * @return
     */
    private static Response buildOK(JsonObject result) {
        return Response.status(Response.Status.OK).entity(result).
            type(MediaType.APPLICATION_JSON).build();
    }

    /*
     * Utility routine to return a simple response with a "value" in the body.
     *
     * @param status Status string to return
     * @return Response
     */
    private static Response statusResponse(String status) {
        JsonObject statusObject = Json.createObjectBuilder().add(VALUE,  status).build();
        return Response.status(Response.Status.OK).entity(statusObject).build();
    }

    /*
     * Utility to build the full path to our "repository" for this
     * connector, which is the concatenation of the prefix path from
     * the configuration data and the "address" passed to us.
     * This also checks that the resulting path starts with the
     * configured "exposed path", so that a relative path cannot be
     * specified.
     *
     * @param config The configuration data for the connector
     * @param serviceCall The Service Call details.
     * @return
     */
    private static Path getFullRepositoryPath(FileSysConnectorConfig config, ServiceCall serviceCall) 
    throws SCMException {
        return buildFullPath(config, Paths.get(serviceCall.getResolvedAddress()));
    }

    /*
     * Utility to build the full path to our "repository" for this
     * connector, which is the concatenation of the prefix path from
     * the configuration data and a specific passed address.
     *
     * @param config The configuration data for the connector
     * @param address Path to add to the configuration prefix
     * @return
     */
    private static Path buildFullPath(FileSysConnectorConfig config, Path address) 
        throws SCMException {
        String pathPrefix = config.getExposedPath();
        Path res = Paths.get(pathPrefix, address.toString()).normalize();
        if (!res.startsWith(pathPrefix))
            throw new SCMException(Status.FORBIDDEN,
                Utils.getStr("BadPath", address.toString()));
        logger.fine("Repository path: "+res.toString());
        return res;
    }
}

