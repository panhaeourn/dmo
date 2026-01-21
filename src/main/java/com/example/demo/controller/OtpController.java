package com.example.demo.controller;

import com.example.demo.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    public String send(@RequestParam String email) {
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000); // 6 digits
        otpService.saveOtp(email, otp, Duration.ofMinutes(2));
        return "OTP saved in Redis for 2 minutes: " + otp;
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String email, @RequestParam String otp) {
        String stored = otpService.getOtp(email);
        if (stored == null) return "OTP expired";
        if (!stored.equals(otp)) return "OTP not match";

        otpService.deleteOtp(email);
        return "OTP verified ✅";
    }
}
