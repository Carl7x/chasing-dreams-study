package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constant.RedisConstants;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tianji.learning.constant.RedisConstants.POINTS_BOARD_KEY_PREFIX;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author kyle
 * @since 2024-03-23
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addPointRecord(SignInMessage message, PointsRecordType recordType) {
        //1.校验参数
        if (message.getUserId() == null || message.getPoints() == null) {
            return;
        }
        //2.判断积分类型是否有上限
        int maxPoints = recordType.getMaxPoints();
        if (maxPoints > 0) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = DateUtils.getDayStartTime(now);
            LocalDateTime endTime = DateUtils.getDayEndTime(now);
            QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
            wrapper.select("sum(points) as totalPoints");
            wrapper.eq("user_id", message.getUserId());
            wrapper.eq("type", recordType);
            wrapper.between("create_time", startTime, endTime);
            Map<String, Object> map = this.getMap(wrapper);
            int points = 0;
            if (map != null) {
                BigDecimal bigDecimal = (BigDecimal) map.get("totalPoints");
                points = bigDecimal.intValue();
            }
            if (points >= maxPoints) {
                return;
            }
            PointsRecord pointsRecord = new PointsRecord();
            pointsRecord.setPoints(points + message.getPoints() > maxPoints ? (maxPoints - points) : message.getPoints());
            pointsRecord.setType(recordType);
            pointsRecord.setUserId(message.getUserId());
            this.save(pointsRecord);

            LocalDate date = LocalDate.now();
            String format = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
            String key = POINTS_BOARD_KEY_PREFIX + format;
            redisTemplate.opsForZSet().incrementScore(key,message.getUserId().toString(),pointsRecord.getPoints());
        }
    }

    @Override
    public List<PointsStatisticsVO> getTotalPoints() {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = DateUtils.getDayStartTime(now);
        LocalDateTime endTime = DateUtils.getDayEndTime(now);
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.select("type","sum(points) as points");
        wrapper.eq("user_id", userId);
        wrapper.between("create_time", startTime, endTime);
        wrapper.groupBy("type");
        List<PointsRecord> list = this.list(wrapper);
        if(CollUtils.isEmpty(list)){
            return CollUtils.emptyList();
        }
        List<PointsStatisticsVO> voList  = new ArrayList<>();
        for (PointsRecord pointsRecord : list) {
            PointsStatisticsVO vo  = new PointsStatisticsVO();
            vo.setMaxPoints(pointsRecord.getPoints());
            vo.setType(pointsRecord.getType().getDesc());
            vo.setMaxPoints(pointsRecord.getType().getMaxPoints());
            voList.add(vo);
        }
        return voList;
    }
}
