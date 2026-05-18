import tkinter as tk
from tkinter import filedialog, messagebox
from PIL import Image, ImageTk
import os

class MainFormularioMNIST(tk.Toplevel):
    def __init__(self, tcp_client, menu):
        super().__init__()
        self.tcp_client = tcp_client
        self.menu = menu
        self.title("Cliente MNIST - prediccion remota en servidor")
        self.geometry("700x720")
        self.configure(bg="white")
        
        self.current_image = None
        self.selected_file = None
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
        titulo = tk.Label(self, text="Clasificador MNIST - Cliente", font=("Arial", 24, "bold"), 
                         bg="white", fg="black")
        titulo.pack(pady=(15, 10), padx=10)
        
        # Panel central
        center_panel = tk.Frame(self, bg="white")
        center_panel.pack(fill="both", expand=True, padx=20, pady=10)
        
        # Label para imagen
        self.image_label = tk.Label(center_panel, text="Ninguna imagen cargada", bg="white", 
                                   fg="gray", width=60, height=20, relief="solid", bd=2)
        self.image_label.pack(pady=10)
        
        # Panel de información
        info_panel = tk.Frame(center_panel, bg="white")
        info_panel.pack(fill="both")
        
        self.path_label = tk.Label(info_panel, text="Archivo: ninguno", bg="white", fg="black")
        self.path_label.pack(pady=5)
        
        self.resultado_label = tk.Label(info_panel, text="Prediccion: pendiente", bg="#f0f0f0", 
                                       fg="black", font=("Arial", 22, "bold"), relief="solid", bd=1)
        self.resultado_label.pack(pady=10, padx=10, fill="x")
        
        # Área de probabilidades
        scroll_frame = tk.Frame(center_panel, bg="white")
        scroll_frame.pack(fill="both", expand=True, pady=10)
        
        scrollbar = tk.Scrollbar(scroll_frame)
        scrollbar.pack(side="right", fill="y")
        
        self.prob_text = tk.Text(scroll_frame, height=8, width=60, font=("Consolas", 11), 
                                yscrollcommand=scrollbar.set)
        self.prob_text.pack(side="left", fill="both", expand=True)
        self.prob_text.insert(tk.END, "Probabilidades por clase: pendiente")
        self.prob_text.config(state="disabled")
        scrollbar.config(command=self.prob_text.yview)
        
        # Panel de botones
        btn_panel = tk.Frame(self, bg="white")
        btn_panel.pack(padx=20, pady=20)
        
        btn_abrir = tk.Button(btn_panel, text="Abrir imagen", command=self.abrir_imagen, 
                             width=12, height=1, font=("Arial", 10))
        btn_abrir.grid(row=0, column=0, padx=5)
        
        btn_limpiar = tk.Button(btn_panel, text="Limpiar pantalla", command=self.limpiar_pantalla, 
                               width=12, height=1, font=("Arial", 10))
        btn_limpiar.grid(row=0, column=1, padx=5)
        
        btn_enviar = tk.Button(btn_panel, text="Enviar al servidor IA", command=self.enviar_al_servidor, 
                              width=17, height=1, font=("Arial", 10))
        btn_enviar.grid(row=0, column=2, padx=5)
        
        btn_menu = tk.Button(btn_panel, text="Volver al menu principal", command=self.cerrar, 
                            width=17, height=1, font=("Arial", 10))
        btn_menu.grid(row=1, column=0, padx=5, pady=10)
        
        btn_salir = tk.Button(btn_panel, text="Salir", command=self.salir_del_sistema, 
                             width=12, height=1, font=("Arial", 10))
        btn_salir.grid(row=1, column=1, columnspan=2, padx=5)

    def abrir_imagen(self):
        path = filedialog.askopenfilename(
            title="Seleccionar imagen MNIST",
            filetypes=[("Imagenes PNG, JPG, JPEG", "*.png *.jpg *.jpeg"), ("Todos los archivos", "*.*")]
        )
        if path:
            try:
                self.selected_file = path
                self.current_image = Image.open(path)
                
                # Mostrar ruta del archivo
                self.path_label.config(text=f"Archivo: {os.path.basename(path)}")
                
                # Redimensionar para visualización (460x360)
                img_display = self.current_image.resize((460, 360), Image.Resampling.LANCZOS)
                photo = ImageTk.PhotoImage(img_display)
                
                self.image_label.config(image=photo, text="")
                self.image_label.photo = photo
                
                # Limpiar predicción previa
                self.resultado_label.config(text="Prediccion: pendiente", fg="black")
                self.prob_text.config(state="normal")
                self.prob_text.delete("1.0", tk.END)
                self.prob_text.insert(tk.END, "Esperando respuesta del servidor...")
                self.prob_text.config(state="disabled")
            except Exception as e:
                messagebox.showerror("Error", f"No se pudo abrir la imagen: {e}")

    def limpiar_pantalla(self):
        self.current_image = None
        self.selected_file = None
        self.image_label.config(image="", text="Ninguna imagen cargada")
        self.image_label.photo = None
        self.path_label.config(text="Archivo: ninguno")
        self.resultado_label.config(text="Prediccion: pendiente", fg="black")
        self.prob_text.config(state="normal")
        self.prob_text.delete("1.0", tk.END)
        self.prob_text.insert(tk.END, "Probabilidades por clase: pendiente")
        self.prob_text.config(state="disabled")

    def enviar_al_servidor(self):
        if not self.current_image:
            messagebox.showwarning("Aviso", "Primero debes cargar una imagen.")
            return
        
        # Procesamiento MNIST: 28x28, Escala de grises, Normalizado 0-1
        img_mnist = self.current_image.convert("L").resize((28, 28))
        pixels = list(img_mnist.getdata())
        normalized = [p / 255.0 for p in pixels]
        csv_data = ",".join([f"{p:.8f}" for p in normalized])
        
        self.resultado_label.config(text="Consultando al servidor...")
        self.prob_text.config(state="normal")
        self.prob_text.delete("1.0", tk.END)
        self.prob_text.insert(tk.END, "Esperando respuesta...")
        self.prob_text.config(state="disabled")
        
        self.tcp_client.send_message(f"MNIST_PREDICT|{csv_data}")

    def message_received(self, message):
        self.after(0, self._procesar_respuesta_seguro, message)

    def _procesar_respuesta_seguro(self, message):
        try:
            if message.startswith("MNIST_RESULT|"):
                parts = message.split("|")
                if len(parts) >= 4:
                    prediccion = parts[1]
                    probabilidades = parts[2].replace(",", "\n")
                    tiempo = parts[3]
                    
                    self.resultado_label.config(text=f"Prediccion: {prediccion}", fg="green")
                    self.prob_text.config(state="normal")
                    self.prob_text.delete("1.0", tk.END)
                    self.prob_text.insert(tk.END, f"Probabilidades por clase:\n{probabilidades}\n\nRespuesta en: {tiempo} ms")
                    self.prob_text.config(state="disabled")
            
            elif message.startswith("ERROR|"):
                self.resultado_label.config(text="Error en servidor", fg="red")
                self.prob_text.config(state="normal")
                self.prob_text.delete("1.0", tk.END)
                self.prob_text.insert(tk.END, f"Error del servidor: {message}")
                self.prob_text.config(state="disabled")
        except Exception as e:
            print(f"Error procesando respuesta: {e}")

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