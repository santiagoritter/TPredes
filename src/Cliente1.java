import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente1 {
    private static final int MI_PUERTO = 9000;
    private static final String MI_CARPETA = "src/cliente1/";

    public static void main(String[] args) {
        new File(MI_CARPETA).mkdirs();
        new Thread(Cliente1::escucharArchivos).start();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n1. Enviar archivo a varios clientes");
            System.out.println("2. Ver archivos");
            System.out.println("3. Salir");
            System.out.print("Opcion: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();
            switch (opcion) {
                case 1 -> enviarArchivo(scanner);
                case 2 -> verArchivos();
                case 3 -> { return; }
                default -> System.out.println("Opcion no valida");
            }
        }
    }

    private static void escucharArchivos() {
        try (ServerSocket servidor = new ServerSocket(MI_PUERTO)) {
            while (true) {
                try (Socket cliente = servidor.accept();
                     DataInputStream entrada = new DataInputStream(cliente.getInputStream())) {
                    String nombreArchivo = entrada.readUTF();
                    long tamañoArchivo = entrada.readLong();
                    File archivoDestino = new File(MI_CARPETA + nombreArchivo);
                    try (FileOutputStream salida = new FileOutputStream(archivoDestino)) {
                        byte[] buffer = new byte[4096];
                        long bytesRecibidos = 0;
                        int bytesLeidos;
                        while (bytesRecibidos < tamañoArchivo &&
                                (bytesLeidos = entrada.read(buffer)) != -1) {
                            salida.write(buffer, 0, bytesLeidos);
                            bytesRecibidos += bytesLeidos;
                        }
                        System.out.println("Archivo recibido: " + nombreArchivo);
                    }
                } catch (IOException e) {
                    System.err.println("Error recibiendo archivo: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error iniciando servidor: " + e.getMessage());
        }
    }

    private static void enviarArchivo(Scanner scanner) {
        System.out.print("Destinos (ip:puerto) separados por coma: ");
        String[] listaDestinos = scanner.nextLine().trim().split(",");
        System.out.print("Nombre del archivo: ");
        String nombreArchivo = scanner.nextLine().trim();
        File archivo = new File(MI_CARPETA + nombreArchivo);
        if (!archivo.exists()) { System.out.println("Archivo no encontrado"); return; }
        for (String destino : listaDestinos) {
            String[] partes = destino.split(":");
            String ip = partes[0].trim();
            int puerto = Integer.parseInt(partes[1].trim());
            try (Socket socket = new Socket(ip, puerto);
                 DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                 FileInputStream archivoEntrada = new FileInputStream(archivo)) {
                salida.writeUTF(archivo.getName());
                salida.writeLong(archivo.length());
                byte[] buffer = new byte[4096];
                int bytesLeidos;
                while ((bytesLeidos = archivoEntrada.read(buffer)) != -1) {
                    salida.write(buffer, 0, bytesLeidos);
                }
                System.out.println("Archivo enviado a " + ip + ":" + puerto);
            } catch (IOException e) {
                System.err.println("Error enviando a " + ip + ":" + puerto + " -> " + e.getMessage());
            }
        }
    }

    private static void verArchivos() {
        File carpeta = new File(MI_CARPETA);
        File[] archivos = carpeta.listFiles();
        if (archivos == null || archivos.length == 0) { System.out.println("No hay archivos"); return; }
        for (File a : archivos) if (a.isFile()) System.out.println(a.getName());
    }
}
