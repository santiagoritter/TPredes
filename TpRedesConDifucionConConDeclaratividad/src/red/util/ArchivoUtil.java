package red.util;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArchivoUtil {

    private ArchivoUtil() {}

    public static void crearDirectorios(Path ruta) throws IOException {
        Files.createDirectories(ruta);
    }

    public static boolean existe(Path ruta) {
        return Files.exists(ruta);
    }

    public static byte[] leerArchivo(Path ruta) throws IOException {
        return Files.readAllBytes(ruta);
    }

    public static void escribirArchivo(Path ruta, byte[] datos) throws IOException {
        Files.write(ruta, datos);
    }

    public static List<String> listarArchivos(Path ruta) throws IOException {
        if (!Files.exists(ruta) || !Files.isDirectory(ruta)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(ruta)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }
}