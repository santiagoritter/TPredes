package red.servidor;

import red.shared.ClienteInfo;
import red.util.Constantes;
import red.util.CriptografiaUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;

public class ProcesadorPeticionServidor implements Runnable {

    private final Socket socket;
    private final RegistroClientes registro;

    public ProcesadorPeticionServidor(Socket socket, RegistroClientes registro) {
        this.socket = socket;
        this.registro = registro;
    }

    @Override
    public void run() {
        try (DataInputStream entrada = new DataInputStream(socket.getInputStream());
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            String tipoMensaje = entrada.readUTF();

            switch (tipoMensaje) {
                case Constantes.COMANDO_REGISTRO:
                    procesarRegistro(entrada, salida);
                    break;
                case Constantes.COMANDO_ARCHIVO:
                    procesarArchivo(entrada, salida);
                    break;
                default:
                    System.err.println("Comando desconocido: " + tipoMensaje);
            }

        } catch (Exception e) {
            System.err.println("Error con cliente: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void procesarRegistro(DataInputStream entrada, DataOutputStream salida) throws Exception {
        String nombre = entrada.readUTF();
        String clavePublicaBase64 = entrada.readUTF();
        String ip = socket.getInetAddress().getHostAddress();

        byte[] clavePublicaBytes = CriptografiaUtil.decodeBase64(clavePublicaBase64);
        PublicKey clavePublica = CriptografiaUtil.cargarClavePublica(clavePublicaBytes);

        registro.registrar(nombre, new ClienteInfo(ip, clavePublica));

        salida.writeUTF("Registro exitoso - Clave pública recibida");
    }

    private void procesarArchivo(DataInputStream entrada, DataOutputStream salida) throws Exception {
        String emisor = entrada.readUTF();
        String nombreArchivo = entrada.readUTF();
        String claveAESBase64 = entrada.readUTF();
        long tamanoArchivo = entrada.readLong();

        byte[] archivoEncriptado = new byte[(int) tamanoArchivo];
        entrada.readFully(archivoEncriptado);

        System.out.println("\n→ Archivo recibido de " + emisor + ": " + nombreArchivo);
        System.out.println("  Tamaño encriptado: " + tamanoArchivo + " bytes");

        salida.writeUTF("Servidor recibió el archivo encriptado");

        ServicioDifusion difusion = new ServicioDifusion(registro, emisor, nombreArchivo, claveAESBase64, archivoEncriptado);
        new Thread(difusion).start();
    }
}