package com.tianji.remark.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    final RabbitMqHelper rabbitMqHelper;

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

        Integer count = this.lambdaQuery()
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
                .count();

        LikedTimesDTO likedTimesDTO = new LikedTimesDTO();
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
        );
    }

    private boolean unlike(LikeRecordFormDTO likeRecordFormDTO, Long user) {
        LikedRecord record = this.lambdaQuery()
                .eq(LikedRecord::getUserId, user)
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
                .one();
        if (record == null) {
            return false;
        }
        boolean result = this.removeById(record.getId());
        return result;
    }

    private boolean like(LikeRecordFormDTO likeRecordFormDTO, Long user) {
        LikedRecord record = this.lambdaQuery()
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
        return result;
    }

    /**
     * 批量获取用户点赞信息
     * @param bizIds
     */
    @Override
    public Set<Long> getLikesByBizIds(List<Long> bizIds) {
        Long user = UserContext.getUser();
        List<LikedRecord> list = this.lambdaQuery()
                .eq(LikedRecord::getUserId, user)
                .in(LikedRecord::getBizId, bizIds)
                .list();
        Set<Long> likeList = list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
        return likeList;
    }
}
