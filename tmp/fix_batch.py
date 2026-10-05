import io

BASE = 'backend/src/main/java/com/chenxi/astrnest/'

def edit(path, pairs):
    s = io.open(BASE + path, encoding='utf-8').read()
    for old, new in pairs:
        assert old in s, f"{path}: anchor missing: {old[:60]!r}"
        s = s.replace(old, new, 1)
    io.open(BASE + path, 'w', encoding='utf-8', newline='\n').write(s)
    print('ok', path)

# 1. AlbumService.getAlbumDetail
edit('album/AlbumService.java', [(
"""    List<AlbumMedia> albumMedias = albumMediaRepository.findByAlbumIdOrderBySortOrderAsc(album.getId());
    Map<String, UploadRecord> recordsByUuid = findRecordsByUuid(
        albumMedias.stream().map(AlbumMedia::getMediaUuid).toList());
    List<AlbumMediaResponse> mediaResponses = new ArrayList<>();
    for (AlbumMedia media : albumMedias) {
      UploadRecord record = recordsByUuid.get(media.getMediaUuid());
      if (record != null) {
        mediaResponses.add(convertToMediaResponse(media, record));
      }
    }

    AlbumDetailResponse response = new AlbumDetailResponse();""",
"""    // 可见性硬不变量（审计复查点①）：非属主看公开相册也只允许露出公开∧非违规的图——
    // 此前整包返回全部 media 的 publicUrl/thumbnailUrl，等于把相册内私图直链送给任意登录用户
    boolean ownerOrAdmin = user != null && album.getUser() != null
        && (album.getUser().getId().equals(user.getId()) || isAdmin(user));

    List<AlbumMedia> albumMedias = albumMediaRepository.findByAlbumIdOrderBySortOrderAsc(album.getId());
    Map<String, UploadRecord> recordsByUuid = findRecordsByUuid(
        albumMedias.stream().map(AlbumMedia::getMediaUuid).toList());
    List<AlbumMediaResponse> mediaResponses = new ArrayList<>();
    for (AlbumMedia media : albumMedias) {
      UploadRecord record = recordsByUuid.get(media.getMediaUuid());
      if (record != null && (ownerOrAdmin || isPublicVisible(record))) {
        mediaResponses.add(convertToMediaResponse(media, record));
      }
    }

    AlbumDetailResponse response = new AlbumDetailResponse();"""
)])

# 2. UploadRecordRepository
edit('upload/record/UploadRecordRepository.java', [(
"  List<UploadRecord> findTop3ByPublicAccessibleTrueAndViolationFalseOrderByLikeCountDesc();",
"""  /** 热门图片（审计复查点②）：对齐画廊 spec 的相册公开性条件——位于私有相册的公开图不上榜，防私有相册元数据外泄 */
  @Query(\"\"\"
      select r from UploadRecord r
      where r.publicAccessible = true and r.violation = false
        and (not exists (select 1 from AlbumMedia am where am.mediaUuid = r.mediaUuid)
             or exists (select 1 from AlbumMedia am where am.mediaUuid = r.mediaUuid
                        and am.album.isPublic = true))
      order by r.likeCount desc
      \"\"\")
  List<UploadRecord> findTopPublicImages(@org.springframework.data.domain.Pageable pageable);

  /** 公开档案统计口径（审计复查点③）：只计公开∧非违规，私图/违规图不进匿名可见的计数 */
  @Query(\"\"\"
      select new com.chenxi.astrnest.upload.record.dto.UserUsageAggregate(
          r.user.id,
          count(r),
          coalesce(sum(r.size),0),
          coalesce(sum(r.likeCount),0)
      )
      from UploadRecord r
      where r.user.id in :userIds and r.publicAccessible = true and r.violation = false
      group by r.user.id
      \"\"\")
  List<UserUsageAggregate> aggregatePublicUsageByUserIds(@Param("userIds") Collection<Long> userIds);"""
)])

# 3. PublicGalleryService caller
edit('gallery/PublicGalleryService.java', [(
"    List<UploadRecord> records = uploadRecordRepository.findTop3ByPublicAccessibleTrueAndViolationFalseOrderByLikeCountDesc();",
"""    List<UploadRecord> records = uploadRecordRepository.findTopPublicImages(
        org.springframework.data.domain.PageRequest.of(0, limit));"""
), (
"""            tagMap.getOrDefault(record.getId(), List.of())
        ))
        .limit(limit)
        .toList();""",
"""            tagMap.getOrDefault(record.getId(), List.of())
        ))
        .toList();"""
)])

# 4. AdminUserService: public usage aggregate
edit('admin/user/AdminUserService.java', [(
"""  private UserUsageAggregate usageFor(Long userId) {
    return usageMap(List.of(userId)).getOrDefault(userId, new UserUsageAggregate(userId, 0, 0, 0));
  }""",
"""  private UserUsageAggregate usageFor(Long userId) {
    return usageMap(List.of(userId)).getOrDefault(userId, new UserUsageAggregate(userId, 0, 0, 0));
  }

  /** 公开档案专用：只聚合公开∧非违规的上传（uploadCount/storageBytes 对匿名可见，不应计入私图） */
  private UserUsageAggregate publicUsageFor(Long userId) {
    return uploadRecordRepository.aggregatePublicUsageByUserIds(List.of(userId))
        .stream()
        .findFirst()
        .orElse(new UserUsageAggregate(userId, 0, 0, 0));
  }"""
), (
"""    UserAccount user = requireUser(userId);
    UserUsageAggregate usage = usageFor(userId);
    return new PublicUserProfileResponse(""",
"""    UserAccount user = requireUser(userId);
    UserUsageAggregate usage = publicUsageFor(userId);
    return new PublicUserProfileResponse("""
)])

# 5. ChenxiAuthService.resetPassword: SSO guard
edit('chenxi/auth/ChenxiAuthService.java', [(
"""    UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号不存在"));
    user.setPassword(passwordEncoder.encode(newPassword));""",
"""    UserAccount user = userAccountRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号不存在"));
    // 影子账号口令通道封死（审计复查）：SSO 账号密码由身份源管理——放行会造成
    // 「邮箱找回改密 → tokenVersion+1 → SSO 登录仍签发 ver=0 令牌被拒」的永久锁死
    if (user.getIdentitySource() != null && !"local".equals(user.getIdentitySource())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "该账号通过外部身份源登录，密码由身份源管理，无法在此重置");
    }
    user.setPassword(passwordEncoder.encode(newPassword));"""
)])

# 6. SsoIdentityService: token version + username whitelist
edit('passport/SsoIdentityService.java', [(
"    String token = jwtTokenService.generateToken(user.getId(), user.getUsername());",
"""    // 带令牌版本：影子账号被「找回密码」动过 tokenVersion 后，签发 ver=0 会全部被过滤器拒绝
    String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());"""
), (
'''  private String sanitizeUsername(String value) {
    String cleaned = value.replaceAll("[\\\\s\\\\p{Cntrl}]+", "");
    return truncate(cleaned, USERNAME_MAX_LENGTH);
  }''',
'''  private String sanitizeUsername(String value) {
    // 与本地注册同一口径（^[A-Za-z0-9_.-]+$）：剔除空白/控制字符后再剥白名单外字符，
    // 中文/HTML 元字符等不得经 SSO 绕过本地用户名规则入库
    String cleaned = value.replaceAll("[\\\\s\\\\p{Cntrl}]+", "")
        .replaceAll("[^A-Za-z0-9_.-]", "");
    return truncate(cleaned, USERNAME_MAX_LENGTH);
  }'''
)])

# 7. AuthProtectionService.normalize lowercase
edit('security/bruteforce/AuthProtectionService.java', [(
'''  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }''',
'''  private String normalize(String value) {
    // 小写归一：登录支持用户名/邮箱且 DB 排序规则大小写不敏感，
    // 否则 User/user/邮箱 等变体各算各的失败窗口，账号维度锁定被绕过
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }'''
)])

# 8. LoginRequest password size cap
edit('user/dto/LoginRequest.java', [(
'''import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "用户名不能为空") String username,
    @NotBlank(message = "密码不能为空") String password
) {}''',
'''import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "用户名不能为空") String username,
    // 长度上限：/login 匿名可达，无上限的超长密码体反复触发 BCrypt(12) 可打满 CPU
    @NotBlank(message = "密码不能为空") @Size(max = 64, message = "密码过长") String password
) {}'''
)])

# 9. AuthController: login rate limit
edit('user/AuthController.java', [(
'''public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    return authService.login(request, httpRequest);
  }''',
'''public class AuthController {

  /** 登录端点进程内限流：兜底防超大密码体反复触发 BCrypt(12) 打满 CPU（账号锁定由防爆破服务承担） */
  private static final int LOGIN_LIMIT_PER_MINUTE = 30;
  private static final java.time.Duration LOGIN_RATE_WINDOW = java.time.Duration.ofMinutes(1);

  private final AuthService authService;
  private final com.chenxi.astrnest.common.ClientIpResolver clientIpResolver;
  private final com.chenxi.astrnest.common.InMemoryRateLimiter rateLimiter;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String ip = clientIpResolver.resolve(httpRequest);
    if (rateLimiter.tryAcquire("login:" + ip, LOGIN_LIMIT_PER_MINUTE, LOGIN_RATE_WINDOW)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
    }
    return authService.login(request, httpRequest);
  }'''
)])

# 10. ChenxiAuthController: register rate limit
edit('chenxi/auth/ChenxiAuthController.java', [(
'''    String ip = clientIpResolver.resolve(httpRequest);
    authProtectionService.ensureRegisterAllowed(request.username(), ip);
    try {''',
'''    String ip = clientIpResolver.resolve(httpRequest);
    // 批量建号防护：每号 200MB 配额，单步注册（无验证码场景）此前完全无限流
    if (rateLimiter.tryAcquire("register:" + ip, 10, java.time.Duration.ofMinutes(1))) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "注册过于频繁，请稍后再试");
    }
    authProtectionService.ensureRegisterAllowed(request.username(), ip);
    try {'''
)])

# 11. wizard username min 3->4
edit('install/InstallSetupService.java', [(
'    if (!username.matches("^[A-Za-z0-9_.-]{3,32}$")) {',
'    if (!username.matches("^[A-Za-z0-9_.-]{4,32}$")) {'
)])

print('ALL OK')
