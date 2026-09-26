package com.chenxi.astrnest.license;

/**
 * 授权校验状态机（SDK 骨架内部口径，N2 可能扩展）。
 *
 * <ul>
 *   <li>{@link #DISABLED}：功能未启用（默认），不做任何校验；</li>
 *   <li>{@link #ACTIVE}：最近一次 verify 成功且在缓存期内；</li>
 *   <li>{@link #GRACE}：verify 不可达，但在离线宽限期内——放行，功能不受限；</li>
 *   <li>{@link #OVERDUE}：离线宽限期已过——<b>仅提示，绝不锁数据</b>：全部业务功能照常，
 *       前端展示横幅提醒站长完成授权；</li>
 *   <li>{@link #INVALID}：verify 明确返回无效（如授权过期/被吊销）——同样仅提示不锁数据。</li>
 * </ul>
 */
public enum LicenseState {
  DISABLED,
  ACTIVE,
  GRACE,
  OVERDUE,
  INVALID;

  /** 是否需要前端横幅提示（仅提示，永不阻断功能）。 */
  public boolean needsBanner() {
    return this == OVERDUE || this == INVALID;
  }
}
