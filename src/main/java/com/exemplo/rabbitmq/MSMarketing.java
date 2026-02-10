package com.exemplo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/*
  Gera três promoções Rio Salvador Manaus em intervalos de
  20s  40 e 60 s 
 */
public class MSMarketing {

    private static final String EXCHANGE = "PromocoesExchange";

    public static void main(String[] args) throws Exception {
        
    	ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

       
        try (Connection conexao = factory.newConnection();
             Channel canal   = conexao.createChannel()) {

            canal.exchangeDeclare(EXCHANGE, "direct");

            // 20 segundos
            Thread.sleep(20_000);  //
            String promoRio = "Desconto especial: 20% Cruzeiro Rio de Janeiro  Buenos Aires!";
            canal.basicPublish(EXCHANGE, "rio", null, promoRio.getBytes("UTF-8"));
            System.out.println("Promoção enviada (rio) após 20 s: " + promoRio);

            
            //40 
            
            Thread.sleep(20_000);  
            String promoSalvador = "Navio Horizonte com 30% de desconto Saída Salvador ";
            canal.basicPublish(EXCHANGE, "salvador", null, promoSalvador.getBytes("UTF-8"));
            System.out.println("Promoção enviada salvador após 40 s: " + promoSalvador);

            // Manaus 60 s
            Thread.sleep(20_000);  // +20 
            String promoManaus = "Oferta exclusiva: Cruzeiro Manaus para Belém – 2 Noites grátis";
            canal.basicPublish(EXCHANGE, "manaus", null, promoManaus.getBytes("UTF-8"));
            System.out.println("Promoção enviada manaus após 60 s: " + promoManaus);

            System.out.println("Todas as promoções foram enviadas. MSMarketing encerrado");
        }
    }
}
