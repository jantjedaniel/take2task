/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.service.toodledo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Utility methods used when communicating with Toodledo.
 */
public class ObjectFetcher
{
    /**
     * Start of the URL for all requests to Toodledo.
     */
    public static final String TOODLEDO_API_URL = "http://api.toodledo.com/2/";    

    private static Gson gson = new Gson(); // GSON library object used to convert between JSON and Java objects.

    /**
     * Make a request to Toodledo, returning an object of the given type.
     * 
     * @param request The URL defining the request of Toodledo.
     * @param classOfT  The class of the object to return.
     * 
     * @return  The object, of the specified class, encapsulating the data returned by Toodledo.
     * 
     * @throws MalformedURLException  If the request is a malformed URL.
     * @throws ToodledoException  If Toodledo returned one of its defined error messages
     *                            rather than the anticipated data.
     * @throws IOException  On any I/O error communicating with Toodledo.
     * @throws JsonParseException On any error parsing Toodledo JSON data.
     */
    public static <T> T request(String request, Class<T> classOfT) 
        throws MalformedURLException, ToodledoException, IOException, JsonParseException
    {
        URL requestUrl = new URL(request);
        URLConnection connection = requestUrl.openConnection();
        PushbackReader json = new PushbackReader(new InputStreamReader(connection.getInputStream()), 
                                                 ToodledoException.ERROR_START.length());
        return convertFromJson(json, classOfT);
    }

    /**
     * Convert the given stream of Toodledo JSON data to an object representation.
     * 
     * @param json  The Toodledo JSON data to convert. This method will always close
     *              the reader before returning, so don't use it after the call.
     * @param classOfT  The class of the object to return.
     * 
     * @return  An object of the specified class.
     * 
     * @throws ToodledoException  If the JSON stream was actually a Toodledo error message
     *                            rather than the anticipated data.
     * @throws IOException  On any I/O error.
     * @throws JsonParseException On any error parsing the JSON data.
     */
    private static <T> T convertFromJson(PushbackReader json, Class<T> classOfT)
        throws ToodledoException, IOException, JsonParseException
    {
        try
        {    
            // First check of the the JSON data is a Toodledo error message,
            // rather than the data we expect.
            checkIfErrorMessage(json);
            
            // It isn't a Toodledo error message (because a ToodledoException would 
            // have been thrown if it was. Try to parse as the expected data. 
            return gson.fromJson(json, classOfT);
        }
        finally
        {
            if (json != null)
            {
                json.close();
            }
        }
    }
    
    /**
     * Check if the given JSON data is a Toodledo error message.
     * If it is, throw a ToodledoException encapsulating the error message.
     * If it is not, just return.
     * 
     * @param json  The JSON data to read. 
     *              This method never closes this reader, even if an exception is thrown.
     *              It is always the caller's responsibility to close it.
     *              
     * @throws ToodledoException  If the JSON data was an error message. This exception contains the error message.
     * @throws IOException  On any I/O error.
     * @throws JsonParseException  On any error parsing the JSON data.
     */
    private static void checkIfErrorMessage(PushbackReader json) 
        throws ToodledoException, IOException, JsonParseException
    {
        final int lookAheadLength = ToodledoException.ERROR_START.length();
        char[] buffer = new char[lookAheadLength];
        final int countRead = json.read(buffer, 0, lookAheadLength);
        json.unread(buffer); // Set the reader back to the start of the JSON data.
        if (countRead == lookAheadLength && ToodledoException.ERROR_START.equals(new String(buffer)))
        {
            // Yep. It is an error message. Convert the whole message from the JSON.
            ToodledoException toodledoException = gson.fromJson(json, ToodledoException.class);
            
            // Throw it to our caller.
            throw toodledoException;
        }
    }
}
