import java.io.*;
import java.net.*;

public class ServidorArchivos {
    private static final int PUERTO = 8080;
    private static final String CARPETA_SERVIDOR = ".idea/carpeta_servidor/";

    public static void main(String[] args) {
        // Crear carpeta del servidor
        File carpetaServidor = new File(CARPETA_SERVIDOR);
        carpetaServidor.mkdirs();

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("🟢 SERVIDOR INICIADO");
            System.out.println("📂 Carpeta del servidor: " + carpetaServidor.getAbsolutePath());
            System.out.println("🔌 Puerto: " + PUERTO);
            System.out.println("⏳ Esperando archivos...\n");

            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("👤 Cliente conectado: " + cliente.getInetAddress());
                new Thread(() -> recibirArchivo(cliente)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Error en servidor: " + e.getMessage());
        }
    }

    private static void recibirArchivo(Socket cliente) {
        try (DataInputStream entrada = new DataInputStream(cliente.getInputStream());
             DataOutputStream salida = new DataOutputStream(cliente.getOutputStream())) {

            String nombreArchivo = entrada.readUTF();
            long tamañoArchivo = entrada.readLong();

            System.out.println("📥 Recibiendo: " + nombreArchivo + " (" + tamañoArchivo + " bytes)");

            File archivoDestino = new File(CARPETA_SERVIDOR + nombreArchivo);
            System.out.println("💾 Guardando en: " + archivoDestino.getAbsolutePath());

            try (FileOutputStream archivoSalida = new FileOutputStream(archivoDestino)) {
                byte[] buffer = new byte[4096];
                long bytesRecibidos = 0;
                int bytesLeidos;

                while (bytesRecibidos < tamañoArchivo) {
                    bytesLeidos = entrada.read(buffer, 0,
                            (int) Math.min(buffer.length, tamañoArchivo - bytesRecibidos));

                    if (bytesLeidos == -1) break;

                    archivoSalida.write(buffer, 0, bytesLeidos);
                    bytesRecibidos += bytesLeidos;

                    int progreso = (int) ((bytesRecibidos * 100) / tamañoArchivo);
                    if (progreso % 25 == 0) {
                        System.out.println("📊 Progreso: " + progreso + "%");
                    }
                }

                System.out.println("✅ Archivo guardado: " + archivoDestino.getAbsolutePath());
                System.out.println("📁 Ve a buscar tu archivo en: " + new File(CARPETA_SERVIDOR).getAbsolutePath());
                salida.writeUTF("✅ Archivo recibido correctamente");
            }

        } catch (IOException e) {
            System.err.println("❌ Error recibiendo archivo: " + e.getMessage());
        } finally {
            try {
                cliente.close();
                System.out.println("🔌 Cliente desconectado\n");
            } catch (IOException e) {
                System.err.println("❌ Error cerrando conexión");
            }
        }
    }
}