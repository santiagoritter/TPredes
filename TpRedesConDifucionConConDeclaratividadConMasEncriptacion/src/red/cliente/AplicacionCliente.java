package red.cliente;

import red.util.ArchivoUtil;
import red.util.Constantes;
import red.util.CriptografiaUtil;

import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.List;
import java.util.Scanner;

public class AplicacionCliente {

    private final String ipServidor;
    private final String nombreUsuario;
    private final Scanner scanner;
    private final ManejadorClaves manejadorClaves;
    private final Path carpetaEnviados = Paths.get(Constantes.CARPETA_ENVIADOS);
    private final Path carpetaRecibidos = Paths.get(Constantes.CARPETA_RECIBIDOS);
    private PublicKey clavePublicaServidor;

    public AplicacionCliente(String ipServidor, String nombreUsuario, Scanner scanner) {
        this.ipServidor = ipServidor;
        this.nombreUsuario = nombreUsuario;
        this.scanner = scanner;
        this.manejadorClaves = new ManejadorClaves(nombreUsuario);
    }

    public void iniciar() {
        try {
            ArchivoUtil.crearDirectorios(carpetaEnviados);
            ArchivoUtil.crearDirectorios(carpetaRecibidos);
            manejadorClaves.inicializar();

            registrarEnServidor();

            if (clavePublicaServidor == null) {
                System.out.println("Error: No se obtuvo clave del servidor.");
                return;
            }

            iniciarReceptor();
            mostrarMenu();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registrarEnServidor() {
        try (Socket socket = new Socket(ipServidor, Constantes.PUERTO_SERVIDOR);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

            salida.writeUTF(Constantes.COMANDO_REGISTRO);
            salida.writeUTF(nombreUsuario);

            String miPublica = CriptografiaUtil.encodeBase64(manejadorClaves.getClavePublica().getEncoded());
            salida.writeUTF(miPublica);

            String serverPublicaBase64 = entrada.readUTF();
            byte[] serverBytes = CriptografiaUtil.decodeBase64(serverPublicaBase64);
            this.clavePublicaServidor = CriptografiaUtil.cargarClavePublica(serverBytes);

            System.out.println("Registro exitoso. Clave Servidor recibida.");

        } catch (Exception e) {
            System.err.println("Error de registro: " + e.getMessage());
        }
    }

    private void iniciarReceptor() {
        ReceptorArchivos receptor = new ReceptorArchivos(Constantes.PUERTO_RECEPTOR_CLIENTE, manejadorClaves.getClavePrivada(), carpetaRecibidos);
        new Thread(receptor).start();
    }

    private void mostrarMenu() {
        while (true) {
            System.out.println("\nMENU");
            System.out.println("1. Enviar archivo");
            System.out.println("2. Ver enviados");
            System.out.println("3. Ver recibidos");
            System.out.println("4. Salir");
            System.out.print("Opcion: ");

            int op = leerOpcion();

            switch (op) {
                case 1: enviarArchivo(); break;
                case 2: mostrarListadoArchivos(carpetaEnviados); break;
                case 3: mostrarListadoArchivos(carpetaRecibidos); break;
                case 4: System.exit(0); break;
                default: System.out.println("Opcion no valida");
            }
        }
    }

    private int leerOpcion() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void enviarArchivo() {
        System.out.print("Nombre archivo: ");
        String nombreArchivo = scanner.nextLine().trim();
        Path archivo = carpetaEnviados.resolve(nombreArchivo);

        if (!ArchivoUtil.existe(archivo)) {
            System.out.println("Archivo no encontrado");
            return;
        }

        try {
            byte[] datosOriginales = ArchivoUtil.leerArchivo(archivo);
            SecretKey claveAES = CriptografiaUtil.generarClaveAES();
            byte[] archivoEncriptado = CriptografiaUtil.encriptarAESConIV(datosOriginales, claveAES);
            byte[] claveAESEncriptada = CriptografiaUtil.encriptarRSA(claveAES.getEncoded(), clavePublicaServidor);

            try (Socket socket = new Socket(ipServidor, Constantes.PUERTO_SERVIDOR);
                 DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                 DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

                salida.writeUTF(Constantes.COMANDO_ARCHIVO);
                salida.writeUTF(nombreUsuario);
                salida.writeUTF(archivo.getFileName().toString());

                salida.writeInt(claveAESEncriptada.length);
                salida.write(claveAESEncriptada);

                salida.writeLong(archivoEncriptado.length);
                salida.write(archivoEncriptado);

                System.out.println(entrada.readUTF());
            }

        } catch (Exception e) {
            System.err.println("Error enviando: " + e.getMessage());
        }
    }

    private void mostrarListadoArchivos(Path carpeta) {
        try {
            List<String> archivos = ArchivoUtil.listarArchivos(carpeta);
            archivos.forEach(nombre -> System.out.println(" - " + nombre));
        } catch (IOException e) {
            System.err.println("Error listar: " + e.getMessage());
        }
    }
}