package com.example.demo.repository;

import com.example.demo.entity.ReceptionistClaimCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ReceptionistClaimCodeRepository extends JpaRepository<ReceptionistClaimCode, Long> {

    Optional<ReceptionistClaimCode> findByCode(String code);

    List<ReceptionistClaimCode> findAllByOrderByIdDesc();

}