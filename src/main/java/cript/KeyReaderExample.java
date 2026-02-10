package cript;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyReaderExample {

    // Diretório fixo onde as chaves estão armazenadas
    private static final String DIRECTORY_PATH = "C:\\Users\\Afonso\\Desktop\\keys";

    public static PublicKey loadPublicKey() throws Exception {
        Path path = Paths.get(DIRECTORY_PATH, "public.key");
        byte[] keyBytes = Files.readAllBytes(path);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("DSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey loadPrivateKey() throws Exception {
        Path path = Paths.get(DIRECTORY_PATH, "private.key");
        byte[] keyBytes = Files.readAllBytes(path);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("DSA");
        return kf.generatePrivate(spec);
    }
}
