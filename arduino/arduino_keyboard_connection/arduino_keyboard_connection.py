
import websocket
import threading
import time
import sys

class WebSocketClient(object):

    def __init__(self, url):
        self._connection = websocket.WebSocketApp(url,
                    on_error=self._on_error,
                    on_open=self._on_open,
                    on_close=self._on_close)

        self.connected = threading.Event()
        self.reconnect = threading.Event()
        self.closing = threading.Event()

    def run_forever(self):
        self._connection.run_forever()
        if not self.closing.is_set() and self.reconnect.is_set():
            print("Reconnecting...")
            time.sleep(1)
            if not self.closing.is_set():
                self.reconnect.clear()
                self.run_forever()

    def close(self):
        self.closing.set()
        self._connection.close()

    def send_input(self, input):
        self._connection.send(input)

    def _on_open(self, ws):
        print("Connected to websocket interface")
        self.connected.set()

    def _on_close(self, ws):
        print("Connection to websocket interface closed")
        self.connected.clear()
        if not self.closing.is_set():
            self.reconnect.set()
            self._connection.close()

    def _on_error(self, ws, error):
        print("WebSocket error", error)
        self.connected.clear()



def cmd_input(ws_client):

    while True:
        print("Waiting for websocket to connect")
        ws_client.connected.wait()
        while ws_client.connected.is_set():
            user_input = input("Input type: ").strip()
            if ws_client.connected.is_set():
                ws_client.send_input(user_input)


def arduino_input(ws_client, usb_device):
    import serial
    print("Connecting to arduino on", usb_device)
    device = serial.Serial(usb_device, 9600)

    while True:
        if not ws_client.connected.is_set():
            print("Waiting for websocket to connect")
            ws_client.connected.wait()

        while ws_client.connected.is_set():
            user_input = device.readline().strip()
            if ws_client.connected.is_set():
                print("Sending input ", user_input)
                ws_client.send_input(user_input)



if __name__ == "__main__":


    if len(sys.argv) < 2:
        print("Usage:", sys.argv[0], "<input_websocket_url> [arduino_usb_device]")
        sys.exit(1)

    ws_url = sys.argv[1]
    usb_device = None

    if len(sys.argv) > 2:
        usb_device = sys.argv[2]

    ws_client = WebSocketClient(ws_url)
    socket_thread = threading.Thread(target=ws_client.run_forever)
    socket_thread.start()
    try:
        if usb_device is not None:
            arduino_input(ws_client, usb_device)
        else:
            cmd_input(ws_client)
    except KeyboardInterrupt:
        print("\nShutting down...")
        ws_client.close()
        socket_thread.join()
        print("Shutting complete")



