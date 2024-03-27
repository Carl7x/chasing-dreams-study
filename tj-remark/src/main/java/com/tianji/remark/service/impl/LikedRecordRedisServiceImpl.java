package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.msg.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.SPELUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author kyle
 * @since 2024-03-17
 */
@Service
@RequiredArgsConstructor
public class LikedRecordRedisServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    final RabbitMqHelper rabbitMqHelper;
    final StringRedisTemplate redisTemplate;

    @Override
    public void addLikeRecord(LikeRecordFormDTO likeRecordFormDTO) {
        Long user = UserContext.getUser();
        boolean flag = true;
        if (likeRecordFormDTO.getLiked()) {
            flag = like(likeRecordFormDTO, user);
        } else {
            flag = unlike(likeRecordFormDTO, user);
        }
        if (!flag) {
            return;
        }

        /*Integer count = this.lambdaQuery()
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
                .count();*/
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + likeRecordFormDTO.getBizId();
        Long count = redisTemplate.opsForSet().size(key);
        if (count == null) {
            return;
        }
        String bizTypeKey = RedisConstants.LIKE_COUNT_KEY_PREFIX + likeRecordFormDTO.getBizType();
        redisTemplate.opsForZSet().add(bizTypeKey, likeRecordFormDTO.getBizId().toString(), count);
        /*LikedTimesDTO likedTimesDTO = new LikedTimesDTO();
        likedTimesDTO.setBizId(likeRecordFormDTO.getBizId());
        likedTimesDTO.setLikedTimes(count);
        //MQ发送消息
        rabbitMqHelper.send(
                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE,
                        likeRecordFormDTO.getBizType()),
                LikedTimesDTO.builder()
                        .likedTimes(count)
                        .bizId(likeRecordFormDTO.getBizId())
                        .build()
        );*/
    }

    private boolean unlike(LikeRecordFormDTO likeRecordFormDTO, Long user) {
        /*LikedRecord record = this.lambdaQuery()
                .eq(LikedRecord::getUserId, user)
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
                .one();
        if (record == null) {
            return false;
        }
        boolean result = this.removeById(record.getId());
        return result;*/
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + likeRecordFormDTO.getBizId();
        Long result = redisTemplate.opsForSet().remove(key, user.toString());
        return result != null && result > 0;
    }

    private boolean like(LikeRecordFormDTO likeRecordFormDTO, Long user) {
       /* LikedRecord record = this.lambdaQuery()
                .eq(LikedRecord::getUserId, user)
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
                .one();
        if (record != null) {
            return false;
        }
        LikedRecord likedRecord = new LikedRecord();
        likedRecord.setBizId(likeRecordFormDTO.getBizId());
        likedRecord.setBizType(likeRecordFormDTO.getBizType());
        likedRecord.setUserId(user);
        boolean result = this.save(likedRecord);
        return result;*/
        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + likeRecordFormDTO.getBizId();
        //redisTemplate.boundSetOps(key).add(user.toString());
        Long result = redisTemplate.opsForSet().add(key, user.toString());
        return result != null && result > 0;
    }

    /**
     * 批量获取用户点赞信息
     *
     * @param bizIds
     */
    @Override
    public Set<Long> getLikesByBizIds(List<Long> bizIds) {
        Long user = UserContext.getUser();
        if (CollUtils.isEmpty(bizIds)) {
            return CollUtils.emptySet();
        }
        List<Object> list = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    StringRedisConnection src = (StringRedisConnection) connection;
                    for (Long bizId : bizIds) {
                        String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
                        src.sIsMember(key, user.toString());
                    }
                    return null;
                }
        );
//        for (Long bizId : bizIds) {
//            String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
//            Boolean isMember = redisTemplate.opsForSet().isMember(key, user.toString());
//            if (Boolean.TRUE.equals(isMember)) {
//                likeList.add(bizId);
//            }
//        }
        return IntStream.range(0, list.size())
                .filter(i -> (boolean) list.get(i))
                .mapToObj(bizIds::get)
                .collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSentMsg(String bizType, int maxBizSize) {
        //1.拼接key
        String bizTypeKey = RedisConstants.LIKE_COUNT_KEY_PREFIX + bizType;
        //2.从redis的zset取出30条
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().popMin(bizTypeKey, maxBizSize);
        List<LikedTimesDTO> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            Long bizId = Long.getLong(typedTuple.getValue());
            Integer times = typedTuple.getScore().intValue();
            LikedTimesDTO dto = new LikedTimesDTO();
            dto.setLikedTimes(times);
            dto.setBizId(bizId);
            list.add(dto);
        }
        //3.封装dto
        //4.发送消息
        rabbitMqHelper.send(
                MqConstants.Exchange.LIKE_RECORD_EXCHANGE,
                StringUtils.format(MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE,
                        bizType),
                list
        );
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long userId = UserContext.getUser();
        if (bizIds == null) {
            return CollUtils.emptySet();
        }
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            StringRedisConnection redisConnection = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                String key = RedisConstants.LIKE_COUNT_KEY_PREFIX + bizId;
                redisConnection.sIsMember(key, userId.toString());
            }
            return null;
        });
        return IntStream.range(0,objects.size())
                .mapToObj(bizIds::get)
                .collect(Collectors.toSet());
    }
}
