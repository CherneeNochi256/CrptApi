package ru.maxim;

import lombok.Data;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CrptApi {

    private final String URL = "http://<server-name>[:server-port]" +
            "/api/v2/{extension}/ rollout?omsId={omsId}";
    private final String CLIENT_TOKEN = "clientToken";
    private final String USER_NAME = "userName";

    private final int requestLimit;
    private final TimeUnit timeUnit;
    private static int counter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        if (requestLimit >= 0) {
            this.requestLimit = requestLimit;
            counter = requestLimit;
        } else {
            throw new IllegalArgumentException("Передано отрицательное число");
        }
    }

    public void runRequest(Document document, String signature) {
        String docJson = getDocJson(document).toString();
        httpRequest(docJson,signature);
    }

    @SuppressWarnings("unchecked")
    private JSONObject getDocJson(Document document) {
        JSONObject doc = new JSONObject();
        if (notNull(document.getDescription())) {
            JSONObject inn = new JSONObject();
            inn.put("participantInn", document.getParticipantInn());
            doc.put("description", inn);
        }
        doc.put("doc_id", document.getDocId());
        doc.put("doc_status", document.getDocStatus());
        doc.put("doc_type", document.getDocType());
        if (notNull(document.getImportRequest())) {
            doc.put("importRequest", document.getImportRequest());
        }
        doc.put("owner_inn", document.getOwnerInn());
        doc.put("participant_inn", document.getParticipantInn());
        doc.put("producer_inn", document.getProducerInn());
        doc.put("production_date", document.getProducerInn());
        doc.put("production_type", document.getProductionType());
        doc.put("reg_date", document.getRegDate());
        doc.put("reg_number", document.getRegNumber());

        Document.Products product = document.getProducts();
        if (product != null) {
            JSONArray productsList = new JSONArray();
            JSONObject products = new JSONObject();
            if (product.getCertificateDocument() != null) {
                products.put("certificate_document", product.getCertificateDocument());
            } else if (notNull(product.getCertificateDocumentDate())) {
                products.put("certificate_document_date", product.getCertificateDocumentDate());
            } else if (notNull(product.getCertificateDocumentNumber())) {
                products.put("certificate_document_number", product.getCertificateDocumentNumber());
            }
            products.put("owner_inn", document.getOwnerInn());
            products.put("producer_inn", document.getProducerInn());
            products.put("production_date", document.getProductionDate());
            if (!document.getProductionDate().equals(product.getProductionDate())) {
                products.put("production_date", product.getProductionDate());
            }
            products.put("tnved_code", product.tnvedCode);
            if (notNull(product.getUitCode())) {
                products.put("uit_code", product.getUitCode());
            } else if (notNull(product.getUituCode())) {
                products.put("uitu_code", product.getUituCode());
            } else {
                throw new IllegalArgumentException("Одно из полей uit_code/uitu_code " +
                        "является обязательным");
            }
            productsList.add(products);
            doc.put("products", productsList);
        }
        return doc;
    }

    private void httpRequest(String json, String signature) {
        if (requestLimit != 0) {
            synchronized (this) {
                counter--;
            }
        }
        try {
            if (counter < 0) {
                Thread.sleep(getTime());
                counter = requestLimit;
            }
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(URL+"/"+signature);

            StringEntity entity = new StringEntity(json);
            post.addHeader("content-type", "application/json");
            post.addHeader("clientToken", CLIENT_TOKEN);
            post.addHeader("userName", USER_NAME);
            post.setEntity(entity);
            httpClient.execute(post);
            httpClient.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean notNull(String check) {
        return check != null;
    }

    public enum TimeUnit {
        SECOND, MINUTE, HOUR
    }

    private long getTime() {
        return switch (timeUnit) {
            case SECOND -> 1000;
            case MINUTE -> 1000 * 60;
            case HOUR -> 1000 * 60 * 60;
        };
    }

    @Data
    public static class Document {
        private String description;
        private final String participantInn;
        private final String docId;
        private final String docStatus;
        private final String docType;
        private String importRequest;
        private final String ownerInn;
        private final String producerInn;
        private final String productionDate;
        private final String productionType;
        private final String regDate;
        private final String regNumber;
        private Products products;

        public Document(String participantInn, String docId, String docStatus,
                        String docType, String ownerInn, String producerInn,
                        String productionDate, String productionType,
                        String regDate, String regNumber) {
            this.participantInn = participantInn;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        @Data
        public static class Products {
            private CertificateType certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            public enum CertificateType {
                CONFORMITY_CERTIFICATE, CONFORMITY_DECLARATION
            }
        }
    }
}