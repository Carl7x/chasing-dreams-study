package com.tianji.learning.mq;

import com.tianji.api.dto.msg.LikedTimesDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.mq
 * @Project：tianji
 * @Date：2024/3/2 16:14
 * @Filename：LessonChangeListener
 */
@Component
@Slf4j
@RequiredArgsConstructor//使用构造器
public class LikeRecordListener {

    final IInteractionReplyService replyService;
//    public LessonChangeListener(ILearningLessonService lessonService) {
//        this.lessonService = lessonService;
//    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "qa.liked.times.queue",
            durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.QA_LIKED_TIMES_KEY))
    public void onMsg(List<LikedTimesDTO> likedTimesDTOList) {
        log.info("LikeRecordListener 接收到了信息{}", likedTimesDTOList);
        //1.校验参数是否正确
        List<InteractionReply> list = new ArrayList<>();
        for (LikedTimesDTO dto : likedTimesDTOList) {
            InteractionReply reply = new InteractionReply();
            reply.setLikedTimes(dto.getLikedTimes());
            reply.setId(dto.getBizId());
            list.add(reply);
        }
        replyService.updateBatchById(list);
        //replyService.updateById(reply);
    }

}
