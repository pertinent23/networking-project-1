import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class TictactoeServer {
    //Server socket
    private ServerSocket server;

    //List of connected clients
    private ArrayList<ServerClient> clients = new ArrayList<>();

    /**
     * List of game, a game can be a game between two player or againts the bot
     * Bot Game are always added at the biggining
     * Dual Player Game are always added at the end
     */
    private LinkedList<ServerGame> games = new LinkedList<>();

    public TictactoeServer() throws Exception {
        this.server = new ServerSocket(TictactoeConst.PORT);
    }

    /**
     * when a client want to play vs another client
     * we have to search amount are avaible game if there is a place
     * @return
     */
    private ServerGame searchAvailablePlace() {
        for (ServerGame game : games) {
            if (!game.hasBot() && game.getPlayerCount() < 2) {
                return game;
            }
        }

        return null;
    }

    /**
     * Disconnect a client from the server
     * @param client
    */
    private void disconnectClient(ServerClient client) {
        client.quit();

        if (client.hasGame()) {
            client.getGame().abortGame();
        } 

        client.removeListener();

        synchronized(clients) {
            clients.remove(client);
        }


        System.out.println(">>> Disconnected Client");
    }

    private void initClient(ServerClient client) throws TictactoeBadResponseException {
        client.listening((message) -> {
            TictactoeResponse response = new TictactoeResponse(message);
            switch (response.getCommand()) {
                /**
                 * The first case is when the client want to play vs the bot
                 * we create a new game and set the client as player X or O
                 * then we send the grid state to the client if he is player O
                 * else we wait for the player O to connect
                */
                case "START BOT":
                    if (response.getLength() == 1) {
                        ClientType type = ClientType.fromString(response.get(0));
                        if (type != null) {
                            ServerGame game = new ServerGame();
                    
                            game.setAsBotgame();
                            client.setGame(game);
                            client.setClientType(type);
                            try {
                                if (type.equals(ClientType.X)) {
                                    game.setPlayerX(client);
                                } else {
                                    game.setPlayerO(client);
                                }

                                client.sendMessageToClient(game.getGridState().concat(TictactoeConst.END_OF_MESSAGE));
                                
                                synchronized(games) {
                                    games.add(game);
                                    games.notifyAll();
                                    System.out.println(">>> Bot Started");
                                }

                                return;
                            } 
                            catch (ServerGameException e) {
                                // This should not happen if the code is correct
                                // So it is not handled further
                                System.err.println("[ServerGameException in START BOT]");
                                return;
                            }
                            catch(ClientSendException e) {
                                // This mean the client disconnected
                                // We just stop the client
                                disconnectClient(client);
                                return;
                            }
                        }
                    }

                    throw new TictactoeBadResponseException();
                
                /**
                 * The second case is when the client want to play vs another player
                 * we search for an avaible game if there is one we set the client as player O
                 * else we create a new game and set the client as player X
                 * then we send the player type to the client
                 * if he is player O we send the grid state to the client
                 * else we wait for the player O to connect
                */
                case "START PLAYER":
                    ServerGame game = searchAvailablePlace();
                    StringBuilder resp = new StringBuilder("PLAYER ");

                    /**
                     * we check if the response is correct
                     * it should be just "START PLAYER" with no argument
                     * else we throw a bad response exception
                    */
                    if (response.getLength() != 0) {
                        throw new TictactoeBadResponseException();
                    }

                    try {
                        if (game != null) {
                            client.setClientType(ClientType.O);
                            game.setPlayerO(client);
                            resp.append(ClientType.O.toString());
                        } else {
                            game = new ServerGame();
                            client.setClientType(ClientType.X);
                            game.setPlayerX(client);
                            resp.append(ClientType.X.toString());
                        }

                        resp.append(TictactoeConst.END_OF_MESSAGE);
                        resp.append(TictactoeConst.END_OF_MESSAGE);
                        
                        synchronized(games) {
                            games.add(game);
                            client.setGame(game);
                            games.notifyAll();
                            client.sendMessageToClient(resp.toString());
                            System.out.println(">>> A new player joined a game");
                        }
                        return;
                    } 
                    catch (ServerGameException e) {
                        // This should not happen if the code is correct
                        // So it is not handled further
                        System.err.println("[ServerGameException in START PLAYER]");
                    }
                    catch(ClientSendException e) {
                        // This mean the client disconnected
                        // We just stop the client
                        disconnectClient(client);
                        return;
                    }
                    throw new TictactoeBadResponseException();

                /**
                 * The third case is when the client want to update his grid state
                 * we just send the grid state to the client
                 * if the client is not in a game we throw a bad response exception
                 * that will be catch below
                 * if the client disconnected we just stop the client
                 * that will be catch below
                */
                case "UPDATE":
                    if(client.hasGame()) {
                        try{
                            System.out.println(">>> Update requested by client");
                            client.sendMessageToClient(client.getGame().getGridState().concat(TictactoeConst.END_OF_MESSAGE));
                            return;
                        } catch(ClientSendException e) {
                            // This mean the client disconnected
                            // We just stop the client
                            disconnectClient(client);
                            return;
                        }
                    } else if (response.getLength() == 0) {
                        try {
                            client.sendMessageToClient("NO GAME MODE".concat(TictactoeConst.END_OF_MESSAGE).concat(TictactoeConst.END_OF_MESSAGE));
                            return;
                        } catch (ClientSendException e) {
                            // This mean the client disconnected
                            // We just stop the client
                            disconnectClient(client);
                            return;
                        }
                    }
                    throw new TictactoeBadResponseException();
                
                /**
                 * The fourth case is when the client want to quit the game
                 * we disconnect the client from the server
                 * if the client is not in a game we just disconnect him
                 * if the client disconnected we just stop the client
                */
                case "QUIT":
                    disconnectClient(client);
                    System.out.println(">>> Quit requested by client");
                    break;
                
                /**
                 * The fifth case is when the client want to play a move
                 * we check if the client is in a game and if the command is correct
                 * if it is we play the move and send the result to the client
                 * if the client is not in a game or the command is incorrect
                 * we throw a bad response exception that will be catch below
                 * if the client disconnected we just stop the client
                 * that will be catch below
                */
                case "PUT":
                    if (response.getLength() == 2) {
                        try {
                            int x = Integer.parseInt(response.get(0));
                            int y = Integer.parseInt(response.get(1));

                            System.out.println(">>> Player move requested: " + x + ":" + y);

                            /**
                             * if the game has not started yet (only one player in the game)
                             * we send a NO GAME MODE response to the client
                             * and we return
                            */
                            if (!client.hasGame()) {
                                client.sendMessageToClient("NO GAME MODE".concat(TictactoeConst.END_OF_MESSAGE).concat(TictactoeConst.END_OF_MESSAGE));
                                return;
                            }

                            StringBuilder result = new StringBuilder(client.getGame().play(client, x, y));

                            result.append(TictactoeConst.END_OF_MESSAGE);
                            client.sendMessageToClient(result.toString());

                            return;
                        } catch (NumberFormatException e) {
                            // If the parsing fail we throw a bad response exception
                            // that will be catch below
                            try {
                                client.sendMessageToClient("WRONG".concat(TictactoeConst.END_OF_MESSAGE).concat(TictactoeConst.END_OF_MESSAGE));
                                return;
                            } catch (ClientSendException a) {
                                // This mean the client disconnected
                                // We just stop the client
                                disconnectClient(client);
                                return;
                            }
                        } catch (ClientSendException e) {
                            // This mean the client disconnected
                            // We just stop the client
                            disconnectClient(client);
                            return;
                        }
                    }

                    throw new TictactoeBadResponseException();
            
                /**
                 * If the command is not recognized we send a WRONG response to the client
                 * if the client disconnected we just stop the client
                */
                default:
                    try {
                        client.sendMessageToClient("WRONG".concat(TictactoeConst.END_OF_MESSAGE).concat(TictactoeConst.END_OF_MESSAGE));
                        System.out.println(">>> Wrong command from client: " + response.getCommand());
                        return;
                    } catch (ClientSendException e) {
                        // This mean the client disconnected
                        // We just stop the client
                        disconnectClient(client);
                        return;
                    }
            }
        });
        client.start();
    }

    /**
     * Main server loop
     * We accept new client and create a new ServerClient for each of them
     * Then we init the client to listen for messages
     * This method is synchronized to avoid multiple clients to be accepted at the same time
     * This could lead to race conditions
    */
    public synchronized void run() {
        while (true) {
            try {
                Socket socketClient = server.accept();
                ServerClient client = new ServerClient(socketClient);


                try {
                    initClient(client);
                    synchronized(clients) {
                        clients.add(client);
                    }
                } catch (TictactoeBadResponseException e) {
                    System.err.println("[TictactoeBadResponseException in TictactoeServer.run()]");
                    client.sendMessageToClient("WRONG".concat(TictactoeConst.END_OF_MESSAGE).concat(TictactoeConst.END_OF_MESSAGE));
                }

                System.out.println(">>> New client connected: ");

            } catch (ClientSendException e) {
                /**
                 * in this case the client have sended a message but the message 
                 * cannot be read due to a bad format then we tried to send a WRONG message
                 * to the client but we failed to do so because the client have disconnected
                */
                System.err.println("[ClientSendException in TictactoeServer.run()]");
            } catch (ClientGetStreamException e) {
                /**
                 * in this case the client have disconnected before we could get his stream
                 * so we just ignore it
                */
                System.err.println("[ClientGtStreamException in TictactoeServer.run()]");
            } catch (TictactoeTimeoutException e) {
                /**
                 * in this case the client have not sended any message in the alloted time
                 * so we just ignore it
                */
                System.err.println("[TictactoeTimeoutException in TictactoeServer.run()]");
            } catch (IOException e) {
                /**
                 * in this case the server socket have been closed
                 * so we just stop the server
                */
                System.err.println("[IOException in TictactoeServer.run()]");
            }
        }
    }

    public static void main(String[] args) {
        try {
            TictactoeServer tictactoeServer = new TictactoeServer();
            System.out.println(">>> Server started on port " + TictactoeConst.PORT);

            tictactoeServer.run();
        } catch (Exception e) {
            System.out.println(">>> Can't start server: " + e.getMessage());
        }
    }
}
