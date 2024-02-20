package duke.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import duke.commands.AddCommand;
import duke.commands.AddDeadlineCommand;
import duke.commands.AddEventCommand;
import duke.commands.AddTodoCommand;
import duke.commands.Command;
import duke.commands.DeleteCommand;
import duke.commands.ExitCommand;
import duke.commands.FindCommand;
import duke.commands.ListCommand;
import duke.commands.MarkCommand;
import duke.commands.UnmarkCommand;
import duke.exceptions.InvalidCommandException;
import duke.parserenums.CommandWord;
import duke.parserenums.TaskType;
import duke.tasks.TaskList;

/**
 * This class represents a parser that reads user input and converts it into Commands
 */
public class Parser {

    private static final int VISUAL_INDEX_OFFSET = 1;

    /**
     * Takes in user input and parses it into a Command that can be executed.
     * TaskList-specific Commands will be executed on the given TaskList.
     *
     * @param userInput The String containing the user input.
     * @param taskList  The TaskList that certain Commands will be executed on.
     * @return The Command generated by parsing the user input.
     * @throws InvalidCommandException if the user input is not a valid command.
     */
    public static Command parseUserInput(String userInput, TaskList taskList) throws InvalidCommandException {

        //Check first word
        String[] commandAndRemaining = userInput.strip().split(" ", 2);
        String commandUpper = commandAndRemaining[0].toUpperCase();
        Command cmd;

        CommandWord commandWord = findInEnum(CommandWord.class, commandUpper);
        if (commandWord == null) {
            throw new InvalidCommandException();
        }
        switch (commandWord) {
        case ADD:
            try {
                cmd = parseAdd(commandAndRemaining[1].strip(), taskList);
                break;
            } catch (IndexOutOfBoundsException e) {
                throw new InvalidCommandException(AddCommand.getUsage());
            }
        case LIST:
            cmd = new ListCommand(taskList);
            break;
        case MARK:
            try {
                cmd = parseMark(commandAndRemaining[1].strip(), taskList);
                break;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new InvalidCommandException(MarkCommand.getUsage());
            }
        case UNMARK:
            try {
                cmd = parseUnmark(commandAndRemaining[1].strip(), taskList);
                break;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new InvalidCommandException(UnmarkCommand.getUsage());
            }
        case DELETE:
            try {
                cmd = parseDelete(commandAndRemaining[1].strip(), taskList);
                break;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new InvalidCommandException(DeleteCommand.getUsage());
            }
        case END:
            cmd = new ExitCommand();
            break;
        case FIND:
            try {
                cmd = new FindCommand(taskList, commandAndRemaining[1]);
            } catch (IndexOutOfBoundsException e) {
                throw new InvalidCommandException(FindCommand.getUsage());
            }
            break;
        default:
            throw new InvalidCommandException();
        }
        return cmd;

    }

    /**
     * Parses the following portion of user input after the "add" keyword.
     *
     * @param inputWithoutAdd The portion of user input containing details of the task to be added.
     * @param taskList        The TaskList to add the new Task to.
     * @return The AddCommand which adds the new Task to the TaskList.
     * @throws InvalidCommandException if the user input is not a valid AddCommand.
     */
    protected static AddCommand parseAdd(String inputWithoutAdd, TaskList taskList) throws InvalidCommandException {

        String[] typeAndRemaining = inputWithoutAdd.split(" ", 2);
        String typeUpper = typeAndRemaining[0].toUpperCase();
        TaskType taskType = findInEnum(TaskType.class, typeUpper);
        if (taskType == null) {
            throw new InvalidCommandException(AddCommand.getUsage());
        }

        switch (taskType) {
        case TODO:
            return parseAddTodo(taskList, typeAndRemaining);
        case DEADLINE:
            return parseAddDeadline(taskList, typeAndRemaining);
        case EVENT:
            return parseAddEvent(taskList, typeAndRemaining);
        default:
            throw new InvalidCommandException(AddCommand.getUsage());
        }
    }

    private static AddEventCommand parseAddEvent(TaskList taskList, String[] typeAndRemaining)
            throws InvalidCommandException {
        try {
            String[] descAndRemaining = typeAndRemaining[1].split("/from", 2);
            String[] fromAndTo = descAndRemaining[1].split("/to", 2);
            String[] fromdateAndFromtime = fromAndTo[0].strip().split(" ", 2);
            String[] todateAndTotime = fromAndTo[1].strip().split(" ", 2);

            String eventDesc = descAndRemaining[0].strip();
            if (eventDesc.isEmpty()) {
                throw new InvalidCommandException(AddEventCommand.getUsage());
            }
            LocalDate fromDate = LocalDate.parse(fromdateAndFromtime[0]);
            LocalTime fromTime = null;
            if (fromdateAndFromtime.length == 2) {
                fromTime = LocalTime.parse(fromdateAndFromtime[1].strip());
            }
            LocalDate toDate = LocalDate.parse(todateAndTotime[0]);
            LocalTime toTime = null;
            if (todateAndTotime.length == 2) {
                toTime = LocalTime.parse(todateAndTotime[1].strip());
            }
            return new AddEventCommand(taskList, eventDesc, fromDate, fromTime, toDate, toTime);
        } catch (ArrayIndexOutOfBoundsException | DateTimeParseException e) {
            throw new InvalidCommandException(AddEventCommand.getUsage());
        }
    }

    private static AddDeadlineCommand parseAddDeadline(TaskList taskList, String[] typeAndRemaining)
            throws InvalidCommandException {
        try {
            String[] descAndBy = typeAndRemaining[1].split("/by", 2);
            String[] bydateAndBytime = descAndBy[1].strip().split(" ", 2);
            LocalDate byDate = LocalDate.parse(bydateAndBytime[0]);
            LocalTime byTime = null;
            if (bydateAndBytime.length == 2) {
                byTime = LocalTime.parse(bydateAndBytime[1].strip());
            }
            return new AddDeadlineCommand(taskList, descAndBy[0], byDate, byTime);
        } catch (ArrayIndexOutOfBoundsException | DateTimeParseException e) {
            throw new InvalidCommandException(AddDeadlineCommand.getUsage());
        }
    }

    private static AddTodoCommand parseAddTodo(TaskList taskList, String[] typeAndRemaining)
            throws InvalidCommandException {
        try {
            String todoDesc = typeAndRemaining[1].strip();
            return new AddTodoCommand(taskList, todoDesc);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidCommandException(AddTodoCommand.getUsage());
        }
    }

    /**
     * Parses the following portion of user input after the "mark" keyword.
     *
     * @param inputWithoutMark The portion of user input containing the task number to be marked as done.
     * @param taskList         The TaskList whom Task will be marked as done.
     * @return The MarkCommand which marks the specified Task in the given TaskList as done.
     */
    protected static MarkCommand parseMark(String inputWithoutMark, TaskList taskList) {
        int index = Integer.parseInt(inputWithoutMark) - VISUAL_INDEX_OFFSET;
        return new MarkCommand(taskList, index);
    }

    /**
     * Parses the following portion of user input after the "unmark" keyword.
     *
     * @param inputWithoutUnmark The portion of user input containing the task number to be marked as not done.
     * @param taskList           The TaskList whom Task will be marked as not done.
     * @return The UnmarkCommand which marks the specified Task in the given TaskList as not done.
     */
    protected static UnmarkCommand parseUnmark(String inputWithoutUnmark, TaskList taskList) {
        int index = Integer.parseInt(inputWithoutUnmark) - VISUAL_INDEX_OFFSET;
        return new UnmarkCommand(taskList, index);
    }

    /**
     * Parses the following portion of user input after the "delete" keyword.
     *
     * @param inputWithoutDelete The portion of user input containing the task number to be deleted.
     * @param taskList           The TaskList whom Task will be deleted from.
     * @return The DeleteCommand which deletes the specified Task from the given TaskList.
     */
    protected static DeleteCommand parseDelete(String inputWithoutDelete, TaskList taskList) {
        int index = Integer.parseInt(inputWithoutDelete) - VISUAL_INDEX_OFFSET;
        return new DeleteCommand(taskList, index);
    }

    private static <T extends Enum<T>> T findInEnum(Class<T> enumClass, String userKeyword) {
        T[] enumElements = enumClass.getEnumConstants();
        assert enumElements != null;
        for (T element : enumElements) {
            if (element.toString().contains(userKeyword)) {
                return element;
            }
        }
        return null;
    }
}
