package com.chenxi.astrnest.user.login;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginEventRepository extends JpaRepository<UserLoginEvent, Long> {

  Page<UserLoginEvent> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);
}
