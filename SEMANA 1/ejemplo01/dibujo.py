import pandas as pd
import matplotlib.pyplot as plt

# Leer datos
df = pd.read_csv("resultados.csv")

H = df["H"]
T = df["Tiempo"]

# Índice del mínimo
idx_min = T.idxmin()

# Graficar línea general
plt.plot(H, T, marker='o')

# Marcar el mínimo (sin especificar color global, solo este punto)
plt.scatter(H[idx_min], T[idx_min], color='red', zorder=5)

# Etiquetar cada punto con H
for i in range(len(H)):
    plt.text(H[i], T[i], str(H[i]), fontsize=9, ha='right', va='bottom')

plt.xlabel("Número de hilos (H)")
plt.ylabel("Tiempo promedio (s)")
plt.title("Escalabilidad")
plt.grid()

plt.savefig("grafico.png")
plt.show()