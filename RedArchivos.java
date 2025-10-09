import java.io.*;
import java.net.*;
import java.util.*;

public class RedArchivos {
    private static final int PUERTO = 8080;
    private static String nombreUsuario;
    private static final String CARPETA_ENVIADOS = "src/enviados/";
    private static final String CARPETA_RECIBIDOS = "src/recibidos/";

    public static void main(String[] args) {
        File enviados = new File(CARPETA_ENVIADOS);
        File recibidos = new File(CARPETA_RECIBIDOS);
        enviados.mkdirs();
        recibidos.mkdirs();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese su nombre: ");
        nombreUsuario = scanner.nextLine().trim();

        Thread servidor = new Thread(() -> iniciarServidor());
        servidor.start();

        while (true) {
            System.out.println("\nMENU");
            System.out.println("1. Enviar archivo");
            System.out.println("2. Ver enviados");
            System.out.println("3. Ver recibidos");
            System.out.println("4. Salir");
            System.out.print("Opcion: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();

            if (opcion == 1) {
                enviarArchivo(scanner);
            } else if (opcion == 2) {
                listarArchivos(CARPETA_ENVIADOS);
            } else if (opcion == 3) {
                listarArchivos(CARPETA_RECIBIDOS);
            } else if (opcion == 4) {
                System.out.println("Fin del programa");
                System.exit(0);
            } else {
                System.out.println("Opcion no valida");
            }
        }
    }

    private static void iniciarServidor() {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor activo en puerto " + PUERTO);
            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> recibirArchivo(cliente)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en servidor: " + e.getMessage());
        }
    }

    private static void recibirArchivo(Socket cliente) {
        try (DataInputStream entrada = new DataInputStream(cliente.getInputStream());
             DataOutputStream salida = new DataOutputStream(cliente.getOutputStream())) {

            String nombreRemitente = entrada.readUTF();
            String nombreArchivo = entrada.readUTF();
            long tamanoArchivo = entrada.readLong();

            File archivoDestino = new File(CARPETA_RECIBIDOS + nombreArchivo);
            try (FileOutputStream archivoSalida = new FileOutputStream(archivoDestino)) {
                byte[] buffer = new byte[4096];
                long bytesRecibidos = 0;
                int bytesLeidos;

                while (bytesRecibidos < tamanoArchivo) {
                    bytesLeidos = entrada.read(buffer, 0, (int) Math.min(buffer.length, tamanoArchivo - bytesRecibidos));
                    if (bytesLeidos == -1) break;
                    archivoSalida.write(buffer, 0, bytesLeidos);
                    bytesRecibidos += bytesLeidos;
                }
            }

            System.out.println("Archivo recibido de " + nombreRemitente + ": " + nombreArchivo);
            salida.writeUTF("Archivo recibido correctamente");

        } catch (IOException e) {
            System.err.println("Error recibiendo archivo: " + e.getMessage());
        } finally {
            try {
                cliente.close();
            } catch (IOException e) {
                System.err.println("Error cerrando conexion");
            }
        }
    }

    private static void enviarArchivo(Scanner scanner) {
        System.out.print("Nombre del archivo a enviar: ");
        String nombreArchivo = scanner.nextLine().trim();

        File archivo = new File(CARPETA_ENVIADOS + nombreArchivo);
        if (!archivo.exists()) {
            System.err.println("El archivo no existe en " + CARPETA_ENVIADOS);
            return;
        }

        System.out.print("Ingrese las IPs de destino separadas por coma: ");
        String linea = scanner.nextLine().trim();
        String[] ips = linea.split(",");

        for (String ip : ips) {
            String ipDestino = ip.trim();
            if (ipDestino.isEmpty()) continue;

            new Thread(() -> {
                try (Socket socket = new Socket(ipDestino, PUERTO);
                     DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                     DataInputStream entrada = new DataInputStream(socket.getInputStream());
                     FileInputStream archivoEntrada = new FileInputStream(archivo)) {

                    salida.writeUTF(nombreUsuario);
                    salida.writeUTF(archivo.getName());
                    salida.writeLong(archivo.length());

                    byte[] buffer = new byte[4096];
                    int bytesLeidos;
                    while ((bytesLeidos = archivoEntrada.read(buffer)) != -1) {
                        salida.write(buffer, 0, bytesLeidos);
                    }

                    String confirmacion = entrada.readUTF();
                    System.out.println("Enviado a " + ipDestino + ": " + confirmacion);

                } catch (IOException e) {
                    System.err.println("No se pudo enviar a " + ipDestino + ": " + e.getMessage());
                }
            }).start();
        }
    }

    private static void listarArchivos(String carpetaRuta) {
        File carpeta = new File(carpetaRuta);
        File[] archivos = carpeta.listFiles();
        if (archivos == null || archivos.length == 0) {
            System.out.println("No hay archivos en " + carpetaRuta);
            return;
        }
        System.out.println("Archivos en " + carpetaRuta + ":");
        for (File archivo : archivos) {
            if (archivo.isFile()) {
                System.out.println(" - " + archivo.getName());
            }
        }
    }
}
