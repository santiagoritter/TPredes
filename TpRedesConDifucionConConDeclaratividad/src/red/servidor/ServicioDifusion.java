package red.servidor;

import red.shared.ClienteInfo;
import red.util.Constantes;
import red.util.CriptografiaUtil;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServicioDifusion implements Runnable {

    private final RegistroClientes registro;
    private final String emisor;
    private final String nombreArchivo;
    private final String claveAESBase64;
    private final byte[] archivoEncriptado;

    public ServicioDifusion(RegistroClientes registro, String emisor, String nombreArchivo, String claveAESBase64, byte[] archivoEncriptado) {
        this.registro = registro;
        this.emisor = emisor;
        this.nombreArchivo = nombreArchivo;
        this.claveAESBase64 = claveAESBase64;
        this.archivoEncriptado = archivoEncriptado;
    }

    @Override
    public void run() {
        List<String> exitosos = new ArrayList<>();
        List<String> fallidos = new ArrayList<>();
        byte[] claveAESBytes = CriptografiaUtil.decodeBase64(claveAESBase64);

        System.out.println("  Difundiendo a " + (registro.getConteo() - 1) + " clientes...");

        for (Map.Entry<String, ClienteInfo> entry : registro.getClientes()) {
            String nombreReceptor = entry.getKey();
            ClienteInfo infoReceptor = entry.getValue();

            if (nombreReceptor.equals(emisor)) continue;

            try {
                enviarArchivoACliente(nombreReceptor, infoReceptor, claveAESBytes);
                exitosos.add(nombreReceptor);
            } catch (Exception e) {
                fallidos.add(nombreReceptor);
                System.out.println("Error al enviar a " + nombreReceptor + ": " + e.getMessage());
            }
        }
        imprimirResultados(exitosos, fallidos);
    }

    private void enviarArchivoACliente(String nombre, ClienteInfo info, byte[] claveAESBytes) throws Exception {
        byte[] claveAESEncriptadaRSA = CriptografiaUtil.encriptarRSA(claveAESBytes, info.clavePublica());

        try (Socket socket = new Socket(info.ip(), Constantes.PUERTO_RECEPTOR_CLIENTE);
             DataOutputStream salida = new DataOutputStream(socket.getOutputStream())) {

            salida.writeUTF(emisor);
            salida.writeUTF(nombreArchivo);
            salida.writeInt(claveAESEncriptadaRSA.length);
            salida.write(claveAESEncriptadaRSA);
            salida.writeLong(archivoEncriptado.length);
            salida.write(archivoEncriptado);

            System.out.println("  ✓ Enviado a " + nombre + " (" + info.ip() + ")");
        }
    }

    private void imprimirResultados(List<String> exitosos, List<String> fallidos) {
        System.out.println("\n  Resultado de difusión:");
        System.out.println("  - Exitosos: " + exitosos.size());
        if (!fallidos.isEmpty()) {
            System.out.println("  - Fallidos: " + String.join(", ", fallidos));
        }
        System.out.println();
    }
}