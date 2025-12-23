package com.wemeetnow.chat_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseTime {
    @CreatedDate
    @Column(
            name = "inp_date",
            updatable = false,
            nullable = false,
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime inpDate;
    @LastModifiedDate
    @Column(name = "mdfy_date")
    private LocalDateTime mdfyDate;

    @CreatedBy
    @Column(name = "inp_user_id", updatable = false)
    private String inpUserId;

    @LastModifiedBy
    @Column(name = "mdfy_user_id")
    private String mdfyUserId;

    protected void setInpUserId(String inpUserId) {
        this.inpUserId = inpUserId;
    }
}