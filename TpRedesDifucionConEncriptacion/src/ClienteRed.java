import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;

public class ClienteRed {
    private static final int PUERTO_SERVIDOR = 8080;
    private static final int PUERTO_RECEPTOR = 8081;
    private static String nombreUsuario;
    private static String ipServidor;
    private static final String CARPETA_ENVIADOS = "src/enviados/";
    private static final String CARPETA_RECIBIDOS = "src/recibidos/";
    private static final String CARPETA_CLAVES = "src/claves/";

    private static KeyPair parClaves;
    private static PublicKey clavePublica;
    private static PrivateKey clavePrivada;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        File env = new File(CARPETA_ENVIADOS);
        File rec = new File(CARPETA_RECIBIDOS);
        File claves = new File(CARPETA_CLAVES);
        env.mkdirs();
        rec.mkdirs();
        claves.mkdirs();

        System.out.print("Ingrese la IP del servidor: ");
        ipServidor = sc.nextLine().trim();

        System.out.print("Ingrese su nombre: ");
        nombreUsuario = sc.nextLine().trim();

        // Generar o cargar claves RSA
        generarOCargarClaves();

        // Registrar en servidor con clave pública
        registrarEnServidor();

        Thread receptor = new Thread(() -> iniciarReceptor());
        receptor.start();

        while (true) {
            System.out.println("\nMENU");
            System.out.println("1. Enviar archivo (encriptado)");
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

    private static void generarOCargarClaves() {
        File archivoPublica = new File(CARPETA_CLAVES + nombreUsuario + "_publica.key");
        File archivoPrivada = new File(CARPETA_CLAVES + nombreUsuario + "_privada.key");

        try {
            if (archivoPublica.exists() && archivoPrivada.exists()) {
                // Cargar claves existentes
                System.out.println("Cargando claves RSA existentes...");
                clavePublica = cargarClavePublica(archivoPublica);
                clavePrivada = cargarClavePrivada(archivoPrivada);
            } else {
                // Generar nuevas claves RSA
                System.out.println("Generando nuevo par de claves RSA (2048 bits)...");
                KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
                generador.initialize(2048);
                parClaves = generador.generateKeyPair();
                clavePublica = parClaves.getPublic();
                clavePrivada = parClaves.getPrivate();

                // Guardar claves
                guardarClave(archivoPublica, clavePublica.getEncoded());
                guardarClave(archivoPrivada, clavePrivada.getEncoded());
                System.out.println("Claves RSA generadas y guardadas.");
            }
        } catch (Exception e) {
            System.err.println("Error con claves RSA: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void guardarClave(File archivo, byte[] datos) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(datos);
        }
    }

    private static PublicKey cargarClavePublica(File archivo) throws Exception {
        byte[] datos = new byte[(int) archivo.length()];
        try (FileInputStream fis = new FileInputStream(archivo)) {
            fis.read(datos);
        }
        return KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(datos));
    }

    private static PrivateKey cargarClavePrivada(File archivo) throws Exception {
        byte[] datos = new byte[(int) archivo.length()];
        try (FileInputStream fis = new FileInputStream(archivo)) {
            fis.read(datos);
        }
        return KeyFactory.getInstance("RSA").generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(datos));
    }

    private static void registrarEnServidor() {
        try (Socket socket = new Socket(ipServidor, PUERTO_SERVIDOR);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
             DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

            salida.writeUTF("registro");
            salida.writeUTF(nombreUsuario);

            // Enviar clave pública en Base64
            String clavePublicaBase64 = Base64.getEncoder().encodeToString(clavePublica.getEncoded());
            salida.writeUTF(clavePublicaBase64);

            String resp = entrada.readUTF();
            System.out.println(resp);

        } catch (IOException e) {
            System.err.println("No se pudo registrar en el servidor: " + e.getMessage());
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

        try {
            // 1. Leer archivo original
            byte[] datosOriginales = leerArchivo(archivo);

            // 2. Generar clave AES aleatoria (256 bits)
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey claveAES = keyGen.generateKey();

            // 3. Encriptar archivo con AES
            byte[] archivoEncriptado = encriptarAES(datosOriginales, claveAES);

            // 4. Convertir clave AES a Base64 para envío
            String claveAESBase64 = Base64.getEncoder().encodeToString(claveAES.getEncoded());

            // 5. Enviar al servidor
            try (Socket socket = new Socket(ipServidor, PUERTO_SERVIDOR);
                 DataOutputStream salida = new DataOutputStream(socket.getOutputStream());
                 DataInputStream entrada = new DataInputStream(socket.getInputStream())) {

                salida.writeUTF("archivo");
                salida.writeUTF(nombreUsuario);
                salida.writeUTF(archivo.getName());
                salida.writeUTF(claveAESBase64);
                salida.writeLong(archivoEncriptado.length);

                // Enviar archivo encriptado
                salida.write(archivoEncriptado);

                String resp = entrada.readUTF();
                System.out.println(resp);
            }

        } catch (Exception e) {
            System.err.println("Error al enviar archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] leerArchivo(File archivo) throws IOException {
        byte[] datos = new byte[(int) archivo.length()];
        try (FileInputStream fis = new FileInputStream(archivo)) {
            fis.read(datos);
        }
        return datos;
    }

    private static byte[] encriptarAES(byte[] datos, SecretKey clave) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, clave);

        byte[] iv = cipher.getIV();
        byte[] datosEncriptados = cipher.doFinal(datos);

        // Concatenar IV + datos encriptados
        byte[] resultado = new byte[iv.length + datosEncriptados.length];
        System.arraycopy(iv, 0, resultado, 0, iv.length);
        System.arraycopy(datosEncriptados, 0, resultado, iv.length, datosEncriptados.length);

        return resultado;
    }

    private static byte[] desencriptarAES(byte[] datosEncriptados, SecretKey clave) throws Exception {
        // Extraer IV (primeros 16 bytes)
        byte[] iv = new byte[16];
        System.arraycopy(datosEncriptados, 0, iv, 0, 16);

        // Extraer datos encriptados
        byte[] datos = new byte[datosEncriptados.length - 16];
        System.arraycopy(datosEncriptados, 16, datos, 0, datos.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, clave, new IvParameterSpec(iv));

        return cipher.doFinal(datos);
    }

    private static byte[] desencriptarRSA(byte[] datosEncriptados, PrivateKey clavePrivada) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, clavePrivada);
        return cipher.doFinal(datosEncriptados);
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

            // Recibir clave AES encriptada con RSA
            int longitudClaveEncriptada = entrada.readInt();
            byte[] claveAESEncriptada = new byte[longitudClaveEncriptada];
            entrada.readFully(claveAESEncriptada);

            long tamano = entrada.readLong();

            // Leer archivo encriptado
            byte[] archivoEncriptado = new byte[(int) tamano];
            entrada.readFully(archivoEncriptado);

            // Desencriptar clave AES con RSA
            byte[] claveAESBytes = desencriptarRSA(claveAESEncriptada, clavePrivada);
            SecretKey claveAES = new SecretKeySpec(claveAESBytes, "AES");

            // Desencriptar archivo con AES
            byte[] archivoDesencriptado = desencriptarAES(archivoEncriptado, claveAES);

            // Guardar archivo desencriptado
            File archivo = new File(CARPETA_RECIBIDOS + nombreArchivo);
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                fos.write(archivoDesencriptado);
            }

            System.out.println("✓ Archivo recibido y desencriptado de " + emisor + ": " + nombreArchivo);

        } catch (Exception e) {
            System.err.println("Error recibiendo archivo: " + e.getMessage());
            e.printStackTrace();
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
        for (File f : archivos) {
            if (f.isFile()) System.out.println(" - " + f.getName());
        }
    }
}
