import json
import random
import datetime
import threading
import unicodedata

# =====================================================================
# INICIALIZACIÓN DEL MODELO DE IA (Carga de pesos locales)
# =====================================================================
# [NO MODIFICAR] Este bloque carga la "mente" del bot.
try:
    with open("pesos_naive_bayes_ventas.json", "r", encoding="utf-8") as f:
        pesos_ia = json.load(f)
except FileNotFoundError:
    print("[ERROR CRÍTICO] No se encontró el archivo 'pesos_naive_bayes_ventas.json'.")
    print("Asegúrate de que esté en la misma carpeta que este script.")
    exit()

def predecir_intencion(texto):
    """[NO MODIFICAR] Motor de inferencia Naive Bayes puro."""
    texto = unicodedata.normalize('NFKD', texto).encode('ASCII', 'ignore').decode('utf-8').lower()
    for char in [',', '.', '¿', '?', '¡', '!', ':', ';', '"', '-']:
        texto = texto.replace(char, ' ')
    
    stopwords = {"el", "la", "los", "las", "un", "una", "y", "o", "de", "en", "por", "para", "a", "mi", "tu", "su", "que", "es", "con"}
    tokens = [t for t in texto.split() if t not in stopwords]
    
    mejores_scores = {}
    for cat in pesos_ia["categorias"]:
        score = pesos_ia["log_prob_priori"][cat]
        for token in tokens:
            if token in pesos_ia["vocabulario"]:
                score += pesos_ia["log_prob_palabras"][cat].get(token, 0)
        mejores_scores[cat] = score
    return max(mejores_scores, key=mejores_scores.get)


# =====================================================================
# 1. BASE DE DATOS Y MÁQUINA DE ESTADOS
# =====================================================================
# [MODIFICABLE] Esta es la base de datos en memoria. 
# Si el encargado de infraestructura desea usar SQLite o archivos, 
# puede reemplazar las lecturas/escrituras a este diccionario por consultas SQL.

base_de_datos = {
    "inventario": {
        "laptop": {"precio": 3500.0, "stock": 5, "detalles": "Laptop Gamer 16GB", "alias": ["laptop", "pc", "computadora", "lactop", "lap"]},
        "rtx": {"precio": 1400.0, "stock": 2, "detalles": "NVIDIA RTX 4060", "alias": ["rtx", "tarjeta", "grafica", "video", "gpu"]},
        "teclado": {"precio": 150.0, "stock": 10, "detalles": "Teclado Mecánico", "alias": ["teclado", "teclas", "keyboard"]},
        "celular": {"precio": 1200.0, "stock": 8, "detalles": "Smartphone AMOLED", "alias": ["celular", "telefono", "movil", "celu", "smartphone"]},
        "mando": {"precio": 250.0, "stock": 15, "detalles": "Mando inalámbrico", "alias": ["mando", "control", "joystick"]},
        "audifonos": {"precio": 120.0, "stock": 20, "detalles": "Audífonos Bluetooth", "alias": ["audifonos", "auriculares", "audifono", "audifnoso", "cascos", "headset"]}
    },
    "clientes": {},
    "pedidos": {}, 
    "metricas": {"ingresos_totales": 0.0, "ventas_realizadas": 0},
    "sesiones": {} 
}

# [MUY IMPORTANTE] Candado para Concurrencia. 
# Usar al modificar stock para evitar Race Conditions entre hilos de Sockets.
lock_inventario = threading.Lock()

# =====================================================================
# 2. FUNCIONES DE APOYO (UX y Extracción)
# =====================================================================
# [NO MODIFICAR] Funciones vitales para la lógica de extracción de entidades.

def extraer_producto(texto):
    texto_min = texto.lower()
    for prod_key, info in base_de_datos["inventario"].items():
        for alias in info.get("alias", [prod_key]):
            if alias in texto_min:
                return prod_key 
    return None

def mostrar_catalogo():
    cat = "--- CATALOGO DOG MESSENGER ---\n"
    for key, info in base_de_datos["inventario"].items():
        estado_stock = f"Stock: {info['stock']}" if info['stock'] > 0 else "[AGOTADO]"
        cat += f"  > {info['detalles'].upper()} | S/. {info['precio']} | {estado_stock}\n"
    return cat

def obtener_sesion(dni):
    with lock_inventario:
        if dni not in base_de_datos["sesiones"]:
            base_de_datos["sesiones"][dni] = {
                "estado": "LIBRE", 
                "producto_temp": None,
                "ultimo_producto_visto": None 
            }
        return base_de_datos["sesiones"][dni]

# =====================================================================
# 3. LOGICA DE NEGOCIO (Filtros Inteligentes + IA)
# =====================================================================
# [NO MODIFICAR] Este es el cerebro de negocio. Maneja la experiencia del 
# usuario (UX), la memoria a corto plazo y las validaciones.

def procesar_intencion(intencion, texto_cliente, dni_actual):
    sesion = obtener_sesion(dni_actual)
    texto_min = texto_cliente.lower().strip()

    # 1. ESTADO DE CONFIRMACION
    if sesion["estado"] == "CONFIRMANDO_COMPRA":
        prod = sesion["producto_temp"]
        if "si" in texto_min or "sí" in texto_min or "ok" in texto_min or "claro" in texto_min:
            
            # BLOQUEO POR CONCURRENCIA (Evita ventas dobles si hay 20 sockets)
            with lock_inventario:
                info = base_de_datos["inventario"][prod]
                if info["stock"] <= 0:
                    sesion["estado"] = "LIBRE"
                    return f"[TIENDA]: Lo sentimos, otro cliente acaba de comprar la ultima unidad de '{prod}'."
                
                info["stock"] -= 1
                nro_fac = f"FAC-{random.randint(1000, 9999)}"
                precio = info["precio"]
                
                if dni_actual not in base_de_datos["pedidos"]:
                    base_de_datos["pedidos"][dni_actual] = []
                
                base_de_datos["pedidos"][dni_actual].append({
                    "comprobante": nro_fac, "producto": prod, "estado": "Preparando envio"
                })
                base_de_datos["metricas"]["ingresos_totales"] += precio
                base_de_datos["metricas"]["ventas_realizadas"] += 1
                
                sesion["estado"] = "LIBRE"
                sesion["producto_temp"] = None
                sesion["ultimo_producto_visto"] = None
                
            return f"[TIENDA]: Venta exitosa! Comprobante generado: {nro_fac}. Total cobrado: S/.{precio}.\n[TIENDA]: Deseas comprar algo mas o ver el seguimiento de tu pedido?"
        
        elif "no" in texto_min or "cancelar" in texto_min:
            sesion["estado"] = "LIBRE"
            sesion["producto_temp"] = None
            return "[TIENDA]: Compra cancelada, no te preocupes.\nQuieres revisar el catalogo para ver otras opciones?"
        else:
            return "[TIENDA]: Por favor, responde 'SI' para confirmar la compra o 'NO' para cancelar."

    # 2. FILTROS HEURISTICOS 
    if "catalogo" in texto_min or "catálogo" in texto_min or "productos" in texto_min:
        return f"[TIENDA]: Claro! Aqui tienes nuestras opciones:\n{mostrar_catalogo()}\nDe cual deseas saber mas o comprar?"

    rechazos_puros = ["no", "nada", "no quiero nada", "ninguno", "ya no", "no gracias"]
    if texto_min in rechazos_puros or texto_min.startswith("no quiero"):
        sesion["ultimo_producto_visto"] = None
        return "[TIENDA]: Entendido, no hay problema. Si cambias de opinion, aqui estare. Puedes pedirme ver el catalogo."

    afirmaciones_puras = ["si", "sí", "claro", "ok", "lo quiero", "quiero comprarlo"]
    if texto_min in afirmaciones_puras and sesion["ultimo_producto_visto"]:
        intencion = "comprar"

    # 3. FLUJO NORMAL DE IA 
    if intencion == "informacion":
        prod = extraer_producto(texto_min)
        if prod:
            sesion["ultimo_producto_visto"] = prod 
            info = base_de_datos["inventario"][prod]
            return f"[TIENDA]: El '{prod}' cuesta S/.{info['precio']}. Detalle: {info['detalles']}.\nTe gustaria comprarlo?"
        return f"[TIENDA]: Aqui tienes lo que ofrecemos:\n{mostrar_catalogo()}\nDe cual deseas saber mas detalles o comprar?"

    elif intencion == "comprar":
        prod = extraer_producto(texto_min)
        if not prod and sesion.get("ultimo_producto_visto"):
            prod = sesion["ultimo_producto_visto"]

        if not prod:
            return f"[TIENDA]: Genial! Aqui tienes nuestro catalogo de productos:\n{mostrar_catalogo()}\nQue producto en especifico te interesa llevar?"
        
        info = base_de_datos["inventario"][prod]
        if info["stock"] > 0:
            sesion["estado"] = "CONFIRMANDO_COMPRA"
            sesion["producto_temp"] = prod
            return f"[TIENDA]: Has seleccionado '{info['detalles']}'. El total a pagar es S/.{info['precio']}.\n[SISTEMA]: Confirmas la transaccion? (Escribe SI o NO)"
        else:
            return f"[TIENDA]: Pucha, lo sentimos. El producto '{prod}' esta agotado por ahora."

    elif intencion == "seguimiento":
        mis_pedidos = base_de_datos["pedidos"].get(dni_actual, [])
        if not mis_pedidos:
            return "[TIENDA]: Verifique en el sistema y actualmente no tienes pedidos activos.\nDeseas ver nuestro catalogo para realizar tu primera compra?"
        
        respuesta = "[TIENDA]: Aqui tienes el estado de tus compras:\n"
        for p in mis_pedidos:
            respuesta += f"   > {p['comprobante']} ({p['producto']}) -> Estado: {p['estado']}\n"
        return respuesta + "Puedo ayudarte con algo mas?"

    elif intencion == "reporte":
        ingresos = base_de_datos["metricas"]["ingresos_totales"]
        ventas = base_de_datos["metricas"]["ventas_realizadas"]
        fecha = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
        return f"[REPORTE AUTOMATICO - {fecha}]\n   > Ventas totales hoy: {ventas}\n   > Ingresos netos: S/.{ingresos}\nDesea realizar otra operacion administrativa?"

    else:
        return "[TIENDA]: Entendido. Recuerda que puedes pedirme el catalogo, comprar un producto o revisar tus pedidos."


# =====================================================================
# 4. INTERFAZ DE CONSOLA (Zona de Eliminación para Sockets)
# =====================================================================
# [ZONA ELIMINABLE] 
# Para la integracion final del proyecto, BORRAR esta funcion completa.
# Reemplazar por el bucle "while True" que recibe los mensajes del "ServerSocket".

def iniciar_consola():
    print("="*60)
    print("NODO DE VENTAS DOG MESSENGER (Entorno Local)")
    print("="*60)
    
    dni_usuario = input("[SISTEMA] Ingrese su ID/DNI para iniciar: ")
    
    if dni_usuario not in base_de_datos["clientes"]:
        nombre = input("[SISTEMA] Usuario nuevo. Ingrese su nombre: ")
        base_de_datos["clientes"][dni_usuario] = {"nombre": nombre, "registrado_el": str(datetime.date.today())}
        print(f"[SISTEMA] Registro exitoso. Bienvenido, {nombre}!\n")
    else:
        nombre = base_de_datos["clientes"][dni_usuario]["nombre"]
        print(f"[SISTEMA] Sesion iniciada. Hola de nuevo, {nombre}!\n")

    print("[BOT]: Hola, soy el asistente virtual. Puedes pedirme ver el catalogo, consultar precios o comprar.")
    print("       (Escribe 'salir' para desconectar)\n")

    while True:
        entrada = input(f"[{nombre}]: ")
        if entrada.lower() == 'salir':
            print("[SISTEMA] Conexion cerrada.")
            break
            
        estado_actual = obtener_sesion(dni_usuario)["estado"]
        
        if estado_actual == "LIBRE":
            intencion_detectada = predecir_intencion(entrada) 
            print(f"  (Debug IA: Intencion -> {intencion_detectada.upper()})")
        else:
            intencion_detectada = "esperando_confirmacion"
            print(f"  (Debug FSM: Esperando SI/NO)")
        
        # [PUNTO DE ENTRADA] 
        # En la version de sockets, aqui se llama a procesar_intencion y 
        # se hace un socket.send(respuesta_final.encode())
        respuesta_final = procesar_intencion(intencion_detectada, entrada, dni_usuario)
        print(respuesta_final + "\n")

if __name__ == "__main__":
    iniciar_consola()