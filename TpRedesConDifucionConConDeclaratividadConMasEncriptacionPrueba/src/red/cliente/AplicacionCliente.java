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
import java.util.Scanner;

public class AplicacionCliente {

    private final String ipServidor;
    private final String nombreUsuario;
    private final Scanner scanner;
    private final ManejadorClaves manejadorClaves;
    private final Path carpetaEnviados = Paths.get(Constantes.CARPETA_ENVIADOS);
    private final Path carpetaRecibidos = Paths.get(Constantes.CARPETA_RECIBIDOS);

    // Estos campos son necesarios para el nuevo Receptor
    private PublicKey clavePublicaServidor;
    private SecretKey claveSesion;

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

            // Verificamos que el handshake haya funcionado antes de iniciar el receptor
            if (clavePublicaServidor == null || claveSesion == null) {
                System.out.println("Error: Falló el handshake (No hay claves).");
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

            // 1. Recibir Clave Pública del Servidor
            String serverPublicaBase64 = entrada.readUTF();
            byte[] serverBytes = CriptografiaUtil.decodeBase64(serverPublicaBase64);
            this.clavePublicaServidor = CriptografiaUtil.cargarClavePublica(serverBytes);

            // 2. Recibir Clave de Sesión (AES) Encriptada
            int len = entrada.readInt();
            byte[] claveSesionEncriptada = new byte[len];
            entrada.readFully(claveSesionEncriptada);

            // 3. Desencriptar Clave de Sesión con mi Privada
            byte[] claveSesionBytes = CriptografiaUtil.desencriptarRSA(claveSesionEncriptada, manejadorClaves.getClavePrivada());
            this.claveSesion = CriptografiaUtil.convertirBytesAClaveAES(claveSesionBytes);

            System.out.println("Handshake Exitoso. Clave de Sesión establecida.");

        } catch (Exception e) {
            System.err.println("Error de registro: " + e.getMessage());
        }
    }

    // ESTE ES EL MÉTODO QUE DABA ERROR EN LA FOTO
    private void iniciarReceptor() {
        // Ahora pasamos la clave de sesión y la del servidor, NO la privada nuestra
        ReceptorArchivos receptor = new ReceptorArchivos(
                Constantes.PUERTO_RECEPTOR_CLIENTE,
                claveSesion,
                clavePublicaServidor,
                carpetaRecibidos
        );
        new Thread(receptor).start();
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

            // Usamos la CLAVE DE SESIÓN para encriptar
            byte[] archivoEncriptado = CriptografiaUtil.encriptarAESConIV(datosOriginales, claveSesion);

            // Firmamos con nuestra PRIVADA
            byte[] firma = CriptografiaUtil.firmar(archivoEncriptado, manejadorClaves.getClavePrivada());

            try (Socket socket = new Socket(ipServidor, Constantes.PUERTO_SERVIDOR);
                 DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                 DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

                salida.writeUTF(Constantes.COMANDO_ARCHIVO);
                salida.writeUTF(nombreUsuario);
                salida.writeUTF(archivo.getFileName().toString());

                // Enviar la firma primero
                salida.writeInt(firma.length);
                salida.write(firma);

                // Enviar el archivo
                salida.writeLong(archivoEncriptado.length);
                salida.write(archivoEncriptado);

                System.out.println(entrada.readUTF());
            }

        } catch (Exception e) {
            System.err.println("Error enviando: " + e.getMessage());
        }
    }

    private void mostrarMenu() {
        while (true) {
            System.out.println("\n1. Enviar  2. Ver Enviados 3. Ver Recibidos 4. Salir");
            String op = scanner.nextLine();

            if (op.equals("1")) enviarArchivo();
            else if (op.equals("2")) mostrarListadoArchivos(carpetaEnviados);
            else if (op.equals("3")) mostrarListadoArchivos(carpetaRecibidos);
            else if (op.equals("4")) System.exit(0);
        }
    }

    private void mostrarListadoArchivos(Path carpeta) {
        try {
            java.util.List<String> archivos = ArchivoUtil.listarArchivos(carpeta);
            archivos.forEach(nombre -> System.out.println(" - " + nombre));
        } catch (IOException e) {
            System.err.println("Error listar: " + e.getMessage());
        }
    }
}