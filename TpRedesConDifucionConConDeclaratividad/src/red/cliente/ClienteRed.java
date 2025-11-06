package red.cliente;

import java.util.Scanner;

public class ClienteRed {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Ingrese la IP del servidor: ");
        String ipServidor = sc.nextLine().trim();

        System.out.print("Ingrese su nombre: ");
        String nombreUsuario = sc.nextLine().trim();

        AplicacionCliente app = new AplicacionCliente(ipServidor, nombreUsuario, sc);
        app.iniciar();
    }
}