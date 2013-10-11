package com.aliao.android.hipster;

/**
 * Class to support additional string manipulation.
 */
public final class StringUtils {
  /**
   * Returns a string that characterizes ranking. For example, 1 will be 1st, 2 will be 2nd and 100
   * will be 100th.
   */
  public static String getRankInString(int ranking) {
    // TODO(aliao): Support internationalization.
    int twoDigit = ranking % 100;
    if (twoDigit == 11 || twoDigit == 12 || twoDigit == 13) {
      return ranking + "th";
    }

    int digit = ranking % 10;
    if (digit == 1) {
      return ranking + "st";
    }
    if (digit == 2) {
      return ranking + "nd";
    }
    if (digit == 3) {
      return ranking + "rd";
    }
    return ranking + "th";
  }
}
