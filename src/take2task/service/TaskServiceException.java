/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service;

/**
 * Any exception thrown from the service layer, other than a specific error message returned by Toodledo
 * (which would be a ToodledoException). A service exception usually wraps a lower level exception.
 */
@SuppressWarnings("serial")
public class TaskServiceException extends Exception
{
    public TaskServiceException(String message)
    {
        super(message);
    }
    
    public TaskServiceException(String message, Throwable cause)
    {
        super(message, cause);
    }
    
    public TaskServiceException(Throwable cause)
    {
        super(cause);
    }
}
