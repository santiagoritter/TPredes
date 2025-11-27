package red.servidor;

import red.util.Constantes;
import red.util.CriptografiaUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ServidorCentral {
    public static PublicKey clavePublicaServidor;
    public static PrivateKey clavePrivadaServidor;

    public static void main(String[] args) {
        try {
            System.out.println("INICIANDO SERVIDOR SEGURO...");
            KeyPair parClaves = CriptografiaUtil.generarParClavesRSA();
            clavePublicaServidor = parClaves.getPublic();
            clavePrivadaServidor = parClaves.getPrivate();
            System.out.println("CLAVES RSA SERVIDOR GENERADAS");

            System.out.println("SERVIDOR ACTIVO EN PUERTO: " + Constantes.PUERTO_SERVIDOR);

            RegistroClientes registro = new RegistroClientes();
            ManejadorConexionesServidor manejador = new ManejadorConexionesServidor(Constantes.PUERTO_SERVIDOR, registro);

            manejador.iniciar();
        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
        }
    }
}