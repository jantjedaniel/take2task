/*
 * Copyright 2011 by Ian Daniel.
 * All rights reserved.
 */

package take2task.domain;

import java.util.List;

/**
 * A task.
 */
public class Task
{
    // Modifier to mark a task as starred.
    private static final String STAR_MODIFIER = "star";

    // Modifier for marking a task as an expense to be entered in The Pig (PocketMoney).
    /*
    private static final String EXPENSES_MODIFIER = "x";    
    private static final String EXPENSES_FOLDER = "p2 - The Pig";    
    private static final String EXPENSES_TAG = "_expenses";    
    private static final String EXPENSES_TITLE_PREFIX = "Expense: ";
     */  
    
    // Modifiers to set an attribute to no value.
    private static final String NO_CONTEXT_MODIFIER = "nocontext";
    private static final String NO_FOLDER_MODIFIER = "nofolder";
    private static final String NO_STAR_MODIFIER = "nostar";
    private static final String NO_TAG_MODIFIER = "notag";
    private static final String NO_DUE_DATE_MODIFIER = "nod";

    // Names of contexts that have special pseudo-dates.
    static final String PERSONAL_CONTEXT_NAME = "Personal";
    static final String WORK_CONTEXT_NAME     = "Work";
    static final String NOTES_CONTEXT_NAME    = "x Notes";

    // Start of a "waiting" task.
    private static final String WAITING_LONG_IDENTIFIER = "Waiting for";
    private static final String WAITING_SHORT_IDENTIFIER = "wf ";
    
    // Start of a "project" task.
    private static final String PROJECT_IDENTIFIER = ".";
    
    // Name of the default personal folder (if a personal task isn't in a folder).
    private static final String DEFAULT_PERSONAL_FOLDER_NAME = "p Personal";
    
    // Name of the default work folder (if a work task isn't in a folder).
    private static final String DEFAULT_WORK_FOLDER_NAME = "w Work";
    
    // Start of the name of any Google Calendar reminder emailed to Toodledo.
    private static final String GOOGLE_CALENDAR_REMINDER_PREFIX = "Reminder: ";
    
    // Start of the modifier banner that goes in the note field.
    private static final String MODIFIER_BANNER_START = "~~";
    
    // Separators between tags in the tag string.
    private static final String TAG_DELIMITER = ",";
    
    // Tag to add to a task to make sure it is seen as a project.
    // Useful if there are not currently any sub-tasks for the project.
    private static final String PROJECT_TAG = "_project";
    
    // Maximum number of characters in a note. A note can be very long when imported from an email.
    private static final int MAXIMUM_NOTE_LENGTH = 600;
    
    // Fields must have the following names for conversion from Toodledo JSON data to work (using GSON).
    private long id;
    private String title;
    private String note;
    private int priority;
    private int star;
    private long duedate;
    private long startdate;
    private String repeat;
    private long context;
    private long folder;
    private String tag;
    private int status;
    private int children;

    /**
     * Which fields to return when requesting tasks. This string is included in the request to Toodledo.
     * (id, title, modified and completed are mandatory and don't have to be included in this string.)
     */
    public static final String FIELDS = "fields=note,priority,star,duedate,startdate,repeat,context,folder,tag,status,children";
    
    // Available contexts. This will be the same for all tasks for this user.
    // If this task has a context, it will be one of these.
    // Transient so that GSON does not include it in the JSON for a task.
    private transient ContextSet availableContexts;
    
    // Available folders. This will be the same for all tasks for this user.
    // If this task has a folder, it will be one of these.
    // Transient so that GSON does not include it in the JSON for a task.
    private transient ContextSet availableFolders;

    // Are we overriding the pseudo-date?
    // Transient so that GSON does not include it in the JSON for a task.
    private transient boolean isOverridingPseudoDate;
    
    // Are we reverting to using the pseudo-date? That is, has the user said to remove the due date.
    private transient boolean isRemovingDueDate;
    
    // ---------- Enumerated types ----------
    
    /**
     * Toodledo priorities.
     */
    public enum Priority
    {
        NEGATIVE(-1),
        LOW(0),
        MEDIUM(1),
        HIGH(2),
        TOP(3);
        
        private int numericValue;
        
        private Priority(int numericValue)
        {
            this.numericValue = numericValue;
        }
        
        public int getNumericValue()
        {
            return this.numericValue;
        }
        
        /**
         * Return the Priority value that matches the given string, ignoring case.
         * 
         * @param str The string to match against.
         * 
         * @return The matching Priority, or null if no match.
         */
        public static Priority valueOfIgnoreCase(String str)
        {
            for (Priority p : Priority.values())
            {
                if (p.toString().equalsIgnoreCase(str))
                {
                    return p;
                }
            }
            return null; // no match
        }
    } // end of enum Priority
    
    /**
     * Toodledo statuses.
     */
    public enum Status
    {
        NONE(0, 1, null),
        NEXT_ACTION(1, 1, "next"),
        ACTIVE(2, 7, null),
        PLANNING(3, 14, "plan"),
        DELEGATED(4, 21, "delegate"),
        WAITING(5, 21, null),
        HOLD(6, 21, null),
        POSTPONED(7, 21, null),
        SOMEDAY(8, 28, null),
        CANCELED(9, 28, null),
        REFERENCE(10, 28, "ref");
        
        public static final Status DEFAULT = NEXT_ACTION;
        public static final Status FUTURE  = HOLD;
        
        private int numericValue;
        private int pseudoDateDayOfMonth;
        private String shortcut;
        
        /**
         * Constructor.
         * 
         * @param numericValue  The numeric value of the status.
         *                      These are the values that Toodledo uses.
         * @param pseudoDateDayOfMonth  The day of month to use for this
         *                              status when determining a pseudo-date.
         * @param shortcut  Shortcut version of status when entered in
         *                  task description
         */
        private Status(int numericValue, int pseudoDateDayOfMonth, String shortcut)
        {
            this.numericValue = numericValue;
            this.pseudoDateDayOfMonth = pseudoDateDayOfMonth;
            this.shortcut = shortcut;
        }
        
        public int getNumericValue()
        {
            return this.numericValue;
        }

        public int getPseudoDateDayOfMonth()
        {
            return this.pseudoDateDayOfMonth;
        }

        /**
         * Return the Status value that matches the given string, ignoring case.
         * Also look for Status value matching defined shortcut strings.
         * 
         * @param str The string to match against.
         * 
         * @return The matching Status, or null if no match.
         */
        public static Status valueOfIgnoreCase(String str)
        {
            for (Status s : Status.values())
            {
                // Look for a match against the shortcut (if one).
                if (s.shortcut != null && s.shortcut.equalsIgnoreCase(str))
                {
                    return s;
                }                      
                
                // Look for a match against the string version of the status.
                if (s.toString().equalsIgnoreCase(str))
                {
                    return s;
                }
            }
            return null; // no match
        }
    } // end of enum Status

    // ---------- Public methods ----------
    
    public long getId()
    {
        return id;
    }
    
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    /**
     * Return the value of the note field.
     * 
     * @return The note field. Return an empty string if the underlying value is null.
     */
    public String getNote()
    {
        return note == null ? "" : note.trim();
    }
    public void setNote(String note)
    {
        this.note = note;
        limitNoteLength();
    }
    private void limitNoteLength()
    {
        if (note != null && note.length() > MAXIMUM_NOTE_LENGTH)
        {
            note = note.substring(0, MAXIMUM_NOTE_LENGTH);
        }
    }
    
    public Priority getPriority()
    {
        for (Priority p : Priority.values())
        {
            if (p.getNumericValue() == this.priority)
                return p;
        }
        return Priority.NEGATIVE;
    }
    public void setPriority(Priority priority)
    {
        this.priority = priority.getNumericValue();
    }
    
    public boolean isStarred()
    {
        return this.star == 1;
    }
    public void setStarred(boolean starred)
    {
        this.star = starred ? 1 : 0;
    }
    
    public Timestamp getDueDate()
    {
        return new Timestamp(this.duedate);
    }
    public void setDueDate(Timestamp timestamp)
    {
        this.duedate = timestamp.getValue();
    }
    
    public Timestamp getStartDate()
    {
        return new Timestamp(this.startdate);
    }
    public void setStartDate(Timestamp timestamp)
    {
        this.startdate = timestamp.getValue();
    }

    public String getRepeat()
    {
        return this.repeat == null ? "" : this.repeat;
    }
    public void setRepeat(String repeat)
    {
        this.repeat = repeat;
    }

    /**
     * Return the context, or null if the task does not have a context.
     */
    public Context getContext()
    {
        return (availableContexts == null) ? null : availableContexts.findById(this.context);
    }
    
    /**
     * Set the context to the given one.
     */
    public void setContext(Context context)
    {
        this.context = (context == null) ? 0 : context.getId();
    }
    
    /**
     * Return the folder, or null if the task does not have a folder.
     */
    public Context getFolder()
    {
        return (availableFolders == null) ? null : availableFolders.findById(this.folder);
    }
    
    /**
     * Set the folder to the given one.
     */
    public void setFolder(Context folder)
    {
        this.folder = (folder == null) ? 0 : folder.getId();
    }
    
    public String getTags()
    {
        return this.tag == null ? "" : this.tag;
    }
    
    /**
     * Remove all tags.
     */
    public void clearTags()
    {
        this.tag = "";
    }

    /**
     * Return whether one of the tags is the given label.
     */
    public boolean tagsContains(String label)
    {
        if (tag == null)
        {
            return false;
        }
        
        for (String t : tag.split(TAG_DELIMITER))
        {
            if (t.trim().equalsIgnoreCase(label))
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Add the given label to the tag string, if it is not already there.
     * 
     * @param label The label to add to the tag string.
     * 
     * @return true if the label was not in the tag string and hence we added it.
     *         false if the label was already in the tag string.
     */
    public boolean addTag(String label)
    {
        if (tagsContains(label))
        {
            return false;
        }
        
        if (tag == null)
        {
            tag = "";
        }

        if (!tag.trim().isEmpty())
        {
            tag += ", ";
        }
        tag += label;

        return true;
     }
    
    public Status getStatus()
    {
        for (Status s : Status.values())
        {
            if (s.getNumericValue() == this.status)
                return s;
        }
        return Status.NONE;
    }
    public void setStatus(Status status)
    {
        this.status = status.getNumericValue();
    }
    
    /**
     * Return whether the status is Waiting.
     */
    public boolean isWaiting()
    {
        return getStatus() == Task.Status.WAITING;
    }
    
    /**
     * Return the number of children that this task has.
     */
    public int getChildrenCount()
    {
        return children;
    }
    
    /**
     * Return whether this task is a project or a checklist,
     * i.e. whether it has or can have sub-tasks.
     */
    public boolean isProject()
    {
        // Todo stores the following strings in the notes field of empty
        // projects or checklists.
        final String PROJECT_IDENTIFIER   = "---- Task Type: Project ----";
        final String CHECKLIST_IDENTIFIER = "---- Task Type: Checklist ----";
        
        return getChildrenCount() > 0 ||
            getTags().contains(PROJECT_TAG) ||
            getNote().contains(PROJECT_IDENTIFIER) || 
            getNote().contains(CHECKLIST_IDENTIFIER);
    }
 
    /**
     * Set the available contexts. This will be the same for all tasks for a user.
     */
    public void setAvailableContexts(ContextSet availableContexts)
    {
        this.availableContexts = availableContexts;
    }

    /**
     * Set the available folders. This will be the same for all tasks for a user.
     */
    public void setAvailableFolders(ContextSet availableFolders)
    {
        this.availableFolders = availableFolders;
    }

    /**
     * Return a string representation of the task.
     */
    @Override
    public String toString()
    {
        return String.format("[%s], Priority: %s, Starred?: %b, Due date: %s, Start date: %s, Repeat: [%s], Context: %s, Folder: %s, Tag: [%s], Status: %s, Children: %d, Note: [%s], Overriding pseudo-date?: %b" ,
                             getTitle(),
                             getPriority(),
                             isStarred(),
                             getDueDate(),
                             getStartDate(),
                             getRepeat(),
                             getContext(),
                             getFolder(),
                             getTags(),
                             getStatus(),
                             getChildrenCount(),
                             getNote(),
                             isOverridingPseudoDate()
        );
    }
    
    /**
     * Inspect the task and modify it based on any modifier strings present
     * in the title, or if a pseudo-date needs setting.
     * 
     * @return true if the tasks was modified, else false.
     */
    public boolean modify()
    {
        boolean isModified = false;
        
        // Is this task the result of Google Calendar emailing us a reminder?
        // If so, remove the prefix Google Calendar puts in the email title.
        if (getTitle().startsWith(GOOGLE_CALENDAR_REMINDER_PREFIX))
        {
            setTitle(getTitle().substring(GOOGLE_CALENDAR_REMINDER_PREFIX.length()));
            isModified = true;
        }
        
        // Parse the title string.
        TaskTokens taskTokens = new TaskTokens(getTitle());
        
        // Update the title. It may or may not have changed, so don't set isModified.
        setTitle(taskTokens.getDescription());
        
        // Due date in the title?
        if (taskTokens.getDueDate() != null)
        {
            // Store that we will need to include the pseudo-date override string
            // in the modifier banner when we store it in the note (later).
            setOverridingPseudoDate(true);
            
            Timestamp dueDate = Timestamp.parse(taskTokens.getDueDate());
            if (dueDate != null && !dueDate.equals(getDueDate()))
            {
                setDueDate(dueDate);
                isModified = true;
            }          
        }
        else // no due date in the title
        {
            // Detect if the task was created in Toodledo (website or app), rather than on a mobile phone.
            // If it was done in Toodledo, we can trust the value of due date.
            if (this.context > 0 || this.folder > 0 | this.status > 0)
            {
                if (duedate == 0 && isNoteContainsPseudoDateOverride())
                {
                    setOverridingPseudoDate(false);
                    isModified = true;
                }
                else if (getDueDate().isActualDate() && !isNoteContainsPseudoDateOverride())
                {
                    setOverridingPseudoDate(true);
                    isModified = true;
                }
            }
        }
        
        // Start date in the title?
        if (taskTokens.getStartDate() != null)
        {
            Timestamp startDate = Timestamp.parse(taskTokens.getStartDate());
            if (startDate != null && !startDate.equals(getStartDate()))
            {
                setStartDate(startDate);
                isModified = true;
            }          
        }
        
        // Repeat string in the title?
        String repeatToken = taskTokens.getRepeat();
        if (repeatToken != null)
        {
            if (!getRepeat().equals(repeatToken))
            {
                setRepeat(repeatToken);
                isModified = true;
            }          
        }
        
        List<String> modifiers = taskTokens.getModifiers();
        
        // Priority in title? (If we find one, don't look for any more.)
        for (String modifier : modifiers)
        {
            Priority p = Priority.valueOfIgnoreCase(modifier);
            if (p != null)
            {
                modifiers.remove(modifier);
                if (getPriority() != p)
                {
                    setPriority(p);
                    isModified = true;
                }
                break;
            }
        }
        
        // Is there a "no due date" in the title?
        if (modifiers.contains(NO_DUE_DATE_MODIFIER))
        {
            modifiers.remove(NO_DUE_DATE_MODIFIER);
            if (isOverridingPseudoDate())
            {
                setOverridingPseudoDate(false);
                isModified = true;
            }
        }
        
        /*
         * Not doing special expenses processing anymore. We are no longer logging all cash expenses.
         *
        // Expenses in title?
        if (modifiers.contains(EXPENSES_MODIFIER))
        {
            modifiers.remove(EXPENSES_MODIFIER);
            setTitle(EXPENSES_TITLE_PREFIX + getTitle());
            setFolder(availableFolders.findByName(EXPENSES_FOLDER));
            addTag(EXPENSES_TAG);
            setPriority(Priority.TOP);
            setStartDate(Timestamp.today());
            isModified = true;
        }
         */        
        
        // Starred in title?
        if (modifiers.contains(STAR_MODIFIER))
        {
            modifiers.remove(STAR_MODIFIER);
            if (!isStarred())
            {
                setStarred(true);
                isModified = true;
            }
        }
        if (modifiers.contains(NO_STAR_MODIFIER))
        {
            modifiers.remove(NO_STAR_MODIFIER);
            if (isStarred())
            {
                setStarred(false);
                isModified = true;
            }
        }
        
        // Context in title?
        if (availableContexts != null)
        {
            // Context in title? 
            // First look for "no context".
            if (modifiers.contains(NO_CONTEXT_MODIFIER))
            {
                modifiers.remove(NO_CONTEXT_MODIFIER);
                if (getContext() != null)
                {
                    setContext(null);
                    isModified = true;
                }            
            }
            else
            {
                // If we find a context, don't look for any more.
                for (String modifier : modifiers)
                {
                    Context c = availableContexts.findByNameIgnoreCaseMatchStart(modifier);
                    if (c != null)
                    {
                        modifiers.remove(modifier);
                        if (!c.equals(getContext()))
                        {
                            setContext(c);
                            isModified = true;
                        }
                        break;
                    }
                }
            }
        }
        
        // Status in title? (If we find one, don't look for any more.)
        for (String modifier : modifiers)
        {
            Status s = Status.valueOfIgnoreCase(modifier);
            if (s != null)
            {
                modifiers.remove(modifier);
                if (s != getStatus())
                {
                    setStatus(s);
                    isModified = true;
                }
                break;
            }
        }

        // If no status set, set it to the default one.
        if (getStatus() == Status.NONE)
        {
            setStatus(Status.DEFAULT);
            isModified = true;
        }
        
        // Folder in title?
        if (availableFolders != null)
        {
            // Folder in title?
            // First look for "no folder".
            if (modifiers.contains(NO_FOLDER_MODIFIER))
            {
                modifiers.remove(NO_FOLDER_MODIFIER);
                if (getFolder() != null)
                {
                    setFolder(null);
                    isModified = true;
                }            
            }
            else
            {
                // If we find a folder, don't look for any more.
                for (String modifier : modifiers)
                {
                    Context f = availableFolders.findByNameIgnoreCaseAndPrefix(modifier);
                    if (f != null)
                    {
                        modifiers.remove(modifier);
                        if (!f.equals(getFolder()))
                        {
                            setFolder(f);
                            isModified = true;
                        }
                        break;
                    }
                }
            }
        }
        
        // Add any remaining modifiers as tags.
        // First look for "no tag".
        if (modifiers.contains(NO_TAG_MODIFIER))
        {
            modifiers.remove(NO_TAG_MODIFIER);
            if (!getTags().trim().isEmpty())
            {
                clearTags();
                isModified = true;
            }            
        }
        // Add tags. (Look for tags even if there was a "no tag",
        // so that we can clear all existing and then add some.)
        for (String modifier : modifiers)
        {
            if (addTag(modifier))
            {
                isModified = true;
            }
        }
        
        /*
         * Not doing this anymore.
         *
        // If a task has a project tag (e.g. "p6 Fix Take2Task"), 
        // then make sure that it is in the matching folder (e.g. "p6 - Geek").
        for (String t : getTags().split(TAG_DELIMITER))
        {
            t = t.trim();
            
            if (t.length() > 0 && Character.isLetter(t.charAt(0)))
            {
                // Find the prefix string (e.g. "p6"), before a space character.
                int indexOfFirstSpace = t.indexOf(' ');
                if (indexOfFirstSpace >= 0)
                {
                    String prefix = t.substring(0, indexOfFirstSpace);
                    
                    // Find the folder that begins with the prefix.
                    Context prefixFolder = availableFolders.findByPrefix(prefix);
                    if (prefixFolder != null && !prefixFolder.equals(getFolder()))
                    {
                        setFolder(prefixFolder);
                        isModified = true;
                    }
                }
            }
        }
         */
        
        // If a task has a folder that starts with a "w", then it must be a work task. 
        // (Obvious, isn't it?) So make sure that it has a context of "Work".
        Context workContext = availableContexts.findByName(WORK_CONTEXT_NAME);
        if (workContext != null && getFolder() != null &&
            (getContext() == null || !getContext().equals(workContext)) &&
            getFolder().getName().length() > 0 && 
            Character.toUpperCase(getFolder().getName().charAt(0)) == Character.toUpperCase(workContext.getName().charAt(0))
           )
        {
            setContext(workContext);
            isModified = true;
        }
        // If a task has a folder that starts with a "p", then it must be a personal task. 
        // So make sure that it has a context of "Personal".
        Context personalContext = availableContexts.findByName(PERSONAL_CONTEXT_NAME);
        if (personalContext != null && getFolder() != null &&
            (getContext() == null || !getContext().equals(personalContext)) &&
            getFolder().getName().length() > 0 && 
            Character.toUpperCase(getFolder().getName().charAt(0)) == Character.toUpperCase(personalContext.getName().charAt(0))
           )
        {
            setContext(personalContext);
            isModified = true;
        }
        
        // If a work task (i.e. context of Work) doesn't have a folder, put it in the default work folder.
        if (workContext != null && getContext() != null &&
            getContext().equals(workContext) && getFolder() == null)
        {
            Context defaultWorkFolder = availableFolders.findByName(DEFAULT_WORK_FOLDER_NAME);
            if (defaultWorkFolder != null)
            {
                setFolder(defaultWorkFolder);
                isModified = true;
            }
        }
        // If a personal task (i.e. context of Personal) doesn't have a folder, put it in the default personal folder.
        if (personalContext != null && getContext() != null &&
            getContext().equals(personalContext) && getFolder() == null)
        {
            Context defaultPersonalFolder = availableFolders.findByName(DEFAULT_PERSONAL_FOLDER_NAME);
            if (defaultPersonalFolder != null)
            {
                setFolder(defaultPersonalFolder);
                isModified = true;
            }
        }

        /*
         * Nah, not doing this any more.
         * 
        // If after all that a task does not have a context, put it in the Personal context.
        if (personalContext != null && getContext() == null)
        {
            setContext(personalContext);
            isModified = true;
        }
         */
        
        /*
         * Nah, not doing this any more.
         * 
        // After all that, if a task is not in a folder, put it in the default personal folder.
        if (getFolder() == null)
        {
            Context defaultFolder = availableFolders.findByName(DEFAULT_PERSONAL_FOLDER_NAME);
            if (defaultFolder != null)
            {
                setFolder(defaultFolder);
                isModified = true;
            }            
        }
         */
        
        /*
         * OLD WAY OF DOING PROJECTS. NOT USED ANYMORE.
         * 
        // If the task is a project, set the status to the defined one for projects.
        // (Probably choosing one so that projects are at the top or bottom of the list of tasks.)
        if (isProject() && getStatus() != Status.PROJECT)
        {
            setStatus(Status.PROJECT);
            addTag(PROJECT_TAG);
            isModified = true;
        }
        if (!isProject() && getStatus() == Status.PROJECT)
        {
            setStatus(Status.DEFAULT);
            isModified = true;
        }
         */
        
        // If title starts with "Waiting", set the status to Waiting.
        // And vice-versa.
        // Ignore projects (the old way) and future tasks.
        if (!isProject() && getStatus() != Status.FUTURE)
        {
            if ((getTitle().startsWith(WAITING_SHORT_IDENTIFIER) || getTitle().startsWith(WAITING_LONG_IDENTIFIER))
                && getStatus() != Status.WAITING)
            {
                setStatus(Status.WAITING);
                isModified = true;
            }
            else if (!getTitle().startsWith(WAITING_SHORT_IDENTIFIER) && !getTitle().startsWith(WAITING_LONG_IDENTIFIER) && 
                     getStatus() == Status.WAITING)
            {
                setStatus(Status.DEFAULT);
                isModified = true;
            }
        }
        
        // If title starts with ".", set the status to Reference. This is the new way of denoting a project.
        // And vice-versa. Ignore future tasks.
        if (getStatus() != Status.FUTURE)
        {
            if (getTitle().startsWith(PROJECT_IDENTIFIER) && getStatus() != Status.REFERENCE)
            {
                setStatus(Status.REFERENCE);
                isModified = true;
            }
            else if (!getTitle().startsWith(PROJECT_IDENTIFIER) && getStatus() == Status.REFERENCE)
            {
                setStatus(Status.DEFAULT);
                isModified = true;
            }
        }
        
        // Not using sub-tasks and projects any more, but an alternate project approach:
        // anything starting with "Project: " or "x " goes into a special status.
        /*
         * 
        if (getStatus() != Status.FUTURE)
        {
            if ((getTitle().startsWith(PROJECT_SHORT_IDENTIFIER) || getTitle().startsWith(PROJECT_LONG_IDENTIFIER))
                && getStatus() != Status.PROJECT)
            {
                setStatus(Status.PROJECT);
                isModified = true;
            }
            else if (!getTitle().startsWith(PROJECT_SHORT_IDENTIFIER) && !getTitle().startsWith(PROJECT_LONG_IDENTIFIER) && 
                     getStatus() == Status.PROJECT)
            {
                setStatus(Status.DEFAULT);
                isModified = true;
            }
        }
         */
        
        // Set pseudo-date.
        if (modifyPseudoDate())
        {
            isModified = true;
        }
        
        /*
        // Future task?
        if (modifyFutureTask())
        {
            isModified = true;
        }
         */
        
        // Update the modifier banner in the note field (if it has changed).
        if (modifyBannerInNote())
        {
            isModified = true;
        }
        
        // Make sure that the note does not get too long to send to Toodledo.
        // The note of a task imported from an email can be very long.
        limitNoteLength();
        
        return isModified;
    }
    
    // ---------- Private helper methods ----------
    
    /**
     * Determine the correct pseudo-date for this task,
     * and set the due date to that pseudo-date.
     *  
     * @return true if we modified the task, otherwise false.
     */
    private boolean modifyPseudoDate()
    {
        // Look for the pseudo-date override modifier in the note.
        // Also don't do anything if the task has been archived.
        if (isOverridingPseudoDate() || getDueDate().isArchived())
        {
            return false;
        }
        
        // Determine the pseudo-date.
        Timestamp pseudoDate = Timestamp.getPseudoDate(getContext(), getStatus());

        // Set the task's due date to be the pseudo-date, if it isn't already set to it.
        if (pseudoDate.equals(getDueDate()))
        {
            return false;
        }
        else
        {
            setDueDate(pseudoDate);
            return true;
        }
    }
    
    /**
     * Return whether or not this task is overriding using a pseudo-date for due date.
     * 
     * @return true if overriding pseudo-date, otherwise false.
     */
    private boolean isOverridingPseudoDate()
    {
        return !isRemovingDueDate && (isOverridingPseudoDate || isNoteContainsPseudoDateOverride());
    }
    
    /**
     * Set whether or not this task is overriding the pseudo-date.
     */
    private void setOverridingPseudoDate(boolean isOverridingPseudoDate)
    {
        this.isOverridingPseudoDate = isOverridingPseudoDate;
        this.isRemovingDueDate = !isOverridingPseudoDate;
    }
    
    /**
     * Return whether the task note has the pseudo-date override characters.
     */
    private boolean isNoteContainsPseudoDateOverride()
    {
        return getNote().contains(MODIFIER_BANNER_START + TaskTokens.DUE_DATE_DELIMITER);
    }
    
    /**
     * Return the modifier banner corresponding to the current values of this task.
     * (The modifier banner is displayed in the notes field of the task.)
     * Return an empty string if there isn't any content to put in a banner.
     */
    private String getRequiredModifierBanner()
    {
        StringBuffer banner = new StringBuffer(MODIFIER_BANNER_START);
        
        if (isOverridingPseudoDate())
        {
            banner.append(TaskTokens.DUE_DATE_DELIMITER);
        }
        
        /**
         * No longer storing context in the note. No use case for it.
         * 
        if (getContext() != null)
        {
            // If we already have some content in the banner, add a blank character
            // before adding the context name.
            if (banner.length() > MODIFIER_BANNER_START.length())
            {
                banner.append(' ');
            }
            banner.append(TaskTokens.DELIMITER);
            banner.append(getContext().getName());             
        }
         */
        
        /**
         * No longer storing folder in the note. No use case for it.
         * 
        if (getFolder() != null)
        {
            // If we already have some content in the banner, add a blank character
            // before adding the folder name.
            if (banner.length() > MODIFIER_BANNER_START.length())
            {
                banner.append(' ');
            }
            banner.append(TaskTokens.DELIMITER);
            banner.append(getFolder().getName());             
        }
         */
        
        if (getTags() != null)
        {
            for (String t : getTags().split(TAG_DELIMITER))
            {
                t = t.trim();
                
                // Skip over blank tags.
                if (t.isEmpty())
                {
                    continue;
                }
                
                /*
                 * Not doing this anymore.
                 *
                // Skip over any project tags. These start with a letter.
                // Special tags that we are interested in, such as "_errands",
                // don't start with a letter.
                if (Character.isLetter(t.charAt(0)))
                {
                    continue;
                }
                
                // Remove the first character from the tag before adding it.
                // We do this to get rid of, say, the underscore at the start of "_errands".
                t = t.substring(1);
                 */

                // If we already have some content in the banner, add a blank character
                // before adding the tag.
                if (banner.length() > MODIFIER_BANNER_START.length())
                {
                    banner.append(' ');
                }
                banner.append(TaskTokens.DELIMITER);
                
                banner.append(t); 
            }
        }

        // If we haven't added anything to the banner string, return a blank string.
        // Otherwise return the banner string.
        return (banner.toString().equals(MODIFIER_BANNER_START)) ? "" : banner.toString();
    }

    
    /**
     * Update the modifier banner in the note field, if it has changed.
     * 
     * @return true if we modify the notes field, otherwise false.
     */
    private boolean modifyBannerInNote()
    {
        final String LINE_SEPARATOR = System.getProperty("line.separator");
        final String BANNER_START_SEQUENCE = MODIFIER_BANNER_START + TaskTokens.DELIMITER;
        
        // What should the banner be?
        String requiredBanner = getRequiredModifierBanner();
        
        // Is there currently a modifier banner in the note, and if so, where?
        int bannerIndex = getNote().indexOf(BANNER_START_SEQUENCE);
        
        if (bannerIndex == -1) // no banner currently in the note
        {
            if (requiredBanner.isEmpty())
            {
                // No current banner, and one is not needed, so no change.
                return false;
            }
            else
            {
                prependBannerToNote(requiredBanner);
                return true;
            }
        }
        else // there currently is a banner in the note
        {
            String[] lines = getNote().split("[\r\n]");
            assert lines.length > 0 : 
                   "There is a banner in the note, so there should be at least one line in the note, however there were no lines.";
            
            // If the banner is at the start of the comment, and it matches the required banner,
            // then we have nothing to do.
            if (bannerIndex == 0 && lines[0].equals(requiredBanner))
            {
                return false;
            }
            
            // Build up a new note, starting with the required banner.
            StringBuffer newNote = new StringBuffer(requiredBanner);
            
            // Add all the lines to the new note, except if the line is the old banner.
            for (String line : lines)
            {
                if (line.trim().startsWith(BANNER_START_SEQUENCE)) // the old banner
                {
                    continue;
                }
                
                if (newNote.length() > 0)
                {
                    newNote.append(LINE_SEPARATOR);
                }
                newNote.append(line);
            }
            
            setNote(newNote.toString());
            return true;
         }
    }

    /**
     * Prepend the given banner string to the note field.
     * IMPORTANT: this method assumes that any existing banner has already been removed.
     * 
     */
    private void prependBannerToNote(String banner)
    {
        if (getNote().isEmpty())
        {
            setNote(banner);
        }
        else 
        {
            // Prepend the banner and a newline character to the existing content of the note.            
            setNote(String.format("%s%n%s", banner, getNote()));
        }
    }
    
    /**
     * Archive future tasks to a very distant date. 
     * Move back archived future tasks starting today.
     * 
     * @return true if we modified the task, otherwise false.
     */
    /*
    private boolean modifyFutureTask()
    {
        Timestamp startDate = getStartDate();
        
        // If the start date is earlier than today or earlier, then we don't need to do anything.
        if (startDate.getValue() <= Timestamp.today().getValue())
        {
            return false;
        }
        
        // If the start date has been archived but is due today, move it back to today.
        if (startDate.isTodayOrRecentButArchived())
        {
            setStartDate(startDate.unarchive());
            setDueDate(getDueDate().unarchive());
            setStatus((getTitle().startsWith(WAITING_SHORT_IDENTIFIER) || getTitle().startsWith(WAITING_LONG_IDENTIFIER)) 
                      ? Status.WAITING : Status.DEFAULT);
            setStatus((getTitle().startsWith(PROJECT_SHORT_IDENTIFIER) || getTitle().startsWith(PROJECT_LONG_IDENTIFIER)) 
                      ? Status.PROJECT : Status.DEFAULT);
            return true;
        }
        
        // If the start date has not been archived, archive it (as we know it is a future task).
        else if (!startDate.isArchived())
        {
            setStartDate(startDate.archive());
            setDueDate(getDueDate().archive());
            setStatus(Status.FUTURE);
            return true;
        }
        
        return false;
    }
     */
}
