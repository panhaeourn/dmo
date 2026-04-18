package com.example.demo.repository;

import com.example.demo.entity.CitoReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CitoReceiptRepository extends JpaRepository<CitoReceipt, Long> {

    Optional<CitoReceipt> findByStudentId(String studentId);

    List<CitoReceipt> findAllByOrderByIdDesc();
    List<CitoReceipt> findByCreatedByReceptionistIgnoreCaseOrderByIdDesc(String createdByReceptionist);
    List<CitoReceipt> findByStudentNameContainingIgnoreCase(String studentName);
    List<CitoReceipt> findByStudentIdContainingIgnoreCase(String studentId);
    List<CitoReceipt> findByStudentCodeContainingIgnoreCase(String studentCode);
    List<CitoReceipt> findByStudentIdContainingIgnoreCaseOrStudentCodeContainingIgnoreCase(String studentId, String studentCode);
    List<CitoReceipt> findByCreatedByReceptionistIgnoreCaseAndStudentNameContainingIgnoreCase(String createdByReceptionist, String studentName);
    List<CitoReceipt> findByCreatedByReceptionistIgnoreCaseAndStudentIdContainingIgnoreCaseOrCreatedByReceptionistIgnoreCaseAndStudentCodeContainingIgnoreCase(
            String createdByReceptionistForStudentId,
            String studentId,
            String createdByReceptionistForStudentCode,
            String studentCode
    );

    Optional<CitoReceipt> findFirstByStudentIdStartingWithOrderByStudentIdDesc(String prefix);
    Optional<CitoReceipt> findFirstByStudentCodeStartingWithOrderByStudentCodeDesc(String prefix);
    Optional<CitoReceipt> findFirstByStudentCodeIgnoreCase(String studentCode);
}
