/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service.toodledo.domain;

/**
 * Toodledo returns a set of task IDs when we delete tasks.
 * GSON converts it to an array of this class.
 */
public class Id
{
    private long id; // Must be called id for GSON to be able to work its magic.
    
    public long getValue()
    {
        return id;
    }
}
