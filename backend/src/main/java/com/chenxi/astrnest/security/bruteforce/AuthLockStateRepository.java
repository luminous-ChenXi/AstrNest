package com.chenxi.astrnest.security.bruteforce;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLockStateRepository extends JpaRepository<AuthLockState, Long> {

  Optional<AuthLockState> findByUsernameAndIpAndDimension(String username, String ip, LockDimension dimension);

  List<AuthLockState> findByLockedUntilAfter(Instant now);
}
