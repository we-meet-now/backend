package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.domain.EnterCodeMgmt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnterCodeMgmtRepository extends JpaRepository<EnterCodeMgmt, Long> {
    @Query(value = """
            SELECT ecm.*
            FROM enter_code_mgmt ecm
            WHERE ecm.enter_code LIKE :datePrefix || '%'
            """, nativeQuery = true)
    List<EnterCodeMgmt> findByEnterCodePrefix(@Param("datePrefix") String datePrefix);

    @Query(value = """
            SELECT ecm.*
            FROM enter_code_mgmt ecm
            WHERE ecm.enter_code = :enterCode
            """, nativeQuery = true)
    EnterCodeMgmt findByEnterCode(@Param("enterCode") String enterCode);
}
