package com.example.surat_challan.Controller;

import com.example.surat_challan.Service.ChallanService;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class ChallanController {

    @Autowired
    private ChallanService challanService;

    @GetMapping("/challan")
    public ResponseEntity<Object> getChallanDetails(@RequestParam String vehicleNumber, @RequestParam String city) {
        try {
            if (StringUtils.isEmpty(vehicleNumber) || StringUtils.isEmpty(city) ) {
                return ResponseEntity.badRequest().body("Vehicle number and city are required.");
            }

            List<Map<String, String>> challanData = challanService.getChallanDataByCity(city, vehicleNumber);

            if (CollectionUtils.isEmpty(challanData)) {
                return ResponseEntity.ok("No challan data found for the provided details.");
            }
            return ResponseEntity.ok(challanData);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("An internal server error occurred.");
        }
    }
}
