package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.remark.task
 * @Project：tianji
 * @Date：2024/3/18 21:30
 * @Filename：LikedTimesCheckTask
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LikedTimesCheckTask {
    private static final List<String> BIZ_TYPES = List.of("QA","NOTE");
    private static final int MAX_BIZ_SIZE = 30;

    private final ILikedRecordService likedRecordService;

    @Scheduled(cron = "0/20 * * * * ?")
    public void checkLikedTimes(){
        for (String bizType : BIZ_TYPES) {
            likedRecordService.readLikedTimesAndSentMsg(bizType,MAX_BIZ_SIZE);
        }
    }
}
