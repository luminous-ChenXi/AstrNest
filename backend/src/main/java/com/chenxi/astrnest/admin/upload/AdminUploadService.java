package com.chenxi.astrnest.admin.upload;

import com.chenxi.astrnest.admin.upload.dto.AdminUploadItemResponse;
import com.chenxi.astrnest.admin.upload.dto.AdminUploadPageResponse;
import com.chenxi.astrnest.storage.PublicAssetUrlResolver;
import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import com.chenxi.astrnest.upload.record.UploadRecordService;
import com.chenxi.astrnest.security.user.UserAccount;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUploadService {

  private final UploadRecordRepository uploadRecordRepository;
  private final PublicAssetUrlResolver publicAssetUrlResolver;
  private final UploadRecordService uploadRecordService;

  @Transactional(readOnly = true)
  public AdminUploadPageResponse searchUploads(String search, Boolean violation, Boolean publicAccessible,
      String storageProvider, int page, int size) {
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "uploadedAt"));
    Specification<UploadRecord> specification = buildSpecification(search, violation, publicAccessible, storageProvider);
    Page<UploadRecord> result = uploadRecordRepository.findAll(specification, pageable);
    List<AdminUploadItemResponse> records = result.getContent().stream()
        .map(this::toResponse)
        .toList();
    return new AdminUploadPageResponse(records, result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
  }

  @Transactional
  public AdminUploadItemResponse updateVisibility(long id, boolean publicAccessible) {
    UploadRecord record = requireRecord(id);
    record.setPublicAccessible(publicAccessible);
    uploadRecordRepository.save(record);
    return toResponse(record);
  }

  @Transactional
  public AdminUploadItemResponse updateViolation(long id, boolean violation) {
    UploadRecord record = requireRecord(id);
    String placeholder = violation ? publicAssetUrlResolver.violationPlaceholderUrl() : null;
    String reviewStatus = violation ? "MANUAL_BLOCKED" : "MANUAL_APPROVED";
    UploadRecord updated = uploadRecordService.markViolation(record, violation, placeholder, violation, reviewStatus);
    return toResponse(updated);
  }

  @Transactional
  public void deleteRecord(long id) {
    if (!uploadRecordRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片记录不存在");
    }
    uploadRecordRepository.deleteById(id);
  }

  private Specification<UploadRecord> buildSpecification(String search, Boolean violation, Boolean publicAccessible,
      String storageProvider) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (StringUtils.hasText(search)) {
        Join<UploadRecord, UserAccount> userJoin = root.join("user", JoinType.LEFT);
        String likePattern = "%" + search.trim().toLowerCase() + "%";
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("fileName")), likePattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("publicUrl")), likePattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("objectKey")), likePattern),
            criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("username")), likePattern),
            criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("displayName")), likePattern),
            criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("email")), likePattern)
        ));
      }

      if (violation != null) {
        predicates.add(criteriaBuilder.equal(root.get("violation"), violation));
      }

      if (publicAccessible != null) {
        predicates.add(criteriaBuilder.equal(root.get("publicAccessible"), publicAccessible));
      }

      if (StringUtils.hasText(storageProvider)) {
        predicates.add(criteriaBuilder.equal(
            criteriaBuilder.lower(root.get("storageProvider")),
            storageProvider.trim().toLowerCase()
        ));
      }

      return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  private UploadRecord requireRecord(long id) {
    return uploadRecordRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图片记录不存在"));
  }

  private AdminUploadItemResponse toResponse(UploadRecord record) {
    UserAccount user = record.getUser();
    return new AdminUploadItemResponse(
        record.getId(),
        record.getFileName(),
        record.getObjectKey(),
        publicAssetUrlResolver.resolve(record),
        record.getSize(),
        record.getContentType(),
        record.getReviewStatus(),
        record.isViolation(),
        record.isPublicAccessible(),
        record.getLikeCount(),
        record.getInvokeCount(),
        user != null ? user.getUsername() : null,
        user != null ? user.getDisplayName() : null,
        user != null ? user.getEmail() : null,
        record.getUploaderIp(),
        record.getStorageProvider(),
        record.getStorageMode(),
        record.getUploadedAt(),
        mapTags(record)
    );
  }

  private List<ChenxiTagResponse> mapTags(UploadRecord record) {
    if (record == null || record.getTags() == null || record.getTags().isEmpty()) {
      return List.of();
    }
    return record.getTags().stream()
        .map(tag -> new ChenxiTagResponse(tag.getId(), tag.getName(), tag.getSlug(), tag.getDescription()))
        .toList();
  }
}
