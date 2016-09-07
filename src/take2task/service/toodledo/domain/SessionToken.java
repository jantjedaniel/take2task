/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service.toodledo.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import take2task.service.TaskService;

/**
 * Class representation of the JSON data returned by Toodledo when you request a session token.
 * The GSON library converts the JSON data into one of these objects.
 */
@SuppressWarnings("serial")
public class SessionToken implements Serializable
{
    private static Logger logger = Logger.getLogger("take2task.service.toodledo");

    /**
     * The length of time that a session object is valid.
     * The Toodledo documentation says that session keys are valid for 4 hours,
     * and that each user is allowed to request 10 tokens per hour.
     * So let's be conservative and say that it is valid for 3 hours.
     * This will still well and truly keep us under the 10 per hour requests threshold.
     */
    private static final int  VALID_SESSION_DURATION_HOURS = 3;
    private static final long VALID_SESSION_DURATION_MILLIS = VALID_SESSION_DURATION_HOURS * 60 * 60 * 1000;

    private static final String FILE_NAME =
        System.getProperty("user.home") + File.separator + TaskService.FILE_SYSTEM_FOLDER_NAME + File.separator + "token";

    private String token;  // Has to be called "token" for conversion from JSON to work.
    private String userId; // We store the user id so that we can cache the session token. 
    private Date creationTime; // The time that the object was first created. 
    
    /**
     * Constructor.
     */
    public SessionToken()
    {
        creationTime = new Date();
    }
    
    /**
     * Return the token value.
     */
    public String getValue()
    {
        return token;
    }
    
    /**
     * Return the Toodledo user id.
     */
    public String getUserId()
    {
        return userId;
    }
    
    /**
     * Set the user id to the given one. Stored so that we can cache session tokens
     * for a given user.
     * 
     * @param userId  Toodledo user id.
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }
    
    /**
     * Return true if the session token has expired.
     * 
     * @returns true if the session token has expired; false if it is still valid.
     */
    public boolean isExpired()
    {
        return System.currentTimeMillis() - creationTime.getTime() > VALID_SESSION_DURATION_MILLIS;
    }
    
    /**
     * Save the session token to disk.
     * 
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public void save() throws FileNotFoundException, IOException
    {
        File tokenFile = new File(FILE_NAME);
        File folder = tokenFile.getParentFile();
        if (!folder.exists())
        {
            if (folder.mkdir())
            {
                logger.info("Created folder " + folder);
            }
            else
            {
                throw new IOException("Could not create folder " + folder);
            }
        }
        
        ObjectOutputStream out = null;
        try
        {
            out = new ObjectOutputStream(new FileOutputStream(tokenFile));
            out.writeObject(this);
        }
        finally
        {
            if (out != null)
            {
                out.close();
            }
        }
    }
    
    /**
     * Load a session token from disk.
     * 
     * @return The session token that was saved on disk.
     * 
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    public static SessionToken load() throws IOException, ClassNotFoundException
    {
        ObjectInputStream in = null;
        try
        {
            in = new ObjectInputStream(new FileInputStream(FILE_NAME));
            SessionToken token = (SessionToken) in.readObject();
            return token;
        }
        catch (FileNotFoundException e)
        {
            // Looks like there isn't a session token saved on disk.
            return null;
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
        }
    }
}
