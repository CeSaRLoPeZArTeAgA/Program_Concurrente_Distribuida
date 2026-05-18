import gzip
import struct
import os
from PIL import Image


def leer_imagenes_mnist(ruta_imagenes):
    with gzip.open(ruta_imagenes, "rb") as f:
        magic, num_images, rows, cols = struct.unpack(">IIII", f.read(16))

        if magic != 2051:
            raise ValueError("Archivo de imágenes inválido.")

        imagenes = []

        for _ in range(num_images):
            data = f.read(rows * cols)
            img = Image.frombytes("L", (cols, rows), data)
            imagenes.append(img)

        return imagenes, rows, cols


def leer_etiquetas_mnist(ruta_etiquetas):
    with gzip.open(ruta_etiquetas, "rb") as f:
        magic, num_labels = struct.unpack(">II", f.read(8))

        if magic != 2049:
            raise ValueError("Archivo de etiquetas inválido.")

        etiquetas = list(f.read(num_labels))

        return etiquetas


def guardar_como_png(ruta_imagenes, ruta_etiquetas, carpeta_salida, limite=100):
    imagenes, rows, cols = leer_imagenes_mnist(ruta_imagenes)
    etiquetas = leer_etiquetas_mnist(ruta_etiquetas)

    os.makedirs(carpeta_salida, exist_ok=True)

    n = min(limite, len(imagenes), len(etiquetas))

    for i in range(n):
        etiqueta = etiquetas[i]

        # Agrandar la imagen para verla mejor
        img = imagenes[i].resize((28, 28), Image.Resampling.NEAREST)

        nombre = f"muestra_{i:05d}_etiqueta_{etiqueta}.png"
        ruta_salida = os.path.join(carpeta_salida, nombre)

        img.save(ruta_salida)

    print(f"Se guardaron {n} imágenes en la carpeta: {carpeta_salida}")


if __name__ == "__main__":
    guardar_como_png(
        ruta_imagenes=r"D:\Concurrente_Proyecto\REV2\MNIST\Imagenes\t10k-images-idx3-ubyte.gz",
        ruta_etiquetas=r"D:\Concurrente_Proyecto\REV2\MNIST\Imagenes\t10k-labels-idx1-ubyte.gz",
        carpeta_salida=r"D:\Concurrente_Proyecto\REV2\MNIST\Imagenes\imagenes_mnist_png",
        limite=100
    )