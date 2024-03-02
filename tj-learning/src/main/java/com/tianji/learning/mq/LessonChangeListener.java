package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
public class LessonChangeListener {

    final ILearningLessonService lessonService;

//    public LessonChangeListener(ILearningLessonService lessonService) {
//        this.lessonService = lessonService;
//    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY))
    public void onMsg(OrderBasicDTO dto) {
        log.info("LessonChangeListener 接收到了信息 用户{}：添加课程{}",dto.getUserId(),dto.getCourseIds());
        //1.校验参数是否正确
        if(dto.getUserId()==null
                || dto.getOrderId() ==null
                || CollUtils.isEmpty(dto.getCourseIds())){
            //不要抛异常，否则会一直重试
            return;
        }

        //2.调用service，保存课程到课表
        lessonService.addUserLesson(dto.getUserId(), dto.getCourseIds());
    }

}
