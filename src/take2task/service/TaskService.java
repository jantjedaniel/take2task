/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service;

import take2task.domain.Task;
import take2task.domain.User;


/**
 * All access to the underlying task service is via methods on this interface.
 */
public interface TaskService
{
    public static final String FILE_SYSTEM_FOLDER_NAME = ".t2t";
    
    /**
     * Ping the underlying task service.
     * 
     * @return true if the task service is available, otherwise false.
     */
    public abstract boolean ping();
    
    /**
     * Authenticate to the underlying task service. 
     * 
     * @param userId  Task service username or user id.
     * @param password Task service password.
     * 
     * @return An object, representing an authenticated user, 
     *         which must be passed to all other methods on the interface.
     * 
     * @throws TaskServiceException  On any error.
     */
    public abstract User authenticate(String userId, String password)
            throws TaskServiceException;

    /**
     * Return all tasks from the underlying task service for the given user.
     * 
     * @param user An authenticated user.
     * 
     * @throws TaskServiceException  On any error.
     */
    public abstract Task[] getAllTasks(User user) throws TaskServiceException;

    /**
     * Return tasks from the underlying task service that have been modified since the
     * last time we fetched tasks or updated tasks. Do this for the given user.
     * 
     * @param user An authenticated user.
     * 
     * @throws TaskServiceException  On any error.
     */
    public abstract Task[] getModifiedTasks(User user) throws TaskServiceException;

    /**
     * Update the given tasks back to the underlying task service for the given user.
     * 
     * @param user An authenticated user.
     * @param tasks The tasks to update back to the server.
     * 
     * @return The number of tasks that were update by the task service.
     * 
     * @throws TaskServiceException  On any error.
     */
    public abstract int updateTasks(User user, Task[] tasks) throws TaskServiceException;

    /**
     * Delete all completed tasks for the given user.
     * 
     * @param user An authenticated user.
     * 
     * @return The number of tasks that were deleted by the task service.
     * 
     * @throws TaskServiceException  On any error.
     */
    public abstract int deleteCompletedTasks(User user) throws TaskServiceException;
}