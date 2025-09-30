/*
 * Copyright (c) 2001-2017 Dassault Systemes.
 * All Rights Reserved
 *
 * Implements the Upload processing.
 *
 * By extending the general class, all we have to provide here
 * is functions to handle the most basic operations.
 * See the individual method desriptions below.
 *
 * For greater control, the higher level methods
 * in the parent class can be overridden.
 */
package com.dassaultsystemes.xsoftware.scmdaemon.bitbucket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.json.JsonObject;

import org.apache.commons.fileupload2.core.DiskFileItem;

import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorConstants;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorFileUtils;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.ConnectorUtils;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.UploadProcessorBase;
import com.dassaultsystemes.xsoftware.scmdaemon.sdk.interfaces.ServiceCall;

public class UploadProcessorImpl extends UploadProcessorBase implements ConnectorConstants  {

    private static final Logger logger = Logger.getLogger(UploadProcessorImpl.class.getName());
    private String workspace;

    /*
     * Constructor takes the basic information needed for processing.
     * Super assigns these to local variables that are available during the processing
     */
    UploadProcessorImpl (ServiceCall _serviceCall, String _repositoryPath, String _version) {
        super(_serviceCall, _repositoryPath, _version);
        // For this use, we want to know the main workspace directory, and this is
        // simply the repository path passed in.
        workspace = _repositoryPath;
    }

    /*
     * This function can be implemented to perform any initialization that is needed.
     * By this point, the following variables are available:
     * parameters: The "parameters" section of the multi-part payload, from which can be
     *    extracted values such as the "commitId".
     *
     * This function can do any argument checking, and also, for example,
     * initialize a working area, if needed.
     *
     * If there is any problem, this routine should return an error String.
     * It should not throw, but if it does then the thrown message will be
     * reported back as an overall failure.
     */
    @Override
    protected String initialize() {
        // Here, we can extract some parameters
        String commitId = getParameters().getString(COMMITID, "").trim();
        //String comment = parameters.getString(COMMENT, "").trim();

        // And we can do some basic checking.
        if (getVersion().isEmpty() || commitId.isEmpty())
            // We return an error message.
            return Utils.getStr("BadParameters");

        // Ideally, we should perform some kind of "lock" checking here
        // to prevent multiple requests modifying the data at the same time.
        // For a full SCM system, this might involve checking this user has a lock
        // on the "branch" of the repository as well.

        return null;
    }

    /*
     * This function can be implemented to perform any final
     * operations once all the individual processing has taken place.
     * completed: Means that all processing was completed. We didn't drop
     *    out with an error from any step.
     * hasFailures: Means that some step in the processing gave a failure
     *
     * This should return an error string if there is a problem, else a null.
     */
    @Override
    protected String finish(boolean completed, boolean hasFailures) {
        // Here we could remove any locks, and for a full SCM perform
        // a checkin or similar.
        return null;
    }

    /*
     * The following methods, implemented in the parent class,
     * implement the different operations
     * for the manifest of changes passed in.
     *
     * These are described here in case an individual connector
     * wishes to override them.
     * However, note that if you override these then you need
     * to take care of handling "combined" changes, such as
     * a file that is both moved and revised.
     *
     * Each takes as input the "request" part from the manifest.
     * Each may return an error string if there is a problem with
     * performing this specific operation.
     *
     * The "manifest" is an array of "requests" to be applied. Each has a "type"
     * describing  the change, and then other parameters appropriate to that type.
     * The types are:
     * "add": To add a NEW file to the repository. This should NOT overwrite an
     *        existing file at the same path
     * "addFolder": To add a NEW folder to the repository. Used to add empty
     *        folders. It should not be considered an error to add a folder
     *        that already exists
     * "remove": To remove an individual file or an entire folder and its content
     * "revise": To change the contents of an individual file. The file should
     *        already exist, but it is left to an individual connector to decide
     *        whether this is an error or not.
     * "move": To move a file or folder to a new location. The file/folder must
     *        already exist. A "move and revise" is accomplished by having separate
     *        "move" and "revise" entries. These can be in any order. The connector
     *        keeps track of the move to make sure the revise is applied correctly.
     *
     * The other information is:
     * "id": The identifier for the file/folder being operated on. .
     *        Required for all entries.
     * "index": A unique number for each manifest entry, used as an indentifier if we need
     *        to report back any problems with the manifest.
     *        Required for all entries.
     * "content": This is the name of a "part" in the multi-part request which contains the
     *        file content for this manifest entry.
     *        Required for add/revise requests, and ignored for others
     * "destination": The new path for an object being moved.
     *        Required for "move" requests, ignored for all others
     * "path": The path for an object being added. Should be a UNIX style path
     *        Required for "add" requests, ignored for all others
     * "properties": Additional properties for a file being added/revised. At present, the
     *        only property supported is "executable", which should be true/false and
     *        indicates whether a file should be marked executable. For "add" this defaults
     *        to "false", for "revise" the existing access is retained if this is not given
     */

    /*
     * Implements the "add" request.
     * The default implementation simply saves the file
     * to the target path, using 'saveFileToWorkspace'.
     */
    // @Override
    //protected String processAddFile(JsonObject request) {}

    /*
     * Implements the "addFolder" request.
     * The default implementation calls addNewFolder, to
     * get the folder created.
     */
    // @Override
    //protected String processAddFolder(JsonObject request) {}

    /*
     * Implements the "revise" request.
     * The default implementation simply saves the file
     * to the path, using saveFileToWorkspace,
     * BUT allows for the fact that
     * the object may already have been moved, in which
     * case the target path is different.
     * The default implementation also allows for the case
     * where the file has already been deleted, so cannot be
     * revised as well, and that is considered an error.
     *
     * NOTE: the assumption is that the "ID" of the request
     * in this case is the original (relative) path of the
     * file. If that is not true, then the method getPathFromRequest
     * can be overriden.
     */
    // @Override
    //protected String processReviseFile(JsonObject request) {}

    /*
     * Implements the "move" request.
     * The default implementation performs the move using 'movePath',
     * and takes care of allowing for something that has already been moved once,
     * or disallowing if something has already been removed.
     * NOTE: the assumption is that the "ID" of the request
     * in this case is the original (relative) path of the
     * file/folder. If that is not true, then the method getPathFromRequest
     * can be overriden.
     */
    // @Override
    //protected String processMove(JsonObject request) {}

    /*
     * Implements the "remove" request.
     * For the default implementation, this calls deletePath 
     * to actually delete the file or folder.
     */
    // @Override
    //protected String processRemove(JsonObject request) {}

    ///////////////////////////////////////////////////////////////
    // Utilties used by the operations code for this connector.
    ///////////////////////////////////////////////////////////////

    /*
     * Set permissions on a new file.
     * New files can be marked as to whether they should be
     * "executable", and this method sets that access.
     * @param path Path of file
     * @param properties File properties passed in
     */
    private void setNewFilePermissions (
            Path path,
            JsonObject properties) throws IOException {
        if (properties == null)
            return;
        boolean isExecutable = properties.getBoolean(EXECUTABLE, false);
        if (isExecutable) {
            logger.fine("Set " + path + " executable");
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    /*
     * Set permissions for a replaced file, which are those of the
     * original file, and then with executable added/removed according to
     * the request.
     * @param path Path of file
     * @param properties File properties passed in
     */
    private void setReplacedFilePermissions(
            Path path,
            JsonObject properties,
            Set<PosixFilePermission> permissions) throws IOException {
        if (properties == null) {
            Files.setPosixFilePermissions(path, permissions);
            return;
        }

        boolean isExecutable = properties.getBoolean(EXECUTABLE, false);

        if( isExecutable ) {
            logger.fine("Set " + path + " executable");
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        } else {
            logger.fine("Set " + path + " normal");
            permissions.remove(PosixFilePermission.OWNER_EXECUTE);
            permissions.remove(PosixFilePermission.GROUP_EXECUTE);
            permissions.remove(PosixFilePermission.OTHERS_EXECUTE);
        }
        Files.setPosixFilePermissions(path, permissions);
    }

    /*
     * Save a file from the request to the workspace
     * This method must be overridden. It is up to the
     * individual connector to define how a file is "saved"
     * to their working area.
     * This may be called on a simple add or a revise, and in the
     * case of revise the object may have already been moved, so
     * the target path is passed in here and must be used.
     * That target path is obviously relative to the repository root.
     *
     * @param request The request entry from the manifest
     * @param path The path to save the file to
     * @param replace Whether to allow replace of an existing file
     */
    @Override
    protected String saveFileToWorkspace(
            JsonObject request,
            String path,
            boolean replace) {

        JsonObject properties = request.getJsonObject(PROPERTIES);
        // Get the file part from the request.
        // Calls to getRequestPart return a String on failure, which
        // should just be returned here.
        Object part = getRequestPart(request);
        if (part instanceof String) return (String)part;
        DiskFileItem content = (DiskFileItem)part;;

        logger.fine("content content type: " + content.getContentType());
        logger.fine("content is form field " + content.isFormField());
        logger.fine("replace: " + replace);

        // For this sample connector, simply copy the file content to disk
        // But we also take some care with permissions.
        try {
            Path filePath = Paths.get(workspace, path);
            logger.fine("filePath: " + filePath);
            InputStream in = content.getInputStream();
            if( replace ) {
                if( ConnectorUtils.isWindows()) {
                        Files.setAttribute(filePath, "dos:readonly", false);
                }
                Set<PosixFilePermission> permissions = null;
                if( ConnectorUtils.isLinux() )
                    permissions = Files.getPosixFilePermissions(filePath);
                Files.deleteIfExists(filePath);
                Files.copy(in, filePath);
                if( ConnectorUtils.isLinux() )
                    setReplacedFilePermissions(filePath, properties, permissions);
            } else {
                // If NOT replace, then we COULD first check that there is no
                // existing file at the target, and return an error if there is.
                Path parentDir = filePath.getParent();
                // We create the target directory if it does not exist.
                Files.createDirectories(parentDir);
                Files.copy(in, filePath);
                if ( ConnectorUtils.isLinux() )
                    setNewFilePermissions(filePath, properties);
            }

        } catch (IOException ex) {
            return Utils.getStr("FileSaveFailed") + ex.toString();
        }
        return null;
    }
    
    /*
     * Create a new folder.
     * The default implementation for "processAddFolder" uses this to
     * actually create the folder.
     * It is up to this implementation to set premissions, if necesary.
     *
     * Return an error string on failure, else null.
     *
     * @param path The relative path to create in the repository
     * @param properties The "proeprties" section of the request.
     */
    @Override
    protected String addNewFolder(String path, JsonObject properties) {
        try {
            Path folderPath =  Paths.get(workspace, path);
            // Directory is created if it does not exist.
            Files.createDirectories(folderPath);
            if ( ConnectorUtils.isLinux() )
                setNewFilePermissions(folderPath, properties);
        } catch (Exception ex) {
            return Utils.getStr("AddFailed") + ": " + ex.getLocalizedMessage();
        }

        return null;
    } 

    /*
     * Move a File or Folder
     * The default implemenation for "processMove" uses this
     * to actually move the file or folder.
     * This routine is then responsible for adding records of the
     * move or all sub-elements when this is a folder.
     * That is necessary, as a subsequent operation on a sub-element
     * (file or folder) needs to know that the original has been moved.
     *
     * @param originalPath The original source path (relative to repository)
     * @param source The source path now (relative to repository), i.e. where
     *    originalPath has previously been moved to.
     * @param destination The target path (relative to repository)
     */
    @Override
    protected String movePath(String originalPath, String source, String destination) {
        // Get the full paths.
        Path destinationPath = Paths.get(workspace, destination);
        Path sourcePath = Paths.get(workspace, source);
        boolean isDirectory = Files.isDirectory(sourcePath);

        // Perform the move.
        // If it throws, return an error string.
        try {
            Files.move(sourcePath, destinationPath);
        } catch (IOException e) {
            return e.getLocalizedMessage();
        }

        // Now, for this connector, if we are moving a directory, 
        // we need to walk the directory
        // and record a move for each object. We have a utility to
        // do that for a simple driectory on disk, so we use that here.
        // If that is not what your connector needs, then you should
        // process each item and call recordObjectMove to register each move.
        if (isDirectory)
            recordMovedObjectsInFolder(originalPath, destination, destinationPath, workspace);
        return null;
    }

    /*
     * This is used by the default implementation to delete a file or
     * folder. It simply has to perform the delete in the target system.
     * In this case, just delete the file or folder.
     */
    @Override
    protected String deletePath(String path) {
        try {
            Path objPath = Paths.get(workspace, path);
            if (ConnectorUtils.isWindows()) {
                Files.setAttribute(objPath, "dos:readonly", false);
            }
            if (Files.isDirectory(objPath))
                ConnectorFileUtils.killDirectory(objPath);
            else
                Files.delete(objPath);
        } catch (Exception ex) {
            return Utils.getStr("RmFailed") + ": " + ex.getLocalizedMessage();
        }
        return null;
    }

}
