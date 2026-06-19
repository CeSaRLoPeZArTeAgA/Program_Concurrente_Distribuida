package redesOk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

public class VentasIA {

    // =====================================================================
    // 1. ESTRUCTURAS DE DATOS (MÁQUINA DE ESTADOS E INVENTARIO)
    // =====================================================================
    static class Producto {
        double precio;
        int stock;
        String detalles;
        List<String> alias;

        Producto(double p, int s, String d, String... a) {
            precio = p;
            stock = s;
            detalles = d;
            alias = Arrays.asList(a);
        }
    }

    static class Sesion {
        String estado = "LIBRE";
        String productoTemp = null;
        String ultimoProductoVisto = null;
    }

    private static final Map<String, Producto> INVENTARIO = new LinkedHashMap<>();
    private static final Map<String, Sesion> SESIONES = new HashMap<>();
    private static final Map<String, List<String>> PEDIDOS = new HashMap<>();

    private static double ingresosTotales = 0.0;
    private static int ventasRealizadas = 0;

    // =====================================================================
    // 2. VARIABLES DEL MODELO NAIVE BAYES
    // =====================================================================
    private static final Map<String, Double> logProbPriori = new HashMap<>();
    private static final Map<String, Map<String, Double>> logProbPalabras = new HashMap<>();
    private static final Set<String> vocabulario = new HashSet<>();
    private static final Set<String> categorias = new HashSet<>();
    private static boolean modeloCargado = false;

    static {
        // Inicializar Inventario (Equivalente al Python)
        INVENTARIO.put("laptop",
                new Producto(3500.0, 5, "Laptop Gamer 16GB", "laptop", "pc", "computadora", "lactop", "lap"));
        INVENTARIO.put("rtx", new Producto(1400.0, 2, "NVIDIA RTX 4060", "rtx", "tarjeta", "grafica", "video", "gpu"));
        INVENTARIO.put("teclado", new Producto(150.0, 10, "Teclado Mecánico", "teclado", "teclas", "keyboard"));
        INVENTARIO.put("celular",
                new Producto(1200.0, 8, "Smartphone AMOLED", "celular", "telefono", "movil", "celu", "smartphone"));
        INVENTARIO.put("mando", new Producto(250.0, 15, "Mando inalámbrico", "mando", "control", "joystick"));
        INVENTARIO.put("audifonos", new Producto(120.0, 20, "Audífonos Bluetooth", "audifonos", "auriculares",
                "audifono", "audifnoso", "cascos", "headset"));

        cargarModeloJSON();
    }

    // =====================================================================
    // 3. LECTOR DE JSON NATIVO (Sin frameworks)
    // =====================================================================
    private static void cargarModeloJSON() {
        // Busca el archivo en la misma carpeta donde está esta clase Java
        try (InputStream is = VentasIA.class.getResourceAsStream("pesos_naive_bayes_ventas.json")) {
            if (is == null) {
                System.err.println("[ERROR] No se encontró pesos_naive_bayes_ventas.json en la carpeta src/redesOk");
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            String section = "";
            String currentCat = "";

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.contains("\"log_prob_priori\":")) {
                    section = "priori";
                    continue;
                }
                if (line.contains("\"log_prob_palabras\":")) {
                    section = "palabras";
                    continue;
                }
                if (line.contains("\"vocabulario\":")) {
                    section = "vocabulario";
                    continue;
                }
                if (line.contains("\"categorias\":")) {
                    section = "categorias";
                    continue;
                }

                if (section.equals("priori") && line.contains(":")) {
                    String[] parts = line.split(":");
                    String cat = parts[0].replaceAll("[\"{}, ]", "");
                    double val = Double.parseDouble(parts[1].replaceAll("[^0-9\\.\\-]", ""));
                    logProbPriori.put(cat, val);
                } else if (section.equals("palabras")) {
                    if (line.matches("\".+\": \\{")) {
                        currentCat = line.split(":")[0].replaceAll("[\"{}, ]", "");
                        logProbPalabras.putIfAbsent(currentCat, new HashMap<>());
                    } else if (line.contains(":") && !currentCat.isEmpty()) {
                        String[] parts = line.split(":");
                        String word = parts[0].replaceAll("[\"{}, ]", "");
                        double val = Double.parseDouble(parts[1].replaceAll("[^0-9\\.\\-]", ""));
                        logProbPalabras.get(currentCat).put(word, val);
                    }
                } else if (section.equals("vocabulario") && line.contains("\"")) {
                    if (line.contains("]")) {
                        section = "";
                    } else {
                        vocabulario.add(line.replaceAll("[\"{}, ]", ""));
                    }
                } else if (section.equals("categorias") && line.contains("\"")) {
                    if (line.contains("]")) {
                        section = "";
                    } else {
                        categorias.add(line.replaceAll("[\"{}, ]", ""));
                    }
                }
            }
            modeloCargado = true;
            System.out.println("[SISTEMA] Modelo IA Naive Bayes cargado exitosamente en Java.");
        } catch (Exception e) {
            System.err.println("[ERROR] Fallo al parsear JSON: " + e.getMessage());
        }
    }

    // =====================================================================
    // 4. MOTOR DE IA (Naive Bayes Multinomial)
    // =====================================================================
    private static String predecirIntencion(String texto) {
        if (!modeloCargado)
            return "informacion"; // Fallback por seguridad

        String t = normalizar(texto);
        List<String> stopwords = Arrays.asList("el", "la", "los", "las", "un", "una", "y", "o", "de", "en", "por",
                "para", "a", "mi", "tu", "su", "que", "es", "con");
        List<String> tokens = new ArrayList<>();
        for (String word : t.split(" ")) {
            if (!word.isBlank() && !stopwords.contains(word))
                tokens.add(word);
        }

        double bestScore = Double.NEGATIVE_INFINITY;
        String bestCat = "informacion";

        for (String cat : logProbPriori.keySet()) {
            double score = logProbPriori.get(cat);
            Map<String, Double> probs = logProbPalabras.get(cat);
            for (String token : tokens) {
                if (vocabulario.contains(token)) {
                    score += probs.getOrDefault(token, 0.0);
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestCat = cat;
            }
        }
        return bestCat;
    }

    private static String normalizar(String s) {
        String n = Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        n = n.replaceAll("[\\p{InCombiningDiacriticalMarks}\\p{Punct}]", ""); // Quita tildes y puntuación
        return n.trim();
    }

    // =====================================================================
    // 5. LÓGICA DE NEGOCIO, CONCURRENCIA Y EXPERIENCIA DE USUARIO
    // =====================================================================
    private static Sesion obtenerSesion(String dni) {
        synchronized (SESIONES) {
            return SESIONES.computeIfAbsent(dni, k -> new Sesion());
        }
    }

    private static String extraerProducto(String texto) {
        for (Map.Entry<String, Producto> entry : INVENTARIO.entrySet()) {
            for (String alias : entry.getValue().alias) {
                if (texto.contains(alias))
                    return entry.getKey();
            }
        }
        return null;
    }

    private static String mostrarCatalogo() {
        StringBuilder cat = new StringBuilder("📋 *CATÁLOGO DOG MESSENGER* 📋\n");
        for (Map.Entry<String, Producto> entry : INVENTARIO.entrySet()) {
            Producto p = entry.getValue();
            String stock = p.stock > 0 ? "Stock: " + p.stock : "❌ AGOTADO";
            cat.append("  🔹 ").append(p.detalles.toUpperCase()).append(" | S/. ").append(p.precio).append(" | ")
                    .append(stock).append("\n");
        }
        return cat.toString();
    }

    // 🚨 FIRMA PRINCIPAL: Requiere el 'sender' para tener memoria por cada usuario
    public static String responder(String sender, String consulta) {
        Sesion sesion = obtenerSesion(sender);
        String textoMin = normalizar(consulta);

        // 🛑 1. ESTADO DE CONFIRMACIÓN (INTERCEPCIÓN)
        if ("CONFIRMANDO_COMPRA".equals(sesion.estado)) {
            String prod = sesion.productoTemp;
            if (textoMin.contains("si") || textoMin.contains("ok") || textoMin.contains("claro")) {

                // ⚠️ BLOQUEO POR CONCURRENCIA (Protege la memoria de hilos concurrentes)
                synchronized (INVENTARIO) {
                    Producto p = INVENTARIO.get(prod);
                    if (p.stock <= 0) {
                        sesion.estado = "LIBRE";
                        return "🤖 [TIENDA]: Lo sentimos, otro cliente acaba de comprar la última unidad de '" + prod
                                + "'.";
                    }

                    p.stock -= 1;
                    int nroFac = 1000 + new Random().nextInt(9000);
                    double precio = p.precio;

                    PEDIDOS.computeIfAbsent(sender, k -> new ArrayList<>())
                            .add("FAC-" + nroFac + " (" + prod + ") -> Estado: Preparando envío");

                    ingresosTotales += precio;
                    ventasRealizadas += 1;

                    sesion.estado = "LIBRE";
                    sesion.productoTemp = null;
                    sesion.ultimoProductoVisto = null;

                    return "🤖 [TIENDA]: ¡Venta exitosa! 🎉 Comprobante generado: FAC-" + nroFac + ". Cobrado: S/."
                            + precio + ".\n💡 ¿Deseas comprar algo más o ver el seguimiento de tu pedido?";
                }
            } else if (textoMin.contains("no") || textoMin.contains("cancelar")) {
                sesion.estado = "LIBRE";
                sesion.productoTemp = null;
                return "🤖 [TIENDA]: Compra cancelada, no te preocupes.\n¿Quieres revisar el catálogo para ver otras opciones?";
            } else {
                return "🤖 [TIENDA]: Por favor, responde 'SÍ' para confirmar la compra o 'NO' para cancelar.";
            }
        }

        // 🛡️ 2. FILTROS HEURÍSTICOS (Reglas antes de la IA)
        if (textoMin.contains("catalogo") || textoMin.contains("productos")) {
            return "🤖 [TIENDA]: ¡Claro! Aquí tienes nuestras opciones:\n" + mostrarCatalogo()
                    + "\n¿De cuál deseas saber más o comprar?";
        }

        List<String> rechazos = Arrays.asList("no", "nada", "ninguno", "ya no");
        if (rechazos.contains(textoMin) || textoMin.startsWith("no quiero")) {
            sesion.ultimoProductoVisto = null;
            return "🤖 [TIENDA]: Entendido, no hay problema. Si cambias de opinión, aquí estaré. 💡 Puedes pedirme ver el catálogo en cualquier momento.";
        }

        // 🟢 3. PREDICCIÓN CON IA (Naive Bayes)
        String intencion = predecirIntencion(consulta);

        List<String> afirmaciones = Arrays.asList("si", "claro", "ok", "lo quiero");
        if (afirmaciones.contains(textoMin) && sesion.ultimoProductoVisto != null) {
            intencion = "comprar";
        }

        // 🟢 4. LÓGICA DE INTENCIONES
        switch (intencion) {
            case "informacion":
                String pInfo = extraerProducto(textoMin);
                if (pInfo != null) {
                    sesion.ultimoProductoVisto = pInfo;
                    Producto p = INVENTARIO.get(pInfo);
                    return "🤖 [TIENDA]: El '" + pInfo + "' cuesta S/." + p.precio + ". Detalle: " + p.detalles
                            + ".\n¿Te gustaría comprarlo?";
                }
                return "🤖 [TIENDA]: Aquí tienes lo que ofrecemos:\n" + mostrarCatalogo()
                        + "\n¿De cuál deseas saber más detalles o comprar?";

            case "comprar":
                String pCompra = extraerProducto(textoMin);
                if (pCompra == null && sesion.ultimoProductoVisto != null)
                    pCompra = sesion.ultimoProductoVisto;

                if (pCompra == null) {
                    return "🤖 [TIENDA]: ¡Genial! Aquí tienes nuestro catálogo de productos:\n" + mostrarCatalogo()
                            + "\n¿Qué producto en específico te interesa llevar?";
                }

                Producto prodToBuy = INVENTARIO.get(pCompra);
                if (prodToBuy.stock > 0) {
                    sesion.estado = "CONFIRMANDO_COMPRA";
                    sesion.productoTemp = pCompra;
                    return "🤖 [TIENDA]: Has seleccionado '" + prodToBuy.detalles + "'. El total a pagar es S/."
                            + prodToBuy.precio + ".\n⚠️ ¿Confirmas la transacción? (Escribe SÍ o NO)";
                } else {
                    return "🤖 [TIENDA]: Pucha, lo sentimos. El producto '" + pCompra + "' está agotado por ahora.";
                }

            case "seguimiento":
                List<String> misPedidos = PEDIDOS.get(sender);
                if (misPedidos == null || misPedidos.isEmpty()) {
                    return "🤖 [TIENDA]: Verifiqué en el sistema y actualmente no tienes pedidos activos.\n¿Deseas ver nuestro catálogo para realizar tu primera compra?";
                }
                StringBuilder respSeg = new StringBuilder("🤖 [TIENDA]: Aquí tienes el estado de tus compras:\n");
                for (String ped : misPedidos)
                    respSeg.append("   📦 ").append(ped).append("\n");
                return respSeg.toString() + "💡 ¿Puedo ayudarte con algo más?";

            case "reporte":
                return "📊 [REPORTE AUTOMÁTICO]\n   📈 Ventas totales hoy: " + ventasRealizadas
                        + "\n   💰 Ingresos netos: S/." + ingresosTotales;

            default:
                return "🤖 [TIENDA]: Entendido. Recuerda que puedes pedirme el catálogo, comprar un producto o revisar tus pedidos.";
        }
    }
}