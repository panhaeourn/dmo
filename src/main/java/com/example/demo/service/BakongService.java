package com.example.demo.service;

import com.example.demo.dto.BakongMerchantConfigDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Course;
import com.example.demo.entity.Enrollment;
import com.example.demo.entity.PaymentTransaction;
import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.PaymentTransactionRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BakongService {

    @Value("${bakong.base-url:https://api-bakong.nbc.gov.kh}")
    private String baseUrl;

    @Value("${bakong.token:}")
    private String token;

    @Value("${bakong.merchant.bakong-account-id}")
    private String merchantBakongAccountId;

    @Value("${bakong.merchant.name}")
    private String merchantName;

    @Value("${bakong.merchant.city:PHNOM PENH}")
    private String merchantCity;

    @Value("${bakong.merchant.account-information}")
    private String accountInformation;

    @Value("${bakong.merchant.acquiring-bank:Bakong}")
    private String acquiringBank;

    @Value("${bakong.qr-expiry-seconds:180}")
    private long qrExpirySeconds;

    @Value("${bakong.currency:USD}")
    private String bakongCurrency;

    private final RestTemplate restTemplate = new RestTemplate();
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AppUserRepository appUserRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    public BakongService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AppUserRepository appUserRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentHistoryRepository paymentHistoryRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.appUserRepository = appUserRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    public BakongMerchantConfigDto getMerchantConfig() {
        BakongMerchantConfigDto dto = new BakongMerchantConfigDto();
        dto.bakongAccountId = merchantBakongAccountId;
        dto.merchantName = merchantName;
        dto.merchantCity = merchantCity;
        dto.currency = normalizedCurrency();
        dto.qrExpirySeconds = (int) qrExpirySeconds;
        return dto;
    }

    public Map<String, Object> generateIndividualKhqr(double amount) {
        return generateIndividualKhqr(amount, qrExpirySeconds);
    }

    public Map<String, Object> generateIndividualKhqr(double amount, long expirySeconds) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        if (expirySeconds <= 0) {
            expirySeconds = qrExpirySeconds;
        }

        IndividualInfo info = new IndividualInfo();
        info.setBakongAccountId(merchantBakongAccountId);
        info.setAccountInformation(accountInformation);
        info.setAcquiringBank(acquiringBank);
        info.setCurrency(resolveKhqrCurrency());
        info.setAmount(amount);
        info.setMerchantName(merchantName);
        info.setMerchantCity(merchantCity);

        long nowMs = System.currentTimeMillis();
        long expiryMs = nowMs + (expirySeconds * 1000L);

        boolean timestampSet =
                tryCall(info, "setExpirationTimestamp", expiryMs) ||
                        tryCall(info, "setExpirationTime", expiryMs) ||
                        tryCall(info, "setTimestamp", expiryMs) ||
                        tryCall(info, "setExpireTimestamp", expiryMs) ||
                        tryCall(info, "setExpirationTimestamp", String.valueOf(expiryMs)) ||
                        tryCall(info, "setExpirationTime", String.valueOf(expiryMs)) ||
                        tryCall(info, "setTimestamp", String.valueOf(expiryMs)) ||
                        tryCall(info, "setExpireTimestamp", String.valueOf(expiryMs));

        KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(info);

        System.out.println("=== BAKONG DEBUG ===");
        System.out.println("merchantBakongAccountId = " + merchantBakongAccountId);
        System.out.println("accountInformation = " + accountInformation);
        System.out.println("acquiringBank = " + acquiringBank);
        System.out.println("merchantName = " + merchantName);
        System.out.println("merchantCity = " + merchantCity);
        System.out.println("amount = " + amount);
        System.out.println("timestampSet = " + timestampSet);
        System.out.println("expirySeconds = " + expirySeconds);
        System.out.println("expiryMs = " + expiryMs);
        System.out.println("status = " + response.getKHQRStatus());
        System.out.println("data = " + response.getData());
        System.out.println("====================");

        if (response.getKHQRStatus() == null || response.getKHQRStatus().getCode() != 0) {
            String msg = response.getKHQRStatus() != null
                    ? response.getKHQRStatus().getMessage()
                    : "Unknown KHQR error";
            throw new IllegalStateException("Bakong KHQR failed: " + msg);
        }

        KHQRData data = response.getData();
        if (data == null || data.getQr() == null || data.getQr().isBlank()) {
            throw new IllegalStateException("Bakong SDK returned empty QR data");
        }

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("amount", amount);
        out.put("qr", data.getQr());
        out.put("md5", data.getMd5());
        out.put("expiresAt", expiryMs);
        out.put("remainingSeconds", expirySeconds);
        return out;
    }

    public Map<String, Object> createCoursePayment(Long courseId, Double amount) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }

        AppUser currentUser = getCurrentUser();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        boolean alreadyEnrolled = enrollmentRepository.existsByUserAndCourse(currentUser, course);
        if (alreadyEnrolled) {
            Map<String, Object> out = new HashMap<>();
            out.put("success", true);
            out.put("alreadyUnlocked", true);
            out.put("courseId", courseId);
            out.put("message", "Course already unlocked");
            return out;
        }

        Map<String, Object> qrData = generateIndividualKhqr(amount);

        String qr = String.valueOf(qrData.get("qr"));
        String md5 = String.valueOf(qrData.get("md5"));

        Object expiresObj = qrData.get("expiresAt");
        long expiresAtMs;

        if (expiresObj instanceof Number) {
            expiresAtMs = ((Number) expiresObj).longValue();
        } else {
            expiresAtMs = System.currentTimeMillis() + (qrExpirySeconds * 1000L);
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setAmount(amount);
        tx.setCurrency(normalizedCurrency());
        tx.setStatus("PENDING");
        tx.setCourseId(courseId);
        tx.setProvider("BAKONG");
        tx.setQrString(qr);
        tx.setBakongMd5(md5);
        tx.setUserId(currentUser.getId());
        tx.setExpiresAt(Instant.ofEpochMilli(expiresAtMs));

        paymentTransactionRepository.save(tx);

        System.out.println("=== CREATE COURSE PAYMENT DEBUG ===");
        System.out.println("courseId = " + courseId);
        System.out.println("userId = " + currentUser.getId());
        System.out.println("transactionId = " + tx.getTransactionId());
        System.out.println("saved md5 = " + md5);
        System.out.println("expiresAt = " + tx.getExpiresAt());
        System.out.println("==================================");

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("transactionId", tx.getTransactionId());
        out.put("qr", qr);
        out.put("md5", md5);
        out.put("expiresAt", expiresAtMs);
        out.put("remainingSeconds", qrExpirySeconds);
        out.put("courseId", courseId);
        return out;
    }

    public Map<String, Object> getPaymentStatus(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }

        AppUser currentUser = getCurrentUser();

        PaymentTransaction tx = paymentTransactionRepository
                .findByTransactionIdAndUserId(transactionId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment transaction not found"));

        System.out.println("=== PAYMENT STATUS DEBUG ===");
        System.out.println("transactionId = " + transactionId);
        System.out.println("currentUser = " + currentUser.getEmail());
        System.out.println("tx.id = " + tx.getId());
        System.out.println("tx.status = " + tx.getStatus());
        System.out.println("tx.md5 = " + tx.getBakongMd5());
        System.out.println("tx.expiresAt = " + tx.getExpiresAt());
        System.out.println("now = " + Instant.now());

        Course course = courseRepository.findById(tx.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        boolean alreadyEnrolled = enrollmentRepository.existsByUserAndCourse(currentUser, course);
        System.out.println("alreadyEnrolled = " + alreadyEnrolled);

        if ("PAID".equalsIgnoreCase(tx.getStatus()) || alreadyEnrolled) {
            if (!alreadyEnrolled) {
                Enrollment enrollment = new Enrollment(currentUser, course);
                enrollmentRepository.save(enrollment);
            }

            paymentHistoryRepository
                    .findFirstByTransactionRefOrderByIdDesc(tx.getTransactionId())
                    .orElseGet(() -> {
                        PaymentHistory history = new PaymentHistory();
                        history.setPaymentType("COURSE");
                        history.setCourseId(course.getId());
                        history.setCourseName(course.getTitle());
                        history.setStudentId(currentUser.getEmail());
                        history.setStudentName(currentUser.getName());
                        history.setAmount(tx.getAmount());
                        history.setPaymentMethod("BAKONG");
                        history.setTransactionRef(tx.getTransactionId());
                        history.setBakongMd5(tx.getBakongMd5());
                        history.setStatus("PAID");

                        LocalDateTime now = LocalDateTime.now();
                        history.setCreatedAt(now);
                        history.setUpdatedAt(now);
                        history.setPaidAt(now);

                        return paymentHistoryRepository.save(history);
                    });

            Map<String, Object> out = new HashMap<>();
            out.put("success", true);
            out.put("paid", true);
            out.put("unlocked", true);
            out.put("status", "PAID");
            out.put("courseId", tx.getCourseId());
            return out;
        }

        if (tx.getExpiresAt() != null && Instant.now().isAfter(tx.getExpiresAt())) {
            tx.setStatus("EXPIRED");
            paymentTransactionRepository.save(tx);

            System.out.println("payment status = EXPIRED");

            Map<String, Object> out = new HashMap<>();
            out.put("success", true);
            out.put("paid", false);
            out.put("unlocked", false);
            out.put("status", "EXPIRED");
            out.put("courseId", tx.getCourseId());
            return out;
        }

        if (tx.getBakongMd5() == null || tx.getBakongMd5().isBlank()) {
            throw new IllegalStateException("Saved payment md5 is empty");
        }

        Map<String, Object> check = checkTransactionByMd5(tx.getBakongMd5());
        System.out.println("Bakong raw check response = " + check);

        boolean paid = isBakongPaid(check);
        System.out.println("isBakongPaid = " + paid);

        if (paid) {
            tx.setStatus("PAID");
            tx.setPaidAt(Instant.now());
            paymentTransactionRepository.save(tx);

            if (!alreadyEnrolled) {
                Enrollment enrollment = new Enrollment(currentUser, course);
                enrollmentRepository.save(enrollment);
            }

            paymentHistoryRepository
                    .findFirstByTransactionRefOrderByIdDesc(tx.getTransactionId())
                    .orElseGet(() -> {
                        PaymentHistory history = new PaymentHistory();
                        history.setPaymentType("COURSE");
                        history.setCourseId(course.getId());
                        history.setCourseName(course.getTitle());
                        history.setStudentId(currentUser.getEmail());
                        history.setStudentName(currentUser.getName());
                        history.setAmount(tx.getAmount());
                        history.setPaymentMethod("BAKONG");
                        history.setTransactionRef(tx.getTransactionId());
                        history.setBakongMd5(tx.getBakongMd5());
                        history.setStatus("PAID");

                        LocalDateTime now = LocalDateTime.now();
                        history.setCreatedAt(now);
                        history.setUpdatedAt(now);
                        history.setPaidAt(now);

                        return paymentHistoryRepository.save(history);
                    });

            System.out.println("PAYMENT DETECTED AS PAID");

            Map<String, Object> out = new HashMap<>();
            out.put("success", true);
            out.put("paid", true);
            out.put("unlocked", true);
            out.put("status", "PAID");
            out.put("courseId", tx.getCourseId());
            return out;
        }

        System.out.println("payment status = PENDING");

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("paid", false);
        out.put("unlocked", false);
        out.put("status", "PENDING");
        out.put("courseId", tx.getCourseId());
        return out;
    }

    private boolean isBakongPaid(Map<String, Object> check) {
        System.out.println("=== isBakongPaid DEBUG ===");
        System.out.println("full check = " + check);

        if (check == null) {
            return false;
        }

        Object dataObj = check.get("data");
        System.out.println("dataObj = " + dataObj);

        if (dataObj == null) {
            return false;
        }

        return containsPaidSignal(dataObj);
    }

    private boolean containsPaidSignal(Object obj) {
        if (obj == null) {
            return false;
        }

        String text = String.valueOf(obj).toLowerCase();
        System.out.println("containsPaidSignal text = " + text);

        if (text.contains("paid")
                || text.contains("success")
                || text.contains("completed")
                || text.contains("approved")
                || text.contains("settled")) {
            return true;
        }

        if (obj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                System.out.println("map key = " + key + ", value = " + value);

                if (containsPaidSignal(value)) {
                    return true;
                }
            }
        }

        if (obj instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (containsPaidSignal(item)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkTransactionByMd5(String md5) {
        if (md5 == null || md5.isBlank()) {
            throw new IllegalArgumentException("md5 is required");
        }

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("bakong.token is empty");
        }

        String url = baseUrl + "/v1/check_transaction_by_md5";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("md5", md5);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("md5", md5);
        out.put("data", response.getBody());
        return out;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkBakongAccount(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId is required");
        }

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("bakong.token is empty");
        }

        String url = baseUrl + "/v1/check_bakong_account";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("accountId", accountId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("accountId", accountId);
        out.put("data", response.getBody());
        return out;
    }

    public Map<String, Object> checkPaymentAndUnlock(String md5, Long courseId) {
        Map<String, Object> check = checkTransactionByMd5(md5);

        boolean paid = isBakongPaid(check);

        if (!paid) {
            throw new IllegalStateException("Payment not completed yet");
        }

        AppUser currentUser = getCurrentUser();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        boolean alreadyEnrolled = enrollmentRepository.existsByUserAndCourse(currentUser, course);

        if (!alreadyEnrolled) {
            Enrollment enrollment = new Enrollment(currentUser, course);
            enrollmentRepository.save(enrollment);
        }

        paymentHistoryRepository
                .findFirstByTransactionRefOrderByIdDesc(md5)
                .orElseGet(() -> {
                    PaymentHistory history = new PaymentHistory();
                    history.setPaymentType("COURSE");
                    history.setCourseId(course.getId());
                    history.setCourseName(course.getTitle());
                    history.setStudentId(currentUser.getEmail());
                    history.setStudentName(currentUser.getName());
                    history.setAmount(course.getPrice());
                    history.setPaymentMethod("BAKONG");
                    history.setTransactionRef(md5);
                    history.setBakongMd5(md5);
                    history.setStatus("PAID");

                    LocalDateTime now = LocalDateTime.now();
                    history.setCreatedAt(now);
                    history.setUpdatedAt(now);
                    history.setPaidAt(now);

                    return paymentHistoryRepository.save(history);
                });

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("paid", true);
        out.put("unlocked", true);
        out.put("courseId", courseId);
        out.put("message", "Course unlocked");
        return out;
    }

    private AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        String email = null;

        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        }

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            email = userDetails.getUsername();
        }

        if ((email == null || email.isBlank()) && authentication.getName() != null) {
            String name = authentication.getName();
            if (!"anonymousUser".equals(name)) {
                email = name;
            }
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Cannot resolve user email from authentication");
        }

        final String resolvedEmail = email;

        return appUserRepository.findByEmail(resolvedEmail)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + resolvedEmail));
    }

    private boolean tryCall(Object target, String methodName, Object arg) {
        if (target == null || arg == null) {
            return false;
        }

        try {
            for (Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(methodName) || m.getParameterCount() != 1) {
                    continue;
                }

                Class<?> p = m.getParameterTypes()[0];

                if ((p == long.class || p == Long.class) && arg instanceof Long) {
                    m.invoke(target, arg);
                    return true;
                }

                if (p == String.class) {
                    m.invoke(target, String.valueOf(arg));
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private String normalizedCurrency() {
        String value = bakongCurrency == null ? "" : bakongCurrency.trim().toUpperCase();
        return "KHR".equals(value) ? "KHR" : "USD";
    }

    private KHQRCurrency resolveKhqrCurrency() {
        return "KHR".equals(normalizedCurrency()) ? KHQRCurrency.KHR : KHQRCurrency.USD;
    }
}
