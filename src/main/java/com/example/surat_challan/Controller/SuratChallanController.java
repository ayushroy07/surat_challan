package com.example.surat_challan.Controller;
import com.example.surat_challan.Service.SuratChallanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class SuratChallanController {

    @Autowired
    private SuratChallanService challanService;

    @GetMapping("/challan")
    public List<Map<String, String>> getChallanDetails(@RequestParam String vehicleNumber) {
        try {
            return challanService.getChallanData(vehicleNumber);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

