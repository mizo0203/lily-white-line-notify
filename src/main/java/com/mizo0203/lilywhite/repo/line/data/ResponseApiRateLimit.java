package com.mizo0203.lilywhite.repo.line.data;

import java.util.Date;

public class ResponseApiRateLimit {
  private final int limit;
  private final int remaining;
  private final int imageLimit;
  private final int imageRemaining;
  private final Date reset;

  public ResponseApiRateLimit(
      int limit, int remaining, int imageLimit, int imageRemaining, Date reset) {
    this.limit = limit;
    this.remaining = remaining;
    this.imageLimit = imageLimit;
    this.imageRemaining = imageRemaining;
    this.reset = reset;
  }

  /** @return 1時間に可能なAPI callの上限回数 */
  public int getLimit() {
    return limit;
  }

  /** @return API callが可能な残りの回数 */
  public int getRemaining() {
    return remaining;
  }

  /** @return 1時間に可能なImage uploadの上限回数 */
  public int getImageLimit() {
    return imageLimit;
  }

  /** @return Image uploadが可能な残りの回数 */
  public int getImageRemaining() {
    return imageRemaining;
  }

  /** @return リセットされる時刻 (UTC epoch seconds) ex:1472195604 */
  public Date getReset() {
    return reset;
  }
}
