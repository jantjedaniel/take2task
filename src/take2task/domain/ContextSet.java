/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * A set of contexts, typically for one user.
 */
public class ContextSet
{
    private Context[] contexts;
    private Map<Long, Context> idMap = new HashMap<Long, Context>();
    private Map<String, Context> nameMap = new HashMap<String, Context>(); 
    
    /**
     * Constructor.
     * 
     * @param contexts  The contexts to store.
     */
    public ContextSet(Context[] contexts)
    {
        this.contexts = contexts;
        for (Context context : contexts)
        {
            idMap.put(new Long(context.getId()), context);
            nameMap.put(context.getName(), context);
        }
    }
    
    /**
     * No-arg constructor. Construct an empty context set.
     */
    public ContextSet()
    {
        this(new Context[0]);
    }
    
    /**
     * Return the context with the given id, 
     * or null if there is not a context with the given id.
     */
    public Context findById(long id)
    {
        return idMap.get(new Long(id));
    }
    
    /**
     * Return the context with the given name, 
     * or null if there is not a context with the given name.
     */
    public Context findByName(String name)
    {
        return nameMap.get(name);
    }
    
    /**
     * Return the context/folder that starts with the given prefix, followed
     * by " - ". For example, folder "p6 - Geek".
     */
    public Context findByPrefix(String prefix)
    {
        prefix = prefix + " - ";
        for (Context context : nameMap.values())
        {
            if (context.getName().startsWith(prefix))
            {
                return context;
            }
        }
        return null;
    }
    
    /**
     * Return the context which starts with the given name, ignoring case,
     * or null if there is not one.
     */
    public Context findByNameIgnoreCaseMatchStart(String name)
    {
        for (Context context : nameMap.values())
        {
            // Truncate the context name to the length of the given name.
            String contextName = context.getName();
            if (contextName.length() > name.length())  
            {
                contextName = contextName.substring(0, name.length());
            }

            if (contextName.equalsIgnoreCase(name))
            {
                return context;
            }
        }
        return null;
    }
    
    /**
     * Return the context/folder with the given name, ignoring case.
     * Also look for a match ignoring the prefix at the start of the context/folder (e.g. "p6 - ")
     * Return null if there is no match.
     */
    public Context findByNameIgnoreCaseAndPrefix(String name)
    {
        for (Context context : nameMap.values())
        {
            if (context.getName().equalsIgnoreCase(name))
            {
                return context;
            }
            if (context.getNameWithoutPrefix().equalsIgnoreCase(name))
            {
                return context;
            }
        }
        return null;
    }
    
    /**
     * Return a string representation of the contexts.
     */
    @Override
    public String toString()
    {
        StringBuffer contextsBuffer = new StringBuffer();
        for (Context context : contexts)
        {
            contextsBuffer.append(String.format("[%s] ", context.getName()));
        }
        return contextsBuffer.toString(); 
    }    
}
