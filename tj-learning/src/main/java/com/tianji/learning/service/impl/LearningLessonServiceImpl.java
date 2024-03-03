package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSearchDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.LessonStatus;
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
    final CatalogueClient catalogueClient;

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
        if (CollectionUtils.isEmpty(simpleInfoList)) {
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
            if (infoDTO != null) {
                learningLessonVO.setCourseName(infoDTO.getName());
                learningLessonVO.setCourseCoverUrl(infoDTO.getCoverUrl());
                learningLessonVO.setSections(infoDTO.getSectionNum());
            }
            voList.add(learningLessonVO);
        }
        //5.封装返回
        //new PageDTO<>()
        return PageDTO.of(page, voList);
    }

    /**
     * 返回当前学习课程
     *
     * @return
     */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        //1.获取当前用户
        Long user = UserContext.getUser();
        //2.最近学习课程 DESC （课程状态 = 1 正在学习中的）
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, user)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }
        //3.调用feign课程信息
        CourseFullInfoDTO courseInfoById = courseClient
                .getCourseInfoById(lesson.getCourseId(), false, false);
        if (courseInfoById==null){
            throw new BizIllegalException("课程不存在");
        }
        //4.查询用户课表课程总数
        Integer count = this.lambdaQuery().eq(LearningLesson::getUserId, user).count();
        //5.调用feign 获取课程小结名称和编号
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient
                .batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if(CollUtils.isEmpty(cataSimpleInfoDTOS)){
            throw new BizIllegalException("小节不存在");
        }
        //6.将po封装到vo 填充
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        vo.setCourseName(courseInfoById.getName());
        vo.setCourseCoverUrl(courseInfoById.getCoverUrl());
        vo.setSections(courseInfoById.getSectionNum());
        vo.setCourseAmount(count);
        //设置小节信息
        CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOS.get(0);
        vo.setLatestSectionName(cataSimpleInfoDTO.getName());
        vo.setLatestSectionIndex(cataSimpleInfoDTO.getCIndex());
        return vo;
    }

    /**
     * 查询当前用户指定课程的学习进度
     * @param courseId 课程id
     * @return 课表信息、学习记录及进度信息
     */
    @Override
    public Long isLessonValid(Long courseId) {
        //1.获取用户
        Long user = UserContext.getUser();
        //2.获取课表
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, user)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if(lesson==null){
            return null;
        }
        //3.校验课程状态
        LocalDateTime expireTime = lesson.getExpireTime();
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(expireTime)){
            return null;
        }
        return lesson.getId();
    }

    /**
     * 查询用户课表中指定课程状态
     * @param courseId
     * @return
     */
    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        //1.获取用户
        Long user = UserContext.getUser();
        //2.获取课表
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, user)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if(lesson==null){
            return null;
        }
        //3.封装vo
        return BeanUtils.copyBean(lesson, LearningLessonVO.class);
    }
}
