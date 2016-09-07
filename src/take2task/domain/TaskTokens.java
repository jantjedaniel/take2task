/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.domain;

import java.util.LinkedList;
import java.util.List;

/**
 * The tokens resulting from parsing a task title using the extra Take2Task syntax.
 */
public class TaskTokens
{
    /**
     * Token delimiter.
     * One of these characters represents the start of each modifier.
     */
    public static final char DELIMITER = '/';
    
    /*
     * The number of delimiter characters in a row which precede a due date.
     */
    private static final int DUE_DATE_DELIMITER_COUNT = 2;
    
    /*
     * The number of delimiter characters in a row which precede a start date.
     * Must be a different number than for due date.
     */
    private static final int START_DATE_DELIMITER_COUNT = 3;

    /*
     * The number of delimiter characters in a row which precede a start date.
     * Must be a different number than for due date and for start date.
     */
    private static final int REPEAT_DELIMITER_COUNT = 4;

    /**
     * The delimiter sequence preceding a due date.
     */
    public static final String DUE_DATE_DELIMITER;
    
    /**
     * The delimiter sequence preceding a start date.
     */
    private static final String START_DATE_DELIMITER;
    
    /**
     * The delimiter sequence preceding a repeat.
     */
    private static final String REPEAT_DELIMITER;
    
    private String description;
    private String dueDate;
    private String startDate;
    private String repeat;
    private List<String> modifiers = new LinkedList<String>();
    
    /**
     * Static initialiser.
     * Initialise the due date and start date delimiter strings.
     */
    static
    {
        // The number of delimiter characters which precedes a due date must not be the same
        // as for a start date. If it is we can't distinguish them when parsing.
        // The number for due date should be shorter, since it is far more common to
        // enter due dates than start dates, hence make the more convenient action for due date.
        // Same goes for the repeat string.
        assert DUE_DATE_DELIMITER_COUNT < START_DATE_DELIMITER_COUNT && 
               START_DATE_DELIMITER_COUNT < REPEAT_DELIMITER_COUNT;
        
        // Build the due date delimiter string.
        StringBuffer dueDateDelimiterBuffer = new StringBuffer();
        for (int i = 0; i < DUE_DATE_DELIMITER_COUNT; i++)
        {
            dueDateDelimiterBuffer.append(DELIMITER);
        }
        DUE_DATE_DELIMITER = dueDateDelimiterBuffer.toString();
        
        // Build the start date delimiter string.
        StringBuffer startDateDelimiterBuffer = new StringBuffer();
        for (int i = 0; i < START_DATE_DELIMITER_COUNT; i++)
        {
            startDateDelimiterBuffer.append(DELIMITER);
        }
        START_DATE_DELIMITER = startDateDelimiterBuffer.toString();
        
        // Build the repeat delimiter string.
        StringBuffer repeatDelimiterBuffer = new StringBuffer();
        for (int i = 0; i < REPEAT_DELIMITER_COUNT; i++)
        {
            repeatDelimiterBuffer.append(DELIMITER);
        }
        REPEAT_DELIMITER = repeatDelimiterBuffer.toString();
    }
    
    /**
     * Constructor. Parse the given task title string into task tokens.
     * 
     * @param title  A task title using the extra Take2Task syntax to specify task fields.
     */
    public TaskTokens(String title)
    {
        int firstDelimiterIndex = title.indexOf(" " + DELIMITER);
        if (firstDelimiterIndex == -1)
        {
            description = title.trim();
            return;
        }
        else
        {
            firstDelimiterIndex++; // to go past the space
        }

        // Get the description
        description = title.substring(0, firstDelimiterIndex).trim();
        String remainder = title.substring(firstDelimiterIndex).trim();
        
        // Look for a repeat string and remove it from the remainder string. 
        // We have to do this before start date and due date, since their 
        // identifier strings are a sub-string of the repeat string.
        String[] results = findAndRemove(remainder, REPEAT_DELIMITER);
        remainder = results[0];
        repeat = results[1];

        // Look for a start date and remove it from the remainder string. 
        // We have to do this before due date, since the due date identifier string
        // is a sub-string of the start date string.
        results = findAndRemove(remainder, START_DATE_DELIMITER);
        remainder = results[0];
        startDate = results[1];

        // Look for a due date and remove it from the remainder string. 
        results = findAndRemove(remainder, DUE_DATE_DELIMITER);
        remainder = results[0];
        dueDate = results[1];
        
        // Parse the remainder into modifiers.
        String[] stringsAfterDelimiters = remainder.split("" + DELIMITER);
        for (String modifier : stringsAfterDelimiters)
        {
            modifier = modifier.trim();
            if (modifier.length() > 0)
            {
                modifiers.add(modifier);
            }
        }
        
        // Users :-) say that they sometimes forget to put two delimiters before a due date.
        // For example, instead of "@@today" they reenter just "@today". They want any valid
        // date after a single delimiter to be treated as a due date as well.
        // So if we haven't already got a due date, parse the remaining delimiters, 
        // and if one matches a date, store it as due date.
        if (dueDate == null)
        {
            for (String modifier : modifiers)
            {
                if (Timestamp.parse(modifier) != null)
                {
                    // This is a hack. We have just parsed the string, but now we are 
                    // going to store the string so it will be parsed again in Task.modify().
                    // So be it!
                    dueDate = modifier;
                    modifiers.remove(modifier);
                    break;
                }
            }
        }
    }
    
    /**
     * Return the description, which is the part of the title before any modifiers, 
     * that is, before the first delimiter character.
     */
    public String getDescription()
    {
        return this.description;
    }
    
    /**
     * Return the due date string which follows the due date delimiter characters.
     * Return an empty string if the due date delimiter characters were found, but
     * if there was no string following them (i.e. indicating just pseudo-date override).
     * Return null if the due date delimiter characters were not found.
     */
    public String getDueDate()
    {
        return this.dueDate;
    }
    
    /**
     * Return the start date string which follows the start date delimiter characters.
     * Return an empty string if the start date delimiter characters were found, but
     * if there was no string following them.
     * Return null if the start date delimiter characters were not found.
     */
    public String getStartDate()
    {
        return this.startDate;
    }
    
    /**
     * Return the repeat string which follows the repeat delimiter characters.
     * Return an empty string if the repeat delimiter characters were found, but
     * if there was no string following them.
     * Return null if the repeat delimiter characters were not found.
     */
    public String getRepeat()
    {
        return this.repeat;
    }
    
    /**
     * Return the modifiers. These are the strings that follow each single
     * delimiter character.
     * 
     * @return The modifiers. If there were no modifiers, return an empty set (not a null).
     */
    public List<String> getModifiers()
    {
        return this.modifiers;
    }
    
    /**
     * Return a string representation of the task.
     */
    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer(
            String.format("Description: [%s], Due date: [%s], Startdate: [%s], Repeat: [%s], Modifiers:" , 
                          getDescription(), getDueDate(), getStartDate(), getRepeat()));
        
        for (String modifier : getModifiers())
        {
            buffer.append(String.format(" [%s]", modifier));
        }
        
        return buffer.toString();
    }
    
    /**
     * Look for the given sequence of delimiters in the given source string.
     * If it is present, remove it and all characters that follow it until
     * the next delimiter or the end of the string.
     * 
     * @param str  The string to parse.
     * @param delimiters  The sequence of delimiters to look for.
     * 
     * @return  An array of two strings.
     *          The zeroth element is the remainder of str after removing
     *          the sequence of delimiters and the string that follows it
     *          until the next delimiter or the end of str. If the sequence
     *          of delimiters is not found, the zeroth element is str unchanged.
     *          The first element is the string that follows the sequence
     *          of delimiters until the next delimiter or the end of str.
     *          If the sequence of delimiters is not found, the first element is null.
     */
    private String[] findAndRemove(String source, String delimiters)
    {
        String[] results = new String[2];
        
        // Look for the delimiter sequence.
        int indexOfDelimiterSequence = source.indexOf(delimiters.toString());
        
        if (indexOfDelimiterSequence == -1) // not found
        {
            results[0] = source;
            results[1] = null;
        }
        else
        {
            String strBeforeDelimiterSequence = source.substring(0, indexOfDelimiterSequence).trim();
            String strAfterDelimiterSequence = source.substring(indexOfDelimiterSequence + delimiters.length());
            int indexOfNextDelimiter = strAfterDelimiterSequence.indexOf(DELIMITER);
            if (indexOfNextDelimiter == -1) // not found
            {
                results[0] = strBeforeDelimiterSequence;
                results[1] = strAfterDelimiterSequence.trim();
            }
            else
            {
                results[0] = strBeforeDelimiterSequence + ' ' + strAfterDelimiterSequence.substring(indexOfNextDelimiter);
                results[1] = strAfterDelimiterSequence.substring(0, indexOfNextDelimiter).trim();
            }
        }
        
        return results;
    }
    
    /**
     * Main method, used for testing.
     * 
     * @param args  Command line arguments. Not used.
     */
    public static void main(String[] args)
    {
        String[] titles = 
        {
            "Do stuff sometime",
            "Buy a newspaper //next tuesday ///next monday /top /star /errand",
            "Fly to the moon //next monday",
            "Fly back from the moon //next friday ///next monday",
            "Do a serious work task /sdk handover",
            "Discuss the serious work task with someone important /sdk handover /discuss /top //tomorrow",
            "Book a holiday /holiday //",
            "Book another holiday /holiday /star // next month",
            "Do this every week //sun ///sat ////weekly",
            "Start this every Monday ///monday ////daily",
            "These should not become tags because no space before the slash: before/during/after"
        };
        
        for (String title : titles)
        {
            TaskTokens taskTokens = new TaskTokens(title);
            System.out.printf("%s : %s\n", title, taskTokens);
        }
    }
}
