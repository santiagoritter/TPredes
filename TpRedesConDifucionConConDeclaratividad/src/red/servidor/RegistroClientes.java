package red.servidor;

import red.shared.ClienteInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegistroClientes {
    private final Map<String, ClienteInfo> clientes = new HashMap<>();

    public synchronized void registrar(String nombre, ClienteInfo info) {
        clientes.put(nombre, info);
        System.out.println("✓ Cliente registrado: " + nombre + " -> " + info.ip());
        System.out.println("  Clientes activos: " + clientes.size());
    }

    public synchronized Set<Map.Entry<String, ClienteInfo>> getClientes() {
        return new HashMap<>(clientes).entrySet();
    }

    public synchronized int getConteo() {
        return clientes.size();
    }
}