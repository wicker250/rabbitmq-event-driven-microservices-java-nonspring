# RabbitMQ Event-Driven Microservices (Java, Non-Spring)

🇧🇷 Português | 🇺🇸 [English](README.md)

Projeto **mão na massa** para demonstrar uma arquitetura de **microsserviços orientados a eventos** usando **RabbitMQ** e **Java puro (sem Spring)**.

Em vez de chamadas REST, os serviços se comunicam de forma **assíncrona** via **exchanges + routing keys**, explorando fundamentos do AMQP, baixo acoplamento e filtragem de mensagens.

---

## O que tem no projeto

### Microsserviços (simulados)
- **MSReserva** — menu de reservas (publica eventos + recebe notificações)
- **MSPagamento** — consome reservas, simula aprovação/recusa e **assina digitalmente** o resultado
- **MSBilhete** — valida assinatura do pagamento e emite bilhetes
- **MSMarketing** — publica promoções por destino
- **AssinantePromocoes** — assinante dinâmico (faz bind de routing keys via menu)

> Observação: está tudo em **um único módulo Maven** por simplicidade (várias classes `main`). O comportamento em execução simula microsserviços.

---

## Visão da arquitetura

### Fluxo Reserva → Pagamento → Bilhete (Exchange: `EXG`)

- **MSReserva** publica um evento de reserva em `EXG` com routing key **`vermelho`**
- **MSPagamento** consome `vermelho`, aprova/recusa aleatoriamente, assina a mensagem e publica em:
  - **`verde`** (aprovado) ou
  - **`azul`** (recusado)
- **MSBilhete** consome `verde`/`azul`
  - quando aprovado, valida a assinatura e publica notificação de bilhete em **`amarelo`**
- **MSReserva** consome `verde`, `azul` e `amarelo`, guardando notificações em memória

### Fluxo de promoções (Exchange: `PromocoesExchange`)
- **MSMarketing** publica promoções usando routing keys: `rio`, `salvador`, `manaus`
- **AssinantePromocoes** cria uma fila exclusiva/autodelete e faz bind das keys conforme o menu

---

## Exchanges, filas e routing keys

### `EXG` (direct)
| Finalidade | Fila | Routing key |
|---|---|---|
| Reserva criada → MSPagamento | `mspagamento_reserva_criada` | `vermelho` |
| Pagamento aprovado → MSReserva | `pagamento-aprovado` | `verde` |
| Pagamento recusado → MSReserva | `pagamento-recusado` | `azul` |
| Bilhete emitido → MSReserva | `bilhete-emitido` | `amarelo` |
| Pagamento aprovado → MSBilhete | `msbilhete_pagamento_aprovado` | `verde` |
| Pagamento recusado → MSBilhete | `msbilhete_pagamento_recusado` | `azul` |

### `PromocoesExchange` (direct)
| Finalidade | Fila | Routing key |
|---|---|---|
| Assinante de promoções | gerada automaticamente (exclusive) | `rio`, `salvador`, `manaus` |

---

## Formato da mensagem (assinatura digital)

Eventos de pagamento são publicados como:

`<texto>|<assinatura_base64>`

Exemplo:

`Pagamento da reserva RESERVA-0001 aprovado|MEU...BASE64...==`

- Algoritmo: `SHA256withDSA`
- `MSPagamento` assina (chave privada)
- `MSReserva` e `MSBilhete` verificam (chave pública)

---

## Pré-requisitos

- **Java 8+** (configurado no `pom.xml`)
- **Maven 3+**
- **RabbitMQ** rodando em `localhost:5672`

Opcional (recomendado):
- RabbitMQ Management UI (`15672`)

---

## Subindo o RabbitMQ

### Opção A — Docker (recomendado)
```bash
docker run --rm -it \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

Management UI: `http://localhost:15672` (padrão: `guest/guest`)

### Opção B — Instalação local
Inicie o RabbitMQ e mantenha as portas padrão.

---

## Configurando as chaves (obrigatório para assinatura)

O loader de chaves usa um **caminho fixo no Windows** em `cript/KeyReaderExample.java`:
`C:\Users\Afonso\Desktop\keys`

Você pode:

1) **Criar as chaves nessa pasta** (Windows), ou  
2) **Editar a constante** `DIRECTORY_PATH` para o seu caminho.

Para gerar as chaves, execute:
- `cript.KeyGeneratorExample`

Ele cria:
- `private.key`
- `public.key`

> Dica: não suba o `private.key` no Git.

---

## Como executar (ordem recomendada)

Você pode executar pelo IDE (várias configs de Run) **ou** via terminal.

### Build (uma vez)
```bash
mvn -q -DskipTests package
```

### Suba os serviços (cada um em um terminal)

1) **MSPagamento**
```bash
mvn -q exec:java -Dexec.mainClass=com.exemplo.rabbitmq.MSPagamento
```

2) **MSBilhete**
```bash
mvn -q exec:java -Dexec.mainClass=com.exemplo.rabbitmq.MSBilhete
```

3) **MSReserva**
```bash
mvn -q exec:java -Dexec.mainClass=com.exemplo.rabbitmq.MSReserva
```

4) (Opcional) **AssinantePromocoes**
```bash
mvn -q exec:java -Dexec.mainClass=com.exemplo.rabbitmq.AssinantePromocoes
```

5) (Opcional) **MSMarketing**
```bash
mvn -q exec:java -Dexec.mainClass=com.exemplo.rabbitmq.MSMarketing
```

---

## Por que “Non-Spring”?

A ideia é mostrar o “por baixo do capô”:
- declaração manual de exchanges/filas
- filtragem por routing key
- uso direto do client Java do RabbitMQ
- base para entender abstrações do Spring AMQP

---

## Possíveis evoluções

- Mensagens em **JSON** (schema/versionamento)
- **ack manual**, retry e **DLQ**
- correlationId / metadata de rastreio
- configuração externa (host, nomes de exchange, path das chaves)
- separar em multi-module (um módulo por serviço)
- versão alternativa com Spring AMQP para comparação

---

## Licença

Projeto educacional/demonstração — use como referência.
