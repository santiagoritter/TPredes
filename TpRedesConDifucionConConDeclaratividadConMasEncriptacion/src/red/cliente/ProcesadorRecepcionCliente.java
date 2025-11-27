package red.cliente;

import red.util.ArchivoUtil;
import red.util.CriptografiaUtil;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.security.PublicKey;

public class ProcesadorRecepcionCliente implements Runnable {

    private final Socket socket;
    private final SecretKey claveSesion;
    private final PublicKey clavePublicaServidor;
    private final Path carpetaRecibidos;

    public ProcesadorRecepcionCliente(Socket socket, SecretKey claveSesion, PublicKey clavePublicaServidor, Path carpetaRecibidos) {
        this.socket = socket;
        this.claveSesion = claveSesion;
        this.clavePublicaServidor = clavePublicaServidor;
        this.carpetaRecibidos = carpetaRecibidos;
    }

    @Override
    public void run() {
        try (DataInputStream entrada = new DataInputStream(socket.getInputStream())) {
            String emisor = entrada.readUTF();
            String nombreArchivo = entrada.readUTF();

            // 1. Recibir Firma Digital del Servidor
            int lenFirma = entrada.readInt();
            byte[] firmaServidor = new byte[lenFirma];
            entrada.readFully(firmaServidor);

            // 2. Recibir Archivo Encriptado
            long tamanoArchivo = entrada.readLong();
            byte[] archivoEncriptado = new byte[(int) tamanoArchivo];
            entrada.readFully(archivoEncriptado);

            // 3. Verificar Firma
            boolean firmaValida = CriptografiaUtil.verificarFirma(archivoEncriptado, firmaServidor, clavePublicaServidor);

            if (!firmaValida) {
                System.err.println("ALERTA DE SEGURIDAD: Firma del servidor inválida para " + nombreArchivo);
                return;
            }

            // 4. Desencriptar con Clave de Sesión (AES)
            byte[] archivoDesencriptado = CriptografiaUtil.desencriptarAESConIV(archivoEncriptado, claveSesion);

            Path rutaSalida = carpetaRecibidos.resolve(nombreArchivo);
            ArchivoUtil.escribirArchivo(rutaSalida, archivoDesencriptado);

            System.out.println("Archivo recibido y verificado: " + nombreArchivo + " (De: " + emisor + ")");

        } catch (Exception e) {
            System.err.println("Error recibiendo archivo: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}