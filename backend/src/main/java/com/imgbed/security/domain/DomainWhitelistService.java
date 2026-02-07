package com.imgbed.security.domain;

import com.imgbed.security.dto.DomainWhitelistRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainWhitelistService {

  private final DomainWhitelistRepository repository;

  public List<DomainWhitelistEntry> listAll() {
    return repository.findAll();
  }

  @Transactional
  public DomainWhitelistEntry create(DomainWhitelistRequest request) {
    repository.findByDomain(request.domain())
        .ifPresent(existing -> {
          throw new IllegalArgumentException("域名已存在");
        });
    DomainWhitelistEntry entry = new DomainWhitelistEntry();
    entry.setDomain(request.domain());
    entry.setRemark(request.remark());
    entry.setApproved(true);
    return repository.save(entry);
  }

  @Transactional
  public void delete(Long id) {
    repository.deleteById(id);
  }
}
