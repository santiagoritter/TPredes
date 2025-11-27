package red.servidor;

import red.shared.ClienteInfo;
import red.util.Constantes;
import red.util.CriptografiaUtil;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Map;

public class ServicioDifusion implements Runnable {

    private final RegistroClientes registro;
    private final String emisor;
    private final String nombreArchivo;
    private final byte[] archivoOriginal;

    public ServicioDifusion(RegistroClientes registro, String emisor, String nombreArchivo, byte[] archivoOriginal) {
        this.registro = registro;
        this.emisor = emisor;
        this.nombreArchivo = nombreArchivo;
        this.archivoOriginal = archivoOriginal;
    }

    @Override
    public void run() {
        System.out.println("Difundiendo...");

        for (Map.Entry<String, ClienteInfo> entry : registro.getClientes()) {
            String nombreReceptor = entry.getKey();
            ClienteInfo infoReceptor = entry.getValue();

            if (nombreReceptor.equals(emisor)) continue;

            try {
                enviarAClienteFirmado(nombreReceptor, infoReceptor);
            } catch (Exception e) {
                System.out.println("Error enviando a " + nombreReceptor + ": " + e.getMessage());
            }
        }
    }

    private void enviarAClienteFirmado(String nombre, ClienteInfo info) throws Exception {
        byte[] archivoEncriptado = CriptografiaUtil.encriptarAESConIV(archivoOriginal, info.claveSesion());

        byte[] firmaDigital = CriptografiaUtil.firmar(archivoEncriptado, ServidorCentral.clavePrivadaServidor);

        try (Socket socket = new Socket(info.ip(), Constantes.PUERTO_RECEPTOR_CLIENTE);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            salida.writeUTF(emisor);
            salida.writeUTF(nombreArchivo);

            salida.writeInt(firmaDigital.length);
            salida.write(firmaDigital);

            salida.writeLong(archivoEncriptado.length);
            salida.write(archivoEncriptado);

            System.out.println("Enviado FIRMADO a " + nombre);
        }
    }
}