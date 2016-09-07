/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service.toodledo.domain;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import take2task.domain.Context;
import take2task.domain.ContextSet;
import take2task.domain.Timestamp;
import take2task.domain.User;
import take2task.service.TaskServiceException;
import take2task.service.toodledo.ObjectFetcher;
import take2task.service.toodledo.ToodledoException;

/**
 * A Toodledo user.
 */
public class ToodledoUser implements User
{
    private static Logger logger = Logger.getLogger("take2task.service.toodledo");

    private static final String APP_ID = "Take2Task";
    private static final String APP_TOKEN = "api4d9e53ec97abf"; // token for "Take2Task" app

    private String userId;
    private String password;
    
    // When we last downloaded stuff.
    private Timestamp lastTaskDownloadTimestamp = new Timestamp(0);
    private Timestamp lastContextDownloadTimestamp = new Timestamp(0);
    private Timestamp lastFolderDownloadTimestamp = new Timestamp(0);
    
    // The contexts and folders for this user (cached).
    private ContextSet contexts;
    private ContextSet folders;

    /**
     * Session token used for all calls to Toodledo. (The authentication key is based on it.)
     * Cached in memory and on disk.
     */
    private SessionToken sessionToken;
    
    /**
     * Authentication key used for all calls to Toodledo.
     * Cached in memory.
     */
    private String authenticationKey;

    /**
     * Constructor.
     * 
     * @param userId Toodledo user id. Not username, but instead the Unique ID
     *               found on the Toodledo Account Settings page.
     * @param password Toodledo password.
     */
    public ToodledoUser(String userId, String password)
    {
        this.userId = userId;
        this.password = password;
    }

    /**
     * Return the last time we downloaded tasks from Toodledo for this user.
     */
    public Timestamp getLastTaskDownloadTimestamp()
    {
        return lastTaskDownloadTimestamp;
    }
    
    /**
     * Set the time we last downloaded tasks to now.
     */
    public void updateLastTaskDownloadTimestamp()
    {
        lastTaskDownloadTimestamp = new Timestamp(); // now
    }
    
    /**
     * Return a Toodledo authentication key.
     * The session token that the authentication key is based on is valid for 4 hours,
     * and Toodledo limits how often you can ask for a new one. So we cache the session
     * token, both in memory and on the file system.
     *
     * @throws ToodledoException If Toodledo responded with an error message.
     * @throws TaskServiceException On any other error. Usually wraps a lower-level exception.
     */
    public String getAuthenticationKey()
        throws ToodledoException, TaskServiceException
    {
        try
        {
            // Do we have a cached authentication key (built from a cached session token)?
            String key = getCachedAuthenticationKey();
            if (key != null)
            {
                logger.finer("Authentication key: " + key);
                return key;
            }

            // We don't have a cached session token, so ask Toodledo for one.
            // Build up the URL to request a session token.
            StringBuffer tokenRequest = new StringBuffer(ObjectFetcher.TOODLEDO_API_URL);
            tokenRequest.append("account/token.php?");
            tokenRequest.append("userid=").append(userId);
            tokenRequest.append(";");
            tokenRequest.append("appid=").append(APP_ID);
            tokenRequest.append(";");
            tokenRequest.append("sig=").append(md5(userId + APP_TOKEN));
            
            // Request a session token.
            sessionToken = ObjectFetcher.request(tokenRequest.toString(), SessionToken.class);
            sessionToken.setUserId(userId);
            sessionToken.save(); // Save it to disk.
            logger.finer("Received a new session token from Toodledo and saved it to disk.");            
            generateAuthenticationKey();
            logger.finer("Authentication key: " + authenticationKey);
            return authenticationKey;
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
     * Look for a cached session token and hence authentication key. Look in memory first,
     * and on disk if not cached in memory.
     *
     * @return The cached authentication key, or null if there is not a cached session token
     * and hence not a cached authentication key.
     * 
     * @throws NoSuchAlgorithmException  If an error generating an MD5 hash. 
     * @throws IOException  On any I/O error loading a session token from disk.
     * @throws ClassNotFoundException  When loading a session token from disk.
     */
    private String getCachedAuthenticationKey() throws NoSuchAlgorithmException, IOException, ClassNotFoundException
    {
        // Is there a session token cached in memory?
        if (sessionToken != null)
        {
            if (sessionToken.isExpired())
            {
                logger.fine("Session token cached in memory has expired.");
                sessionToken = null;
                authenticationKey = null;
            }
            else
            {
                logger.fine("Session token cached in memory is valid.");

                    // In-memory session token is non-null and current. Whenever we cache a session token
                // in memory, we also cache the authentication key (only in memory). So if the session
                // token is non-null, so should the authentication key be non-null. Check it.
                //
                // BTW, an alternate design would have the authentication key stored in the session token.
                // We don't do that because the authentication key includes the password. Session tokens
                // are stored on disk, so we don't want to store the authentication key there. There are 
                // ways of serialising objects so that some fields aren't serialised, but it did seem to
                // point anyway to a poor solution.
                assert authenticationKey != null : "Session token cached in memory, but no authentication key.";
                return authenticationKey;
            }
        }
        
        // No session token cached in memory. Try out on disk.
        SessionToken restoredSessionToken = SessionToken.load();
        if (restoredSessionToken == null)
        {
            logger.fine("No session token available on disk.");
            return null;
        }
        else if (restoredSessionToken.getUserId().equals(userId) && !restoredSessionToken.isExpired())
        {
            logger.fine("Using session token loaded from disk.");
            sessionToken = restoredSessionToken;
            generateAuthenticationKey();
            return authenticationKey;
        }
        
        // There is not a cached session token (nor authentication key).
        logger.fine("No cached session token.");
        return null;
    }
    
    /**
     * Generate an authentication key from this object's session token (sessionToken field).
     * The session token must not be null, and this method asserts if it is null.
     * Store the authentication key to the object's authenticationKey field.
     * 
     * @throws NoSuchAlgorithmException  If an error generating an MD5 hash. 
     */
    private void generateAuthenticationKey() throws NoSuchAlgorithmException
    {
        assert sessionToken != null : "Generating authentication key, but no session object.";
        
        authenticationKey = md5(md5(password) + APP_TOKEN + sessionToken.getValue());
        assert authenticationKey != null : "Generated authentication key, but it is null.";
    }
    
    /**
     * Return an MD5 hash of the given string.
     * 
     * @throws NoSuchAlgorithmException  If a MessageDigest error.
     */
    private static String md5(String str) throws NoSuchAlgorithmException
    {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(str.getBytes(), 0, str.length());
        byte[] digest = messageDigest.digest();

        // Convert the digest to hex.
        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < digest.length; i++)
        {
            String hex = Integer.toHexString(0xff & digest[i]);
            if (hex.length() == 1)
                hash.append('0');
            hash.append(hex);
        }   
        
        return hash.toString();
    }
    
    /**
     * Return the last time we downloaded contexts from Toodledo for this user.
     */
    private Timestamp getLastContextDownloadTimestamp()
    {
        return lastContextDownloadTimestamp;
    }
    
    /**
     * Set the time we last downloaded contexts to now.
     */
    private void updateLastContextDownloadTimestamp()
    {
        lastContextDownloadTimestamp = new Timestamp(); // now
    }
    
    /**
     * Return the last time we downloaded folders from Toodledo for this user.
     */
    private Timestamp getLastFolderDownloadTimestamp()
    {
        return lastFolderDownloadTimestamp;
    }
    
    /**
     * Set the time we last downloaded folders to now.
     */
    private void updateLastFolderDownloadTimestamp()
    {
        lastFolderDownloadTimestamp = new Timestamp(); // now
    }
    
    /**
     * Return the contexts for this user.
     * Only get them from Toodledo if they haven't changed from when we last downloaded them.
     * 
     * @param lastEditedContextTimestamp  When Toodledo says that the contexts last changed.
     * 
     * @return  The contexts for this user.
     * 
     * @throws ToodledoException If Toodledo responded with an error message.
     * @throws TaskServiceException On any other error. Usually wraps a lower-level exception.
     */
    public ContextSet getContexts(Timestamp lastEditedContextTimestamp) 
        throws ToodledoException, TaskServiceException
    {
        // If the contexts haven't changed since we last downloaded them, return the cached ones.
        if (contexts != null && 
            lastEditedContextTimestamp.getValue() < getLastContextDownloadTimestamp().getValue())
        {
            return contexts;
        }
        
        // Ask Toodledo for the contexts for this user.
        try
        {
            String request = ObjectFetcher.TOODLEDO_API_URL + "contexts/get.php?key=" + getAuthenticationKey();
            contexts = new ContextSet(ObjectFetcher.request(request, Context[].class));
            logger.info("Downloaded contexts: " + contexts);            
            updateLastContextDownloadTimestamp();
            return contexts;
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
     * Return the folders for this user.
     * Only get them from Toodledo if they haven't changed from when we last downloaded them.
     * 
     * @param lastEditedContextTimestamp  When Toodledo says that the folders last changed.
     * 
     * @return  The folders for this user.
     * 
     * @throws ToodledoException If Toodledo responded with an error message.
     * @throws TaskServiceException On any other error. Usually wraps a lower-level exception.
     */
    public ContextSet getFolders(Timestamp lastEditedFolderTimestamp) 
        throws ToodledoException, TaskServiceException
    {
        // If the contexts haven't changed since we last downloaded them, return the cached ones.
        if (folders != null && 
            lastEditedFolderTimestamp.getValue() < getLastFolderDownloadTimestamp().getValue())
        {
            return folders;
        }
        
        // Ask Toodledo for the folders for this user.
        try
        {
            String request = ObjectFetcher.TOODLEDO_API_URL + "folders/get.php?key=" + getAuthenticationKey();
            folders = new ContextSet(ObjectFetcher.request(request, Context[].class));
            logger.info("Downloaded folders: " + folders);            
            updateLastFolderDownloadTimestamp();
            return folders;
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
