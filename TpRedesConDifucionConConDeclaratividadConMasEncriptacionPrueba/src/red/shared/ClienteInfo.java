package red.shared;

import java.security.PublicKey;
import javax.crypto.SecretKey;

public record ClienteInfo(String ip, PublicKey clavePublica, SecretKey claveSesion) {
}