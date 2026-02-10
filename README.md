# RabbitMQ Event-Driven Microservices (Java, Non-Spring)

🇺🇸 English | 🇧🇷 [Português](README.pt-BR.md)

A **hands-on** project that demonstrates an **event-driven microservices** approach using **RabbitMQ** and **pure Java (no Spring)**.

Instead of REST calls, services communicate **asynchronously** through RabbitMQ **exchanges + routing keys**, highlighting the fundamentals of AMQP messaging, loose coupling, and message filtering.

---

## What’s inside

### Microservices (simulated)
- **MSReserva** — reservation menu (publishes reservation events + receives notifications)
- **MSPagamento** — consumes reservations, simulates payment decision, **digitally signs** the result
- **MSBilhete** — verifies payment signature and issues tickets
- **MSMarketing** — publishes promotions by destination
- **AssinantePromocoes** — dynamic subscriber (bind/unbind routing keys at runtime)

> Note: all services are in a **single Maven module** for simplicity (multiple `main` classes). The runtime behavior simulates microservices.

---

## Architecture overview

### Reservation → Payment → Ticket flow (Exchange: `EXG`)

- **MSReserva** publishes a reservation event to `EXG` with routing key **`vermelho`**
- **MSPagamento** consumes `vermelho`, randomly approves/rejects payment, signs the message, and publishes to:
  - **`verde`** (approved) or
  - **`azul`** (rejected)
- **MSBilhete** consumes `verde`/`azul`
  - when approved, verifies the signature and publishes a ticket notification to **`amarelo`**
- **MSReserva** consumes `verde`, `azul`, and `amarelo` and stores notifications in an in-memory queue

### Promotions flow (Exchange: `PromocoesExchange`)
- **MSMarketing** publishes promotions using routing keys: `rio`, `salvador`, `manaus`
- **AssinantePromocoes** creates an exclusive auto-delete queue and binds keys based on the user menu

---

## Exchanges, queues and routing keys

### `EXG` (direct)
| Purpose | Queue | Routing key |
|---|---|---|
| Reservation created → MSPagamento | `mspagamento_reserva_criada` | `vermelho` |
| Payment approved → MSReserva | `pagamento-aprovado` | `verde` |
| Payment rejected → MSReserva | `pagamento-recusado` | `azul` |
| Ticket issued → MSReserva | `bilhete-emitido` | `amarelo` |
| Payment approved → MSBilhete | `msbilhete_pagamento_aprovado` | `verde` |
| Payment rejected → MSBilhete | `msbilhete_pagamento_recusado` | `azul` |

### `PromocoesExchange` (direct)
| Purpose | Queue | Routing key |
|---|---|---|
| Promotions subscriber | auto-generated (exclusive) | `rio`, `salvador`, `manaus` |

---

## Message format (digital signature)

Payment events are published as:

`<text>|<base64_signature>`

Example:

`Pagamento da reserva RESERVA-0001 aprovado|MEU...BASE64...==`

- Signing algorithm: `SHA256withDSA`
- `MSPagamento` signs (private key)
- `MSReserva` and `MSBilhete` verify (public key)

---

## Prerequisites

- **Java 8+** (project is configured for Java 8 in `pom.xml`)
- **Maven 3+**
- **RabbitMQ** running locally on `localhost:5672`

Optional (recommended):
- RabbitMQ Management UI (`15672`)

---

## Running RabbitMQ

### Option A — Docker (recommended)
```bash
docker run --rm -it \
  -p 5672:5672 -p 15672:15672 \
  rabbitmq:3-management
```

Management UI: `http://localhost:15672` (default: `guest/guest`)

### Option B — Local install
Start RabbitMQ and keep the default ports.

---

## Keys setup (required for signature)

The key loader uses a **fixed Windows path** in `cript/KeyReaderExample.java`:
`C:\Users\Afonso\Desktop\keys`

You have two options:

1) **Create keys in that folder** (Windows), or  
2) **Edit the constant** `DIRECTORY_PATH` to point to your machine.

To generate keys, run:
- `cript.KeyGeneratorExample`

It will create:
- `private.key`
- `public.key`

> Tip: don’t commit your `private.key` to Git.

---

## How to run (recommended order)

You can run each service from your IDE (multiple run configurations) **or** from the terminal.

### Build once
```bash
mvn -q -DskipTests package
```

### Start services (each in a separate terminal)

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

4) (Optional) **AssinantePromocoes** (subscribe to `rio/salvador/manaus`)
```bash
mvn -q exec:java -Dexec.mainClass=com.exemplo.rabbitmq.AssinantePromocoes
```

5) (Optional) **MSMarketing** (publishes promotions with delays)
```bash
mvn -q exec:java -Dexec.mainClass=com.exemplo.rabbitmq.MSMarketing
```

---

## Why “Non-Spring”?

This project intentionally avoids Spring to demonstrate:
- manual exchange/queue declarations
- routing key filtering
- raw RabbitMQ Java client usage
- the core mechanics behind Spring AMQP abstractions

---

## Possible improvements (roadmap ideas)

- Use **JSON** messages (schema/versioning)
- Add **manual ack**, retries and **DLQ**
- Add correlation IDs / tracing metadata
- Externalize configuration (host, exchange names, key paths)
- Split into multi-module (one module per microservice)
- Provide a Spring AMQP version for comparison

---

## License

Educational/demo project — feel free to use as a reference.
