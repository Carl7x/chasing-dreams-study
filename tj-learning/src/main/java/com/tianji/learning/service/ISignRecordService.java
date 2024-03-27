package com.tianji.learning.service;

import com.tianji.learning.domain.vo.SignResultVO;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.service
 * @Project：tianji
 * @Date：2024/3/23 16:07
 * @Filename：ISignRecordService
 */
public interface ISignRecordService {
    SignResultVO addSignRecords();

    Integer[] getSignRecords();
}
