package com.wemeetnow.auth_service.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Table(name = "\"user_place\"")
public class UserPlace extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "start_place_nm", length = 255)
    private String startPlaceNm;

    @Column(name = "start_place_desc", length = 255)
    private String startPlaceDesc;

    @Column(name = "post_cd", length = 5)
    private String postCd;

    @Column(name = "addr", length = 255)
    private String addr;

    @Column(name = "detail_addr", length = 255)
    private String detailAddr;

    @Column(name = "order_seq", length = 10)
    private String orderSeq;

    @Builder
    public UserPlace(User user, String startPlaceNm, String startPlaceDesc, String postCd, String addr, String detailAddr, String orderSeq) {
        this.user = user;
        this.startPlaceNm = startPlaceNm;
        this.startPlaceDesc = startPlaceDesc;
        this.postCd = postCd;
        this.addr = addr;
        this.detailAddr = detailAddr;
        this.orderSeq = orderSeq;
    }
}
