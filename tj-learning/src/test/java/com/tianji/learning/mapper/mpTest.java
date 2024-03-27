package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.LearningApplication;
import com.tianji.learning.LessonStatus;
import com.tianji.learning.PlanStatus;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.service.ILearningLessonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：PACKAGE_NAME
 * @Project：tianji
 * @Date：2024/3/4 9:57
 * @Filename：mpTest
 */

@SpringBootTest(classes = LearningApplication.class)
class mpTest {

    @Autowired
    private ILearningLessonService service;

    @Test
    void test() {
        QueryWrapper<LearningLesson> wrapper = new QueryWrapper<>();
        wrapper.select("sum(week_freq) as plansTotal");
        wrapper.eq("user_id",2);
        wrapper.in("status", LessonStatus.NOT_BEGIN,LessonStatus.LEARNING);
        wrapper.eq("plan_status", PlanStatus.PLAN_RUNNING);
        Map<String, Object> map = service.getMap(wrapper);
        Integer plansTotal = Integer.valueOf(map.get("plansTotal").toString());
        System.out.println(plansTotal);
    }
}