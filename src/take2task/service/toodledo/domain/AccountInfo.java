/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service.toodledo.domain;

import take2task.domain.Timestamp;

/**
 * Class representation of the JSON data returned by Toodledo when you request account info.
 * The GSON library converts the JSON data into one of these objects.
 */
public class AccountInfo
{
    // All these fields have to have these names for the conversion from JSON.
    private long lastedit_folder;
    private long lastedit_context;
    private long lastedit_task;
    
    /**
     * Return the last time that a task was modified.
     */
    public Timestamp getLastEditedTaskTimestamp()
    {
        return new Timestamp(lastedit_task);
    }
    
    /**
     * Return the last time that a context was modified.
     */
    public Timestamp getLastEditedContextTimestamp()
    {
        return new Timestamp(lastedit_context);
    }
    
    /**
     * Return the last time that a folder was modified.
     */
    public Timestamp getLastEditedFolderTimestamp()
    {
        return new Timestamp(lastedit_folder);
    }
}
