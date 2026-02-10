package cript;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyGeneratorExample {
    public static void main(String[] args) throws Exception {
        
        // Caminho onde vamos salvar as chaves
        String directoryPath = "C:\\Users\\Afonso\\Desktop\\keys";

        // Cria o diretório se não existir
        createDirectoryIfNotExists(directoryPath);

        // Gera um par de chaves (Privada e Pública)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Salva as chaves nos arquivos
        saveKeyToFile(directoryPath + "\\private.key", privateKey.getEncoded());
        saveKeyToFile(directoryPath + "\\public.key", publicKey.getEncoded());

        System.out.println("Chaves geradas e salvas em: " + directoryPath);
    }

    private static void saveKeyToFile(String path, byte[] key) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(key);
        }
    }

    private static void createDirectoryIfNotExists(String path) throws IOException {
        Path directory = Paths.get(path);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
}

