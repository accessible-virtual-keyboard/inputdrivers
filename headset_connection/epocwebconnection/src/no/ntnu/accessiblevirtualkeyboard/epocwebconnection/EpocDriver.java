package no.ntnu.accessiblevirtualkeyboard.epocwebconnection;

import com.emotiv.Iedk.Edk;
import com.emotiv.Iedk.EdkErrorCode;
import com.emotiv.Iedk.EmoState;
import com.emotiv.Iedk.EmotivCloudClient;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.Pointer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * This driver uses web sockets to connect the Accessible Virtual Keyboard with
 * the Emotiv Epoc+ EEG headset. The driver does the following;
 *
 * 1: Connects to the Epoc+ headset via it's proprietary USB dongle.
 * 2: Connects to an Emotiv cloud account and loads a specific user profile.
 * 3: Establishes a web socket connection to the Accessible Virtual Keyboard server.
 * 4: Listens for input from the headset, interprets the received headset data,
 * and sends the appropriate data to the keyboards web socket.
 *
 * @author Kristian Honningsvag.
 */
public class EpocDriver implements Runnable {

    private static final Pointer E_EVENT = Edk.INSTANCE.IEE_EmoEngineEventCreate();
    private static final Pointer E_STATE = Edk.INSTANCE.IEE_EmoStateCreate();

    private int state = 0;
    private IntByReference engineUserID = null;
    private IntByReference userCloudID = null;
    private IntByReference profileID = null;

    private String accountName = "yourAccountName";          // Name of the Emotiv cloud account.
    private String accountPassword = "yourAccountPassword";  // The cloud accounts password.
    private String profileName = "yourProfileName";          // A specific user profile.
    private String keyboardServerURL = "ws://localhost:43879/input";
    private URI keyboardURI = null;
    private WebSocketClient webSocketClient = null;
    private boolean isRunning = true;

    private final float[] TIME_OF_PREVIOUS_TRIGGER = {0, 0, 0, 0};  // Keeps track of the time passed since each command have been triggered.
    private float currentTimeStamp = 0;
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int PUSH = 2;
    private static final int PULL = 3;
    private float timeBetweenCommands = 1.5f;  // Dictates how often one can send the same mental command (seconds between commands).
    private float triggerTreshold = 0.30f;     // How strong the mental command must be in order to trigger.

    /**
     * Constructs the HeadsetConnection object, and connects to the necessary
     * resources that the application will use.
     */
    public EpocDriver() {
        engineUserID = new IntByReference(0);
        userCloudID = new IntByReference(0);
        profileID = new IntByReference(-1);

        connectToHeadset();
        connectToEmotivServer();
        loadProfile();
        connectToKeyboard();
    }

    /**
     * Main method. Starts the application.
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
//        System.setProperty( "java.library.path", "bin/linux64" );  // Alternatively set this in VM options: -Djava.library.path="bin/"
//        System.load("/bin/linux64/libedk.so");
        EpocDriver emoDriver = new EpocDriver();
        Thread t1 = new Thread(emoDriver);
        t1.start();
        t1.join();
    }

    /**
     * Connects to the Epoc+ headset.
     */
    private void connectToHeadset() {
//        if (Edk.INSTANCE.IEE_EngineRemoteConnect("127.0.0.1", (short) 1726, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {  // Connects to composer.
        if (Edk.INSTANCE.IEE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {  // Connects to headset.
            System.err.println("Failed to establish headset connection.");
        } else {
            System.out.println("Headset connection established.");
        }
    }

    /**
     * Connects to the Emotiv server.
     */
    private void connectToEmotivServer() {
        // Connect to Emotiv server.
        if (EmotivCloudClient.INSTANCE.EC_Connect() != EdkErrorCode.EDK_OK.ToInt()) {
            System.err.println("Cannot connect to Emotiv server.");
        } else {
            System.out.println("Connected to Emotiv server.");
        }

        // Login to Emotiv account.
        if (EmotivCloudClient.INSTANCE.EC_Login(accountName, accountPassword) != EdkErrorCode.EDK_OK.ToInt()) {
            System.err.println("Login attempt failed. Username or password may be incorrect");
            return;
        } else {
            System.out.println("Logged in as " + accountName);
        }

        // Check user details.
        if (EmotivCloudClient.INSTANCE.EC_GetUserDetail(userCloudID) != EdkErrorCode.EDK_OK.ToInt()) {
            System.err.println("Failed to get user cloud ID.");
        } else {
            System.out.println("userCloudID is: " + userCloudID.getValue());
        }
    }

    /**
     * Loads a user profile from the Emotiv server.
     */
    private void loadProfile() {
        int numberOfProfiles = EmotivCloudClient.INSTANCE.EC_GetAllProfileName(userCloudID.getValue());
        if (numberOfProfiles > 0) {  // If profiles exist.

            // List available profiles.
            System.out.println("Available profiles:");
            for (int i = 0; i < numberOfProfiles; i++) {
                System.out.println("  " + EmotivCloudClient.INSTANCE.EC_ProfileNameAtIndex(userCloudID.getValue(), i));
            }

            // Get profileId for a specific profile.
            int edkErrorCode = EmotivCloudClient.INSTANCE.EC_GetProfileId(userCloudID.getValue(), profileName, profileID);
            if (edkErrorCode != EdkErrorCode.EDK_OK.ToInt()) {
                System.err.println("Failed to load profile id for: " + profileName);
            } else {
                System.out.println("Profile id for " + profileName + " is " + profileID);
            }

            // Load specific profile to headset.
            System.out.println("Loading profile " + profileName + " (" + profileID.getValue() + ") to headset.");
            edkErrorCode = EmotivCloudClient.INSTANCE.EC_LoadUserProfile(userCloudID.getValue(), engineUserID.getValue(), profileID.getValue(), -1);
            if (edkErrorCode == EdkErrorCode.EDK_OK.ToInt()) {
                System.out.println("Loaded user profile: " + profileName + "(" + profileID.getValue() + ") to headset.");
            } else {
                System.err.println("Failed to load user profile. | edkErrorCode: " + edkErrorCode + " | profileID: " + profileID.getValue());
            }
        } else {
            System.err.println("No profiles available.");
        }
    }

    /**
     * Connects to the web socket of the keyboard server.
     */
    private void connectToKeyboard() {
        try {
            keyboardURI = new URI(keyboardServerURL);
            createSocketClient();
        } catch (URISyntaxException ex) {
            System.err.println("Failed to connect to the Accessible Virtual Keboard server.");
            Logger.getLogger(EpocDriver.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    /**
     * Main loop of the application. Listens for data from the headset,
     * interprets it, and sends the proper responses to the web socket.
     */
    private void mainLoop() {

        System.out.println("Waiting for incoming data...");

        while (isRunning) {

            // Reconnect to the web socket if needed.
            if (webSocketClient.getReadyState() == WebSocketClient.READYSTATE.CLOSED) {
                createSocketClient();
            }

            // Get new event and handle it.
            state = Edk.INSTANCE.IEE_EngineGetNextEvent(E_EVENT);
            if (state == EdkErrorCode.EDK_OK.ToInt()) {

                // Handle the EmoState if it has been updated.
                int eventType = Edk.INSTANCE.IEE_EmoEngineEventGetType(E_EVENT);
                if (eventType == Edk.IEE_Event_t.IEE_EmoStateUpdated.ToInt()) {
                    Edk.INSTANCE.IEE_EmoEngineEventGetEmoState(E_EVENT, E_STATE);
                    System.out.print("TimeStamp: " + EmoState.INSTANCE.IS_GetTimeFromStart(E_STATE)
                            + " | WifiStatus: " + EmoState.INSTANCE.IS_GetWirelessSignalStatus(E_STATE)
                            + " | CloudUser: " + userCloudID.getValue()
                            + " | ProfileId: " + profileID.getValue()
                            + " | MentalCommandID: " + EmoState.INSTANCE.IS_MentalCommandGetCurrentAction(E_STATE)
                            + " | CommandStrength: " + EmoState.INSTANCE.IS_MentalCommandGetCurrentActionPower(E_STATE));
                    sendToKeyboard();
                }
            } else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
                System.err.println("Internal error in Emotiv Engine!");
                isRunning = false;
                break;
            }
        }
        stop();
    }

    /**
     * Frees used memory. Should be called when closing application.
     */
    private void stop() {
        System.out.println("Application is quitting...");
        Edk.INSTANCE.IEE_EngineDisconnect();
        Edk.INSTANCE.IEE_EmoStateFree(E_STATE);
        Edk.INSTANCE.IEE_EmoEngineEventFree(E_EVENT);
    }

    /**
     * Creates a web socket connection.
     */
    private void createSocketClient() {
        webSocketClient = new WebSocketClient(keyboardURI) {
            @Override
            public void onOpen(ServerHandshake sh) {
                System.out.println("Web socket connection established.");
            }

            @Override
            public void onMessage(String string) {
                // Not implemented.
            }

            @Override
            public void onClose(int i, String string, boolean bln) {
                System.err.println("Websocket onClose() was triggered");
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("Websocket onError() was triggered");
                System.err.println(ex);
            }
        };
        System.out.println("Attempting web socket connection to AVK server.");
        webSocketClient.connect();
    }

    /**
     * Reads the current mental command and sends the appropriate data to the
     * keyboard server.
     */
    private void sendToKeyboard() {
        currentTimeStamp = EmoState.INSTANCE.IS_GetTimeFromStart(E_STATE);

        // Command strength needs to be over the treshold in order to trigger.
        if (EmoState.INSTANCE.IS_MentalCommandGetCurrentActionPower(E_STATE) >= triggerTreshold) {

            // Don't attempt sending output if web socket is not ready.
            if (webSocketClient.getReadyState() != WebSocketClient.READYSTATE.OPEN) {
                return;
            }

            switch (EmoState.INSTANCE.IS_MentalCommandGetCurrentAction(E_STATE)) {
                case 32: // Left.
                    if (currentTimeStamp - TIME_OF_PREVIOUS_TRIGGER[LEFT] >= timeBetweenCommands) {
                        TIME_OF_PREVIOUS_TRIGGER[LEFT] = currentTimeStamp;
                        System.out.print(" | SendingToSocket: 1");
                        webSocketClient.send("1");
                    }
                    break;

                case 64: // Right.
                    if (currentTimeStamp - TIME_OF_PREVIOUS_TRIGGER[RIGHT] >= timeBetweenCommands) {
                        TIME_OF_PREVIOUS_TRIGGER[RIGHT] = currentTimeStamp;
                        System.out.print(" | SendingToSocket: 2");
                        webSocketClient.send("2");
                    }
                    break;

                case 2: // Push.
                    if (currentTimeStamp - TIME_OF_PREVIOUS_TRIGGER[PUSH] >= timeBetweenCommands) {
                        TIME_OF_PREVIOUS_TRIGGER[PUSH] = currentTimeStamp;
                        System.out.print(" | SendingToSocket: 3");
                        webSocketClient.send("3");
                    }
                    break;

                case 4: // Pull.
                    if (currentTimeStamp - TIME_OF_PREVIOUS_TRIGGER[PULL] >= timeBetweenCommands) {
                        TIME_OF_PREVIOUS_TRIGGER[PULL] = currentTimeStamp;
                        System.out.print(" | SendingToSocket: 4");
                        webSocketClient.send("4");
                    }
                    break;
            }
        }
        System.out.println("");
    }

    // Overrides.
    @Override
    public void run() {
        mainLoop();
    }

}
