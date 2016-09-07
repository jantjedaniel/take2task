/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.domain;

/**
 * A task context or folder. (Same class used for context and folder.)
 */
public class Context
{
    // Fields must have the following names for conversion from Toodledo JSON data to work (using GSON).
    private long id;
    private String name;
    
    /**
     * Return the context id.
     */
    public long getId()
    {
        return id;
    }
    
    /**
     * Return the context name.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Return the context/folder name, but with any prefix removed.
     * For example, if the folder name is "p3. Geek", return "Geek".
     * But also, if the folder name is "p3.g Computer Repair", return "Computer Repair".
     * So just strip off until the first space character.
     */
    public String getNameWithoutPrefix()
    {
        final String PREFIX_DELIMITER = " ";
        
        int indexOfPrefixDelimiter = name.indexOf(PREFIX_DELIMITER);
        if (indexOfPrefixDelimiter <= 0)
        {
            return name;
        }
        int indexAfterPrefixDelimiter = indexOfPrefixDelimiter + PREFIX_DELIMITER.length();
        return name.substring(indexAfterPrefixDelimiter).trim();
    }
    
    /**
     * Return a string representation of the context.
     */
    @Override
    public String toString()
    {
        return String.format("[Id: %d, Name: %s]" , getId(), getName());
    }    

    /**
     * Return whether the given object is equal in state to this one.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        
        try
        {
            Context other = (Context) obj;
            return getId() == other.getId() && getName().equals(other.getName());
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }
}
