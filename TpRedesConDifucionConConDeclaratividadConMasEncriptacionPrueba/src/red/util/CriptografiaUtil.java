package red.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class CriptografiaUtil {

    private CriptografiaUtil() {}

    public static KeyPair generarParClavesRSA() throws NoSuchAlgorithmException {
        KeyPairGenerator generador = KeyPairGenerator.getInstance(Constantes.ALGORITMO_RSA);
        generador.initialize(Constantes.TAMANO_CLAVE_RSA);
        return generador.generateKeyPair();
    }

    public static SecretKey generarClaveAES() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(Constantes.ALGORITMO_AES);
        keyGen.init(Constantes.TAMANO_CLAVE_AES);
        return keyGen.generateKey();
    }

    public static PublicKey cargarClavePublica(byte[] datosClave) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(datosClave);
        KeyFactory kf = KeyFactory.getInstance(Constantes.ALGORITMO_RSA);
        return kf.generatePublic(spec);
    }

    public static PrivateKey cargarClavePrivada(byte[] datosClave) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(datosClave);
        KeyFactory kf = KeyFactory.getInstance(Constantes.ALGORITMO_RSA);
        return kf.generatePrivate(spec);
    }

    public static byte[] encriptarRSA(byte[] datos, Key clave) throws Exception {
        Cipher cipher = Cipher.getInstance(Constantes.TRANSFORMACION_RSA);
        cipher.init(Cipher.ENCRYPT_MODE, clave);
        return cipher.doFinal(datos);
    }

    public static byte[] desencriptarRSA(byte[] datos, Key clave) throws Exception {
        Cipher cipher = Cipher.getInstance(Constantes.TRANSFORMACION_RSA);
        cipher.init(Cipher.DECRYPT_MODE, clave);
        return cipher.doFinal(datos);
    }

    public static byte[] encriptarAESConIV(byte[] datos, SecretKey clave) throws Exception {
        Cipher cipher = Cipher.getInstance(Constantes.TRANSFORMACION_AES);
        cipher.init(Cipher.ENCRYPT_MODE, clave);
        byte[] iv = cipher.getIV();
        byte[] datosEncriptados = cipher.doFinal(datos);

        byte[] resultado = new byte[iv.length + datosEncriptados.length];
        System.arraycopy(iv, 0, resultado, 0, iv.length);
        System.arraycopy(datosEncriptados, 0, resultado, iv.length, datosEncriptados.length);

        return resultado;
    }

    public static byte[] desencriptarAESConIV(byte[] datosConIV, SecretKey clave) throws Exception {
        byte[] iv = new byte[Constantes.TAMANO_IV_AES];
        System.arraycopy(datosConIV, 0, iv, 0, iv.length);

        byte[] datosEncriptados = new byte[datosConIV.length - iv.length];
        System.arraycopy(datosConIV, iv.length, datosEncriptados, 0, datosEncriptados.length);

        Cipher cipher = Cipher.getInstance(Constantes.TRANSFORMACION_AES);
        cipher.init(Cipher.DECRYPT_MODE, clave, new IvParameterSpec(iv));
        return cipher.doFinal(datosEncriptados);
    }

    public static byte[] firmar(byte[] datos, PrivateKey clavePrivada) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(clavePrivada);
        signature.update(datos);
        return signature.sign();
    }

    public static boolean verificarFirma(byte[] datos, byte[] firma, PublicKey clavePublica) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(clavePublica);
        signature.update(datos);
        return signature.verify(firma);
    }

    public static SecretKey convertirBytesAClaveAES(byte[] bytesClave) {
        return new SecretKeySpec(bytesClave, 0, bytesClave.length, Constantes.ALGORITMO_AES);
    }

    public static String encodeBase64(byte[] datos) {
        return Base64.getEncoder().encodeToString(datos);
    }

    public static byte[] decodeBase64(String datos) {
        return Base64.getDecoder().decode(datos);
    }
}