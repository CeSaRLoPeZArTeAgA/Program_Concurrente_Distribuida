package tetris;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


//utilidad para cargar imágenes desde la carpeta data.
//esta clase centraliza la lectura de imagenes para que otras clases no tengan
//que repetir la ruta base ni el manejo de errores
public class ImageLoader {

    //carga una imagen ubicada dentro de la carpeta data
    public static BufferedImage loadImage(String path) {
        try {
            //construye la ruta real del archivo y lo lee con ImageIO
            return ImageIO.read(new File("data" + path));
        } catch (IOException e) {
            //se convierte IOException en RuntimeException para simplificar el uso del metodo
            throw new RuntimeException("No se pudo cargar la imagen: data" + path, e);
        }
    }
}
