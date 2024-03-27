package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.msg.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.service.ISignRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.mq
 * @Project：tianji
 * @Date：2024/3/24 9:37
 * @Filename：LearningPointListener
 */
@Slf4j
@Component
public class LearningPointListener {

    @Autowired
    private IPointsRecordService pointsRecordService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "sign.points.queue", durable = "ture"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN
    ))
    public void listenSignListener(SignInMessage message){
        log.info("收到了消息：{}",message);
        pointsRecordService.addPointRecord(message, PointsRecordType.SIGN);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "qa.points.queue", durable = "ture"),
            exchange = @Exchange(value = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY
    ))
    public void listenReplyListener(SignInMessage message){
        log.info("收到了消息：{}",message);
    }
}
