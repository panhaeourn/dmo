package com.example.demo.dto;

import java.util.List;

public record CertificatePublishRequest(List<String> verificationCodes) {
}
