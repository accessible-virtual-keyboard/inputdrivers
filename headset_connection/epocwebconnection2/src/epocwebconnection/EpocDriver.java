package epocwebconnection;

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
 * This is a web socket driver for the Emotiv Epoc+ EEG headset. The driver does
 * the following;
 *
 * 1: Connects to the Epoc+ headset via it's proprietary USB dongle. 2: Connects
 * to an Emotiv cloud account and loads a specific user profile. 3: Establishes
 * a web socket connection to the Accessible Virtual Keyboard server. 4:
 * Interprets the received headset data, and sends the appropriate data to the
 * keyboards web socket.
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

    private String accountName = "student_group57";         // Name of the Emotiv cloud account.
    private String accountPassword = "pralina2017PRALINA";  // The cloud accounts password.
    private String profileName = "ingalill";                // A specific user profile.
    private String keyboardServerURL = "ws://avikeyb.myr1.net/input";
    private URI keyboardURI = null;
    private WebSocketClient webSocketClient = null;
    private boolean isRunning = true;

    private final float[] TIME_OF_PREVIOUS_TRIGGER = {0, 0, 0, 0};  // Keeps track of the time passed since each command have been triggered.
    private float currentTimeStamp = 0;
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int PUSH = 2;
    private static final int PULL = 3;
    private float timeBetweenCommands = 1;  // Dictates how often one can send the same mental command (seconds between commands).
    private float triggerTreshold = 0.70f;  // How strong the mental command must be in order to trigger.

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
        doSomething();
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
        emoDriver.mainLoop();
    }

    /**
     * Connects to the Epoc+ headset.
     */
    private void connectToHeadset() {
//        if (Edk.INSTANCE.IEE_EngineRemoteConnect("127.0.0.1", (short) 1726, "Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {  // Connects to composer.
        if (Edk.INSTANCE.IEE_EngineConnect("Emotiv Systems-5") != EdkErrorCode.EDK_OK.ToInt()) {  // Connects to headset.
            System.err.println("Failed to establish headset connection.");
        }
        else {
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
        }
        else {
            System.out.println("Connected to Emotiv server.");
        }

        // Login to Emotiv account.
        if (EmotivCloudClient.INSTANCE.EC_Login(accountName, accountPassword) != EdkErrorCode.EDK_OK.ToInt()) {
            System.err.println("Login attempt failed. Username or password may be incorrect");
            return;
        }
        else {
            System.out.println("Logged in as " + accountName);
        }

        // Check user details.
        if (EmotivCloudClient.INSTANCE.EC_GetUserDetail(userCloudID) != EdkErrorCode.EDK_OK.ToInt()) {
            System.err.println("Failed to get user cloud ID.");
        }
        else {
            System.out.println("userCloudID: " + userCloudID.getValue());
        }
    }

    /**
     * Loads a user profile from the Emotiv server.
     */
    private void loadProfile() {
        int getNumberProfile = EmotivCloudClient.INSTANCE.EC_GetAllProfileName(userCloudID.getValue());
        int edkErrorCode = EmotivCloudClient.INSTANCE.EC_GetProfileId(userCloudID.getValue(), profileName, profileID);

        if (getNumberProfile > 0) {  // If profiles exist.
            if (EmotivCloudClient.INSTANCE.EC_LoadUserProfile(userCloudID.getValue(), engineUserID.getValue(), profileID.getValue(), -1) == EdkErrorCode.EDK_OK.ToInt()) {
                System.out.println("Loaded user profile: " + profileID.getValue());
            }
            else {
                System.err.println("Failed to load user profile.");
                System.err.println("edkErrorCode:" + edkErrorCode + " | profileID: " + profileID.getValue());
            }
        }
    }

    /**
     * Connects to the web socket of the keyboard server.
     */
    private void connectToKeyboard() {
        try {
            keyboardURI = new URI(keyboardServerURL);
            createSocketClient();
            System.out.println("Connected to the Accessible Virtual Keyboard server.");
        }
        catch (URISyntaxException ex) {
            System.err.println("Failed to connect to the Accessible Virtual Keboard server.");
            Logger.getLogger(EpocDriver.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    /**
     * TODO: Is this necessary?
     */
    private void doSomething() {
        state = Edk.INSTANCE.IEE_EngineGetNextEvent(E_EVENT);
        if (state == EdkErrorCode.EDK_OK.ToInt()) {
            int eventType = Edk.INSTANCE.IEE_EmoEngineEventGetType(E_EVENT);
            Edk.INSTANCE.IEE_EmoEngineEventGetUserId(E_EVENT, engineUserID);

            if (eventType == Edk.IEE_Event_t.IEE_UserAdded.ToInt()) {
                System.out.println("New user " + engineUserID.getValue() + " added");
            }
            else if (eventType == Edk.IEE_Event_t.IEE_EmoStateUpdated.ToInt()) {
                Edk.INSTANCE.IEE_EmoEngineEventGetEmoState(E_EVENT, E_STATE);
            }
        }
    }

    /**
     * Main loop of the application. Listens for data from the headset,
     * interprets it, and sends the proper responses to the web socket.
     */
    private void mainLoop() {

        System.out.println("Waiting for incoming data...");

        while (isRunning) {
            state = Edk.INSTANCE.IEE_EngineGetNextEvent(E_EVENT);

            // Handle new event.
            if (state == EdkErrorCode.EDK_OK.ToInt()) {

                int eventType = Edk.INSTANCE.IEE_EmoEngineEventGetType(E_EVENT);
                Edk.INSTANCE.IEE_EmoEngineEventGetUserId(E_EVENT, userCloudID);

                // Handle the EmoState if it has been updated.
                if (eventType == Edk.IEE_Event_t.IEE_EmoStateUpdated.ToInt()) {

                    Edk.INSTANCE.IEE_EmoEngineEventGetEmoState(E_EVENT, E_STATE);

                    System.out.print("TimeStamp: " + EmoState.INSTANCE.IS_GetTimeFromStart(E_STATE)
                            + " | WifiStatus: " + EmoState.INSTANCE.IS_GetWirelessSignalStatus(E_STATE)
                            + " | CloudUser: " + userCloudID.getValue()
                            + " | MentalCommandID: " + EmoState.INSTANCE.IS_MentalCommandGetCurrentAction(E_STATE)
                            + " | CommandStrength: " + EmoState.INSTANCE.IS_MentalCommandGetCurrentActionPower(E_STATE));
                    sendToKeyboard();
                }
            }
            else if (state != EdkErrorCode.EDK_NO_EVENT.ToInt()) {
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
            }

            @Override
            public void onMessage(String string) {
            }

            @Override
            public void onClose(int i, String string, boolean bln) {
                // Reconnect if connection is lost.
                createSocketClient();
            }

            @Override
            public void onError(Exception ex) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        webSocketClient.connect();
    }

    /**
     * Reads the current mental command and sends the appropriate data to the
     * keyboard server.
     */
    private void sendToKeyboard() {

        currentTimeStamp = EmoState.INSTANCE.IS_GetTimeFromStart(E_STATE);

        if (EmoState.INSTANCE.IS_MentalCommandGetCurrentActionPower(E_STATE) >= triggerTreshold) {

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
    }

}
