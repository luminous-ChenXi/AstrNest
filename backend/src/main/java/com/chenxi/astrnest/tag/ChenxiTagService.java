package com.chenxi.astrnest.tag;

import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChenxiTagService {

  private static final int DEFAULT_SEARCH_LIMIT = 20;
  private static final int MAX_TAG_LENGTH = 20;
  private static final Pattern UNSAFE_CONTENT_PATTERN = Pattern.compile(
      "(<|>|\"|'|`|;|\\b(script|javascript:|onerror|onload)\\b)",
      Pattern.CASE_INSENSITIVE
  );

  private final ChenxiTagRepository chenxiTagRepository;

  public List<ChenxiTagResponse> search(String keyword, Integer limit) {
    int pageSize = limit != null && limit > 0 ? Math.min(limit, 100) : DEFAULT_SEARCH_LIMIT;
    String sanitizedKeyword = sanitizeKeyword(keyword);
    List<ChenxiTag> results;
    if (sanitizedKeyword != null) {
      results = chenxiTagRepository.findTop50ByNameContainingIgnoreCaseOrderByNameAsc(sanitizedKeyword).stream()
          .limit(pageSize)
          .toList();
    } else {
      results = chenxiTagRepository.findTop50ByOrderByCreatedAtDesc().stream()
          .limit(pageSize)
          .toList();
    }
    return results.stream().map(this::toResponse).toList();
  }

  @Transactional
  public ChenxiTagResponse create(String name, String description) {
    String sanitized = sanitizeNameStrict(name);
    ChenxiTag tag = new ChenxiTag();
    tag.setName(sanitized);
    tag.setDescription(StringUtils.hasText(description) ? description.trim() : null);
    ChenxiTag saved = chenxiTagRepository.save(tag);
    return toResponse(saved);
  }

  @Transactional
  public Set<ChenxiTag> resolveTags(List<String> rawNames) {
    if (rawNames == null || rawNames.isEmpty()) {
      return Set.of();
    }
    List<String> sanitized = rawNames.stream()
        .map(this::sanitizeName)
        .filter(StringUtils::hasText)
        .toList();
    if (sanitized.isEmpty()) {
      return Set.of();
    }
    List<String> normalizedKeys = sanitized.stream()
        .map(name -> name.toLowerCase(Locale.ROOT))
        .distinct()
        .toList();
    Map<String, ChenxiTag> existing = chenxiTagRepository.findByNormalizedNames(normalizedKeys).stream()
        .collect(Collectors.toMap(tag -> tag.getName().toLowerCase(Locale.ROOT), Function.identity(), (a, b) -> a));
    Set<ChenxiTag> resolved = new LinkedHashSet<>();
    List<ChenxiTag> toPersist = new ArrayList<>();
    for (String name : sanitized) {
      String key = name.toLowerCase(Locale.ROOT);
      ChenxiTag tag = existing.get(key);
      if (tag == null) {
        tag = new ChenxiTag();
        tag.setName(name);
        toPersist.add(tag);
      }
      resolved.add(tag);
    }
    if (!toPersist.isEmpty()) {
      List<ChenxiTag> saved = chenxiTagRepository.saveAll(toPersist);
      saved.forEach(tag -> existing.put(tag.getName().toLowerCase(Locale.ROOT), tag));
      resolved = resolved.stream()
          .map(tag -> tag.getId() == null ? existing.get(tag.getName().toLowerCase(Locale.ROOT)) : tag)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    return resolved;
  }

  public long countTags() {
    return chenxiTagRepository.count();
  }

  public ChenxiTagResponse toResponse(ChenxiTag tag) {
    if (tag == null) {
      return null;
    }
    return new ChenxiTagResponse(tag.getId(), tag.getName(), tag.getSlug(), tag.getDescription());
  }

  private String sanitizeKeyword(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > MAX_TAG_LENGTH) {
      throw new IllegalArgumentException("搜索关键词长度不能超过 " + MAX_TAG_LENGTH + " 个字符");
    }
    if (UNSAFE_CONTENT_PATTERN.matcher(normalized).find()) {
      throw new IllegalArgumentException("搜索关键词包含非法字符");
    }
    return normalized;
  }

  private String sanitizeNameStrict(String value) {
    String sanitized = sanitizeName(value, true);
    if (!StringUtils.hasText(sanitized)) {
      throw new IllegalArgumentException("请输入合法的标签名称");
    }
    return sanitized;
  }

  private String sanitizeName(String value) {
    return sanitizeName(value, false);
  }

  private String sanitizeName(String value, boolean strict) {
    if (!StringUtils.hasText(value)) {
      if (strict) {
        throw new IllegalArgumentException("请输入标签名称");
      }
      return null;
    }
    String normalized = value.trim();
    if (!StringUtils.hasText(normalized)) {
      if (strict) {
        throw new IllegalArgumentException("请输入标签名称");
      }
      return null;
    }
    if (normalized.length() > MAX_TAG_LENGTH) {
      if (strict) {
        throw new IllegalArgumentException("标签名称长度不能超过 " + MAX_TAG_LENGTH + " 个字符");
      }
      return null;
    }
    if (UNSAFE_CONTENT_PATTERN.matcher(normalized).find()) {
      if (strict) {
        throw new IllegalArgumentException("标签名称包含非法字符");
      }
      return null;
    }
    return normalized;
  }
}
