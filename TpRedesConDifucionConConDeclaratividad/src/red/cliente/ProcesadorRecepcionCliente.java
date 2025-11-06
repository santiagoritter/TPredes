package red.cliente;

import red.util.ArchivoUtil;
import red.util.CriptografiaUtil;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.security.PrivateKey;

public class ProcesadorRecepcionCliente implements Runnable {

    private final Socket socket;
    private final PrivateKey clavePrivada;
    private final Path carpetaRecibidos;

    public ProcesadorRecepcionCliente(Socket socket, PrivateKey clavePrivada, Path carpetaRecibidos) {
        this.socket = socket;
        this.clavePrivada = clavePrivada;
        this.carpetaRecibidos = carpetaRecibidos;
    }

    @Override
    public void run() {
        try (DataInputStream entrada = new DataInputStream(socket.getInputStream())) {
            String emisor = entrada.readUTF();
            String nombreArchivo = entrada.readUTF();

            int longitudClave = entrada.readInt();
            byte[] claveAESEncriptada = new byte[longitudClave];
            entrada.readFully(claveAESEncriptada);

            long tamanoArchivo = entrada.readLong();
            byte[] archivoEncriptado = new byte[(int) tamanoArchivo];
            entrada.readFully(archivoEncriptado);

            byte[] claveAESBytes = CriptografiaUtil.desencriptarRSA(claveAESEncriptada, clavePrivada);
            SecretKey claveAES = CriptografiaUtil.convertirBytesAClaveAES(claveAESBytes);

            byte[] archivoDesencriptado = CriptografiaUtil.desencriptarAESConIV(archivoEncriptado, claveAES);

            Path rutaSalida = carpetaRecibidos.resolve(nombreArchivo);
            ArchivoUtil.escribirArchivo(rutaSalida, archivoDesencriptado);

            System.out.println("✓ Archivo recibido y desencriptado de " + emisor + ": " + nombreArchivo);

        } catch (Exception e) {
            System.err.println("Error recibiendo archivo: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}