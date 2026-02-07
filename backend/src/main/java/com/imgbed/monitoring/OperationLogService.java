package com.imgbed.monitoring;

import com.imgbed.monitoring.dto.OperationLogEntry;
import com.imgbed.security.apikey.ApiKeyRepository;
import com.imgbed.upload.record.UploadRecord;
import com.imgbed.upload.record.UploadRecordRepository;
import com.imgbed.security.user.UserAccount;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperationLogService {

  private static final int MAX_ENTRIES = 8;

  private final UploadRecordRepository uploadRecordRepository;
  private final ApiKeyRepository apiKeyRepository;

  public OperationLogService(UploadRecordRepository uploadRecordRepository, ApiKeyRepository apiKeyRepository) {
    this.uploadRecordRepository = uploadRecordRepository;
    this.apiKeyRepository = apiKeyRepository;
  }

  @Transactional(readOnly = true)
  public List<OperationLogEntry> fetchRecentLogs() {
    ZoneId zoneId = ZoneId.systemDefault();
    Instant startOfToday = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant();
    List<OperationLogEntry> entries = new ArrayList<>();

    apiKeyRepository.findFirstByOrderByCreatedAtDesc()
        .ifPresent(apiKey -> entries.add(new OperationLogEntry(
            "apikey-" + apiKey.getId(),
            "生成 " + apiKey.getName() + " 访问令牌",
            "System",
            apiKey.getCreatedAt()
        )));

    long approvedToday = uploadRecordRepository.countByViolationFalseAndUploadedAtAfter(startOfToday);
    if (approvedToday > 0) {
      entries.add(new OperationLogEntry(
          "review-" + startOfToday.toEpochMilli(),
          "内容审查通过 " + approvedToday + " 张",
          "内容风控",
          Instant.now()
      ));
    }

    uploadRecordRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "uploadedAt")))
        .forEach(record -> entries.add(new OperationLogEntry(
            "upload-" + record.getId(),
            resolveAction(record),
            resolveUploader(record),
            record.getUploadedAt()
        )));

    return entries.stream()
        .sorted(Comparator.comparing(OperationLogEntry::timestamp).reversed())
        .limit(MAX_ENTRIES)
        .toList();
  }

  private String resolveAction(UploadRecord record) {
    String name = record.getFileName();
    if (!StringUtils.hasText(name)) {
      name = "图片";
    }
    return name + " 上传完成";
  }

  private String resolveUploader(UploadRecord record) {
    UserAccount user = record.getUser();
    if (user == null) {
      return "匿名用户";
    }
    if (StringUtils.hasText(user.getDisplayName())) {
      return user.getDisplayName();
    }
    return user.getUsername();
  }
}
