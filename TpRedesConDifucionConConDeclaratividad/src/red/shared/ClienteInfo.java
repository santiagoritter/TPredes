package red.shared;

import java.security.PublicKey;

public record ClienteInfo(String ip, PublicKey clavePublica) {
}