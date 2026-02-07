package com.imgbed.security.policy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentPolicyRepository extends JpaRepository<ContentPolicy, String> {
}
