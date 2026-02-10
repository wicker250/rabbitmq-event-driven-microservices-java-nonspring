
package com.exemplo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import cript.KeyReaderExample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

public class MSBilhete {

    // chave publica load
    private static final PublicKey PUBLIC_KEY;
    static {
        try {
            PUBLIC_KEY = KeyReaderExample.loadPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar chave publica", e);
        }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection conexao = factory.newConnection();
             Channel    canal   = conexao.createChannel()) {

            final String exchange = "EXG";
            canal.exchangeDeclare(exchange, "direct");

            final String filaPagamentoAprovado = "msbilhete_pagamento_aprovado";
            canal.queueDeclare(filaPagamentoAprovado, false, false, false, null);
            canal.queueBind   (filaPagamentoAprovado, exchange, "verde");

            final String filaPagamentoRecusado = "msbilhete_pagamento_recusado";
            canal.queueDeclare(filaPagamentoRecusado, false, false, false, null);
            canal.queueBind   (filaPagamentoRecusado, exchange, "azul");

            System.out.println("MSBilhete online aguardando confirmações de pagamentos");

            //pagamento aprovado
            DeliverCallback callbackAprovado = (tag, delivery) -> {
                
            	String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);

                String[] partes = payload.split("\\|", 2);
                if (partes.length != 2) {
                    System.out.println("erro: nao é possivel verificar a assinatura");
                    return;
                }
                String mensagem   = partes[0];
                String assinatura = partes[1];

                boolean assinaturaValida;
                
                try {
                    Signature verifier = Signature.getInstance("SHA256withDSA");
                    verifier.initVerify(PUBLIC_KEY);
                    verifier.update(mensagem.getBytes(StandardCharsets.UTF_8));
                    assinaturaValida = verifier.verify(Base64.getDecoder().decode(assinatura));
                } catch (Exception e) {
                     e.printStackTrace();
                     return;
                }

                if (!assinaturaValida) {
                    System.out.println("Assinatura invalida mensagem ignorada: " + mensagem);
                    return;
                }

                
                System.out.println("Assinatura valida gerando bilhete para: " + mensagem);

                String bilhete = "Bilhete gerado para " + mensagem;

                // publica notificação na routing-key: amarelo, que o MSReserva consome
               
                 canal.basicPublish(exchange,"amarelo",null,bilhete.getBytes(StandardCharsets.UTF_8));

                System.out.println("Notificação de bilhete enviada  key: amarelo bilhete:"+ bilhete);
            };

            //pagamento recusado
            
            DeliverCallback callbackRecusado = (tag, delivery) -> {
                String mensagem = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Pagamento RECUSADO :  " + mensagem);
            };

            canal.basicConsume(filaPagamentoAprovado, true, callbackAprovado, cTag -> {});
            canal.basicConsume(filaPagamentoRecusado, true, callbackRecusado, cTag -> {});

            // mantém a thread viva
           // System.out.println("Pressione Ctrl+C para encerrar o MSBilhete");
            Thread.currentThread().join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
