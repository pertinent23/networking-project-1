import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Client represent etheir a player we want to play and try to connect to the server
 * from is command line or a player that is connected to the server
 * and is waiting for an opponent 
*/
public abstract class Client extends Thread {
    /*
     * Type of client (Player or Spectator)
    */
    protected ClientType type;

    /**
     * Socket of the client
    */
    protected final Socket client;

    /**
     * Output stream of the client
    */
    protected final OutputStream out;

    /**
     * Input stream of the client
    */
    protected final InputStream in;

    /**
     * Event listener for the client
     * when we receive a message from the server we can trigger an event
     * to notify the main program that we received a message 
    */
    protected ClientEventListener myListener;

    /**
     * Buffered reader to read the input stream of the client
     * we use this to read the input stream line by line
     *  this will be useful to read the messages from the server
     *  because the messages are separated by \r\n
     */
    protected final BufferedReader reader;

    /** 
     *
     * Boolean to know if the client is running or not
     * used to stop the listening thread 
    */
    protected boolean running = false;

    /**
     * Temporary string to store the message received from the server
     * this will be useful to store the message received from the server
     * if the message is not complete (not ending with \r\n)
     * we will store it in this variable and wait for the next message
     * to complete the message
    */
    protected String temp = "";


    /**
     * Boolean to know if the client is a server or a client
     * this will be useful to know if we need to send a QUIT message
     * to the server when we stop the client
     * or if we just need to close the socket
    */
    protected boolean isClientSever = false;

    public Client(Socket client) throws ClientGetStreamException, TictactoeTimeoutException {
        this.client = client;
        /**
         * We try to get the input and output stream of the client
         * if we fail we throw a ClientGetStreamException
        */

        try {
            this.out = this.client.getOutputStream();
            this.in = this.client.getInputStream();
            this.reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        } catch (IOException e) {
            throw new ClientGetStreamException();
        }
    }

    public void setClientType(ClientType type) {
        this.type = type;
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * Send a message using the output stream of the client
     * this method throw a ClientSendException if it fails to send the message
     * @param message
     * @throws Exception
    */
    protected void sendMessage(String message) throws ClientSendException {
        if(!this.running) {
            throw new ClientSendException();
        }

        try {
            out.write(message.getBytes("UTF-8"));
            out.flush();
        } catch (IOException e) {
            /**
             * we throw a ClientReceiveException if we fail to receive a message
             * this will be catch in the run method to stop the client
            */
            throw new ClientSendException();
        }
    }

    /**
     * Receive a message using the input stream of the client
     * this method throw a ClientReceiveException if it fails to receive the message
     * @return
     * @throws ClientReceiveException
     */
    protected String receiveMessage() throws ClientReceiveException {
        try {
            String line = reader.readLine();

            if (line == null) {
                throw new IOException("Connection closed by server");
            } 

            return line.concat(TictactoeConst.END_OF_MESSAGE);
        } catch (IOException e) {
            /**
             * we throw a ClientReceiveException if we fail to receive a message
             * this will be catch in the run method to stop the client
            */
            throw new ClientReceiveException();
        }
    }

    /**
     * Keep listening for messages from the server
     * 
     * @throws ClientReceiveException
    */
    public void listening(ClientEventListener myListener) throws TictactoeBadResponseException {
        this.myListener = myListener;
    }


    /**
     * Remove the listener
    */
    public void removeListener() {
        this.myListener = null;
    }


    @Override
    public void run() {
        setRunning(true);
        while (running) {
            try {
                String message = this.receiveMessage();
                String goodEnding =  "".concat(TictactoeConst.END_OF_MESSAGE).concat(TictactoeConst.END_OF_MESSAGE);

                if(message == null) {
                    continue;
                }

                String totalMessage = temp.concat(message);

                if (isClientSever && totalMessage.equals("\\s+".concat(goodEnding))) {
                    try {
                        this.sendMessage("WRONG".concat(TictactoeConst.END_OF_MESSAGE).concat(TictactoeConst.END_OF_MESSAGE));
                        continue;
                    } catch (ClientSendException ex) {}
                } else if (totalMessage != null && totalMessage.contains(goodEnding)) {
                    String[] parts = totalMessage.split(goodEnding);

                    if (!totalMessage.endsWith(goodEnding)) {
                        temp = parts[parts.length - 1];
                        parts = totalMessage.substring(0, totalMessage.length() - temp.length()).split(goodEnding);
                    } else {
                        temp = "";
                    } 

                    if (myListener != null) {
                        for(String part : parts) {
                            myListener.onEvent(part.concat(goodEnding));
                        }
                    }
                } else {
                    temp = totalMessage;
                }
            }
            catch (TictactoeBadResponseException e) {
                if (isRunning() && isClientSever) {
                    /**
                     * if we receive a bad response from the server
                     * we send a WRONG message to the client
                     * and continue listening for messages
                     * if we fail to send the message we stop the client
                     * this will be catch below
                    */
                    StringBuilder request = new StringBuilder("WRONG");
                    request.append(TictactoeConst.END_OF_MESSAGE);
                    request.append(TictactoeConst.END_OF_MESSAGE);
                    try {
                        this.sendMessage(request.toString());
                        System.err.println("[Client Bad Response Exception (resolved)]");
                        continue;
                    } catch (ClientSendException ex) {}
                } 


                setRunning(false);
                Thread.currentThread().interrupt();

                System.err.println("[Client Bad Response Exception (client)]");
            } 
            catch (ClientReceiveException e) {
                /**
                 * if we fail to receive a message from the server
                 * we stop the client   
                 * 
                */
                StringBuilder request = new StringBuilder("QUIT");
                request.append("\r\n");

                try {
                    if (this.myListener != null) {
                        this.myListener.onEvent(request.toString());
                    }
                } catch (TictactoeBadResponseException ex) {
                    System.err.println("[ClientReceiveException Bad Response Exception (Client)]");
                }

                setRunning(false);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Check if the client is running
     * @return
    */
    public boolean isRunning() {
        return this.running;
    }
    

    /**
     * Close the client socket
     * if it fails to close the socket it catch the exception and print an error message
     * but this method will be ameliorated later with @override in the subclasses
    */
    public void quit() {
        try {
            setRunning(false);
            this.client.close();
        } catch (IOException e) {
            //In this case the socket is probably already closed
        }
    }

    /**
     * Get the type of the client
     * @return
    */
    public ClientType getType() {
        return this.type;
    }
}
