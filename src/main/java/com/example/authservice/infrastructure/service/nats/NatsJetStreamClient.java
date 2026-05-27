package com.example.authservice.infrastructure.service.nats;

import io.nats.client.JetStream;
import io.nats.client.Connection;
import io.nats.client.Options;
import io.nats.client.api.PublishAck;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.MessageHandler;

public class NatsJetStreamClient {
    private final Connection natsConnection;
    private final JetStream jetStream;

    public NatsJetStreamClient(String origin, String username, String password ) throws Exception {
        Options options = new Options.Builder()
            .server(origin)
            .userInfo(username, password)
            .maxReconnects(5)
            .build();

        this.natsConnection = Nats.connect(options);
        this.jetStream = natsConnection.jetStream();
    }

    public void publish(String subject, String message) throws Exception {
        PublishAck ack = jetStream.publish(subject, message.getBytes());
        System.out.println("Published to " + subject + ", ack: " + ack);
    }

    public void subscribe(String subject, MessageHandler handler) {
        Dispatcher dispatcher = natsConnection.createDispatcher(handler);

        dispatcher.subscribe(subject);
        System.out.println("Subscribed to " + subject);
    }


    public JetStream getJetStream() {
        return jetStream;
    }
} 
