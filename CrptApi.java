import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger requestCount;
    private final int requestLimit;
    private final long timeIntervalMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
        this.requestCount = new AtomicInteger(0);
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);

        this.scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
            requestCount.set(0);
        }, this.timeIntervalMillis, this.timeIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public String createDocumentForGoodsIntroduction(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();

        try {
            RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(document),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Signature", signature)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                return response.body().string();
            }
        } finally {
            requestCount.incrementAndGet();
        }
    }

    public static class Document {
        public Description description;
        public String docId;
        public String docStatus;
        public String docType;
        public boolean importRequest;
        public String ownerInn;
        public String participantInn;
        public String producerInn;
        public String productionDate;
        public String productionType;
        public Product[] products;
        public String regDate;
        public String regNumber;

        public static class Description {
            public String participantInn;

            public Description() {}

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }

            public String getParticipantInn() {
                return participantInn;
            }

            public void setParticipantInn(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        public static class Product {
            public String certificateDocument;
            public String certificateDocumentDate;
            public String certificateDocumentNumber;
            public String ownerInn;
            public String producerInn;
            public String productionDate;
            public String tnvedCode;
            public String uitCode;
            public String uituCode;

            public Product() {}

            public Product(String certificateDocument, String certificateDocumentDate, String certificateDocumentNumber,
                           String ownerInn, String producerInn, String productionDate, String tnvedCode,
                           String uitCode, String uituCode) {
                this.certificateDocument = certificateDocument;
                this.certificateDocumentDate = certificateDocumentDate;
                this.certificateDocumentNumber = certificateDocumentNumber;
                this.ownerInn = ownerInn;
                this.producerInn = producerInn;
                this.productionDate = productionDate;
                this.tnvedCode = tnvedCode;
                this.uitCode = uitCode;
                this.uituCode = uituCode;
            }

            //геттеры сеттеры
        }

        public Document() {}

        public Document(Description description, String docId, String docStatus, String docType,
                        boolean importRequest, String ownerInn, String participantInn, String producerInn,
                        String productionDate, String productionType, Product[] products,
                        String regDate, String regNumber) {
            this.description = description;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
			//геттеры сеттеры
        }

       
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);

        Document.Description description = new Document.Description("1234567890");
        Document.Product product = new Document.Product("certDoc", "2020-01-23", "certNum", "ownerInn", "producerInn", "2020-01-23", "tnvedCode", "uitCode", "uituCode");
        Document document = new Document(description, "docId", "docStatus", "LP_INTRODUCE_GOODS", true, "ownerInn", "participantInn", "producerInn", "2020-01-23", "productionType", new Document.Product[]{product}, "2020-01-23", "regNumber");

        String signature = "someSignature";
        String response = api.createDocumentForGoodsIntroduction(document, signature);

        System.out.println(response);
    }
}