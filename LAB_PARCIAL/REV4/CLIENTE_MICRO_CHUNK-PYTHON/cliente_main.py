import tkinter as tk
from tkinter import simpledialog, messagebox
from tcp_client_50 import TCPClient50
from main_menu_cliente import MainMenuCliente
import threading

class Cliente50:
    def __init__(self):
        self.tcp_client = None
    
    def iniciar(self):
        root = tk.Tk()
        root.withdraw()
        
        ip = simpledialog.askstring("IP", "IP del servidor:", initialvalue="127.0.0.1")
        if not ip:
            root.destroy()
            return
        
        port = simpledialog.askinteger("Puerto", "Puerto del servidor:", initialvalue=4444)
        if not port:
            root.destroy()
            return
        
        name = simpledialog.askstring("Nombre", "Nombre del cliente:", initialvalue="Cliente")
        if not name:
            root.destroy()
            return
        
        root.destroy()
        
        self.tcp_client = TCPClient50(ip, port, self.on_tcp_event)
        threading.Thread(target=self.tcp_client.run, daemon=True).start()
        
        menu = MainMenuCliente(self.tcp_client, name, ip, port)
        menu.mainloop()
    
    def on_tcp_event(self, event_type, data=None):
        pass

if __name__ == "__main__":
    app = Cliente50()
    app.iniciar()