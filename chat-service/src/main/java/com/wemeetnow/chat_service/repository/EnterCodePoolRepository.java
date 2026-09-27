package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.EnterCodePool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnterCodePoolRepository extends JpaRepository<EnterCodePool, Long> {
    @Query(value = """
            SELECT ecp.*
            FROM enter_code_pool ecp
            WHERE ecp.code_date = :codeDate
            AND ecp.use_yn = 'Y'
            """, nativeQuery = true)
    List<EnterCodePool> findUsedCodesByDate(@Param("codeDate") LocalDate codeDate);

    @Query(value = """
            SELECT ecp.*
            FROM enter_code_pool ecp
            WHERE ecp.code_date = :codeDate
            AND ecp.random_code = :randomCode
            """, nativeQuery = true)
    Optional<EnterCodePool> findByCodeDateAndRandomCode(@Param("codeDate") LocalDate codeDate, @Param("randomCode") String randomCode);
}
