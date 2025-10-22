public abstract class TictactoeConst {
    /**
     * The port where the server is running
     * In a real-world application, this would be configurable
     * For simplicity, we assume the server is always running on this port
    */
    public static final int PORT = 2629;

    /**
     * The host where the server is running
     * In a real-world application, this would be configurable
     * For simplicity, we assume the server is always running on localhost
    */
    public static final String HOST = "localhost";

    /*
     * How lotg to wait for a response from the server (in milliseconds)
     * before throwing a TictactoeTimeoutException
    */
    public static final int RESPONSE_TIMEOUT = 3000; 

    /**
     * End of message sequence
     * This is used to indicate the end of a message
     * In a real-world application, this would be more complex
     * For simplicity, we assume that the end of a message is always "\r\n"
    */
    public static final String END_OF_MESSAGE = "\r\n";
}
