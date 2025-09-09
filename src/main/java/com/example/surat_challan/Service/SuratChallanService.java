package com.example.surat_challan.Service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SuratChallanService {

    public List<Map<String, String>> getChallanData(String vehicleNumber) throws IOException {
        String url = "https://www.suratcitypolice.org/home/search";

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/x-www-form-urlencoded");
        headers.put("Cookie", "ci_session=a%3A5%3A%7Bs%3A10%3A%22session_id%22%3Bs%3A32%3A%22d5ffba805252e8055ec7e2ff434ae648%22%3Bs%3A10%3A%22ip_address%22%3Bs%3A14%3A%2213.200.118.132%22%3Bs%3A10%3A%22user_agent%22%3Bs%3A117%3A%22Mozilla%2F5.0%20%28Macintosh%3B%20Intel%20Mac%20OS%20X%2010_15_7%29%20AppleWebKit%2F537.36%20%28KHTML%2C%20like%20Gecko%29%20Chrome%2F139.0.0.0%20Safari%2F537.36%22%3Bs%3A13%3A%22last_activity%22%3Bi%3A1757402415%3Bs%3A9%3A%22user_data%22%3Bs%3A0%3A%22%22%3B%7D98a7efb9ffd666f9bb46e90effc6e4245c4d764a");

        Map<String, String> formData = new HashMap<>();
        formData.put("vehicleno", vehicleNumber);

        Document postDoc = Jsoup.connect(url)
                .headers(headers)
                .data(formData)
                .post();

        Element table = postDoc.selectFirst("table[cellspacing=0][width=100%][border=1]");
        if (table == null) {
            return new ArrayList<>();
        }

        List<String> headersList = new ArrayList<>();
        Elements headerCells = table.select("thead th");
        if (headerCells.isEmpty()) {
            return new ArrayList<>();
        }
        for (Element headerCell : headerCells) {
            headersList.add(headerCell.text().trim());
        }

        List<Map<String, String>> challans = new ArrayList<>();
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
        return challans;
    }
}
