import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import java.util.Base64;

public class ServidorCentral {
    private static final int PUERTO = 8080;
    private static final Map<String, ClienteInfo> clientes = new HashMap<>();

    // Clase para almacenar información del cliente (IP + clave pública RSA)
    static class ClienteInfo {
        String ip;
        PublicKey clavePublica;

        ClienteInfo(String ip, PublicKey clavePublica) {
            this.ip = ip;
            this.clavePublica = clavePublica;
        }
    }

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("  SERVIDOR CENTRAL ACTIVO (ENCRIPTACIÓN RSA+AES)");
            System.out.println("  Puerto: " + PUERTO);
            System.out.println("═══════════════════════════════════════════════\n");

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
                // REGISTRO: Recibir nombre + clave pública RSA
                String nombre = entrada.readUTF();
                String clavePublicaBase64 = entrada.readUTF();
                String ip = socket.getInetAddress().getHostAddress();

                // Reconstruir clave pública RSA desde Base64
                byte[] clavePublicaBytes = Base64.getDecoder().decode(clavePublicaBase64);
                PublicKey clavePublica = KeyFactory.getInstance("RSA")
                        .generatePublic(new java.security.spec.X509EncodedKeySpec(clavePublicaBytes));

                // Guardar cliente con su IP y clave pública
                synchronized (clientes) {
                    clientes.put(nombre, new ClienteInfo(ip, clavePublica));
                }

                System.out.println("✓ Cliente registrado: " + nombre + " -> " + ip);
                System.out.println("  Clientes activos: " + clientes.size());
                salida.writeUTF("Registro exitoso - Clave pública recibida");
            }

            else if (tipoMensaje.equals("archivo")) {
                // RECEPCIÓN DE ARCHIVO: Ya viene encriptado con AES
                String emisor = entrada.readUTF();
                String nombreArchivo = entrada.readUTF();
                String claveAESBase64 = entrada.readUTF();  // Clave AES en Base64
                long tamanoArchivo = entrada.readLong();

                // Leer archivo encriptado completo
                byte[] archivoEncriptado = new byte[(int) tamanoArchivo];
                entrada.readFully(archivoEncriptado);

                System.out.println("\n→ Archivo recibido de " + emisor + ": " + nombreArchivo);
                System.out.println("  Tamaño encriptado: " + tamanoArchivo + " bytes");

                salida.writeUTF("Servidor recibió el archivo encriptado");

                // Difundir a todos los clientes (excepto el emisor)
                difundirArchivo(emisor, nombreArchivo, claveAESBase64, archivoEncriptado);
            }

        } catch (Exception e) {
            System.err.println("Error con cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void difundirArchivo(String emisor, String nombreArchivo,
                                        String claveAESBase64, byte[] archivoEncriptado) {
        List<String> exitosos = new ArrayList<>();
        List<String> fallidos = new ArrayList<>();

        // Convertir clave AES de Base64 a bytes
        byte[] claveAESBytes = Base64.getDecoder().decode(claveAESBase64);

        synchronized (clientes) {
            System.out.println("  Difundiendo a " + (clientes.size() - 1) + " clientes...");

            for (Map.Entry<String, ClienteInfo> entry : clientes.entrySet()) {
                String nombre = entry.getKey();
                ClienteInfo info = entry.getValue();

                // No enviar al emisor
                if (nombre.equals(emisor)) continue;

                try {
                    // ENCRIPTAR la clave AES con la clave pública RSA del destinatario
                    // Esta es la parte clave: cada destinatario recibe la clave AES
                    // encriptada con SU propia clave pública RSA
                    byte[] claveAESEncriptada = encriptarRSA(claveAESBytes, info.clavePublica);

                    // Enviar al cliente: archivo encriptado + clave AES encriptada con su RSA
                    try (Socket socket = new Socket(info.ip, 8081);
                         DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

                        salida.writeUTF(emisor);                          // Quién envió
                        salida.writeUTF(nombreArchivo);                   // Nombre del archivo
                        salida.writeInt(claveAESEncriptada.length);       // Tamaño de clave encriptada
                        salida.write(claveAESEncriptada);                 // Clave AES encriptada con RSA
                        salida.writeLong(archivoEncriptado.length);       // Tamaño del archivo
                        salida.write(archivoEncriptado);                  // Archivo encriptado con AES

                        exitosos.add(nombre);
                        System.out.println("  ✓ Enviado a " + nombre + " (" + info.ip + ")");
                    }

                } catch (Exception e) {
                    fallidos.add(nombre);
                    System.out.println("  ✗ Error al enviar a " + nombre + ": " + e.getMessage());
                }
            }
        }

        // Resumen de difusión
        System.out.println("\n  Resultado de difusión:");
        System.out.println("  - Exitosos: " + exitosos.size());
        if (!fallidos.isEmpty()) {
            System.out.println("  - Fallidos: " + String.join(", ", fallidos));
        }
        System.out.println();
    }

    // Método para encriptar datos con clave pública RSA
    private static byte[] encriptarRSA(byte[] datos, PublicKey clavePublica) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, clavePublica);
        return cipher.doFinal(datos);
    }
}