/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service.toodledo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import take2task.domain.ContextSet;
import take2task.domain.Task;
import take2task.domain.User;
import take2task.service.TaskService;
import take2task.service.TaskServiceException;
import take2task.service.toodledo.domain.AccountInfo;
import take2task.service.toodledo.domain.Id;
import take2task.service.toodledo.domain.ToodledoUser;

/**
 * All access to Toodledo is via this service.
 */
public class ToodledoService implements TaskService
{
    private static Logger logger = Logger.getLogger("take2task.service.toodledo");

    private static Gson gson = new Gson(); // GSON library object used to convert between JSON and Java objects.

    /**
     * Ping Toodledo.
     * 
     * @return true if the task service is available, otherwise false.
     */
    @Override
    public boolean ping()
    {
        InputStream in = null;
        try
        {
            URL pingUrl = new URL(ObjectFetcher.TOODLEDO_API_URL);
            URLConnection connection = pingUrl.openConnection();
            in = connection.getInputStream();
            return (in.read() != -1);
        }
        catch (IOException e)
        {
            logger.log(Level.FINE, "ping() failed", e);
            return false;
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                } 
                catch (IOException e)
                {
                    logger.log(Level.FINE, "Couldn't close URL connection input stream", e);
                }
            }
        }
    }  

    /**
     * Authenticate to Toodledo. 
     * 
     * @param userId Toodledo user id. Not username, but instead the Unique ID
     *               found on the Toodledo Account Settings page.
     * @param password Toodledo password.
     * 
     * @return An object, representing an authenticated user, 
     *         which must be passed to all other methods on the interface.
     *
     * @throws ToodledoException If Toodledo responded with an error message.
     * @throws TaskServiceException On any other error. Usually wraps a lower-level exception.
     */
    @Override
    public User authenticate(String userId, String password)
        throws ToodledoException, TaskServiceException
    {
        ToodledoUser user = new ToodledoUser(userId, password);
        
        // Get account info from Toodledo.
        // This will do all the necessary steps to authenticate to Toodledo:
        // generating session token and authentication key, and then getting
        // account information using that authentication key.
        // If the method call succeeds (returns without throwing an exception), 
        // then authentication has succeeded. If it throws an exception, 
        // authentication has failed.
        getAccountInfo(user);
        logger.fine("Authentication successful.");
        return user;
    }
    
    /**
     * @see take2task.service.TaskService#getAllTasks(take2task.domain.User)
     */
    @Override
    public Task[] getAllTasks(User user) throws ToodledoException, TaskServiceException
    {
        return getTasks(user, false, false);
    }
    
    /**
     * @see take2task.service.TaskService#getModifiedTasks(take2task.domain.User)
     */
    @Override
    public Task[] getModifiedTasks(User user) throws ToodledoException, TaskServiceException
    {
        return getTasks(user, true, false);
    }
    
    /**
     * @see take2task.service.TaskService#updateTasks(take2task.domain.User, take2task.domain.Task[])
     */
    @Override
    public int updateTasks(User user, Task[] tasks) throws ToodledoException, TaskServiceException
    {
        // Toodledo says that you can update a maximum of 50 tasks in one update.
        // When I tried with 50, however, I got HTTP Error 414 Request URI too long.
        // So let's be much more conservative.
        // Once I started having large comments, even 10 was failing.
        final int MAXIMUM_TASK_UPDATE_COUNT = 3;
        
        int updatedTaskCount = 0;
        for (int i = 0; i < tasks.length; i += MAXIMUM_TASK_UPDATE_COUNT)
        {
            int chunkSize = tasks.length - i;
            if (chunkSize > MAXIMUM_TASK_UPDATE_COUNT)
                chunkSize = MAXIMUM_TASK_UPDATE_COUNT;
            Task[] tasksChunk = Arrays.copyOfRange(tasks, i, i + chunkSize);
            
            // Build up the request to update the tasks.
            String key = ((ToodledoUser)user).getAuthenticationKey();
            StringBuffer updateRequest = new StringBuffer(ObjectFetcher.TOODLEDO_API_URL);
            updateRequest.append("tasks/edit.php?key=").append(key);
            
            try
            {
                // Convert the tasks to URL encoded JSON, and add it to the request.
                String tasksUrlParameter = URLEncoder.encode(gson.toJson(tasksChunk), "UTF-8");
                updateRequest.append(";tasks=").append(tasksUrlParameter);
                updateRequest.append(';').append(Task.FIELDS);
                
                // Ask Toodledo to update the tasks.
                Task[] updatedTasks = ObjectFetcher.request(updateRequest.toString(), Task[].class);
                if (updatedTasks.length != tasksChunk.length)
                {
                    logger.warning(String.format("We asked Toodledo to update %d tasks, but it only updated %d tasks.", 
                                                 tasksChunk.length, updatedTasks.length));
                }
                updatedTaskCount += updatedTasks.length;
            }
            catch (TaskServiceException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new TaskServiceException(e);
            }
        }
        
        return updatedTaskCount;
    }
 
    /**
     * @see take2task.service.TaskService#deleteCompletedTasks(take2task.domain.User)
     */
    @Override
    public int deleteCompletedTasks(User user) throws ToodledoException, TaskServiceException
    {
        Task[] completedTasks = getTasks(user, false, true);
        if (completedTasks.length == 0)
        {
            logger.finer("No completed tasks to delete.");
            return 0;
        }
        
        // Build up the request to delete the tasks.
        String key = ((ToodledoUser)user).getAuthenticationKey();
        StringBuffer deleteRequest = new StringBuffer(ObjectFetcher.TOODLEDO_API_URL);
        deleteRequest.append("tasks/delete.php?key=").append(key);
        
        // Extract the ids from the completed tasks.
        long[] ids = new long[completedTasks.length];
        for (int i = 0; i < completedTasks.length; i++)
        {
            ids[i] = completedTasks[i].getId();
        }

        try
        {
            // Convert the ids to URL encoded JSON, and add it to the request.
            String idsUrlParameter = URLEncoder.encode(gson.toJson(ids), "UTF-8");
            deleteRequest.append(";tasks=").append(idsUrlParameter);
    
            // Ask Toodledo to delete the tasks.
            Id[] deletedTaskIds = ObjectFetcher.request(deleteRequest.toString(), Id[].class);
            if (deletedTaskIds.length != ids.length)
            {
                logger.warning(String.format("We asked Toodledo to delete %d tasks, but it only deleted %d tasks.", 
                                             ids.length, deletedTaskIds.length));
            }
            return deletedTaskIds.length;
        }
        catch (TaskServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TaskServiceException(e);
        }        
    }
    
    // ---------- Private helper methods ----------

    /**
     * Return account info from Toodledo. This includes timestamps of when tasks, etc, 
     * were last updated. We can use this to determine which tasks and other things
     * we need to fetch.
     * 
     * @throws ToodledoException If Toodledo responded with an error message.
     * @throws TaskServiceException On any other error. Usually wraps a lower-level exception.
     */
    private AccountInfo getAccountInfo(User user) throws ToodledoException, TaskServiceException
    {
        String key = ((ToodledoUser)user).getAuthenticationKey();
        String accountInfoRequest = ObjectFetcher.TOODLEDO_API_URL + "account/get.php?key=" + key;
        try
        {
            return ObjectFetcher.request(accountInfoRequest, AccountInfo.class);
        }
        catch (TaskServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TaskServiceException(e);
        }        
    }
    
    /**
     * Return tasks from Toodledo matching the given criteria for the given user.
     * 
     * @param user  The user to get tasks for.
     * @param modifiedOnly  If true, only get tasks modified since we last checked.
     *                      If false, get all tasks.
     * @param completed  If true, get completed tasks.
     *                   If false, get uncompleted tasks.
     */
    private Task[] getTasks(User user, boolean modifiedOnly, boolean completed) 
        throws ToodledoException, TaskServiceException
    {
        ToodledoUser toodledoUser = (ToodledoUser) user;
        
        // Ask Toodledo for account info, including timestamps of last changes.
        AccountInfo accountInfo = getAccountInfo(user);
        
        // If we are only getting tasks modified since we last downloaded, and if no tasks have been modified, 
        // then we don't need to do anything.
        if (modifiedOnly && 
            accountInfo.getLastEditedTaskTimestamp().getValue() < toodledoUser.getLastTaskDownloadTimestamp().getValue())
        {
            return new Task[0];
        }

        //Build the request for Toodledo.
        String key = ((ToodledoUser)user).getAuthenticationKey();
        StringBuffer request = new StringBuffer(ObjectFetcher.TOODLEDO_API_URL);
        request.append("tasks/get.php?key=").append(key);
        request.append(';').append(Task.FIELDS);
        
        if (modifiedOnly)
        {
            request.append(";modafter=").append(toodledoUser.getLastTaskDownloadTimestamp().getValue());            
        }
        
        request.append(completed ? ";comp=1" : ";comp=0");

        // Make the request.
        try
        {
            Task[] tasksWithDummy = ObjectFetcher.request(request.toString(), Task[].class);
            
            // Set the time that we last checked tasks to now.
            // Don't do this, however, if we have downloaded completed tasks,
            // because it is only uncompleted tasks that might have changed.
            // We delete any completed tasks each time we download them.
            if (!completed)
            {
                toodledoUser.updateLastTaskDownloadTimestamp();
            }

            // The first entry in JSON that Toodledo returns is not a task, but instead
            // some meta-data that we don't need. GSON restores it as a task with a null title,
            // stored as the zeroth element of the array. We need to remove it.
            assert tasksWithDummy[0].getTitle() == null : "Zeroth entry of tasks array returned from Toodledo was not a 'dummy'.";
            Task[] tasks = Arrays.copyOfRange(tasksWithDummy, 1, tasksWithDummy.length);
            
            // Insert the set of available contexts and folders (for this user) into every task.
            ContextSet contexts = toodledoUser.getContexts(accountInfo.getLastEditedContextTimestamp());
            ContextSet folders = toodledoUser.getFolders(accountInfo.getLastEditedFolderTimestamp());
            for (Task task : tasks)
            {
                task.setAvailableContexts(contexts);
                task.setAvailableFolders(folders);
            }
            
            return tasks;
        }
        catch (TaskServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TaskServiceException(e);
        }        
    }
}
