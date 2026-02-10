package com.exemplo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Consumidor de promoções com fila exclusiva/autodelete.
 
 */
public class AssinantePromocoes {

    private static final String EXCHANGE = "PromocoesExchange";

    // routing-keys
    private static final String RIO      = "rio";
    private static final String SALVADOR = "salvador";
    private static final String MANAUS   = "manaus";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection conexao = factory.newConnection();
             Channel canal   = conexao.createChannel();
             Scanner sc      = new Scanner(System.in)) {

            canal.exchangeDeclare(EXCHANGE, "direct");

            // fila  exclusiva + autodelete
            String fila = canal.queueDeclare("", false, true, true, null).getQueue();

            DeliverCallback cb = (tag, delivery) -> {
                String msg   = new String(delivery.getBody(), "UTF-8");
                String chave = delivery.getEnvelope().getRoutingKey();
                System.out.printf("\nPromoção recebida (%s): %s%n", chave, msg);
            };
            canal.basicConsume(fila, true, cb, tag -> {});

            Set<String> inscritos = new HashSet<>();
            boolean rodando = true;

            while (rodando) {
                System.out.println("\n====== MENU PROMOÇÕES ======");
                System.out.println("1 - Inscrever-se para Rio de Janeiro");
                System.out.println("2 - Inscrever-se para Salvador");
                System.out.println("3 - Inscrever-se para Manaus");
                System.out.println("0 - Sair");
                System.out.print("Escolha uma opção: ");

                String opcao = sc.nextLine().trim();

                switch (opcao) {
                    case "1":
                        inscrever(canal, fila, RIO, inscritos, "Rio de Janeiro");
                        break;
                    case "2":
                        inscrever(canal, fila, SALVADOR, inscritos, "Salvador");
                        break;
                    case "3":
                        inscrever(canal, fila, MANAUS, inscritos, "Manaus");
                        break;
                    case "0":
                        rodando = false;
                        break;
                    default:
                        System.out.println("Opção inválida.");
                }
            }

            // limpa explicitamente
            canal.queueDelete(fila);
            System.out.println("Assinante encerrado Fila removida ");
        }
    }

    private static void inscrever(Channel canal,String fila,String routingKey,Set<String> inscritos,String destino) throws Exception {

        if (inscritos.add(routingKey)) {
            canal.queueBind(fila, EXCHANGE, routingKey);
            
            System.out.println("Inscrição realizada para promoções de: " + destino );
        } else {
            System.out.println("Você já está inscrito em " + destino );
        }
    }
}
