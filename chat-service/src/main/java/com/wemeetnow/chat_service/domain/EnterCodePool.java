package com.wemeetnow.chat_service.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "enter_code_pool", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code_date", "random_code"})
})
public class EnterCodePool extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pool_id")
    private Long poolId;

    @Column(name = "code_date", nullable = false)
    private LocalDate codeDate;

    @Column(name = "random_code", nullable = false, length = 4)
    private String randomCode;

    @Column(name = "use_yn", nullable = false)
    private Character useYn;

    @Builder
    public EnterCodePool(LocalDate codeDate, String randomCode, Character useYn) {
        this.codeDate = codeDate;
        this.randomCode = randomCode;
        this.useYn = useYn;
    }
}
