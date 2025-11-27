package red.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ManejadorConexionesServidor {
    private final int puerto;
    private final RegistroClientes registro;

    public ManejadorConexionesServidor(int puerto, RegistroClientes registro) {
        this.puerto = puerto;
        this.registro = registro;
    }

    public void iniciar() {
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            while (true) {
                Socket clienteSocket = servidor.accept();
                ProcesadorPeticionServidor procesador = new ProcesadorPeticionServidor(clienteSocket, registro);
                new Thread(procesador).start();
            }
        } catch (IOException e) {
            System.err.println("Error en servidor: " + e.getMessage());
        }
    }
}