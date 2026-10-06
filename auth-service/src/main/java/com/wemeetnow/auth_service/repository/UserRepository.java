package com.wemeetnow.auth_service.repository;

import com.wemeetnow.auth_service.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    @Query("select u from User u where u.id in :userIdList")
    List<User> findAllInUserIdList(@Param("userIdList") List<Long> userIdList);
    Optional<User> findById(Long id);

    @Query("select u from User u where u.uuid = :uuid")
    Optional<User> findByUuid(@Param("uuid") String uuid);

    @Query("select case when count(u) > 0 then true else false end from User u where u.uuid = :uuid")
    boolean existsByUuid(@Param("uuid") String uuid);
}
