package org.lessonsmentor.sorting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final HttpClient httpClient;
    private final Semaphore semaphore;
    private AtomicInteger requestCount;
    private final Duration period;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.semaphore = new Semaphore(requestLimit);
        this.requestCount = new AtomicInteger(0);
        this.period = Duration.ofMillis(timeUnit.toMillis(1));
        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            requestCount.set(0);
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, 0, this.period.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void createDocument(String jsonDocument, String signature) throws InterruptedException {
        if (semaphore.tryAcquire()) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer token")
                        .header("Signature", signature)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            System.out.println("Response status code: " + response.statusCode());
                            System.out.println("Response body: " + response.body());
                        });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                requestCount.incrementAndGet();
            }
        } else {
            System.out.println("                       ,         ...");
            Thread.sleep(period.toMillis());
            createDocument(jsonDocument, signature);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        String jsonDocument = "{\"description\": { \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\", \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true, \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\", \"products\": [ { \"certificate_document\": \"string\", \"certificate_document_date\": \"2020-01-23\", \"certificate_document_number\": \"string\", \"owner_inn\": \"string\", \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\", \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ], \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
        String signature = "signature_string";

        crptApi.createDocument(jsonDocument, signature);
    }


    class Document {
        private String description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;
    }

    class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

}
