package com.tianji.learning.constant;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.constant
 * @Project：tianji
 * @Date：2024/3/23 16:25
 * @Filename：RedisConstants
 */
public interface RedisConstants {
    /**
     * 签到
     */
    String SIGN_RECORD_KEY_PREFIX = "sign:uid:";

    /**
     * 积分排行榜
     */
    String POINTS_BOARD_KEY_PREFIX = "boards:";
}
