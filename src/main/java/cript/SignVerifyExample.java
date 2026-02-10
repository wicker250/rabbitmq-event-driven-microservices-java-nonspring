package cript;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class SignVerifyExample {
    public static void main(String[] args) throws Exception {

        // Carrega as chaves do diretório fixo
        PrivateKey privateKey = KeyReaderExample.loadPrivateKey();
        PublicKey publicKey = KeyReaderExample.loadPublicKey();

        String message = "Mensagem que será assinada usando chaves de arquivos";

        // Assinar a mensagem
        Signature signature = Signature.getInstance("SHA256withDSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes());
        byte[] signedBytes = signature.sign();

        System.out.println("Mensagem assinada!");

        // Verificar a assinatura
        Signature verifier = Signature.getInstance("SHA256withDSA");
        verifier.initVerify(publicKey);
        verifier.update(message.getBytes());
        boolean isCorrect = verifier.verify(signedBytes);

        System.out.println("Assinatura é válida? " + isCorrect);
    }
}
