public class ClientSendException extends Exception {
    public ClientSendException() {
        super("Failed to send message to client");
    }
}
