package red.util;


public interface Constantes {
    int PUERTO_SERVIDOR = 8080;
    int PUERTO_RECEPTOR_CLIENTE = 8081;

    String ALGORITMO_RSA = "RSA";
    String ALGORITMO_AES = "AES";
    String TRANSFORMACION_RSA = "RSA/ECB/PKCS1Padding";
    String TRANSFORMACION_AES = "AES/CBC/PKCS5Padding";

    int TAMANO_CLAVE_RSA = 2048;
    int TAMANO_CLAVE_AES = 256;
    int TAMANO_IV_AES = 16;

    String COMANDO_REGISTRO = "registro";
    String COMANDO_ARCHIVO = "archivo";

    String CARPETA_ENVIADOS = "src/enviados/";
    String CARPETA_RECIBIDOS = "src/recibidos/";
    String CARPETA_CLAVES = "src/claves/";
}