/*
 * Event listener for the client
 * when we receive a message from the server we can trigger an event
 * to notify the main program that we received a message
*/
@FunctionalInterface
public interface ClientEventListener {
    void onEvent(String data) throws TictactoeBadResponseException;
}