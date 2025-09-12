package com.example.surat_challan.Controller;

import com.example.surat_challan.Service.ChallanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
            if (vehicleNumber == null || vehicleNumber.isEmpty()) {
                return ResponseEntity.badRequest().body("Vehicle number is required.");
            }

            List<Map<String, String>> challanData = challanService.getChallanDataByCity(city, vehicleNumber);

            if (challanData == null || challanData.isEmpty()) {
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
