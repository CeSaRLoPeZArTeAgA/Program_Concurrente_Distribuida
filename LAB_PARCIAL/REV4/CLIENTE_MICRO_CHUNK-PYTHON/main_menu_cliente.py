import tkinter as tk
from tkinter import messagebox
from form_mnist import MainFormularioMNIST
from form_w2v import MainFormularioWord2Vec

class MainMenuCliente(tk.Tk):
    def __init__(self, client, name, ip, port):
        super().__init__()
        self.client = client
        self.client_name = name
        self.ip = ip
        self.port = port
        
        self.title(f"Cliente MICRO CHUNK - {name} - {ip}:{port}")
        self.geometry("380x560")
        self.configure(bg="white")
        self.resizable(False, False)
        
        self.opcion = tk.IntVar(self, value=1)
        self.client.add_listener(self)
        self.crear_interfaz()
        
        # Centrar ventana
        self.update_idletasks()
        x = (self.winfo_screenwidth() // 2) - (self.winfo_width() // 2)
        y = (self.winfo_screenheight() // 2) - (self.winfo_height() // 2)
        self.geometry(f"+{x}+{y}")

    def crear_interfaz(self):
        # Panel principal con borde
        main_panel = tk.Frame(self, bg="white", highlightthickness=4, highlightbackground="black")
        main_panel.pack(fill="both", expand=True)
        
        # Título
        titulo = tk.Label(main_panel, text="M I C R O   C H U N K", font=("Arial", 20, "bold"), 
                         bg="white", fg="black")
        titulo.pack(pady=(55, 20), padx=10)
        
        # Panel de opciones
        panel_opciones = tk.Frame(main_panel, bg="white")
        panel_opciones.pack(pady=(90, 70), padx=(35, 25))
        
        # Estado del servidor
        estado = tk.Label(panel_opciones, text=f"Servidor: {self.ip}:{self.port}", 
                         font=("Serif", 13, "bold"), bg="white")
        estado.pack(pady=5)
        
        tk.Label(panel_opciones, text="", bg="white").pack(pady=10)
        
        # Radio buttons
        rb1 = tk.Radiobutton(panel_opciones, text="Modelo de procesamiento de imagenes", 
                            variable=self.opcion, value=1, bg="white", font=("Serif", 13), 
                            fg="black", selectcolor="white")
        rb1.pack(pady=5, anchor="w")
        
        rb2 = tk.Radiobutton(panel_opciones, text="Modelo de procesamiento de texto", 
                            variable=self.opcion, value=2, bg="white", font=("Serif", 13), 
                            fg="black", selectcolor="white")
        rb2.pack(pady=5, anchor="w")
        
        tk.Label(panel_opciones, text="", bg="white").pack(pady=10)
        
        # Panel de botones
        panel_botones = tk.Frame(main_panel, bg="white")
        panel_botones.pack(pady=(10, 25), padx=30)
        
        btn_entrar = tk.Button(panel_botones, text="Entrar a IA", command=self.entrar_al_modelo,
                              width=12, height=2, bg="white", fg="black", 
                              font=("Serif", 13), relief="solid", bd=3)
        btn_entrar.grid(row=0, column=0, padx=(0, 35))
        
        btn_salir = tk.Button(panel_botones, text="Salir", command=self.salir_del_sistema,
                             width=12, height=2, bg="white", fg="black", 
                             font=("Serif", 13), relief="solid", bd=3)
        btn_salir.grid(row=0, column=1)

    def entrar_al_modelo(self):
        if not self.client.is_connected():
            messagebox.showwarning("Conexion no lista", 
                                  "Todavia no hay conexion activa con el servidor.")
            return
        
        val = self.opcion.get()
        self.withdraw()
        
        if val == 1:
            MainFormularioMNIST(self.client, self)
        elif val == 2:
            MainFormularioWord2Vec(self.client, self)
        else:
            messagebox.showwarning("Aviso", "Selecciona un modelo antes de continuar.")
            self.deiconify()

    def volver_a_mostrar(self):
        self.deiconify()
        self.update_idletasks()
        x = (self.winfo_screenwidth() // 2) - (self.winfo_width() // 2)
        y = (self.winfo_screenheight() // 2) - (self.winfo_height() // 2)
        self.geometry(f"+{x}+{y}")

    def salir_del_sistema(self):
        opcion = messagebox.askyesno("Confirmar salida", "Deseas salir del sistema?")
        if opcion:
            self.client.stop_client()
            self.destroy()

    def message_received(self, m):
        pass
    
    def on_connected(self):
        self.client.send_message(f"CLIENT_HELLO|{self.client_name}")
    
    def on_disconnected(self):
        pass
    
    def on_error(self, m):
        self.after(0, lambda: messagebox.showerror("Error cliente", m))