package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author kyle
 * @since 2024-03-17
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    void addLikeRecord(LikeRecordFormDTO likeRecordFormDTO);

    Set<Long> getLikesByBizIds(List<Long> bizIds);

    void readLikedTimesAndSentMsg(String bizType, int maxBizSize);

    Set<Long> isBizLiked(List<Long> answerId);
}
