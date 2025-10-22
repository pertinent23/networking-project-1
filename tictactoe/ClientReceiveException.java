public class ClientReceiveException extends Exception {
    public ClientReceiveException() {
        super("Failed to receive message from client");
    }
}
