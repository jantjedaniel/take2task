/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.domain;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * An instance in time, derived from a Unix timestamp with second (not millisecond) resolution.
 */
public class Timestamp
{
    /**
     * java.util.Date representation of the timestamp. 
     * java.util.Date stores timestamp in milliseconds, not seconds.
     */
    private Date date;
    
    /**
     * Constructor.
     * 
     * @param value  Number of seconds from 1 January 1970 GMT.
     */
    public Timestamp(long value)
    {
        this.date = new Date(value * 1000); // Convert the timestamp from seconds to milliseconds.
    }
    
    /**
     * Constructor.
     * 
     * @param cal  A date object representing a time.
     *             If null, set the timestamp to the start of the "epoch", that is, 1 January 1970 GMT.
     */
    public Timestamp(Date date)
    {
        this.date = (date == null) ? new Date(0) : date;
    }
    
    /**
     * Constructor.
     * 
     * @param cal  A calendar object representing a time.
     *             If null, set the timestamp to the start of the "epoch", that is, 1 January 1970 GMT.
     */
    public Timestamp(Calendar cal)
    {
        this.date = (cal == null) ? new Date(0) : cal.getTime();
    }
    
    /**
     * No arg constructor. Set the time to now.
     */
    public Timestamp()
    {
        this.date = new Date();
    }
    
    /**
     * Returns the timestamp value as a number of seconds since 1 January 1970 GMT in seconds.
     */
    public long getValue()
    {
        return date.getTime() / 1000;
    }
    
    /**
     * Return a string representation of the task.
     */
    @Override
    public String toString()
    {
        return date.getTime() == 0 ? "0" : date.toString();
    }    

    /**
     * Return whether the given object is equal in state to this one.
     */
    @Override
    public boolean equals(Object obj)
    {
        try
        {
            Timestamp other = (Timestamp) obj;
            return getValue() == other.getValue();
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }
    
    /**
     * Return whether this is an actual date, that is, not "blank" and not a pseudo-date.
     */
    public boolean isActualDate()
    {
        long value = this.getValue();
        return value > 0 && value < LOWEST_PSEUDO_DATE.getValue();
    }
    
    // -------------------- Today --------------------
    
    /**
     * Return a timestamp representing today in the form Toodledo is happy with,
     * that is, with the time component set to exactly 12 noon.
      */
    public static Timestamp today()
    {
        return new Timestamp(Timestamp.todayCalendar());
    }

    /**
     * Return a calendar object representing today in the form Toodledo is happy with,
     * that is, with the time component set to exactly 12 noon.
      */
    private static Calendar todayCalendar()
    {
        // Get today in our timezone.
        Calendar todayOurTimezone = Calendar.getInstance();
        
        // Set today on a GMT calendar, since Toodledo date timestamps have to be 
        // time from 1 January 1970 _GMT_.
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        today.set(Calendar.YEAR, todayOurTimezone.get(Calendar.YEAR));
        today.set(Calendar.DAY_OF_YEAR, todayOurTimezone.get(Calendar.DAY_OF_YEAR));
        
        // Toodledo wants date timestamps to have a time component of exactly noon.
        today.set(Calendar.HOUR_OF_DAY, 12);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        return today;
    }
    
    // -------------------- Pseudo-dates --------------------
    
    private static final int DEFAULT_MONTH_OFFSET = 3;
    private static final int WORK_MONTH_OFFSET = 4;
    private static final int NOTES_MONTH_OFFSET = 5;
    
    // The pseudo-date with the lowest actual value.
    private static final Timestamp LOWEST_PSEUDO_DATE = Timestamp.getPseudoDate((String)null, Task.Status.NEXT_ACTION);

    /**
     * Return a pseudo-date.
     * 
     * @param context  A task folder.
     * @param status   A task status.
     */
    public static Timestamp getPseudoDate(Context context, Task.Status status)
    {
        String contextName = (context == null) ? null : context.getName();
        return getPseudoDate(contextName, status);
    }

    /**
     * Return a pseudo-date.
     * 
     * @param context  A task context.
     * @param status   A task status.
     */
    private static Timestamp getPseudoDate(String context, Task.Status status)
    {
        int monthOffset = DEFAULT_MONTH_OFFSET;

        // (This is ugly code. This should be a lookup into a table, or perhaps
        // we need a PseudoDate class?)
        if (context != null && context.equals(Task.WORK_CONTEXT_NAME))
        {
            monthOffset = WORK_MONTH_OFFSET;
        }
        else if (context != null && context.equals(Task.NOTES_CONTEXT_NAME))
        {
            monthOffset = NOTES_MONTH_OFFSET;
        }
        
        return getPseudoDate(monthOffset, status.getPseudoDateDayOfMonth());
    }
    
    /**
     * Return a pseudo-date.
     * 
     * @param monthOffset  The offset in months from today.
     * @param dayOfMonth   The day of the month.
     */
    private static Timestamp getPseudoDate(int monthOffset, int dayOfMonth)
    {
        Calendar cal = todayCalendar();
        cal.add(Calendar.MONTH, monthOffset);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return new Timestamp(cal);
    }
    
    // -------------------- Archiving future tasks --------------------

    /**
     * Return whether this date has been archived. 
     */
    public boolean isArchived()
    {
        // We move dates forward a year when we archive them, so use a threshold
        // of 300 days, allowing for about two months of days before today that might
        // be archived. This should only happen if Take2Task hasn't been running 
        // for a while.
        final int ARCHIVE_THRESHOLD_DAYS = 300;
        
        // Set a date at the archive threshold.
        Calendar archiveThreshold = Timestamp.todayCalendar();
        archiveThreshold.add(Calendar.DAY_OF_YEAR, ARCHIVE_THRESHOLD_DAYS);
        
        // Are we beyond the threshold, and hence an archived date?
        return date.getTime() > archiveThreshold.getTime().getTime();
    }
    
    /**
     * Return whether this date represents today or a recent date, but having been archived. 
     */
    public boolean isTodayOrRecentButArchived()
    {
        return isArchived() && unarchive().date.getTime() <= today().date.getTime();
    }
    
    /**
     * Return a new date representing this one having been archived.
     */
    public Timestamp archive()
    {
        return archive(true);
    }
    
    /**
     * Return a new date representing this one having been archived.
     */
    public Timestamp unarchive()
    {
        return archive(false);
    }
    
    /**
     * Return a new date representing this one having been archived or unarchived.
     * 
     * @param forwards  true to archive, false to unarchive.
     */
    private Timestamp archive(boolean forwards)
    {
        // If archiving, go forwards a year. If unarchiving, go backwards a year.
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, forwards ? 1 : -1);
        return new Timestamp(cal);
    }
    
    // -------------------- Dates from keywords --------------------
    
    /**
     * Return a new date parsed from the given string.
     * 
     * @param source  The string to parse. Either a keyword such as "tomorrow"
     *                or a date in a format such as DD/MM/YY (depending on locale).
     *                
     * 
     * @return A timestamp, or null if there is not a date for the given keyword.
     */
    public static Timestamp parse(String source)
    {
        if (source == null || source.trim().length() == 0)
        {
            return null;
        }
        
        // Add a week if the string starts with the following.
        final String ADD_WEEK_KEYWORD = "next";
        boolean addWeek = false;
        if (source.startsWith(ADD_WEEK_KEYWORD))
        {
            addWeek = true;
            source = source.substring(ADD_WEEK_KEYWORD.length()).trim();
        }
        
        if (source.equals("today") || source.equals("tod"))
        {
            return today();
        }
        else if (source.equals("tomorrow") || source.equals("tom"))
        {
            Calendar tomorrow = todayCalendar();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            return new Timestamp(tomorrow);
        }
        else if (source.equals("monday") || source.equals("mon"))
        {
            return dateFromDayOfWeek(Calendar.MONDAY, addWeek);
        }
        else if (source.equals("tuesday") || source.equals("tues") || source.equals("tue"))
        {
            return dateFromDayOfWeek(Calendar.TUESDAY, addWeek);
        }
        else if (source.equals("wednesday") || source.equals("wed"))
        {
            return dateFromDayOfWeek(Calendar.WEDNESDAY, addWeek);
        }
        else if (source.equals("thursday") || source.equals("thurs") || source.equals("thur"))
        {
            return dateFromDayOfWeek(Calendar.THURSDAY, addWeek);
        }
        else if (source.equals("friday") || source.equals("fri"))
        {
            return dateFromDayOfWeek(Calendar.FRIDAY, addWeek);
        }
        else if (source.equals("saturday") || source.equals("sat"))
        {
            return dateFromDayOfWeek(Calendar.SATURDAY, addWeek);
        }
        else if (source.equals("sunday") || source.equals("sun"))
        {
            return dateFromDayOfWeek(Calendar.SUNDAY, addWeek);
        }
        else
        {
            try
            {
                DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date date = format.parse(source);
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                cal.setTime(date);
                cal.add(Calendar.HOUR_OF_DAY, 12);
                return new Timestamp(cal);
            }
            catch (ParseException e)
            {
                return null;
            }
        }
    }

    /**
     * Return a date representing the next day that has the given day of the week.
     * 
     * @param dayOfWeek A day of the week as defined by the Calendar class.
     *                  For example, Calendar.MONDAY.
     *                  
     * @return A timestamp.
     */
    private static Timestamp dateFromDayOfWeek(int dayOfWeek, boolean addWeek)
    {
        Calendar cal = todayCalendar();
        cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        
        // If the date is earlier than today, add a week, because
        // we want the next date (including but not earlier than today) with the given day.
        if (cal.before(todayCalendar()))
        {
            cal.add(Calendar.DAY_OF_YEAR, 7);
        }
        
        // Have we been asked to add a week?
        if (addWeek)
        {
            cal.add(Calendar.DAY_OF_YEAR, 7);
        }
        return new Timestamp(cal);
    }
    
    // -------------------- Testing --------------------
    
    /**
     * Main method, used for testing.
     * 
     * @param args  Command line arguments. Not used.
     */
    public static void main(String[] args)
    {
        Timestamp today = Timestamp.today();
        System.out.printf("Today: %s\n", today);

        System.out.printf("Default 'next action' pseudo-date: %s\n", Timestamp.getPseudoDate((String)null, Task.Status.NEXT_ACTION));
        System.out.printf("Default active pseudo-date:        %s\n", Timestamp.getPseudoDate((String)null, Task.Status.ACTIVE));
        System.out.printf("Default planning pseudo-date:      %s\n", Timestamp.getPseudoDate((String)null, Task.Status.PLANNING));
        System.out.printf("Default waiting pseudo-date:       %s\n", Timestamp.getPseudoDate((String)null, Task.Status.WAITING));

        System.out.printf("Work 'next action' pseudo-date: %s\n", Timestamp.getPseudoDate(Task.WORK_CONTEXT_NAME, Task.Status.NEXT_ACTION));
        System.out.printf("Work active pseudo-date:        %s\n", Timestamp.getPseudoDate(Task.WORK_CONTEXT_NAME, Task.Status.ACTIVE));
        System.out.printf("Work planning pseudo-date:      %s\n", Timestamp.getPseudoDate(Task.WORK_CONTEXT_NAME, Task.Status.PLANNING));
        System.out.printf("Work waiting pseudo-date:       %s\n", Timestamp.getPseudoDate(Task.WORK_CONTEXT_NAME, Task.Status.WAITING));
        System.out.printf("Notes reference pseudo-date:       %s\n", Timestamp.getPseudoDate(Task.NOTES_CONTEXT_NAME, Task.Status.REFERENCE));
        
        System.out.printf("Today but archived: %s\n", today.archive());
        System.out.printf("Today archived and then unarchived: %s\n", today.archive().unarchive());
        System.out.printf("Is today equal to today archived and then unarchived?: %s\n", today.equals(today.archive().unarchive()));
        
        // Archive the date a month ago, and test that it is seen as archived.
        Calendar cal = Timestamp.todayCalendar();
        cal.add(Calendar.MONTH, -1);
        Timestamp lastMonth = new Timestamp(cal);
        System.out.printf("Last month: %s\n", lastMonth);
        System.out.printf("Last month archived: %s\n", lastMonth.archive());
        System.out.printf("Is last month archived seen as archived?: %s\n", lastMonth.archive().isArchived());        

        System.out.printf("Is today seen as archived? (hopefully not): %s\n", today.isArchived());        
        System.out.printf("Is today archived seen as archived?: %s\n", today.archive().isArchived());
        
        System.out.printf("Is today archived seen as isTodayOrRecentButArchived()?: %s\n", today.archive().isTodayOrRecentButArchived());
        System.out.printf("Is last month archived seen as isTodayOrRecentButArchived()? (hopefully yes): %s\n", lastMonth.archive().isTodayOrRecentButArchived());

        cal = Timestamp.todayCalendar();
        cal.add(Calendar.MONTH, -6);
        Timestamp sixMonthsAgo = new Timestamp(cal);
        System.out.printf("Is six months ago archived seen as isTodayOrRecentButArchived()? (hopefully not): %s\n", sixMonthsAgo.archive().isTodayOrRecentButArchived());
        
        String[] dateStrings =
        {
            null,
            "",
            " ",
            "today",
            "tod",
            "tomorrow",
            "tom",
            "monday",
            "mon",
            "tuesday",
            "tues",
            "tue",
            "wednesday",
            "wed",
            "thursday",
            "thurs",
            "thur",
            "friday",
            "fri",
            "saturday",
            "sat",
            "sunday",
            "sun",
            "next monday",
            "next tuesday",
            "next wednesday",
            "next thursday",
            "next friday",
            "next saturday",
            "next sunday",
            "fish", // nonsense date string
            "24/5/11",
            "24/5/2011",
            "24/05/2011",
            "1/10/11",
            "2/10/11", // (daylight saving starts)
            "1/1/12",
        };
        for (String str : dateStrings)
        {
            System.out.printf("'%s': %s\n", str, Timestamp.parse(str));
        }
    }
}
