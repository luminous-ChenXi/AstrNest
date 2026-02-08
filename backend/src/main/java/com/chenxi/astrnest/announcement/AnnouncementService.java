package com.chenxi.astrnest.announcement;

import com.chenxi.astrnest.announcement.dto.AnnouncementPageResponse;
import com.chenxi.astrnest.announcement.dto.AnnouncementRequest;
import com.chenxi.astrnest.announcement.dto.AnnouncementResponse;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserRole;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

  private final AnnouncementRepository announcementRepository;
  private final UserAccountRepository userAccountRepository;

  @Transactional
  public AnnouncementResponse create(AnnouncementRequest request) {
    Announcement announcement = new Announcement();
    applyRequest(announcement, request, true);
    Announcement saved = announcementRepository.save(announcement);
    return toResponse(saved);
  }

  @Transactional
  public AnnouncementResponse update(@NonNull Long id, AnnouncementRequest request) {
    Announcement announcement = announcementRepository.findById(id)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Announcement not found"));
    applyRequest(announcement, request, false);
    Announcement saved = announcementRepository.save(Objects.requireNonNull(announcement));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public AnnouncementResponse getPublic(@NonNull Long id) {
    return announcementRepository.findOne(publicSpecification(null, AnnouncementStatus.PUBLISHED, true, null, id))
        .map(this::toResponse)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Announcement not found"));
  }

  @Transactional(readOnly = true)
  public AnnouncementResponse getAdmin(@NonNull Long id) {
    return announcementRepository.findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Announcement not found"));
  }

  @Transactional(readOnly = true)
  public AnnouncementPageResponse listPublic(AnnouncementLevel level, String keyword, int page, int size) {
    Pageable pageable = pageable(page, size);
    Page<Announcement> result = announcementRepository.findAll(
        publicSpecification(level, AnnouncementStatus.PUBLISHED, true, keyword, null),
        pageable
    );
    return toPageResponse(result);
  }

  @Transactional(readOnly = true)
  public AnnouncementPageResponse listAdmin(AnnouncementLevel level, AnnouncementStatus status, String keyword, int page, int size) {
    Pageable pageable = pageable(page, size);
    Page<Announcement> result = announcementRepository.findAll(
        adminSpecification(level, status, keyword),
        pageable
    );
    return toPageResponse(result);
  }

  @Transactional
  public void delete(@NonNull Long id) {
    if (announcementRepository.existsById(id)) {
      announcementRepository.deleteById(id);
    }
  }

  private void applyRequest(Announcement announcement, AnnouncementRequest request, boolean isCreate) {
    String normalizedTitle = request.title() != null ? request.title().trim() : null;
    String normalizedSummary = trimToLength(request.summary(), 320);
    String normalizedContent = request.contentMarkdown() != null ? request.contentMarkdown().trim() : null;
    AnnouncementLevel level = request.level() != null ? request.level() : AnnouncementLevel.NOTICE;
    AnnouncementStatus status = request.status() != null ? request.status() : AnnouncementStatus.DRAFT;

    announcement.setTitle(normalizedTitle);
    announcement.setSummary(StringUtils.hasText(normalizedSummary) ? normalizedSummary : generateSummary(normalizedContent));
    announcement.setContentMarkdown(normalizedContent);
    announcement.setLevel(level);
    announcement.setStatus(status);
    announcement.setPinned(request.pinned());

    UserAccount currentUser = currentUser();
    if ((isCreate || announcement.getAuthorUserId() == null) && currentUser != null) {
      announcement.setAuthorUserId(currentUser.getId());
      announcement.setAuthorAvatar(currentUser.getAvatarUrl());
      announcement.setAuthorRole(resolveRoleLabel(currentUser));
      announcement.setAuthor(currentUser.getDisplayName());
    } else if (!StringUtils.hasText(announcement.getAuthor()) && StringUtils.hasText(request.author())) {
      announcement.setAuthor(request.author().trim());
    }

    if (!StringUtils.hasText(announcement.getAuthor())) {
      announcement.setAuthor("系统公告");
      if (announcement.getAuthorRole() == null) {
        announcement.setAuthorRole("系统");
      }
    }

    Long authorUserId = announcement.getAuthorUserId();
    if (authorUserId != null && (announcement.getAuthorRole() == null || announcement.getAuthorAvatar() == null)) {
      userAccountRepository.findById(authorUserId).ifPresent(author -> {
        announcement.setAuthorRole(resolveRoleLabel(author));
        announcement.setAuthorAvatar(author.getAvatarUrl());
        if (!StringUtils.hasText(announcement.getAuthor())) {
          announcement.setAuthor(author.getDisplayName());
        }
      });
    }

    if (currentUser != null) {
      announcement.setUpdatedBy(currentUser.getUsername());
    } else if (StringUtils.hasText(request.author())) {
      announcement.setUpdatedBy(request.author().trim());
    }

    Instant requestedPublishedAt = request.publishedAt();
    if (status == AnnouncementStatus.PUBLISHED) {
      if (requestedPublishedAt != null) {
        announcement.setPublishedAt(requestedPublishedAt);
      } else if (announcement.getPublishedAt() == null) {
        announcement.setPublishedAt(Instant.now());
      }
    } else {
      announcement.setPublishedAt(requestedPublishedAt);
    }
  }

  private AnnouncementPageResponse toPageResponse(Page<Announcement> page) {
    List<AnnouncementResponse> items = page.getContent().stream().map(this::toResponse).toList();
    return new AnnouncementPageResponse(items, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
  }

  private AnnouncementResponse toResponse(Announcement announcement) {
    return new AnnouncementResponse(
        announcement.getId(),
        announcement.getTitle(),
        announcement.getSummary(),
        announcement.getLevel(),
        announcement.getStatus(),
        announcement.isPinned(),
        announcement.getPublishedAt(),
        announcement.getUpdatedAt(),
        announcement.getAuthor(),
        announcement.getAuthorUserId(),
        announcement.getAuthorRole(),
        announcement.getAuthorAvatar(),
        announcement.getContentMarkdown()
    );
  }

  private @NonNull Specification<Announcement> publicSpecification(AnnouncementLevel level, AnnouncementStatus status, boolean onlyPublishedTimeValid, String keyword, Long targetId) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (level != null) {
        predicates.add(cb.equal(root.get("level"), level));
      }
      if (onlyPublishedTimeValid) {
        predicates.add(cb.isNotNull(root.get("publishedAt")));
        predicates.add(cb.lessThanOrEqualTo(root.get("publishedAt"), Instant.now()));
      }
      if (StringUtils.hasText(keyword)) {
        String like = "%" + keyword.trim().toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("title")), like),
            cb.like(cb.lower(root.get("summary")), like)
        ));
      }
      if (targetId != null) {
        predicates.add(cb.equal(root.get("id"), targetId));
      }
      if (query != null) {
        query.orderBy(
            cb.desc(root.get("pinned")),
            cb.desc(root.get("publishedAt")),
            cb.desc(root.get("updatedAt"))
        );
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private @NonNull Specification<Announcement> adminSpecification(AnnouncementLevel level, AnnouncementStatus status, String keyword) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (level != null) {
        predicates.add(cb.equal(root.get("level"), level));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (StringUtils.hasText(keyword)) {
        String like = "%" + keyword.trim().toLowerCase() + "%";
        predicates.add(cb.or(
            cb.like(cb.lower(root.get("title")), like),
            cb.like(cb.lower(root.get("summary")), like)
        ));
      }
      if (query != null) {
        query.orderBy(
            cb.desc(root.get("pinned")),
            cb.desc(root.get("publishedAt")),
            cb.desc(root.get("updatedAt"))
        );
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private @NonNull Pageable pageable(int page, int size) {
    int normalizedPage = Math.max(page, 0);
    int normalizedSize = Math.min(Math.max(size, 1), 30);
    Sort sort = Sort.by(
        Sort.Order.desc("pinned"),
        Sort.Order.desc("publishedAt"),
        Sort.Order.desc("updatedAt")
    );
    return PageRequest.of(normalizedPage, normalizedSize, sort);
  }

  private UserAccount currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
      return null;
    }
    return userAccountRepository.findByUsername(userDetails.getUsername()).orElse(null);
  }

  private String resolveRoleLabel(UserAccount userAccount) {
    if (userAccount == null || userAccount.getRoles() == null || userAccount.getRoles().isEmpty()) {
      return "系统";
    }
    Set<String> roleNames = userAccount.getRoles().stream()
        .map(UserRole::getName)
        .map(name -> name == null ? "" : name.toUpperCase())
        .collect(Collectors.toSet());
    if (roleNames.contains("ADMIN")) {
      return "系统管理员";
    }
    if (roleNames.contains("MODERATOR")) {
      return "内容审核";
    }
    if (roleNames.contains("USER")) {
      return "认证用户";
    }
    return "站点角色";
  }

  private String trimToLength(String value, int max) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() <= max) {
      return trimmed;
    }
    return trimmed.substring(0, max);
  }

  private String generateSummary(String content) {
    if (!StringUtils.hasText(content)) {
      return null;
    }
    String normalized = content.trim();
    if (normalized.length() <= 140) {
      return normalized;
    }
    return normalized.substring(0, 140) + "...";
  }
}
