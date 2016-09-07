/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import take2task.domain.Task;
import take2task.domain.User;
import take2task.service.TaskService;
import take2task.service.TaskServiceException;
import take2task.service.toodledo.ToodledoService;

/**
 * Main class of Take2Task.
 * Fetches tasks from Toodledo and processes them, including setting a "pseudo date"
 * for each task's due date unless told otherwise.
 */
public class Processor
{
    private static Logger logger = Logger.getLogger("take2task");

    private TaskService service = new ToodledoService();
    private String userId;
    private String password;
    private User user;
    private Calendar lastFullUpdateTime = new GregorianCalendar(1970, 1, 1); // well before now
    
    // ---------- Public methods ----------
    
    /**
     * Constructor.
     * 
     * @param userId  Task service user id.
     * @param password  Task service password.
     */
    public Processor(String userId, String password)
    {
        this.userId = userId;
        this.password = password;
    }
    
    /**
     * Do one processing cycle. That is, fetch tasks from Toodledo, process them,
     * then write back the modified tasks to Toodledo.
     */
    public void runOnce()
    {
        if (!service.ping())
        {
            logger.warning("Task service is unavailable.");
        }
        else
        {
            try
            {
                if (user == null)
                {
                    user = service.authenticate(userId, password);
                }
                
                // Ask the task service to delete any completed tasks.
                int deletedTaskCount = service.deleteCompletedTasks(user);
                logger.info("Completed tasks that were deleted: " + deletedTaskCount);
                
                // Get the list of tasks to inspect from the task service.
                Task[] tasks = getTasksToInspect();
                if (tasks == null)
                {
                    logger.severe("Task service returned a null array of tasks.");
                }
                else
                {
                    logger.info("Tasks to inspect: " + tasks.length);
                    logger.finer(toString(tasks));

                    if (tasks.length > 0)
                    {
                        // Modify tasks as needed.
                        Task[] modifiedTasks = modifyTasks(tasks);
                        logger.info("Tasks modified: " + modifiedTasks.length);
                        
                        if (modifiedTasks.length > 0)
                        {
                            logger.info(toString(modifiedTasks));
                        
                            // Write any modified tasks back to the task service.
                            int updatedTaskCount = service.updateTasks(user, modifiedTasks);
                            logger.info("Modified tasks that were updated: " + updatedTaskCount);
                        }
                    }
                }
                
                // The task array goes out of scope at the end of this loop, so all the elements
                // in it should be garbage collected, but just in case, empty out the array so
                // that there are no references to the elements (the tasks) any more.
                for (int i = 0; i < tasks.length; i++)
                {
                    tasks[i] = null;
                }
                
            }
            catch (Exception e)
            {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        logger.info("------------------------------------------------------------");
    }
    
    /**
     * Loop forever doing processing cycles at the specified interval.
     * 
     * @param long intervalMillis  Interval between each processing cycle.
     */
    public void run(final int intervalMinutes)
    {
        final long intervalMillis = intervalMinutes * 60 * 1000;
        
        Runnable runner = new Runnable()
        {
            public void run()
            {
                while (true) // Infinite loop.
                {
                    runOnce();
                    
                    try
                    {
                        Thread.sleep(intervalMillis);
                    } 
                    catch (InterruptedException e)
                    {
                        logger.log(Level.WARNING, "Sleep between runs interrupted.", e);
                    }
                }
            }
        };
        Thread thread = new Thread(runner);
        thread.run();
    }
    
    /**
     * Main method.
     * 
     * @param args  Command line arguments.
     *              args[0] = user id
     *              args[1] = password
     *              args[2] = run interval in minutes
     */
    public static void main(String[] args)
    {
        final int DEFAULT_INTERVAL_MINUTES = 5;
        
        // Get username and password from the command line.
        if (args.length != 2 && args.length != 3)
        {
            System.out.printf("Usage: %s %s", Processor.class.getName(), "<username> <password> [\"once\" | <interval in minutes>]");
            return;
        }
        final String userId = args[0];
        final String password = args[1];
        Processor processor = new Processor(userId, password);
        
        if (args.length == 3 && args[2].equals("once"))
        {
            processor.runOnce();
        }
        else
        {
            final int intervalMinutes = (args.length == 3) ? Integer.parseInt(args[2]) : DEFAULT_INTERVAL_MINUTES;
            processor.run(intervalMinutes);
        }
    }
    
    // ---------- Private helper methods ----------
    
    /**
     * Return a string representation of the given array of tasks.
     */
    private String toString(Task[] tasks)
    {
        StringBuffer taskDump = new StringBuffer();
        for (Task task : tasks)
        {
            taskDump.append(String.format("%s\n", task));
        }
        return taskDump.toString();
    }
    
    /**
     * Determine whether we need to do a full or an incremental update.
     * Return the appropriate set of tasks.
     * 
     * @throws TaskServiceException  On any error getting the tasks from the task service.
     */
    private Task[] getTasksToInspect() throws TaskServiceException
    {
        Calendar now = new GregorianCalendar();
        if (now.get(Calendar.YEAR) > lastFullUpdateTime.get(Calendar.YEAR) ||
            now.get(Calendar.DAY_OF_YEAR) > lastFullUpdateTime.get(Calendar.DAY_OF_YEAR))
        {
            logger.info("First update of the day (or for this program run), so doing a full update...");
            lastFullUpdateTime = now;
            return service.getAllTasks(user);
        }
        else
        {
            return service.getModifiedTasks(user);
        }
    }
    
    /**
     * Inspect the given tasks and modify any of them that need modifying.
     * 
     * @param tasks  The tasks to inspect.
     * 
     * @return  The modified tasks (a subset of the tasks given to inspect).
     */
    private Task[] modifyTasks(Task[] tasks)
    {
        List<Task> modifiedTasks = new ArrayList<Task>();
        for (Task task : tasks)
        {
            if (task.modify())
            {
                modifiedTasks.add(task);
            }
        }
        return modifiedTasks.toArray(new Task[modifiedTasks.size()]);
    }
}
