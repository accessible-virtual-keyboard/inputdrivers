package epocwebconnection;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Websocket connection to a server.
 */
public class SocketClient extends WebSocketClient {

    /**
     *
     * @param serverURI
     */
    public SocketClient(URI serverURI) {
        super(serverURI);
    }

    /**
     *
     * @param serverUri
     * @param draft
     */
    public SocketClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Opened connection");
        // If you plan to refuse connection based on ip or httpfields, then overload: onWebsocketHandshakeReceivedAsClient.
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received message: " + message);
    }

    @Override
    public void onFragment(Framedata fragment) {
        System.out.println("Received fragment: " + new String(fragment.getPayloadData().array()));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The codecodes are documented in the class org.java_websocket.framing.CloseFrame.
        System.out.println("Connection closed by " + (remote ? "remote peer" : "us"));
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // If the error is fatal, then onClose will be called additionally.
    }

}
