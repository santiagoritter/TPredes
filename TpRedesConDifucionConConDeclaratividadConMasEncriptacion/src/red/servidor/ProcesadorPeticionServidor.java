package red.servidor;

import red.shared.ClienteInfo;
import red.util.Constantes;
import red.util.CriptografiaUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;

public class ProcesadorPeticionServidor implements Runnable {

    private final Socket socket;
    private final RegistroClientes registro;

    public ProcesadorPeticionServidor(Socket socket, RegistroClientes registro) {
        this.socket = socket;
        this.registro = registro;
    }

    @Override
    public void run() {
        try (DataInputStream entrada = new DataInputStream(socket.getInputStream());
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            String tipoMensaje = entrada.readUTF();

            switch (tipoMensaje) {
                case Constantes.COMANDO_REGISTRO:
                    procesarRegistro(entrada, salida);
                    break;
                case Constantes.COMANDO_ARCHIVO:
                    procesarArchivo(entrada, salida);
                    break;
            }

        } catch (Exception e) {
            System.err.println("Error con cliente: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void procesarRegistro(DataInputStream entrada, DataOutputStream salida) throws Exception {
        String nombre = entrada.readUTF();
        String clavePublicaClienteBase64 = entrada.readUTF();
        String ip = socket.getInetAddress().getHostAddress();

        byte[] claveBytes = CriptografiaUtil.decodeBase64(clavePublicaClienteBase64);
        PublicKey claveCliente = CriptografiaUtil.cargarClavePublica(claveBytes);
        registro.registrar(nombre, new ClienteInfo(ip, claveCliente));

        String claveServidorBase64 = CriptografiaUtil.encodeBase64(ServidorCentral.clavePublicaServidor.getEncoded());
        salida.writeUTF(claveServidorBase64);
    }

    private void procesarArchivo(DataInputStream entrada, DataOutputStream salida) throws Exception {
        String emisor = entrada.readUTF();
        String nombreArchivo = entrada.readUTF();

        int tamanoClave = entrada.readInt();
        byte[] claveAESEncriptadaParaServidor = new byte[tamanoClave];
        entrada.readFully(claveAESEncriptadaParaServidor);

        long tamanoArchivo = entrada.readLong();
        byte[] archivoEncriptado = new byte[(int) tamanoArchivo];
        entrada.readFully(archivoEncriptado);

        System.out.println("Archivo recibido de " + emisor);

        byte[] claveAESBytes = CriptografiaUtil.desencriptarRSA(claveAESEncriptadaParaServidor, ServidorCentral.clavePrivadaServidor);

        ServicioDifusion difusion = new ServicioDifusion(registro, emisor, nombreArchivo, claveAESBytes, archivoEncriptado);
        new Thread(difusion).start();

        salida.writeUTF("Recibido seguro.");
    }
}