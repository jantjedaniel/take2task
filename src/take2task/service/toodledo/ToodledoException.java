/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service.toodledo;

import take2task.service.TaskServiceException;

/**
 * An exception wrapping up a Toodledo error message, 
 * identified by an error code and description specified in the 
 * <a href="http://api.toodledo.com/2/index.php">Toodledo Developer's API Documentation</a>.
 */
@SuppressWarnings("serial")
public class ToodledoException extends TaskServiceException
{
    /**
     * The start of a Toodledo error message response.
     * Used to determine if an error is returned or the expected data.
     */
    static final String ERROR_START = "{\"error";
    
    private int errorCode; // Must be called "errorCode" for conversion from JSON to work.
    private String errorDesc; // Must be called "errorDesc" for conversion from JSON to work.

    /**
     * Default constructor.
     */
    public ToodledoException()
    {
        super("");
    }
    
    /**
     * Return the Toodledo error code.
     */
    public int getCode()
    {
        return errorCode;
    }
    
    /**
     * Return the Toodledo error description.
     */
    public String getDescription()
    {
        return errorDesc;
    }
    
    /**
     * Return the exception message.
     */
    @Override
    public String getMessage()
    {
        return String.format("Toodledo error %d : %s", getCode(), getDescription());
    }
    
    /**
     * Return a string representation of the exception.
     */
    @Override
    public String toString()
    {
        return getMessage();
    }
}
