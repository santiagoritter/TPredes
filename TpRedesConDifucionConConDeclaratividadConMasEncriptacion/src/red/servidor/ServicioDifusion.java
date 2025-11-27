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
    private final byte[] claveAESBytes;
    private final byte[] archivoEncriptado;

    public ServicioDifusion(RegistroClientes registro, String emisor, String nombreArchivo, byte[] claveAESBytes, byte[] archivoEncriptado) {
        this.registro = registro;
        this.emisor = emisor;
        this.nombreArchivo = nombreArchivo;
        this.claveAESBytes = claveAESBytes;
        this.archivoEncriptado = archivoEncriptado;
    }

    @Override
    public void run() {
        System.out.println("Difundiendo a " + (registro.getConteo() - 1) + " clientes");

        for (Map.Entry<String, ClienteInfo> entry : registro.getClientes()) {
            String nombreReceptor = entry.getKey();
            ClienteInfo infoReceptor = entry.getValue();

            if (nombreReceptor.equals(emisor)) continue;

            try {
                enviarAClienteSeguro(nombreReceptor, infoReceptor);
            } catch (Exception e) {
                System.out.println("Error enviando a " + nombreReceptor);
            }
        }
    }

    private void enviarAClienteSeguro(String nombre, ClienteInfo info) throws Exception {
        byte[] claveParaReceptor = CriptografiaUtil.encriptarRSA(claveAESBytes, info.clavePublica());

        try (Socket socket = new Socket(info.ip(), Constantes.PUERTO_RECEPTOR_CLIENTE);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            salida.writeUTF(emisor);
            salida.writeUTF(nombreArchivo);

            salida.writeInt(claveParaReceptor.length);
            salida.write(claveParaReceptor);

            salida.writeLong(archivoEncriptado.length);
            salida.write(archivoEncriptado);

            System.out.println("Enviado seguro a " + nombre);
        }
    }
}