package red.cliente;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.PublicKey;

public class ReceptorArchivos implements Runnable {

    private final int puerto;
    private final SecretKey claveSesion;
    private final PublicKey clavePublicaServidor;
    private final Path carpetaRecibidos;

    public ReceptorArchivos(int puerto, SecretKey claveSesion, PublicKey clavePublicaServidor, Path carpetaRecibidos) {
        this.puerto = puerto;
        this.claveSesion = claveSesion;
        this.clavePublicaServidor = clavePublicaServidor;
        this.carpetaRecibidos = carpetaRecibidos;
    }

    @Override
    public void run() {
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Receptor activo en puerto " + puerto);
            while (true) {
                Socket clienteSocket = servidor.accept();
                ProcesadorRecepcionCliente procesador = new ProcesadorRecepcionCliente(clienteSocket, claveSesion, clavePublicaServidor, carpetaRecibidos);
                new Thread(procesador).start();
            }
        } catch (IOException e) {
            System.err.println("Error en receptor: " + e.getMessage());
        }
    }
}