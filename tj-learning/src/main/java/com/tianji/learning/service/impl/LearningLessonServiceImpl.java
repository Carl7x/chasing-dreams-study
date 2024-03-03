package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author kyle
 * @since 2024-03-02
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    //注入feign api
    final CourseClient courseClient;

    /**
     * 保存课程到课表
     *
     * @param userId
     * @param courseIds
     */
    @Override
    public void addUserLesson(Long userId, List<Long> courseIds) {
        //1.通过feign远程调用课程服务，得到课程信息，计算时间
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        //2.封装po实体类，填充过期时间
        List<LearningLesson> list = new ArrayList<>();
        for (CourseSimpleInfoDTO courseSimpleInfoDTO : simpleInfoList) {
            LearningLesson lesson = new LearningLesson();
            lesson.setUserId(userId);
            lesson.setCourseId(courseSimpleInfoDTO.getId());
            //课程有效期 单位为月
            Integer validDuration = courseSimpleInfoDTO.getValidDuration();

            if (validDuration != null) {
                LocalDateTime now = LocalDateTime.now();
                //过期时间
                LocalDateTime expireTime = now.plusMonths(validDuration);
                lesson.setCreateTime(now);
                lesson.setExpireTime(expireTime);
            }
            list.add(lesson);
        }
        //3.批量保存
        this.saveBatch(list);
    }

    /**
     * 查询我的课程
     *
     * @param query
     * @return
     */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1.获取当前用户信息
        Long user = UserContext.getUser();
        if (user == null) {
            throw new BadRequestException("必须要登录");
        }
        //2.分页查询
        Page<LearningLesson> page = this.lambdaQuery()
                .eq(LearningLesson::getId, user)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        //3.远程调用feign获取课程信息
        List<Long> courseIds = records
                .stream().map(LearningLesson::getCourseId)
                .collect(Collectors.toList());
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollectionUtils.isEmpty(simpleInfoList)){
            throw new BizIllegalException("课程不存在");
        }
        //将课程信息放进map里
        Map<Long, CourseSimpleInfoDTO> infoDTOMap = simpleInfoList.
                stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        //4.po填充数据vo
        ArrayList<LearningLessonVO> voList = new ArrayList<>();
        for (LearningLesson record : records) {
            LearningLessonVO learningLessonVO = BeanUtils.copyBean(record, LearningLessonVO.class);
            CourseSimpleInfoDTO infoDTO = infoDTOMap.get(record.getCourseId());
            if(infoDTO!=null){
                learningLessonVO.setCourseName(infoDTO.getName());
                learningLessonVO.setCourseCoverUrl(infoDTO.getCoverUrl());
                learningLessonVO.setSections(infoDTO.getSectionNum());
            }
            voList.add(learningLessonVO);
        }
        //5.封装返回
        //new PageDTO<>()
        return PageDTO.of(page,voList);
    }
}
