
package com.exemplo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import cript.KeyReaderExample;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.Signature;

import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/* escuta reserva criadas chave vermelho , 
 * simula pagamento assina digitalmente e publica na exchange com a chave verde aprovado azul recusado */
public class MSPagamento {

   //chave privada 
     private static final PrivateKey PRIVATE_KEY;
    static {
        try {
            PRIVATE_KEY = KeyReaderExample.loadPrivateKey();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar chave privada", e);
        }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection conexao = factory.newConnection();
        Channel canal     = conexao.createChannel();

        String exchange = "EXG";
        canal.exchangeDeclare(exchange, "direct");

        //fila e bind para receber reservas
        String filaReservaCriada = "mspagamento_reserva_criada";
        canal.queueDeclare(filaReservaCriada, false, false, false, null);
        canal.queueBind   (filaReservaCriada, exchange, "vermelho");

         System.out.println("MSPagamento online ");

         //configuracao de recebimento de mensagem gera de forma aleatoria aprovacao ou nao do pagamento
        DeliverCallback callbackReserva = (tag, delivery) -> {
            String mensagemReserva = new String(delivery.getBody(), "UTF-8");
            System.out.println("Recebido: " + mensagemReserva);

            boolean aprovado = new Random().nextBoolean();
            String idReserva = extrairIdReserva(mensagemReserva);

            String text;
            String routingKey;
            
            if (aprovado) {
                text       =  "Pagamento da reserva " + idReserva + " aprovado";
                routingKey  =  "verde";
            } else {
                text       =  "Pagamento da reserva " + idReserva + " recusado";
                routingKey  =  "azul";
            }

            try {
                // ASSINA a mensagem
                Signature sig = Signature.getInstance("SHA256withDSA");
                sig.initSign(PRIVATE_KEY);
                sig.update(text.getBytes("UTF-8"));
                String assinaturaB64 = Base64.getEncoder().encodeToString(sig.sign());

                String payload = text + "|" + assinaturaB64;

                canal.basicPublish(exchange, routingKey, null,payload.getBytes("UTF-8"));

                if (aprovado) {
                    System.out.printf("Pagamento aprovado, mensagem assinada  na chave %s\n", routingKey);
                } else {
                    System.out.printf("Pagamento recusado, mensagem assinada  na chave %s\n", routingKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
         //basicConsume(String queue, boolean autoAck, DeliverCallback deliverCallback, CancelCallback cancelCallback)
         canal.basicConsume(filaReservaCriada, true, callbackReserva, cTag -> {});
    }

    private static String extrairIdReserva(String msg) {
        String[] p = msg.split(" ");
        if (p.length >= 2) {
            return p[1];
        } else {
            return "valor invalido";
        }
    }
}
