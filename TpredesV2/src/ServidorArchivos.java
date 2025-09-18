import java.io.*;
import java.net.*;

public class ServidorArchivos {
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("SERVIDOR P2P INICIADO");
            System.out.println("Puerto: " + PUERTO);
            System.out.println("Esperando clientes...\n");

            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("Cliente conectado: " + cliente.getInetAddress());

                new Thread(() -> {
                    try (DataInputStream entrada = new DataInputStream(cliente.getInputStream());
                         DataOutputStream salida = new DataOutputStream(cliente.getOutputStream())) {

                        String mensaje = entrada.readUTF();
                        System.out.println("Recibido: " + mensaje);

                        salida.writeUTF("Servidor activo - Conexion establecida");

                    } catch (IOException e) {
                        System.out.println("Cliente desconectado");
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Error en servidor: " + e.getMessage());
        }
    }
}
