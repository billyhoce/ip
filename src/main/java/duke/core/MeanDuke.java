package duke.core;

import duke.commands.Command;
import duke.exceptions.MeanDukeException;
import duke.tasks.TaskList;

/**
 * This class represents a MEAN chatbot with task-tracking capabilities
 */
public class MeanDuke {
    //Creates an empty task list
    static TaskList tasklist = new TaskList();

    public static void main(String[] args) {

        //Try to load Task List from hard disk. If missing or corrupted, create a new file
        tasklist = Storage.load();

        //Prints intro
        Ui.printIntro();

        //Repeatedly take in input and processes it until "end" command is given
        boolean isExit = false;
        while (!isExit) {
            String userInput = Ui.readInput();
            Ui.printSpacer();
            try {
                Command cmd = Parser.parseUserInput(userInput, tasklist);
                cmd.execute();
                Storage.save(tasklist);
                isExit = cmd.isExitCommand();
            } catch (MeanDukeException e) {
                Ui.printError(e);
            }
            Ui.printSpacer();
        }
    }
}