package com.example.surat_challan.Service;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ChallanService {

    private final OkHttpClient client;

    public ChallanService() {
        this.client = getUnsafeOkHttpClient();
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final X509TrustManager trustAllCerts = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException { }
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustAllCerts }, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustAllCerts)
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create unsafe OkHttpClient", e);
        }
    }

    public List<Map<String, String>> getChallanDataByCity(String city, String vehicleNumber) throws IOException {
        if (city.equalsIgnoreCase("surat")) {
            String url = "https://www.suratcitypolice.org/home/search";
            return getChallanDataWithOkHttp(url, vehicleNumber, "vehicleno", "table[cellspacing=0][width=100%][border=1]", null);
        } else if (city.equalsIgnoreCase("vadodara")) {
            String url = "https://vadodaraechallan.co.in";
            return getChallanDataWithOkHttp(url, vehicleNumber, "ctl00$ContentPlaceHolder1$txtVehicleNo", null, "section.challan-list");
        } else {
            throw new IllegalArgumentException("City is not valid! Supported cities are Surat and Vadodara.");
        }
    }

    private List<Map<String, String>> getChallanDataWithOkHttp(String url,
                                                               String vehicleNumber,
                                                               String vehicleNumberField,
                                                               String tableSelector,
                                                               String sectionSelector) throws IOException {

        Request getRequest = new Request.Builder().url(url).get().build();
        try (Response getResponse = client.newCall(getRequest).execute()) {
            if (!getResponse.isSuccessful() || getResponse.body() == null) {
                throw new IOException("Failed to fetch initial page: " + getResponse.code());
            }

            String getHtml = getResponse.body().string();
            Document doc = Jsoup.parse(getHtml, url);


            Map<String, String> formData = new HashMap<>();
            Elements hiddenInputs = doc.select("form input[type=hidden]");
            for (Element input : hiddenInputs) {
                String name = input.attr("name");
                String value = input.attr("value");
                if (name != null && !name.isEmpty()) {
                    formData.put(name, value != null ? value : "");
                }
            }

            formData.put(vehicleNumberField, vehicleNumber);
            formData.putIfAbsent("ctl00$ContentPlaceHolder1$btnSubmit", " Go !");

            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<String, String> e : formData.entrySet()) {
                if (e.getKey() != null && !e.getKey().isEmpty()) {
                    formBuilder.add(e.getKey(), e.getValue() == null ? "" : e.getValue());
                }
            }
            RequestBody postBody = formBuilder.build();

            Request.Builder postReqBuilder = new Request.Builder()
                    .url(url)
                    .post(postBody);

            List<String> setCookies = getResponse.headers("Set-Cookie");
            if (!setCookies.isEmpty()) {
                postReqBuilder.header("Cookie", getCookiesAsString(setCookies));
            }

            Request postRequest = postReqBuilder.build();

            try (Response postResponse = client.newCall(postRequest).execute()) {
                if (!postResponse.isSuccessful() || postResponse.body() == null) {
                    throw new IOException("Failed to post form data: " + postResponse.code());
                }

                String postHtml = postResponse.body().string();
                Document postDoc = Jsoup.parse(postHtml, url);

                // Parse results based on provided selectors
                List<Map<String, String>> challans = new ArrayList<>();

                if (tableSelector != null) {
                    Element table = postDoc.selectFirst(tableSelector);
                    if (table == null) return new ArrayList<>();

                    Elements headerCells = table.select("thead th");
                    List<String> headersList = new ArrayList<>();
                    for (Element header : headerCells) {
                        headersList.add(header.text().trim());
                    }

                    Elements dataRows = table.select("tbody tr");
                    for (Element row : dataRows) {
                        Elements cells = row.select("td");
                        if (cells.size() == headersList.size()) {
                            Map<String, String> challan = new HashMap<>();
                            for (int i = 0; i < headersList.size(); i++) {
                                challan.put(headersList.get(i), cells.get(i).text().trim());
                            }
                            challans.add(challan);
                        }
                    }
                } else if (sectionSelector != null) {
                    Elements challanSections = postDoc.select(sectionSelector);
                    for (Element section : challanSections) {
                        Map<String, String> challan = new HashMap<>();

                        Element dateElement = section.selectFirst("span[id~=lblNoticeDate]");
                        Element noticeNoElement = section.selectFirst("span[id~=lblNoticeNo]");
                        Element amountElement = section.selectFirst("span[id~=lblAmount]");
                        Element violationTypeElement = section.selectFirst("span[id~=lblViolationType]");
                        Element placeElement = section.selectFirst("span[id~=lblPlace]");

                        if (dateElement != null) challan.put("date", dateElement.text().trim());
                        if (noticeNoElement != null) challan.put("notice_number", noticeNoElement.text().trim());
                        if (amountElement != null) challan.put("amount", amountElement.text().trim());
                        if (violationTypeElement != null) challan.put("violation_type", violationTypeElement.text().trim());
                        if (placeElement != null) challan.put("place", placeElement.text().trim());

                        challans.add(challan);
                    }
                }

                return challans;
            }
        }
    }

    private String getCookiesAsString(List<String> cookies) {
        List<String> pairs = new ArrayList<>();
        for (String cookie : cookies) {
            String[] parts = cookie.split(";", 2);
            if (parts.length > 0 && parts[0] != null && !parts[0].isEmpty()) {
                pairs.add(parts[0]);
            }
        }
        return String.join("; ", pairs);
    }
}
