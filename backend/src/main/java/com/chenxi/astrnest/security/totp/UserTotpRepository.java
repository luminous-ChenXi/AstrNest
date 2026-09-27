package com.chenxi.astrnest.security.totp;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTotpRepository extends JpaRepository<UserTotp, Long> {

  boolean existsByUserIdAndConfirmedTrue(Long userId);

  @Query("select t.userId from UserTotp t where t.userId in :userIds and t.confirmed = true")
  List<Long> findConfirmedUserIds(@Param("userIds") List<Long> userIds);
}
