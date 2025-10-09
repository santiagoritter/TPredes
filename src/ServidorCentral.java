import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorCentral {
    private static final int PUERTO = 8080;
    private static final Map<String, String> clientes = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("SERVIDOR CENTRAL ACTIVO EN PUERTO " + PUERTO);
            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> manejarCliente(cliente)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en servidor: " + e.getMessage());
        }
    }

    private static void manejarCliente(Socket socket) {
        try (DataInputStream entrada = new DataInputStream(socket.getInputStream());
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            String tipoMensaje = entrada.readUTF();

            if (tipoMensaje.equals("registro")) {
                String nombre = entrada.readUTF();
                String ip = socket.getInetAddress().getHostAddress();
                synchronized (clientes) {
                    clientes.put(nombre, ip);
                }
                System.out.println("Cliente registrado: " + nombre + " -> " + ip);
                salida.writeUTF("Registro exitoso");
            }

            else if (tipoMensaje.equals("archivo")) {
                String emisor = entrada.readUTF();
                String nombreArchivo = entrada.readUTF();
                long tamanoArchivo = entrada.readLong();

                File archivoTemporal = new File("src/servidor_temporal/" + nombreArchivo);
                archivoTemporal.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(archivoTemporal)) {
                    byte[] buffer = new byte[4096];
                    long bytesRecibidos = 0;
                    int leidos;

                    while (bytesRecibidos < tamanoArchivo) {
                        leidos = entrada.read(buffer, 0, (int)Math.min(buffer.length, tamanoArchivo - bytesRecibidos));
                        if (leidos == -1) break;
                        fos.write(buffer, 0, leidos);
                        bytesRecibidos += leidos;
                    }
                }

                System.out.println("Archivo recibido de " + emisor + ": " + nombreArchivo);
                salida.writeUTF("Servidor recibio el archivo correctamente");
                difundirArchivo(emisor, archivoTemporal);

            }

        } catch (IOException e) {
            System.err.println("Error con cliente: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void difundirArchivo(String emisor, File archivo) {
        byte[] buffer = new byte[4096];
        List<String> fallidos = new ArrayList<>();

        synchronized (clientes) {
            for (Map.Entry<String, String> entry : clientes.entrySet()) {
                String nombre = entry.getKey();
                String ip = entry.getValue();

                if (nombre.equals(emisor)) continue;

                try (Socket socket = new Socket(ip, 8081);
                     DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                     FileInputStream fis = new FileInputStream(archivo)) {

                    salida.writeUTF(emisor);
                    salida.writeUTF(archivo.getName());
                    salida.writeLong(archivo.length());

                    int leidos;
                    while ((leidos = fis.read(buffer)) != -1) {
                        salida.write(buffer, 0, leidos);
                    }

                    System.out.println("Archivo reenviado a " + nombre + " (" + ip + ")");

                } catch (IOException e) {
                    fallidos.add(nombre);
                }
            }
        }

        if (!fallidos.isEmpty()) {
            System.out.println("No se pudo enviar a: " + String.join(", ", fallidos));
        }
    }
}
