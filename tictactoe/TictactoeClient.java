import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * TictactoeClient represent we want to be connected  to a Tictactoe server
 * using his terminal, it can start a game
 * against another player or againt a bot
*/
public class TictactoeClient extends Client {
    /**
     * equal to true if there is 
     * a previous request 
    */
    private boolean isLastResponse = false;

    public TictactoeClient(Socket client) throws ClientGetStreamException, TictactoeTimeoutException {
        super(client);
    }

    /**
     * Receive a message from the input stream of the client
     * is a just a super method to be better understood by myself
     * when i'm coding the TictactoeClient class
     * @return
     * @throws ClientReceiveException
    */
    protected String receiveMessageFromServer() throws ClientReceiveException {
        return this.receiveMessage();
    }

    /**
     * Send a message to the server using the output stream of the client
     * in the same wy as the receiveMessageFromServer method
     * @param message
     * @throws ClientSendException
    */
    private void sendRequest(String request) throws ClientSendException {
        this.sendMessage(request);
    }

    
    /**
     * Build a request to send to the server
     * @param command
     * @param args
     * @return
    */
    private String buildRequest(String command, String... args) {
        StringBuilder request = new StringBuilder(command);

        for (String arg : args) {
            request.append(" ").append(arg);
        }

        request.append(TictactoeConst.END_OF_MESSAGE).append(TictactoeConst.END_OF_MESSAGE);

        return request.toString();
    }

    /**
     * send a request to start the bot
     * @return
     */
    public boolean startBot() {
        try {
            // We send a request to the server to start a bot game
            String request = this.buildRequest("START BOT", this.type.toString());
            this.sendRequest(request);
        } catch (ClientSendException e) {
            return false;
        } 

        return true;
    }

    /**
     * send a request to play against
     * another connected player
     * @return
     */
    public boolean startPlayer() {
        try {
            // We send a request to the server to start a player game
            String request = this.buildRequest("START PLAYER");
            this.sendRequest(request);
        } catch (ClientSendException e) {
            return false;
        } 

        return true;
    }

    /**
     * send a request to add a key
     * in the grid
     * @param x
     * @param y
     * @return
     */
    public boolean put(int x, int y) {
        try {
            // We send a request to the server to put a mark on the board
            String request = this.buildRequest("PUT", Integer.toString(x), Integer.toString(y));
            this.sendRequest(request);
        } catch (ClientSendException e) {
            return false;
        } 

        return true;
    }

    /**
     * Send a request to get an update of the
     * current game
     * @return
     */
    public boolean update() {
        try {
            // We send a request to the server to update the board
            String request = this.buildRequest("UPDATE");
            this.sendRequest(request);
        } catch (ClientSendException e) {
            return false;
        } 

        return true;
    }

    /**
     * send a request to quit
     * the game
    */
    @Override
    public void quit() {
        try {
            // We send a request to the server to quit the game
            String request = this.buildRequest("QUIT");
            this.sendRequest(request);
        } catch (ClientSendException e) {
            // the connection is probably already closed
        } finally {
            super.quit();
            System.exit(0);
        }
    }

    /**
     * return true if there is a response
     * @return
     */
    public boolean hasLastResponse() {
        return this.isLastResponse;
    }

    /**
     * mark we have get a response
     * @return
     */
    public void addLastResponse() {
        this.isLastResponse = true;
    }

    /**
     * clear the last update
     */
    public void clearLastResponse() {
        this.isLastResponse = false;
    }

    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        String readedLine;
        int mode = 0;
        boolean canShowMenu = true;

        try {
            Socket clientSocket = new Socket(TictactoeConst.HOST, TictactoeConst.PORT);

            /**
             * We will repeaetly ask the user to choose a mode
             * until he chooses a valid mode (1 or 2)
             * 1. Play against a bot
             * 2. Play against another player 
            */

            do {
                System.out.println("Choose a mode:");
                System.out.println("1. Play against a bot");
                System.out.println("2. Play against another player");
                System.out.print(">>> ");
                readedLine = scanner.nextLine();

                try {
                    mode = Integer.parseInt(readedLine.trim());
                } catch (NumberFormatException e) {
                    mode = 0;
                } finally {
                    // to put a spcace between the lines of each step of the program
                    System.out.println("");
                }
            } while (mode < 1 || mode > 2);


            try {
                /**
                 * If the user chose to play against a bot
                 * we will ask him to choose a symbol (X or O)
                 * until he chooses a valid symbol
                */

                if (mode == 1) {
                    do {
                        System.out.println("Choose your symbol X or O:");
                        System.out.print(">>> ");
                        readedLine = scanner.nextLine();
                    } while (ClientType.fromString(readedLine.trim().toUpperCase()) == null);

                    // to put a spcace between the lines of each step of the program
                    System.out.println("");
                } 

                TictactoeClient client = new TictactoeClient(clientSocket);

                client.start();

                /**
                 * If the user chose to play against another player, 
                 * we have to wait for the server to tell us our symbol
                 * so we will listen for messages from the server
                 * and when we receive the message with our symbol
                 * we will set the client type and stop listening
                 * then we will start the game
                 */
                if (mode == 2) {
                    System.out.println("");
                    synchronized(client) {
                        client.listening((message) -> {
                            TictactoeResponse response = new TictactoeResponse(message);
                            if (response.getCommand().equals("PLAYER")) {
                                if (response.getLength() == 1) {
                                    String symbol = response.get(0);
                                    if (ClientType.fromString(symbol) != null) {
                                        synchronized(client) {
                                            client.setClientType(ClientType.fromString(symbol));
                                            client.notifyAll();
                                            client.addLastResponse();
                                            return;
                                        }
                                    }
                                }
                            }
                            throw new TictactoeBadResponseException();
                        });

                        // if we fail to start a player game it mean that
                        // we are not connected to the server anymore
                        if (!client.startPlayer()) {
                            client.quit();
                        }

                        client.wait(TictactoeConst.RESPONSE_TIMEOUT);

                        if (!client.hasLastResponse()) {
                            throw new TictactoeTimeoutException();
                        }
                    
                        client.clearLastResponse();
                        client.removeListener();
                    }
                } else {
                    /*
                     * If the user chose to play against a bot
                     * we can set the client type directly
                     * without waiting for the server to tell us
                     * because we already asked the user to choose a symbol
                     * and we can send the symbol to the server when we start the game
                    */
                    synchronized(client) {
                        client.listening((message) -> {
                            TictactoeResponse response = new TictactoeResponse(message);
                            if (response.getCommand().equals("PUZZLE")) {
                                printGrid(response, client.getType());

                                synchronized(client) {
                                    client.notifyAll();
                                    client.addLastResponse();
                                    return;
                                }
                            }

                            throw new TictactoeBadResponseException();
                        });

                        client.setClientType(ClientType.fromString(readedLine));

                        // if we fail to start a bot game it mean that
                        // we are not connected to the server anymore
                        if (!client.startBot()) {
                            client.quit();
                        }

                        /**
                         * if we are in a bot game and we are 'O'
                         * we have to wait for the bot to play
                         * before showing the menu
                        */
                        client.wait(TictactoeConst.RESPONSE_TIMEOUT);

                        if (!client.hasLastResponse()) {
                            throw new TictactoeTimeoutException();
                        }
                    
                        client.clearLastResponse();
                        client.removeListener();
                    }
                }

                /**
                 * now you can to the client which
                 * operation he want to do on the server
                 * 1. Put his symbol on the grid
                 * 2. Update the grid
                 * 3. Quit the game
                */
                
                do {
                    System.out.println("Place Your symbol on the grid: 'row' 'col'");
                    System.out.println("Update of the grid: u");
                    System.out.println("Quit: q");
                    System.out.print(">>> ");
                    readedLine = scanner.nextLine().trim().toLowerCase();

                    System.out.println("");

                    String[] parts = readedLine.split("\\s+");
                    switch (parts.length) {
                        case 1:
                            if (readedLine.equals("u")) {
                                /**
                                 * we send a resquest to the server to have a update
                                 * of the grid and we have to listen for the response
                                 * from the server to know the state of the game
                                 * if the response is not valid we throw a ClientBadResponseException
                                 * to stop the game 
                                */

                                synchronized(client) {
                                    client.listening((message) -> {
                                        TictactoeResponse response = new TictactoeResponse(message);
                                        if (response.getCommand().equals("PUZZLE")) {
                                            printGrid(response, client.getType());
                                            synchronized(client) {
                                                client.notifyAll();
                                                client.addLastResponse();
                                                return;
                                            }
                                        } 

                                        throw new TictactoeBadResponseException();
                                    });

                                    //if we fail to send the update request it mean that
                                    // we are not connected to the server anymore
                                    if(!client.update()) {
                                        client.quit();
                                    }

                                    client.wait(TictactoeConst.RESPONSE_TIMEOUT);

                                    if (!client.hasLastResponse()) {
                                        throw new TictactoeTimeoutException();
                                    }

                                    client.clearLastResponse();
                                    client.removeListener();
                                }
                            } else if (readedLine.equals("q")) {
                                /**
                                 * we quit the game
                                 * and we close the connection
                                */
                                canShowMenu = false;
                                scanner.close();
                                client.quit();
                            } else {
                                continue;
                            }
                            
                            break;
                        
                        case 2:
                            /**
                             * we send a request to the server to put our symbol on the grid
                             * at the position (row, col)
                             * and we have to listen for the response from the server
                             * to know the state of the game
                             * if the response is not valid we throw a ClientBadResponseException
                             * to stop the game
                            */
                            int row = 0;
                            int col = 0;

                            try {
                                row = Integer.parseInt(parts[0]);
                                col = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            
                            synchronized(client) {
                                client.listening((message) -> {
                                    TictactoeResponse response = new TictactoeResponse(message);
                                    
                                    /**
                                     * if the response is a PUZZLE we print the grid
                                     * otherwise we just print the command
                                     * this is to handle the case when the game is over
                                     * and the server send us a WIN, LOSE or DRAW command
                                     * so we can show the user the final state of the grid
                                     * before showing him the result of the game
                                    */
                                    if (response.getCommand().equals("PUZZLE")) {
                                        printGrid(response, client.getType());
                                    } else {
                                        System.out.println(response.getCommand());
                                    }

                                    System.out.println("");

                                    synchronized(client) {
                                        client.notifyAll();
                                        client.addLastResponse();
                                        return;
                                    }
                                });

                                if (!client.put(row, col)) {
                                    client.quit();
                                }

                                client.wait(TictactoeConst.RESPONSE_TIMEOUT);

                                if (!client.hasLastResponse()) {
                                    throw new TictactoeTimeoutException();
                                }

                                client.clearLastResponse();
                                client.removeListener();
                            }
                            break;
                    
                        default:
                            break;
                    }
                } while (canShowMenu);

            } catch (ClientGetStreamException e) {
                // if we fail to get the input/output stream of the client
                // that should never happen
                System.exit(1);
            } catch (IllegalThreadStateException e) {
                // if we try to start a thread that is already started
                // that should never happen
                System.exit(2);
            } catch (TictactoeBadResponseException e) {
                // if we receive a bad response from the server
                // we exit the program
                // that mean that the server is not working properly
                // Because "WRONG" is not considered as a bad response
                System.exit(3);
            } catch (InterruptedException e) {
                // if the thread is interrupted
                // that should never happen
                System.exit(4);
            } catch( TictactoeTimeoutException e) {
                // if we fail to set the timeout of the socket
                // That mean that the server is not responding
                System.exit(5);
            }
        } catch (IOException e) {
            // if we fail to connect to the server
            // we print an error message and exit the program
            System.exit(6);
        } catch(IllegalArgumentException e) {
            // this should never happen
            // Because it happen only if the port is invalid
            System.exit(7);
        }

        scanner.close();
        System.exit(0);
    }

    /**
     * When we receive a message from the server and the message
     * is a grid of the game we print the grid
     * @param response
     * @param type
    */
    private synchronized static void printGrid(TictactoeResponse response, ClientType type) {
        if (response.getCommand().equals("PUZZLE")) {
            System.out.println("   | 0 | 1 | 2 |");
            System.out.println("-------------------");
            /*
             * We print the grid
             * we know that the grid is always 3x3
             * so we can use a for loop to print the grid
             * we use a switch case to print the symbol
             * if the symbol is ' ' we print an empty space
             * otherwise we print the symbol
            */
            for (char counter : new char[]{0, 1, 2}) {
                char[] lines = response.getLine(counter);
                System.out.print(String.format(" %d |", (int)counter));
                for (char line: lines) {
                    switch (line) {
                        case ' ':
                            System.out.print(" - |");
                            break;
                        
                        default:
                            System.out.print(String.format(" %c |", line));
                            break;
                    }
                }
                System.out.println("\n-------------------");
            }

            int counter = 0;
            /*
             * We print the state of the game
             * we know that the state of the game is always
             * the last line of the response
             * so we can use a while loop to print the state
            */
            while (counter < response.getLength())
                System.out.print(response.get(counter++) + " ");

            System.out.println("");
            System.out.println(">> Your symbol is " + type.toString());
            System.out.println(">> '-' represent an empty space");
            System.out.println("");
        }
    } 
}