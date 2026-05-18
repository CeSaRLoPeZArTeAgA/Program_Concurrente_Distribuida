import tkinter as tk
from tkinter import scrolledtext, messagebox

class MainFormularioWord2Vec(tk.Toplevel):
    def __init__(self, tcp_client, menu):
        super().__init__()
        self.tcp_client = tcp_client
        self.menu = menu
        self.title("Modelo Word2Vec - Cliente")
        self.geometry("880x700")
        self.configure(bg="white")
        
        self.tcp_client.add_listener(self)
        self.protocol("WM_DELETE_WINDOW", self.cerrar)
        self.crear_interfaz()
        
        # Centrar ventana
        self.update_idletasks()
        x = (self.winfo_screenwidth() // 2) - (self.winfo_width() // 2)
        y = (self.winfo_screenheight() // 2) - (self.winfo_height() // 2)
        self.geometry(f"+{x}+{y}")

    def crear_interfaz(self):
        # Título
        titulo = tk.Label(self, text="Modelo Word2Vec - Cliente", font=("Arial", 26, "bold"),
                         bg="white", fg="black")
        titulo.pack(pady=(15, 10), padx=10)
        
        # Panel central
        panel_centro = tk.Frame(self, bg="white")
        panel_centro.pack(fill="both", expand=True, padx=25, pady=10)
        
        # Panel superior - Analogías
        panel_superior = tk.Frame(panel_centro, bg="white")
        panel_superior.pack(fill="x", pady=10)
        
        # Panel de Analogía
        panel_analogia = tk.LabelFrame(panel_superior, text="Analogia: A - B + C", bg="white", 
                                      fg="black", font=("Arial", 10, "bold"), padx=10, pady=10)
        panel_analogia.pack(fill="x", pady=(0, 10))
        
        # Labels para campos
        label_frame = tk.Frame(panel_analogia, bg="white")
        label_frame.pack(fill="x")
        
        tk.Label(label_frame, text="A", bg="white", font=("Arial", 10, "bold")).grid(row=0, column=0, padx=10)
        tk.Label(label_frame, text="B", bg="white", font=("Arial", 10, "bold")).grid(row=0, column=1, padx=10)
        tk.Label(label_frame, text="C", bg="white", font=("Arial", 10, "bold")).grid(row=0, column=2, padx=10)
        
        # Entry fields
        entry_frame = tk.Frame(panel_analogia, bg="white")
        entry_frame.pack(fill="x", pady=5)
        
        self.palabra_a = tk.Entry(entry_frame, width=15, font=("Arial", 14))
        self.palabra_a.grid(row=0, column=0, padx=10)
        
        self.palabra_b = tk.Entry(entry_frame, width=15, font=("Arial", 14))
        self.palabra_b.grid(row=0, column=1, padx=10)
        
        self.palabra_c = tk.Entry(entry_frame, width=15, font=("Arial", 14))
        self.palabra_c.grid(row=0, column=2, padx=10)
        
        btn_resolver = tk.Button(entry_frame, text="Enviar analogia al servidor", 
                                command=self.resolver_analogia, font=("Arial", 10))
        btn_resolver.grid(row=0, column=3, padx=10)
        
        # Ejemplo
        ejemplo = tk.Label(panel_analogia, text="Ejemplo: rey - hombre + mujer ≈ reina",
                          font=("Arial", 11, "italic"), bg="white")
        ejemplo.pack(pady=5)
        
        # Panel de palabras cercanas
        panel_nearest = tk.LabelFrame(panel_superior, text="Palabras mas cercanas", bg="white",
                                     fg="black", font=("Arial", 10, "bold"), padx=10, pady=10)
        panel_nearest.pack(fill="x")
        
        frame_nearest = tk.Frame(panel_nearest, bg="white")
        frame_nearest.pack(fill="x")
        
        tk.Label(frame_nearest, text="Palabra: ", bg="white", font=("Arial", 10)).pack(side="left", padx=5)
        
        self.palabra_nearest = tk.Entry(frame_nearest, width=20, font=("Arial", 14))
        self.palabra_nearest.pack(side="left", padx=5)
        
        btn_nearest = tk.Button(frame_nearest, text="Buscar cercanas en servidor",
                               command=self.buscar_cercanas, font=("Arial", 10))
        btn_nearest.pack(side="left", padx=5)
        
        # Área de resultados
        self.resultado_area = scrolledtext.ScrolledText(panel_centro, height=18, width=100,
                                                       font=("Consolas", 11), bg="white", fg="black")
        self.resultado_area.pack(fill="both", expand=True, pady=10)
        self.resultado_area.insert(tk.END, "Resultado pendiente.")
        
        # Label de estado
        self.estado_label = tk.Label(panel_centro, text="Estado: esperando consulta", bg="#f0f0f0",
                                    fg="black", font=("Arial", 10), relief="solid", bd=1)
        self.estado_label.pack(fill="x", pady=5)
        
        # Panel de botones
        panel_botones = tk.Frame(self, bg="white")
        panel_botones.pack(padx=25, pady=20)
        
        btn_limpiar = tk.Button(panel_botones, text="Limpiar", command=self.limpiar,
                               width=15, font=("Arial", 10))
        btn_limpiar.grid(row=0, column=0, padx=5)
        
        btn_menu = tk.Button(panel_botones, text="Volver al menu principal", command=self.cerrar,
                            width=20, font=("Arial", 10))
        btn_menu.grid(row=0, column=1, padx=5)
        
        btn_salir = tk.Button(panel_botones, text="Salir", command=self.salir_del_sistema,
                             width=15, font=("Arial", 10))
        btn_salir.grid(row=0, column=2, padx=5)

    def resolver_analogia(self):
        if not self.tcp_client.is_connected():
            messagebox.showerror("Error", "Servidor desconectado. Vuelve al menú.")
            return
        
        a = self.palabra_a.get().strip()
        b = self.palabra_b.get().strip()
        c = self.palabra_c.get().strip()
        
        if not (a and b and c):
            messagebox.showwarning("Aviso", "Ingresa las tres palabras para la analogía.")
            return
        
        self.estado_label.config(text="Estado: consultando servidor...")
        self.tcp_client.send_message(f"W2V_ANALOGY|{a}|{b}|{c}|10")

    def buscar_cercanas(self):
        if not self.tcp_client.is_connected():
            messagebox.showerror("Error", "Servidor desconectado. Vuelve al menú.")
            return
        
        palabra = self.palabra_nearest.get().strip()
        
        if not palabra:
            messagebox.showwarning("Aviso", "Ingresa una palabra para buscar.")
            return
        
        self.estado_label.config(text="Estado: buscando palabras cercanas...")
        self.tcp_client.send_message(f"W2V_NEAREST|{palabra}|10")

    def message_received(self, message):
        self.after(0, self.procesar_gui, message)

    def procesar_gui(self, message):
        try:
            if message.startswith("W2V_RESULT|"):
                parts = message.split("|")
                if len(parts) >= 3:
                    consulta = parts[1]
                    ranking_raw = parts[2]
                    tiempo = parts[3] if len(parts) > 3 else "N/A"

                    texto_final = f"\n--- RESULTADO DE ANALOGÍA ---\n"
                    texto_final += f"Operación: {consulta}\n"
                    texto_final += f"Tiempo: {tiempo} ms\n"
                    texto_final += f"{'-'*50}\n"
                    
                    resultados = ranking_raw.split(",")
                    for i, res in enumerate(resultados, 1):
                        texto_final += f"{i}. {res}\n"

                    self.resultado_area.insert(tk.END, texto_final)
                    self.resultado_area.see(tk.END)
                    self.estado_label.config(text="Estado: resultado recibido")
            
            elif message.startswith("W2V_NEAREST|"):
                parts = message.split("|")
                if len(parts) >= 2:
                    palabra = parts[1]
                    ranking_raw = parts[2] if len(parts) > 2 else ""
                    tiempo = parts[3] if len(parts) > 3 else "N/A"

                    texto_final = f"\n--- PALABRAS MÁS CERCANAS ---\n"
                    texto_final += f"Palabra: {palabra}\n"
                    texto_final += f"Tiempo: {tiempo} ms\n"
                    texto_final += f"{'-'*50}\n"
                    
                    resultados = ranking_raw.split(",")
                    for i, res in enumerate(resultados, 1):
                        if res.strip():
                            texto_final += f"{i}. {res}\n"

                    self.resultado_area.insert(tk.END, texto_final)
                    self.resultado_area.see(tk.END)
                    self.estado_label.config(text="Estado: resultado recibido")
            
            elif message.startswith("ERROR|"):
                self.resultado_area.insert(tk.END, f"\n[ERROR]: {message}\n")
                self.estado_label.config(text="Estado: error del servidor")
        except Exception as e:
            print(f"Error procesando respuesta: {e}")

    def limpiar(self):
        self.palabra_a.delete(0, tk.END)
        self.palabra_b.delete(0, tk.END)
        self.palabra_c.delete(0, tk.END)
        self.palabra_nearest.delete(0, tk.END)
        self.resultado_area.delete("1.0", tk.END)
        self.resultado_area.insert(tk.END, "Resultado pendiente.")
        self.estado_label.config(text="Estado: pantalla limpiada")

    def salir_del_sistema(self):
        respuesta = messagebox.askyesno("Confirmar salida", "Deseas salir del sistema?")
        if respuesta:
            self.tcp_client.stop_client()
            self.menu.destroy()

    def cerrar(self):
        self.tcp_client.remove_listener(self)
        self.menu.volver_a_mostrar()
        self.destroy()
    
    def on_error(self, m):
        self.after(0, lambda: messagebox.showerror("Error", m))
    
    def on_connected(self):
        pass
    
    def on_disconnected(self):
        pass