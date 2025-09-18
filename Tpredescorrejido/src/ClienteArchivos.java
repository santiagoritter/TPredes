import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteArchivos {
    private static final String SERVIDOR_IP = "localhost";
    private static final int SERVIDOR_PUERTO = 8080;
    private static final String CARPETA_CLIENTE = "src/carpeta_cliente/";

    public static void main(String[] args) {
        File carpetaCliente = new File(CARPETA_CLIENTE);
        carpetaCliente.mkdirs();

        Scanner scanner = new Scanner(System.in);

        System.out.println("CLIENTE DE ARCHIVOS");
        System.out.println("Carpeta del cliente: " + carpetaCliente.getAbsolutePath());
        System.out.println("Servidor: " + SERVIDOR_IP + ":" + SERVIDOR_PUERTO);

        while (true) {
            System.out.println("\nMENU");
            System.out.println("1. Enviar archivo");
            System.out.println("2. Ver mis archivos");
            System.out.println("3. Salir");
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
                    System.out.println("Hasta luego!");
                    return;
                default:
                    System.out.println("Opcion no valida");
            }
        }
    }

    private static void enviarArchivo(Scanner scanner) {
        System.out.print("Nombre del archivo: ");
        String nombreArchivo = scanner.nextLine().trim();

        String[] carpetas = {CARPETA_CLIENTE, "src/carpeta_cliente/", "carpeta_cliente/", "src/"};
        File archivo = null;

        for (String carpeta : carpetas) {
            File temp = new File(carpeta + nombreArchivo);
            if (temp.exists()) {
                archivo = temp;
                System.out.println("Archivo encontrado en: " + carpeta);
                break;
            }
        }

        if (archivo == null) {
            System.err.println("El archivo '" + nombreArchivo + "' no existe");
            System.out.println("Buscado en:");
            for (String carpeta : carpetas) {
                System.out.println("   - " + new File(carpeta).getAbsolutePath());
            }
            return;
        }

        System.out.println("Enviando: " + nombreArchivo);

        try (Socket socket = new Socket(SERVIDOR_IP, SERVIDOR_PUERTO);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             DataInputStream entrada = new DataInputStream(socket.getInputStream());
             FileInputStream archivoEntrada = new FileInputStream(archivo)) {

            System.out.println("Conectado al servidor");

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

            String confirmacion = entrada.readUTF();
            System.out.println(confirmacion);

        } catch (ConnectException e) {
            System.err.println("No se pudo conectar al servidor");
            System.err.println("Esta el servidor ejecutandose?");
        } catch (IOException e) {
            System.err.println("Error enviando: " + e.getMessage());
        }
    }

    private static void verArchivos() {
        System.out.println("\nARCHIVOS DISPONIBLES PARA ENVIAR");

        String[] carpetas = {CARPETA_CLIENTE, "src/carpeta_cliente/", "carpeta_cliente/", "src/"};
        String[] nombres = {"carpeta_cliente/", "src/carpeta_cliente/", "carpeta_cliente/ (raiz)", "src/"};

        boolean hayArchivos = false;

        for (int i = 0; i < carpetas.length; i++) {
            File carpeta = new File(carpetas[i]);
            if (!carpeta.exists()) continue;

            File[] archivos = carpeta.listFiles();
            if (archivos != null && archivos.length > 0) {
                System.out.println("\nEn " + nombres[i] + ":");
                for (File archivo : archivos) {
                    if (archivo.isFile()) {
                        long tamaño = archivo.length();
                        String tamañoStr = tamaño < 1024 ? tamaño + " B" :
                                tamaño < 1024*1024 ? (tamaño/1024) + " KB" :
                                        (tamaño/1024/1024) + " MB";
                        System.out.println("   " + archivo.getName() + " (" + tamañoStr + ")");
                        hayArchivos = true;
                    }
                }
            }
        }

        if (!hayArchivos) {
            System.out.println("No hay archivos en ninguna carpeta");
            System.out.println("Pon archivos en alguna de estas carpetas:");
            for (String carpeta : carpetas) {
                System.out.println("   - " + new File(carpeta).getAbsolutePath());
            }
        }
    }
}