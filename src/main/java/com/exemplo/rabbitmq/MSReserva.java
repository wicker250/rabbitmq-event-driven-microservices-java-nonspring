// MSReserva.java
package com.exemplo.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import cript.KeyReaderExample;

import java.io.IOException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class MSReserva {

	private static boolean executando = true;
    private static final Queue<String> filaNotificacao = new LinkedList<>();
    private static int contadorReserva = 1;
	
   private static final List<String> ITINERARIOS = Arrays.asList(
        "Rio de Janeiro , Data saída: 15/01/2026, Navio: Atlântida - Porto Embarque: Rio de Janeiro - Porto Desembarque: Buenos Aires - Noites: 5 - Valor: R$ 2500",
        "Salvador , Data saída: 22/03/2025 , Navio: Horizonte - Porto Embarque: Salvador -  Porto Desembarque: Ilhéus - Noites: 3 - Valor: R$ 1200",
        "Manaus , Data saída: 05/07/2025 , Navio: Amazonas - Porto Embarque: Manaus -  Porto Desembarque: Belém - Noites: 6 - Valor: R$ 1.800",
        "Recife , Data saída: 10/11/2025 , Navio: Tropical - Porto Embarque: Recife -  Porto Desembarque: Fortaleza - Noites: 4 - Valor: R$ 1.500",
        "Vitória - Data saída: 28/02/2026 , Navio: Estrela do Mar - Porto Embarque: Vitória -  Porto Desembarque: Rio de Janeiro - Noites: 2 - Valor:  R$ 950"
    );

    

    //LOAD chave publica 
    private static final PublicKey PUBLIC_KEY;
     static {
         try {
            PUBLIC_KEY = KeyReaderExample.loadPublicKey();
         } catch (Exception e) {
           
        	 throw new RuntimeException("Error: nao foi possivel abrir a chave", e);
        }
   }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Connection conexao = null;
        Channel canal     = null;
       
        try {
        	//abertura da conexão
        	ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            conexao = factory.newConnection();
            canal   = conexao.createChannel();

            //criação da exchange tipo direct
             String exgReserva = "EXG";
             canal.exchangeDeclare(exgReserva, "direct");

            
            String filaPagAprovados = "pagamento-aprovado";
            String filaPagRecusados = "pagamento-recusado";
            String filaBilhetes     = "bilhete-emitido";

            
            //declaracao de fila ( nome, persistir em disco ou nao , exclusiva da conexao , autodelete se nao tiver consumidor, ttl ou outro parametro)
            canal.queueDeclare(filaPagAprovados, false, false, false, null);
           //(ligar a fila a exg    (fila,exg,Routing key))
            canal.queueBind   (filaPagAprovados, exgReserva, "verde");

            canal.queueDeclare(filaPagRecusados, false, false, false, null);
            canal.queueBind   (filaPagRecusados, exgReserva, "azul");

            
            canal.queueDeclare(filaBilhetes, false, false, false, null);
            canal.queueBind   (filaBilhetes,   exgReserva, "amarelo");

            // callbacks + verificação  assinatura
            DeliverCallback cbAprovado = (tag, delivery) -> {
                tratarNotificacaoPagamento(delivery.getBody(), true);
            };
            canal.basicConsume(filaPagAprovados, true, cbAprovado, t -> {});

            DeliverCallback cbRecusado = (tag, delivery) -> {
                tratarNotificacaoPagamento(delivery.getBody(), false);
            };
            canal.basicConsume(filaPagRecusados, true, cbRecusado, t -> {});

            DeliverCallback cbBilhete = (tag, delivery) -> {
                String msg = new String(delivery.getBody(), "UTF-8");
                filaNotificacao.add("BILHETE EMITIDO" + msg);
            };
            canal.basicConsume(filaBilhetes, true, cbBilhete, t -> {});

            /* ---------------- laço de menu ---------------- */
            while (executando) {
                System.out.println("\n========== MENU RESERVAS ============");
                System.out.println("1 - Consultar Itinerarios");
                System.out.println("2 - Efetuar Reserva");
                System.out.println("3 - Conferir Notificações");
                System.out.println("0 - Sair");
                System.out.print("Escolha uma opção: ");
                
                String opcao = sc.nextLine();

                switch (opcao) {
                case "1":
                    consultarItinerariosDisponiveis();
                    break;

                case "2":
                    System.out.println("\n Digite o numero do itinerario de 1 a " + ITINERARIOS.size());
                    int iti;
                    
                    try { 
                    	iti = Integer.parseInt(sc.nextLine()) - 1; }
                   
                    catch (NumberFormatException e) 
                      { 
                    	System.out.println("Entrada invalida.");
                    break;
                    }

                    if (iti >= 0 && iti < ITINERARIOS.size()) {
                        System.out.print("Quantidade de passageiros:  ");
                        int qtdPassageiros = Integer.parseInt(sc.nextLine());
                        System.out.print("Quantidade de cabines:  ");
                        int qtdCabines     = Integer.parseInt(sc.nextLine());

                        efetuarReserva(ITINERARIOS.get(iti),  qtdPassageiros, qtdCabines, canal,  exgReserva);
                    } else {
                        System.out.println("itinerario invalido");
                    }
                    break;

                case "3":
                    conferirNotificacoes();
                    break;

                case "0":
                    executando = false;
                    break;

                default:
                    System.out.println("Opção invalida");
            }
            }
        } catch (Exception e) {
            System.err.println("ERRO: Falha na execução do MSReserva: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { 
            	if (canal   != null) 
            		{canal.close();
            		}   
            	} 
            	catch (Exception ignore) {}
            try { 
            	if (conexao != null) { conexao.close();}
            	} 
            	catch (Exception ignore) {}
             
            sc.close();
        }

        System.out.println("\nMSReserva encerrado.");
    }

    /*Funcao que trata a mensagem de pagamento aprovado ou nega*/
    private static void tratarNotificacaoPagamento(byte[] body, boolean aprovado) throws IOException {
        String payload = new String(body, "UTF-8");

        String texto;
        boolean valida;

        
        
        //separa a mensagem texto assinatura , se der outro tamanho  considera invalido
        String[] partes = payload.split("\\|", 2);
        
        if (partes.length == 2) {
            texto  = partes[0];
            valida = verificarAssinatura(texto, partes[1]);
        } else {               
            texto  = payload;
            valida = false;
        }

        String prefixo;
        if (aprovado) {
            prefixo = "PAGAMENTO APROVADO";
        } else {
            prefixo = "PAGAMENTO RECUSADO";
        }
        String status;
        if (valida) 
        	{
             status = "(ASSINATURA VALIDA)";
             } else 
             {
            status = "(ASSINATURA INVALIDA)";
             }
        
        filaNotificacao.add(prefixo + " , " + status + " , " + texto);
    }

    private static boolean verificarAssinatura(String texto, String assinaturaB64) {
        try {
            Signature verifier = Signature.getInstance("SHA256withDSA");
            verifier.initVerify(PUBLIC_KEY);
            verifier.update(texto.getBytes("UTF-8"));
            return verifier.verify(Base64.getDecoder().decode(assinaturaB64));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* Percorrer a lista de ITINERARIOS */

    private static void consultarItinerariosDisponiveis() {
        System.out.println("\nItinerários disponíveis:");
        for (int i = 0; i < ITINERARIOS.size(); i++) {
            System.out.println((i + 1) + " ,  " + ITINERARIOS.get(i));
        }
    }

 /*   private static void coletarDadosReserva(Scanner sc, Channel canal,String exchange)  throws IOException {
        System.out.println("\nDigite o número do itinerário 1 a " + ITINERARIOS.size() );
        int iti;
        try { 
        		iti = Integer.parseInt(sc.nextLine()) - 1;
        	}
        
        catch (NumberFormatException e) { 
        	System.out.println("Erro: entrada invalida"); 
        	return; }

        if (iti < 0 || iti >= ITINERARIOS.size()) {
            System.out.println("Opção de itinerario invalido"); 
            return;
        }

        System.out.print("Quantidade de passageiros: ");
        int qtdPassageiros = Integer.parseInt(sc.nextLine());
       
        System.out.print("Quantidade de cabines: ");
        int qtdCabines     = Integer.parseInt(sc.nextLine());

        efetuarReserva(ITINERARIOS.get(iti), qtdPassageiros, qtdCabines,canal, exchange);
    } */

    private static void efetuarReserva(String itinerario, int qtdPass,int qtdCab, Channel canal,String exchange) throws IOException {
        String idReserva = String.format("RESERVA-%04d", contadorReserva++);
        System.out.println("\nEfetuando reserva");
        System.out.println("Detalhes: " + itinerario + " | Passageiros: " + qtdPass + " | Cabines: "     + qtdCab);

        String mensagem = "Reserva " + idReserva + " criada para " + itinerario;
        canal.basicPublish(exchange, "vermelho", null, mensagem.getBytes("UTF-8"));
        System.out.println("Notificação enviada ao MSPagamento chave vermelho\n");
    }

    private static void conferirNotificacoes() {
         if (filaNotificacao.isEmpty()) {
             System.out.println("\nNenhuma notificação pendente");
             return;
        }
        System.out.println("\n---- Notificações ----");
         while (!filaNotificacao.isEmpty()) {
            System.out.println(filaNotificacao.poll());
        }
    }
}
