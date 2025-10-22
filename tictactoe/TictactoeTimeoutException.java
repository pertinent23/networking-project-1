public class TictactoeTimeoutException extends Exception {
    public TictactoeTimeoutException() {
        super("Response from server timed out: " + TictactoeConst.RESPONSE_TIMEOUT + " ms exceeded");
    }
} 
