import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * TictactoeResponse will represent a response from any side of the 
 * communication (server or client)
 * It will contain the command of the response if there is one
 * and the data of the response
 * and all the lines of the response if there is any 
*/
public class TictactoeResponse {
    // Command will contain the command of the response if there is one
    private String command = "";

    // Data will contain of part of a the first line of the response
    private final ArrayList<String> params = new ArrayList<>();

    // Regex to validate the command if it matches the protocol
    private final Pattern command_regex = Pattern.compile("^(\\s*[A-Za-z]+)(\\s+[A-Za-z]{2,})?(\\s+[A-Za-z]{2,})?(\\s+[A-Za-z]|\\s+\\-?[0-9]+)?(\\s+[A-Za-z]|\\s+\\-?[0-9]+)?\\s*\\r\\n(\\r\\n)?$");

    // Regex to validate the puzzle if it matches the protocol
    private final Pattern puzzle_regex = Pattern.compile("^([XxOo\\s]{3}\\r\\n){3}(([XxOo] WON|DRAW|OPPONENT QUIT)\\r\\n)?(\\r\\n)?$");

    /**
     * Lines will contain all the lines of the response each
     * line will an TicactoeResponseLine object
    */
    private ArrayList<char[]> lines = new ArrayList<>();

    public TictactoeResponse(String data) throws TictactoeBadResponseException {
        Matcher command_matcher = this.command_regex.matcher(data);
        Matcher puzzle_matcher = this.puzzle_regex.matcher(data);

        if (puzzle_matcher.find()) {
            this.command = "PUZZLE";

            /**
             * If the data is a puzzle we split the data by lines
             * and we take the first 3 lines as the lines of the puzzle
             * and the rest as the params of the puzzle
            */
            int line_count = 0;

            for (String part : data.split("\r\n")) {
                if (line_count < 3) {
                   lines.add(part.toCharArray());
                } else {
                    for (String item : part.trim().split("\\s+")) {
                        params.add(item.trim());
                    }
                }

                line_count++;
            }
        } else if (command_matcher.find()) {
            /**
             * If the data is a single line command
             * we split the data by spaces and we take
             * the first part as the command and the rest
             * as the params of the command
            */
            StringBuilder builder = new StringBuilder();

            if (command_matcher.groupCount() != 6) {
                throw new TictactoeBadResponseException();
            }

            for(int command_index: new int[]{1,2,3,4,5}) {
                String group = command_matcher.group(command_index);
                if (group != null) {
                    if (command_index > 3) {
                        params.add(group.trim());
                    } else {
                        builder.append(group.trim()).append(" ");
                    }
                }
            }

            this.command = builder.toString().trim();
        } else {
            throw new TictactoeBadResponseException();
        }
    }

    /*
     * Getters return the main command
     * of the response in uppercase
    */
    public String getCommand() {
        return this.command.toUpperCase();
    }

    /*
     * Getters return the number of params
     * of the response
    */
    public int getLength() {
        return this.params.size();
    }

    /*
     * Getters return the param at the given index
     * or null if the index is out of bounds
    */
    public String get(int index) {
        if (index < 0 || index >= this.params.size()) {
            return null;
        }

        return this.params.get(index);
    }

    /**
     * Getters return all the lines of the puzzle
     * @return
    */
    public ArrayList<char[]> getLines() {
        return this.lines;
    }

    /*
      * Getters return the line at the given index
      * or null if the index is out of bounds
    */
    public char[] getLine(int index) {
        if (index < 0 || index >= this.lines.size()) {
            return null;
        }

        return this.lines.get(index);
    }

    /*
     * Getters return the number of lines of the puzzle
    */
    public int getLinesCount() {
        return this.lines.size();
    }
}
