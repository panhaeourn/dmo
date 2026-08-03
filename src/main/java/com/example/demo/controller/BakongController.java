package com.example.demo.controller;

import com.example.demo.service.BakongService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bakong")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class BakongController {

    private final BakongService bakongService;

    public BakongController(BakongService bakongService) {
        this.bakongService = bakongService;
    }

    @GetMapping("/merchant")
    public Object merchant() {
        return bakongService.getMerchantConfig();
    }

    @GetMapping("/qr")
    public Map<String, Object> qr(
            @RequestParam double amount,
            @RequestParam(required = false) Long expirySeconds
    ) {
        if (expirySeconds == null || expirySeconds <= 0) {
            return bakongService.generateIndividualKhqr(amount);
        }
        return bakongService.generateIndividualKhqr(amount, expirySeconds);
    }

    @PostMapping("/check")
    public Map<String, Object> check(@RequestBody Map<String, String> body) {
        return bakongService.checkTransactionByMd5(body.get("md5"));
    }

    @PostMapping("/check-payment")
    public Map<String, Object> checkPayment(@RequestBody Map<String, Object> body) {
        String md5 = String.valueOf(body.get("md5"));
        Long courseId = Long.valueOf(String.valueOf(body.get("courseId")));
        return bakongService.checkPaymentAndUnlock(md5, courseId);
    }

    @PostMapping("/course-payment")
    public Map<String, Object> createCoursePayment(@RequestBody Map<String, Object> body) {
        Long courseId = Long.valueOf(String.valueOf(body.get("courseId")));
        Double amount = Double.valueOf(String.valueOf(body.get("amount")));
        return bakongService.createCoursePayment(courseId, amount);
    }

    @GetMapping("/payment-status/{transactionId}")
    public Map<String, Object> paymentStatus(@PathVariable String transactionId) {
        return bakongService.getPaymentStatus(transactionId);
    }

    @PostMapping("/manual-unlock/{transactionId}")
    public Map<String, Object> manualUnlock(@PathVariable String transactionId) {
        return bakongService.manualUnlockCoursePayment(transactionId);
    }
}
