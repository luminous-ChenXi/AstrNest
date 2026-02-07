package com.imgbed.security.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByUsername(String username);

  Optional<UserAccount> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query("select count(u) from UserAccount u where u.email is not null and u.email <> ''")
  long countUsersWithEmailBound();

  @Query("select count(distinct u) from UserAccount u join u.roles r where r.name = :roleName")
  long countUsersByRole(@Param("roleName") String roleName);
}
