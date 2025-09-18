import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente2 {
    private static final String SERVIDOR_IP = "localhost";  // Cambiar por IP del servidor
    private static final int PUERTO_SERVIDOR = 8080;
    private static final int MI_PUERTO = 9002;
    private static final String MI_CARPETA = "src/cliente2/";

    public static void main(String[] args) {
        new File(MI_CARPETA).mkdirs();

        new Thread(() -> escucharArchivos()).start();

        Scanner scanner = new Scanner(System.in);

        System.out.println("CLIENTE 2 INICIADO");
        System.out.println("Mi carpeta: " + MI_CARPETA);
        System.out.println("Escuchando en puerto: " + MI_PUERTO);

        while (true) {
            System.out.println("\nMENU CLIENTE 2");
            System.out.println("1. Enviar archivo a Cliente1");
            System.out.println("2. Ver archivos");
            System.out.println("3. Probar conexion con servidor");
            System.out.println("4. Salir");
            System.out.print("Opcion: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();

            switch (opcion) {
                case 1:
                    enviarArchivo(scanner);
                    break;
                case 2:
                    verArchivos();
                    break;
                case 3:
                    probarServidor();
                    break;
                case 4:
                    System.out.println("Hasta luego!");
                    return;
                default:
                    System.out.println("Opcion no valida");
            }
        }
    }

    private static void escucharArchivos() {
        try (ServerSocket servidor = new ServerSocket(MI_PUERTO)) {
            System.out.println("Esperando archivos de Cliente1...\n");

            while (true) {
                try (Socket cliente = servidor.accept();
                     DataInputStream entrada = new DataInputStream(cliente.getInputStream())) {

                    String nombreArchivo = entrada.readUTF();
                    long tamañoArchivo = entrada.readLong();

                    System.out.println("\nRecibiendo archivo: " + nombreArchivo + " (" + tamañoArchivo + " bytes)");

                    File archivoDestino = new File(MI_CARPETA + nombreArchivo);

                    try (FileOutputStream salida = new FileOutputStream(archivoDestino)) {
                        byte[] buffer = new byte[4096];
                        long bytesRecibidos = 0;
                        int bytesLeidos;

                        while (bytesRecibidos < tamañoArchivo) {
                            bytesLeidos = entrada.read(buffer, 0,
                                    (int) Math.min(buffer.length, tamañoArchivo - bytesRecibidos));

                            if (bytesLeidos == -1) break;

                            salida.write(buffer, 0, bytesLeidos);
                            bytesRecibidos += bytesLeidos;

                            int progreso = (int) ((bytesRecibidos * 100) / tamañoArchivo);
                            if (progreso % 25 == 0) {
                                System.out.println("Recibido: " + progreso + "%");
                            }
                        }

                        System.out.println("Archivo recibido correctamente: " + nombreArchivo);
                    }
                } catch (IOException e) {
                    System.err.println("Error recibiendo archivo: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error iniciando servidor de archivos: " + e.getMessage());
        }
    }

    private static void enviarArchivo(Scanner scanner) {
        System.out.print("IP de Cliente1 (o localhost): ");
        String ipCliente1 = scanner.nextLine().trim();
        if (ipCliente1.isEmpty()) ipCliente1 = "localhost";

        System.out.print("Nombre del archivo: ");
        String nombreArchivo = scanner.nextLine().trim();

        File archivo = new File(MI_CARPETA + nombreArchivo);
        if (!archivo.exists()) {
            System.out.println("Archivo no encontrado en " + MI_CARPETA);
            return;
        }

        try (Socket socket = new Socket(ipCliente1, 9001);  // Puerto de Cliente1
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             FileInputStream archivoEntrada = new FileInputStream(archivo)) {

            System.out.println("Conectado a Cliente1, enviando: " + nombreArchivo);

            salida.writeUTF(archivo.getName());
            salida.writeLong(archivo.length());

            byte[] buffer = new byte[4096];
            long bytesEnviados = 0;
            int bytesLeidos;

            while ((bytesLeidos = archivoEntrada.read(buffer)) != -1) {
                salida.write(buffer, 0, bytesLeidos);
                bytesEnviados += bytesLeidos;

                int progreso = (int) ((bytesEnviados * 100) / archivo.length());
                if (progreso % 25 == 0) {
                    System.out.println("Enviado: " + progreso + "%");
                }
            }

            System.out.println("Archivo enviado correctamente a Cliente1");

        } catch (IOException e) {
            System.err.println("Error enviando archivo: " + e.getMessage());
            System.err.println("Verifica que Cliente1 este ejecutandose");
        }
    }

    private static void verArchivos() {
        System.out.println("\nArchivos en " + MI_CARPETA + ":");
        File carpeta = new File(MI_CARPETA);
        File[] archivos = carpeta.listFiles();

        if (archivos != null && archivos.length > 0) {
            for (File archivo : archivos) {
                if (archivo.isFile()) {
                    System.out.println("  " + archivo.getName() + " (" + archivo.length() + " bytes)");
                }
            }
        } else {
            System.out.println("  No hay archivos");
        }
    }

    private static void probarServidor() {
        try (Socket socket = new Socket(SERVIDOR_IP, PUERTO_SERVIDOR);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

            salida.writeUTF("Hola desde Cliente2");
            String respuesta = entrada.readUTF();
            System.out.println("Respuesta del servidor: " + respuesta);

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor: " + e.getMessage());
        }
    }
}