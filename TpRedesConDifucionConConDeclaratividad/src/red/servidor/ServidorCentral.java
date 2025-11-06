package red.servidor;

import red.util.Constantes;

public class ServidorCentral {
    public static void main(String[] args) {
        System.out.println("  SERVIDOR CENTRAL ACTIVO (ENCRIPTACIÓN RSA+AES)");
        System.out.println("  Puerto: " + Constantes.PUERTO_SERVIDOR);

        RegistroClientes registro = new RegistroClientes();
        ManejadorConexionesServidor manejador = new ManejadorConexionesServidor(Constantes.PUERTO_SERVIDOR, registro);

        manejador.iniciar();
    }
}