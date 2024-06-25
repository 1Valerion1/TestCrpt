package org.example;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 1);

        CrptApi.Document document = new CrptApi.Document("123", "nice");
        String signature = "exampleSignature";

        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                try {
                    crptApi.createDocument(document, signature);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
    }
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final int requestLimit;
    private final AtomicInteger requestCount;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(this::resetRequestCount, 0, 1, timeUnit);
    }

    public synchronized void createDocument(Document document, String signature) throws IOException, InterruptedException {

        while (requestCount.get() >= requestLimit) {
            System.out.println("Thread will be paused" );
            Thread.sleep(2000);
            wait();
        }
        requestCount.incrementAndGet();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(convertToJson(document)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("Document created successfully!");

        } else {
            throw new IOException("Failed create document: " + response.body());
        }
        notifyAll();
    }


    private synchronized void resetRequestCount() {
        requestCount.set(0);
        notifyAll();
    }

    private String convertToJson(Document document) throws JsonProcessingException {
        return objectMapper.writeValueAsString(document);
    }

    // DTO-классы
    static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type = "LP_INTRODUCE_GOODS";
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        static class Description {
            public String participantInn;

            public Description() {
            }

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;

            public Product() {

            }

            public Product(String certificate_document, String certificate_document_date, String certificate_document_number,
                           String owner_inn, String producer_inn, String production_date, String tnved_code,
                           String uit_code, String uitu_code) {
                this.certificate_document = certificate_document;
                this.certificate_document_date = certificate_document_date;
                this.certificate_document_number = certificate_document_number;
                this.owner_inn = owner_inn;
                this.producer_inn = producer_inn;
                this.production_date = production_date;
                this.tnved_code = tnved_code;
                this.uit_code = uit_code;
                this.uitu_code = uitu_code;
            }
        }

        public Document(){

        }
        public Document(String doc_id, String doc_status) {
            this.doc_id = doc_id;
            this.doc_status = doc_status;
        }

        public Document(Description description, String doc_id, String doc_status, String doc_type, boolean importRequest,
                        String owner_inn, String participant_inn, String producer_inn, String production_date,
                        String production_type, Product[] products, String reg_date, String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

    }

}
