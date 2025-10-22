import java.net.Socket;

public class ServerClient extends Client {
    /**
     * A client can only be connected to only
     * one game, game represend this game
     */
    private ServerGame game;

    public ServerClient(Socket client) throws ClientGetStreamException, TictactoeTimeoutException {
        super(client);
        isClientSever = true;
    }

    /**
     * we override the method which do the same thing
     * than receiveMessage but to be better understood when
     * i'm coding the Tictactoe Connected client
     * @return
     * @throws ClientReceiveException
    */
    protected String receiveMessageFromClient() throws ClientReceiveException {
        return this.receiveMessage();
    }

    /**
     * we override the method which do the same thing
     * than sendMessage but to be better understood when
     * i'm coding the Tictactoe Connected client
     * @param message
     * @throws ClientSendException
    */
    public void sendMessageToClient(String message) throws ClientSendException {
        this.sendMessage(message);
    }
    
    /**
     * return true if the client is connected to a game
     * @return
     */
    public boolean hasGame() {
        return (this.game != null);
    }

    /**
     * set the game of the client
    */
    public void setGame(ServerGame game) {
        this.game = game;
    }

    /**
     * return the client current game
     * @return
     */
    public ServerGame getGame() {
        return game;
    }
}
