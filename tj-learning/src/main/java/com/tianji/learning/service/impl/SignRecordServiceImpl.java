package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constant.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.service.impl
 * @Project：tianji
 * @Date：2024/3/23 16:09
 * @Filename：SignRecordServiceImpl
 */
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;

    @Override
    public SignResultVO addSignRecords() {
        Long userId = UserContext.getUser();
//        SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
//        String date = format.format(new Date());
        LocalDate localDate = LocalDate.now();
        String format = localDate.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString() + format;
        long offset = localDate.getDayOfMonth() - 1;
        Boolean setBit = redisTemplate.opsForValue().setBit(key, offset, true);
        if (Boolean.TRUE.equals(setBit)) {
            throw new BizIllegalException("不能重复签到");
        }
        int dayOfMonth = localDate.getDayOfMonth();
        int days = countSignedDays(key, dayOfMonth);
        int rewardPoints;
        switch (days) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 30;
                break;
            default:
                rewardPoints = 0;
        }
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1));
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(days);
        vo.setRewardPoints(rewardPoints);
        return null;
    }

    private int countSignedDays(String key, int dayOfMonth) {
        List<Long> bitField = redisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned((int) (dayOfMonth))).valueAt(0));
        if (CollUtils.isEmpty(bitField)) {
            return 0;
        }
        Long bit = bitField.get(0);
        int days = 0;
        while ((bit & 1) == 1) {
            days += 1;
            bit = bit >>> 1;
        }
        return days;
    }

    /**
     * 查询从本月第一天到今天签到情况
     *
     * @return
     */
    @Override
    public Integer[] getSignRecords() {
        Long userId = UserContext.getUser();
        LocalDate localDate = LocalDate.now();
        String format = localDate.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX + userId.toString() + format;
        int dayOfMonth = localDate.getDayOfMonth();
        List<Long> bitField = redisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType
                        .unsigned(dayOfMonth)).valueAt(0));
        if (CollUtils.isEmpty(bitField)) {
            return null;
        }
        Integer[] arr = new Integer[dayOfMonth];
        Long bit = bitField.get(0);
        for (int i = 0; i < dayOfMonth; i++) {
            int tmp = (int) (bit & 1);
            arr[dayOfMonth -i - 1] = tmp;
            bit = bit >>> 1;
        }
        return arr;
    }

}
