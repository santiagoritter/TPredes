import java.io.*;
import java.net.*;
import java.util.*;

public class ClienteRed {
    private static final int PUERTO_SERVIDOR = 8080;
    private static final int PUERTO_RECEPTOR = 8081;
    private static String nombreUsuario;
    private static String ipServidor;
    private static final String CARPETA_ENVIADOS = "src/enviados/";
    private static final String CARPETA_RECIBIDOS = "src/recibidos/";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        File env = new File(CARPETA_ENVIADOS);
        File rec = new File(CARPETA_RECIBIDOS);
        env.mkdirs();
        rec.mkdirs();

        System.out.print("Ingrese la IP del servidor: ");
        ipServidor = sc.nextLine().trim();

        System.out.print("Ingrese su nombre: ");
        nombreUsuario = sc.nextLine().trim();

        registrarEnServidor();

        Thread receptor = new Thread(() -> iniciarReceptor());
        receptor.start();

        while ( true) {
            System.out.println("\nMENU");
            System.out.println("1. Enviar archivo");
            System.out.println("2. Ver enviados");
            System.out.println("3. Ver recibidos");
            System.out.println("4. Salir");
            System.out.print("Opcion: ");
            int op = sc.nextInt();
            sc.nextLine();

            if (op == 1) enviarArchivo(sc);
            else if (op == 2) listarArchivos(CARPETA_ENVIADOS);
            else if (op == 3) listarArchivos(CARPETA_RECIBIDOS);
            else if (op == 4) System.exit(0);
            else System.out.println("Opcion no valida");
        }
    }

    private static void registrarEnServidor() {
        try (Socket socket = new Socket(ipServidor, PUERTO_SERVIDOR);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

            salida.writeUTF("registro");
            salida.writeUTF(nombreUsuario);
            String resp = entrada.readUTF();
            System.out.println(resp);

        } catch (IOException e) {
            System.err.println("No se pudo registrar en el servidor");
        }
    }

    private static void enviarArchivo(Scanner sc) {
        System.out.print("Nombre del archivo en enviados/: ");
        String nombreArchivo = sc.nextLine().trim();
        File archivo = new File(CARPETA_ENVIADOS + nombreArchivo);
        if (!archivo.exists()) {
            System.out.println("Archivo no encontrado");
            return;
        }

        try (Socket socket = new Socket(ipServidor, PUERTO_SERVIDOR);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             DataInputStream entrada = new DataInputStream(socket.getInputStream());
             FileInputStream fis = new FileInputStream(archivo)) {

            salida.writeUTF("archivo");
            salida.writeUTF(nombreUsuario);
            salida.writeUTF(archivo.getName());
            salida.writeLong(archivo.length());

            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = fis.read(buffer)) != -1) {
                salida.write(buffer, 0, leidos);
            }

            String resp = entrada.readUTF();
            System.out.println(resp);

        } catch (IOException e) {
            System.err.println("No se pudo enviar al servidor: " + e.getMessage());
        }
    }

    private static void iniciarReceptor() {
        try (ServerSocket servidor = new ServerSocket(PUERTO_RECEPTOR)) {
            System.out.println("Receptor activo en puerto " + PUERTO_RECEPTOR);
            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> recibirArchivo(cliente)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en receptor: " + e.getMessage());
        }
    }

    private static void recibirArchivo(Socket cliente) {
        try (DataInputStream entrada = new DataInputStream(cliente.getInputStream())) {
            String emisor = entrada.readUTF();
            String nombreArchivo = entrada.readUTF();
            long tamano = entrada.readLong();

            File archivo = new File(CARPETA_RECIBIDOS + nombreArchivo);
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                byte[] buffer = new byte[4096];
                long recibidos = 0;
                int leidos;

                while (recibidos < tamano) {
                    leidos = entrada.read(buffer, 0, (int)Math.min(buffer.length, tamano - recibidos));
                    if (leidos == -1) break;
                    fos.write(buffer, 0, leidos);
                    recibidos += leidos;
                }
            }

            System.out.println("Archivo recibido de " + emisor + ": " + nombreArchivo);

        } catch (IOException e) {
            System.err.println("Error recibiendo archivo: " + e.getMessage());
        } finally {
            try { cliente.close(); } catch (IOException ignored) {}
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
        for (File f : archivos) if (f.isFile()) System.out.println(" - " + f.getName());
    }
}
