package com.learnova.elearning.module.payment.repository;

import com.learnova.elearning.module.payment.entity.PaymentOrder;
import com.learnova.elearning.module.payment.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    Optional<PaymentOrder> findByStudent_IdAndCourse_IdAndStatus(Long studentId, Long courseId, PaymentStatus status);

    boolean existsByStudent_IdAndCourse_IdAndStatus(Long studentId, Long courseId, PaymentStatus status);

    boolean existsByOrderCode(Long orderCode);
}
