package com.example.surat_challan.Service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChallanService {

    public static void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, String>> getChallanDataByCity(String city, String vehicleNumber) throws IOException {
        if (city == null || vehicleNumber == null) {
            throw new IllegalArgumentException("City and vehicle number are required.");
        }

        if (city.equalsIgnoreCase("surat")) {
            String url = "https://www.suratcitypolice.org/home/search";
            return getChallanData(url, vehicleNumber, " Go !", "table[cellspacing=0][width=100%][border=1]", null, city);
        } else if (city.equalsIgnoreCase("vadodara")) {
            String url = "https://vadodaraechallan.co.in";
            return getChallanData(url, vehicleNumber, " Go !", null, "section.challan-list", city);
        } else {
            throw new IllegalArgumentException("City is not valid! Supported cities are Surat and Vadodara.");
        }
    }

    private List<Map<String, String>> getChallanData(String url, String vehicleNumber, String submitButtonValue, String tableSelector, String sectionSelector, String city) throws IOException {
        Connection.Response response = Jsoup.connect(url)
                .method(Connection.Method.GET)
                .ignoreContentType(true)
                .execute();
        Document doc = response.parse();

        Map<String, String> formData = new HashMap<>();
        Elements hiddenInputs = doc.select("form input[type=hidden]");
        for (Element input : hiddenInputs) {
            formData.put(input.attr("name"), input.attr("value"));
        }
        if (city.equalsIgnoreCase("surat")) {
            formData.put("vehicleno", vehicleNumber);
        }
        else if (city.equalsIgnoreCase("vadodara")) {
            formData.put("ctl00$ContentPlaceHolder1$txtVehicleNo", vehicleNumber);
            formData.put("ctl00$ContentPlaceHolder1$btnSubmit", submitButtonValue);
        }

        disableSSLVerification();
        Document postDoc = Jsoup.connect(url)
                .data(formData)
                .cookies(response.cookies())
                .post();

        List<Map<String, String>> challans = new ArrayList<>();
        if (tableSelector != null) {
            Element table = postDoc.selectFirst(tableSelector);
            if (table == null) {
                return new ArrayList<>();
            }
            List<String> headersList = new ArrayList<>();
            Elements headerCells = table.select("thead th");
            if (!headerCells.isEmpty()) {
                for (Element headerCell : headerCells) {
                    headersList.add(headerCell.text().trim());
                }
            } else {
                return new ArrayList<>();
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
