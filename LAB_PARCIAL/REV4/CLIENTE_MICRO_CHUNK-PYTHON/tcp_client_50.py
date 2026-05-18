import socket
import threading

class TCPClient50:
    DEFAULT_SERVER_PORT = 4444

    def __init__(self, ip, port=DEFAULT_SERVER_PORT, callback=None):
        self.server_ip = ip
        self.server_port = port
        self.listeners = []
        self.callback = callback
        self.running = False
        self.connected = False
        self.socket = None
        self.lock = threading.Lock()

    def add_listener(self, listener):
        if listener not in self.listeners:
            self.listeners.append(listener)

    def remove_listener(self, listener):
        if listener in self.listeners:
            self.listeners.remove(listener)

    def is_connected(self):
        return self.connected

    def send_message(self, message):
        if self.socket and self.connected:
            try:
                self.socket.sendall((message + "\n").encode('utf-8'))
                return True
            except:
                self.connected = False
                self.notify_error("Conexión perdida al enviar.")
                return False
        else:
            self.notify_error("No hay conexión activa con el servidor.")
            return False

    def stop_client(self):
        self.running = False
        self.connected = False
        if self.socket:
            try:
                self.socket.shutdown(socket.SHUT_RDWR)
                self.socket.close()
            except: 
                pass
        self.socket = None

    def run(self):
        self.running = True
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(5.0)
            self.socket.connect((self.server_ip, self.server_port))
            self.socket.settimeout(None)
            self.connected = True
            self.notify_connected()

            with self.socket.makefile('r', encoding='utf-8') as reader:
                while self.running:
                    line = reader.readline()
                    if not line: 
                        break
                    clean_line = line.strip()
                    if clean_line:
                        self.notify_message(clean_line)
        except Exception as e:
            if self.running: 
                self.notify_error(f"Fallo de red: {e}")
        finally:
            self.stop_client()
            self.notify_disconnected()

    def notify_message(self, msg):
        for l in list(self.listeners):
            if hasattr(l, 'message_received'): 
                l.message_received(msg)

    def notify_connected(self):
        for l in list(self.listeners):
            if hasattr(l, 'on_connected'): 
                l.on_connected()

    def notify_disconnected(self):
        for l in list(self.listeners):
            if hasattr(l, 'on_disconnected'): 
                l.on_disconnected()

    def notify_error(self, msg):
        for l in list(self.listeners):
            if hasattr(l, 'on_error'): 
                l.on_error(msg)