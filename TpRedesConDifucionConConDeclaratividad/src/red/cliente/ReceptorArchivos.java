package red.cliente;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.PrivateKey;

public class ReceptorArchivos implements Runnable {

    private final int puerto;
    private final PrivateKey clavePrivada;
    private final Path carpetaRecibidos;

    public ReceptorArchivos(int puerto, PrivateKey clavePrivada, Path carpetaRecibidos) {
        this.puerto = puerto;
        this.clavePrivada = clavePrivada;
        this.carpetaRecibidos = carpetaRecibidos;
    }

    @Override
    public void run() {
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Receptor activo en puerto " + puerto);
            while (true) {
                Socket clienteSocket = servidor.accept();
                ProcesadorRecepcionCliente procesador = new ProcesadorRecepcionCliente(clienteSocket, clavePrivada, carpetaRecibidos);
                new Thread(procesador).start();
            }
        } catch (IOException e) {
            System.err.println("Error en receptor: " + e.getMessage());
        }
    }
}