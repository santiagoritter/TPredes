package red.cliente;

import red.util.ArchivoUtil;
import red.util.Constantes;
import red.util.CriptografiaUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ManejadorClaves {

    private final Path rutaPublica;
    private final Path rutaPrivada;

    private PublicKey clavePublica;
    private PrivateKey clavePrivada;

    public ManejadorClaves(String nombreUsuario) {
        Path carpetaClaves = Paths.get(Constantes.CARPETA_CLAVES);
        this.rutaPublica = carpetaClaves.resolve(nombreUsuario + "_publica.key");
        this.rutaPrivada = carpetaClaves.resolve(nombreUsuario + "_privada.key");
    }

    public void inicializar() {
        try {
            ArchivoUtil.crearDirectorios(rutaPublica.getParent());
            if (ArchivoUtil.existe(rutaPublica) && ArchivoUtil.existe(rutaPrivada)) {
                cargarClavesExistentes();
            } else {
                generarYGuardarClavesNuevas();
            }
        } catch (Exception e) {
            System.err.println("Error crítico con claves RSA: " + e.getMessage());
            System.exit(1);
        }
    }

    private void cargarClavesExistentes() throws Exception {
        System.out.println("Cargando claves RSA existentes...");
        byte[] publicaBytes = ArchivoUtil.leerArchivo(rutaPublica);
        byte[] privadaBytes = ArchivoUtil.leerArchivo(rutaPrivada);
        this.clavePublica = CriptografiaUtil.cargarClavePublica(publicaBytes);
        this.clavePrivada = CriptografiaUtil.cargarClavePrivada(privadaBytes);
    }

    private void generarYGuardarClavesNuevas() throws Exception {
        System.out.println("Generando nuevo par de claves RSA (" + Constantes.TAMANO_CLAVE_RSA + " bits)...");
        KeyPair parClaves = CriptografiaUtil.generarParClavesRSA();
        this.clavePublica = parClaves.getPublic();
        this.clavePrivada = parClaves.getPrivate();

        ArchivoUtil.escribirArchivo(rutaPublica, clavePublica.getEncoded());
        ArchivoUtil.escribirArchivo(rutaPrivada, clavePrivada.getEncoded());
        System.out.println("Claves RSA generadas y guardadas.");
    }

    public PublicKey getClavePublica() {
        return clavePublica;
    }

    public PrivateKey getClavePrivada() {
        return clavePrivada;
    }
}