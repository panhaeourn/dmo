package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.CitoReceipt;
import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.CitoReceiptRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import com.example.demo.service.BakongService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/reception/receipts")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CitoReceiptController {

    private final CitoReceiptRepository receiptRepository;
    private final BakongService bakongService;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final AppUserRepository appUserRepository;

    public CitoReceiptController(
            CitoReceiptRepository receiptRepository,
            BakongService bakongService,
            PaymentHistoryRepository paymentHistoryRepository,
            AppUserRepository appUserRepository
    ) {
        this.receiptRepository = receiptRepository;
        this.bakongService = bakongService;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.appUserRepository = appUserRepository;
    }

    @CrossOrigin(
            origins = "http://localhost:5173",
            allowCredentials = "true",
            methods = {
                    RequestMethod.GET,
                    RequestMethod.POST,
                    RequestMethod.PUT,
                    RequestMethod.PATCH,
                    RequestMethod.DELETE,
                    RequestMethod.OPTIONS
            }
    )
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CitoReceipt request, Authentication authentication) {
        String email = extractEmail(authentication);

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        if (request.getCourseName() == null || request.getCourseName().isBlank()) {
            return ResponseEntity.badRequest().body("Course name is required");
        }

        String receiptType = normalizeReceiptType(request.getReceiptType());
        if (receiptType == null) {
            return ResponseEntity.badRequest().body("Receipt type must be COURSE or MONTHLY");
        }

        if ("MONTHLY".equals(receiptType)
                && (request.getMonthlyPeriod() == null || request.getMonthlyPeriod().isBlank())) {
            return ResponseEntity.badRequest().body("Monthly period is required for monthly receipts");
        }

        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            return ResponseEntity.badRequest().body("Student name is required");
        }

        if (request.getBookPrice() == null || request.getBookPrice() <= 0) {
            return ResponseEntity.badRequest().body("Book price must be greater than 0");
        }

        if (request.getProgramPrice() == null || request.getProgramPrice() <= 0) {
            return ResponseEntity.badRequest().body("Course or monthly price must be greater than 0");
        }

        AppUser receptionist = appUserRepository.findByEmail(email).orElse(null);
        String receptionistName = resolveReceptionistDisplayName(receptionist, email);

        CitoReceipt receipt = new CitoReceipt();
        String generatedStudentId = generateStudentId();
        receipt.setStudentId(generatedStudentId);
        receipt.setStudentCode("MONTHLY".equals(receiptType) ? generatedStudentId : resolveStudentCode(request));
        receipt.setReceiptType(receiptType);
        receipt.setCourseName(request.getCourseName());
        receipt.setMonthlyPeriod("MONTHLY".equals(receiptType) ? request.getMonthlyPeriod() : null);
        receipt.setMonthlyPaidMonths("");
        receipt.setStudentName(request.getStudentName());
        receipt.setStudentNameEnglish(request.getStudentNameEnglish());
        receipt.setStudentNameKhmer(request.getStudentNameKhmer());
        receipt.setGender(request.getGender());
        receipt.setPhone(request.getPhone());
        receipt.setContactInfo(request.getContactInfo());
        receipt.setEmail(request.getEmail());
        receipt.setAddress(request.getAddress());
        receipt.setSchedule(request.getSchedule());
        receipt.setBookPrice(request.getBookPrice());
        receipt.setProgramPrice(request.getProgramPrice());
        receipt.setTotalPrice(request.getBookPrice() + request.getProgramPrice());
        receipt.setPaymentStatus("Pending");
        receipt.setQrImage(request.getQrImage());
        receipt.setQrText(request.getQrText());
        receipt.setBakongTranId(request.getBakongTranId());
        receipt.setCreatedAt(
                request.getCreatedAt() != null && !request.getCreatedAt().isBlank()
                        ? request.getCreatedAt()
                        : LocalDateTime.now().toString()
        );
        receipt.setCreatedByReceptionist(email);
        receipt.setCreatedByReceptionistName(receptionistName);

        CitoReceipt saved = receiptRepository.save(receipt);
        paymentHistoryRepository.save(buildPendingHistory(saved, email));

        decorateReceipt(saved);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<?> getAll(Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        if (isAdmin(authentication)) {
            return ResponseEntity.ok(decorateReceipts(receiptRepository.findAllByOrderByIdDesc()));
        }

        return ResponseEntity.ok(
                decorateReceipts(receiptRepository.findByCreatedByReceptionistIgnoreCaseOrderByIdDesc(email))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        return receiptRepository.findById(id)
                .<ResponseEntity<?>>map(receipt -> {
                    if (!canAccessReceipt(authentication, email, receipt)) {
                        return ResponseEntity.status(403).body("Forbidden");
                    }
                    decorateReceipt(receipt);
                    return ResponseEntity.ok(receipt);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        String email = extractEmail(authentication);

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        return receiptRepository.findById(id)
                .map(receipt -> {
                    if (!canAccessReceipt(authentication, email, receipt)) {
                        return ResponseEntity.status(403).body("Forbidden");
                    }
                    receiptRepository.delete(receipt);
                    return ResponseEntity.ok("Receipt deleted successfully");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/paid")
    public ResponseEntity<?> markPaid(@PathVariable Long id, Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Optional<CitoReceipt> receiptOpt = receiptRepository.findById(id);

        if (receiptOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CitoReceipt receipt = receiptOpt.get();
        if (!canAccessReceipt(authentication, email, receipt)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        if ("MONTHLY".equalsIgnoreCase(receipt.getReceiptType())) {
            String nextUnpaidMonth = findNextUnpaidMonth(receipt);
            if (nextUnpaidMonth == null) {
                return ResponseEntity.badRequest().body("All due months are already marked as paid");
            }

            Set<String> paidMonths = parsePaidMonths(receipt.getMonthlyPaidMonths());
            paidMonths.add(nextUnpaidMonth);
            receipt.setMonthlyPaidMonths(String.join(",", paidMonths));
            receipt.setPaymentStatus(isMonthlyUpToDate(receipt) ? "Paid" : "Pending");
            receiptRepository.save(receipt);

            paymentHistoryRepository.save(buildPaidHistory(
                    receipt,
                    paymentHistoryRepository.findFirstByReceiptIdOrderByIdDesc(receipt.getId()).orElseGet(PaymentHistory::new),
                    "manual",
                    null,
                    "Monthly payment marked as paid for " + nextUnpaidMonth
            ));

            decorateReceipt(receipt);
            return ResponseEntity.ok(receipt);
        }

        receipt.setPaymentStatus("Paid");
        receiptRepository.save(receipt);

        paymentHistoryRepository.save(buildPaidHistory(
                receipt,
                paymentHistoryRepository.findFirstByReceiptIdOrderByIdDesc(receipt.getId()).orElseGet(PaymentHistory::new),
                "manual",
                null,
                "Receipt manually marked as paid"
        ));

        decorateReceipt(receipt);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/{id}/payment-status")
    public ResponseEntity<?> checkReceiptPayment(@PathVariable Long id) {
        return receiptRepository.findById(id)
                .<ResponseEntity<?>>map(receipt -> {
                    if ("Paid".equalsIgnoreCase(receipt.getPaymentStatus())) {
                        return ResponseEntity.ok(receipt);
                    }

                    String md5 = receipt.getBakongTranId();

                    if (md5 != null && !md5.isBlank()) {
                        try {
                            Map<String, Object> check = bakongService.checkTransactionByMd5(md5);

                            Object dataObj = check.get("data");
                            String raw = String.valueOf(dataObj).toLowerCase();

                            boolean paid =
                                    raw.contains("paid")
                                            || raw.contains("completed")
                                            || raw.contains("approved")
                                            || raw.contains("settled");

                            if (paid) {
                                receipt.setPaymentStatus("Paid");
                                receiptRepository.save(receipt);

                                paymentHistoryRepository.save(buildPaidHistory(
                                        receipt,
                                        paymentHistoryRepository.findFirstByReceiptIdOrderByIdDesc(receipt.getId()).orElseGet(PaymentHistory::new),
                                        "system",
                                        String.valueOf(dataObj),
                                        "Receipt payment confirmed from Bakong status check"
                                ));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    decorateReceipt(receipt);
                    return ResponseEntity.ok(receipt);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private synchronized String generateStudentId() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = "CITO" + year;

        Optional<CitoReceipt> latestOpt =
                receiptRepository.findFirstByStudentIdStartingWithOrderByStudentIdDesc(prefix);

        int nextNumber = 1;

        if (latestOpt.isPresent()) {
            String latestId = latestOpt.get().getStudentId();

            if (latestId != null && latestId.length() > prefix.length()) {
                String numberPart = latestId.substring(prefix.length());
                try {
                    nextNumber = Integer.parseInt(numberPart) + 1;
                } catch (NumberFormatException ignored) {
                    nextNumber = 1;
                }
            }
        }

        return prefix + String.format("%03d", nextNumber);
    }

    private String resolveStudentCode(CitoReceipt request) {
        if (request.getStudentCode() != null && !request.getStudentCode().isBlank()) {
            return request.getStudentCode().trim().toUpperCase();
        }

        return null;
    }

    private String extractEmail(Authentication authentication) {
        try {
            if (authentication == null) return null;

            Object principal = authentication.getPrincipal();

            if (principal instanceof OAuth2User oAuth2User) {
                String email = oAuth2User.getAttribute("email");
                if (email != null) return email;
            }

            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            }

            String name = authentication.getName();
            if ("anonymousUser".equals(name)) return null;
            return name;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeReceiptType(String receiptType) {
        if (receiptType == null || receiptType.isBlank()) {
            return "COURSE";
        }

        String normalized = receiptType.trim().toUpperCase();
        if ("COURSE".equals(normalized) || "MONTHLY".equals(normalized)) {
            return normalized;
        }

        return null;
    }

    private Set<String> parsePaidMonths(String paidMonthsCsv) {
        Set<String> paidMonths = new LinkedHashSet<>();
        if (paidMonthsCsv == null || paidMonthsCsv.isBlank()) {
            return paidMonths;
        }

        for (String part : paidMonthsCsv.split(",")) {
            if (part != null && !part.isBlank()) {
                paidMonths.add(part.trim());
            }
        }
        return paidMonths;
    }

    private String findNextUnpaidMonth(CitoReceipt receipt) {
        List<String> dueMonths = buildDueMonths(resolveMonthlyStartPeriod(receipt));
        Set<String> paidMonths = parsePaidMonths(receipt.getMonthlyPaidMonths());

        for (String month : dueMonths) {
            if (!paidMonths.contains(month)) {
                return month;
            }
        }

        return null;
    }

    private boolean isMonthlyUpToDate(CitoReceipt receipt) {
        return findNextUnpaidMonth(receipt) == null;
    }

    private List<String> buildDueMonths(String startPeriod) {
        List<String> periods = new ArrayList<>();
        if (startPeriod == null || startPeriod.isBlank()) {
            return periods;
        }

        try {
            YearMonth start = YearMonth.parse(startPeriod);
            YearMonth current = YearMonth.now();
            YearMonth cursor = start;

            while (!cursor.isAfter(current)) {
                periods.add(cursor.toString());
                cursor = cursor.plusMonths(1);
            }
        } catch (Exception ignored) {
        }

        return periods;
    }

    private String resolveMonthlyStartPeriod(CitoReceipt receipt) {
        if (receipt.getMonthlyPeriod() != null && !receipt.getMonthlyPeriod().isBlank()) {
            return receipt.getMonthlyPeriod();
        }

        try {
            if (receipt.getCreatedAt() != null && !receipt.getCreatedAt().isBlank()) {
                LocalDateTime createdAt = LocalDateTime.parse(receipt.getCreatedAt());
                return YearMonth.from(createdAt).toString();
            }
        } catch (Exception ignored) {
        }

        return YearMonth.now().toString();
    }

    private PaymentHistory buildPendingHistory(CitoReceipt receipt, String checkedBy) {
        PaymentHistory history = baseHistory(receipt);
        LocalDateTime now = LocalDateTime.now();
        history.setStatus("PENDING");
        history.setGatewayResponse(null);
        history.setCreatedAt(now);
        history.setUpdatedAt(now);
        history.setPaidAt(null);
        history.setCheckedBy(checkedBy);
        history.setNote("Receipt created and waiting for payment");
        return history;
    }

    private PaymentHistory buildPaidHistory(
            CitoReceipt receipt,
            PaymentHistory history,
            String defaultCheckedBy,
            String gatewayResponse,
            String note
    ) {
        PaymentHistory target = baseHistory(receipt, history);
        LocalDateTime now = LocalDateTime.now();
        target.setStatus("PAID");
        target.setGatewayResponse(gatewayResponse);
        target.setUpdatedAt(now);
        target.setPaidAt(now);
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(now);
        }
        if (target.getCheckedBy() == null || target.getCheckedBy().isBlank()) {
            target.setCheckedBy(defaultCheckedBy);
        }
        target.setNote(note);
        return target;
    }

    private PaymentHistory baseHistory(CitoReceipt receipt) {
        return baseHistory(receipt, new PaymentHistory());
    }

    private PaymentHistory baseHistory(CitoReceipt receipt, PaymentHistory history) {
        history.setPaymentType("RECEIPT");
        history.setReceiptId(receipt.getId());
        history.setCourseId(null);
        history.setStudentId(receipt.getStudentId());
        history.setStudentName(receipt.getStudentName());
        history.setCourseName(receipt.getCourseName());
        history.setAmount(receipt.getTotalPrice());
        history.setPaymentMethod("BAKONG");
        history.setTransactionRef(receipt.getStudentId());
        history.setBakongMd5(receipt.getBakongTranId());
        return history;
    }

    @GetMapping("/search")
    public List<CitoReceipt> searchReceipts(
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentId,
            Authentication authentication
    ) {
        String email = extractEmail(authentication);
        if (email == null || email.isBlank()) {
            return List.of();
        }

        boolean admin = isAdmin(authentication);

        if (studentName != null && !studentName.isBlank()) {
            if (!admin) {
                return decorateReceipts(
                        receiptRepository.findByCreatedByReceptionistIgnoreCaseAndStudentNameContainingIgnoreCase(
                                email,
                                studentName
                        )
                );
            }
            return decorateReceipts(receiptRepository.findByStudentNameContainingIgnoreCase(studentName));
        }

        if (studentId != null && !studentId.isBlank()) {
            if (!admin) {
                return decorateReceipts(
                        receiptRepository
                                .findByCreatedByReceptionistIgnoreCaseAndStudentIdContainingIgnoreCaseOrCreatedByReceptionistIgnoreCaseAndStudentCodeContainingIgnoreCase(
                                        email,
                                        studentId,
                                        email,
                                        studentId
                                )
                );
            }
            return decorateReceipts(
                    receiptRepository.findByStudentIdContainingIgnoreCaseOrStudentCodeContainingIgnoreCase(studentId, studentId)
            );
        }

        if (!admin) {
            return decorateReceipts(receiptRepository.findByCreatedByReceptionistIgnoreCaseOrderByIdDesc(email));
        }
        return decorateReceipts(receiptRepository.findAllByOrderByIdDesc());
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private boolean canAccessReceipt(Authentication authentication, String email, CitoReceipt receipt) {
        if (isAdmin(authentication)) {
            return true;
        }
        String owner = receipt.getCreatedByReceptionist();
        return owner != null && owner.equalsIgnoreCase(email);
    }

    private List<CitoReceipt> decorateReceipts(List<CitoReceipt> receipts) {
        receipts.forEach(this::decorateReceipt);
        return receipts;
    }

    private void decorateReceipt(CitoReceipt receipt) {
        if (receipt == null) {
            return;
        }

        if (receipt.getCreatedByReceptionistName() != null && !receipt.getCreatedByReceptionistName().isBlank()) {
            return;
        }

        String receptionistEmail = receipt.getCreatedByReceptionist();
        if (receptionistEmail == null || receptionistEmail.isBlank()) {
            receipt.setCreatedByReceptionistName("-");
            return;
        }

        AppUser user = appUserRepository.findByEmail(receptionistEmail).orElse(null);
        receipt.setCreatedByReceptionistName(resolveReceptionistDisplayName(user, receptionistEmail));
    }

    private String resolveReceptionistDisplayName(AppUser user, String fallbackEmail) {
        if (user != null) {
            if (user.getName() != null && !user.getName().isBlank()) {
                return user.getName().trim();
            }
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                return user.getUsername().trim();
            }
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                return user.getEmail().trim();
            }
        }

        return fallbackEmail == null || fallbackEmail.isBlank() ? "-" : fallbackEmail.trim();
    }
}
