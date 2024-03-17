package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CatalogueDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author kyle
 * @since 2024-03-09
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {
    final IInteractionReplyService replyService;
    final UserClient userClient;
    final SearchClient searchClient;
    final CourseClient courseClient;
    final CatalogueClient catalogueClient;
    final CategoryCache categoryCache;

    /**
     * 保存互动提问问题
     *
     * @param questionFormDTO
     */
    @Override
    public void saveQuestion(QuestionFormDTO questionFormDTO) {
        //1.获取当前用户id
        Long userId = UserContext.getUser();
        //2.dto转po
        InteractionQuestion question = BeanUtils.copyBean(questionFormDTO, InteractionQuestion.class);
        question.setUserId(userId);
        //3.保存
        this.save(question);
    }

    /**
     * 修改互动问题
     *
     * @param id
     * @param questionFormDTO
     */
    @Override
    public void updateQuestion(Long id, QuestionFormDTO questionFormDTO) {
        //1.校验参数
        if (StringUtils.isBlank(questionFormDTO.getTitle()) || StringUtils.isBlank(questionFormDTO.getDescription())
                || questionFormDTO.getAnonymity() == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionQuestion question = this.getById(id);
        if (question == null) {
            throw new BadRequestException("非法参数id");
        }
        Long user = UserContext.getUser();
        if (!user.equals(question.getUserId())) {
            throw new BadRequestException("非法参数id,不能修改他人评论");
        }
        //2. dto转po
        question.setTitle(questionFormDTO.getTitle());
        question.setDescription(questionFormDTO.getDescription());
        question.setAnonymity(questionFormDTO.getAnonymity());
        //3.更新
        this.updateById(question);
    }

    /**
     * 分页查询互动问题
     *
     * @param questionPageQuery
     * @return
     */
    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery questionPageQuery) {
        //1.校验 参数courseId
        if (questionPageQuery.getCourseId() == null) {
            throw new BadRequestException("请求参数异常");
        }
        //2.获取登录用户id
        Long user = UserContext.getUser();
        //3.分页查询互动问题interaction——question
        //条件 courseId onlyMine为true 加userId sectionId不为空 加sectionId
        //hidden为false 查询分页按时间排序
        Page<InteractionQuestion> page = this.lambdaQuery()
                .select(InteractionQuestion.class,
                        tableFieldInfo -> !tableFieldInfo.getProperty().equals("description"))
                .eq(InteractionQuestion::getCourseId, questionPageQuery.getCourseId())
                .eq(questionPageQuery.getOnlyMine(), InteractionQuestion::getUserId, user)
                .eq(questionPageQuery.getSectionId() != null, InteractionQuestion::getSectionId, questionPageQuery.getSectionId())
                .eq(InteractionQuestion::getHidden, false)
                .page(questionPageQuery.toMpPage("create_time", false));
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
//        //4.根据回答id 查询最新回答信息
//        Set<Long> userIds = records.stream()
//                .filter(c -> !c.getAnonymity())
//                .map(InteractionQuestion::getUserId)
//                .collect(Collectors.toSet());
//        Set<Long> latestAnswerIds = records.stream()
//                .filter(c -> c.getLatestAnswerId() != null)
//                .map(InteractionQuestion::getLatestAnswerId)
//                .collect(Collectors.toSet());
//        //查询回答记录表
//        if(CollUtils.isNotEmpty(latestAnswerIds)){
//            List<InteractionReply> replies = replyService.listByIds(latestAnswerIds);
//            replies.stream()
//                    .filter(c->c.getUserId()!=null)
//                    .collect(Collectors.toMap(InteractionReply::getId, InteractionReply::getUserId));
//        }
        //4.根据回答id 查询最新回答信息
        Set<Long> latestAnswerIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (InteractionQuestion record : records) {
            if (!record.getAnonymity()) {
                userIds.add(record.getUserId());
            }
            if (record.getLatestAnswerId() != null) {
                latestAnswerIds.add(record.getLatestAnswerId());
            }
        }
        //批量查询回答信息 放入map<回答id，回答>
        HashMap<Long, InteractionReply> replyMap = new HashMap<>();
        if (CollUtils.isNotEmpty(latestAnswerIds)) {
            //List<InteractionReply> replies = replyService.listByIds(latestAnswerIds);
            List<InteractionReply> replies = replyService.list(Wrappers.<InteractionReply>lambdaQuery()
                    .in(InteractionReply::getId, latestAnswerIds)
                    .eq(InteractionReply::getHidden, false));
            for (InteractionReply reply : replies) {
                if (!reply.getAnonymity()) {
                    userIds.add(reply.getUserId());
                }
                replyMap.put(reply.getId(), reply);
            }
        }
        //5.远程调用用户服务 获取用户信息
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        //6.封装vo返回
        List<QuestionVO> voList = new ArrayList<>();
        for (InteractionQuestion record : records) {
            QuestionVO vo = BeanUtils.copyBean(record, QuestionVO.class);
            if (!vo.getAnonymity()) {
                UserDTO userDTO = userDTOMap.get(record.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }
            InteractionReply reply = replyMap.get(record.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
            }
            if (!record.getAnonymity()) {
                UserDTO userDTO = userDTOMap.get(reply.getUserId());
                if (userDTO != null) {
                    vo.setLatestReplyUser(userDTO.getName());
                }
            }
            voList.add(vo);
        }
        return PageDTO.of(page, voList);
    }

    /**
     * 根据id查询问题详情
     *
     * @param id
     * @return
     */
    @Override
    public QuestionVO queryQuestionById(Long id) {
        //1.参数校验
        if (id == null) {
            throw new BadRequestException("参数异常");
        }
        //2.查询互动问题表 id
        InteractionQuestion question = this.getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }
        //3.判断是否hidden
        if (question.getHidden()) {
            return null;
        }
        //4.匿名提问 不查询昵称和头像
        // 5.封装vo
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (!question.getAnonymity()) {
            UserDTO userDTO = userClient.queryUserById(question.getUserId());
            if (userDTO != null) {
                vo.setUserName(userDTO.getName());
                vo.setUserIcon(userDTO.getIcon());
            }
        }
        return vo;
    }

    /**
     * 分页查询问题列表-管理端
     *
     * @param query
     * @return
     */
    @Override
    public PageDTO<QuestionAdminVO> queryQuestionAdminVOPage(QuestionAdminPageQuery query) {
        //1.查询互动问题表
        List<Long> courseIds = null;
        //从ES里面通过课程名称查询课程id
        if (StringUtils.isNotBlank(query.getCourseName())) {
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)) {
                return PageDTO.empty(0L, 0L);
            }
        }
        Page<InteractionQuestion> page = this.lambdaQuery()
                .in(CollUtils.isNotEmpty(courseIds), InteractionQuestion::getCourseId, courseIds)
                .eq(query.getStatus() != null, InteractionQuestion::getStatus, query.getStatus())
                .between(query.getBeginTime() != null && query.getEndTime() != null,
                        InteractionQuestion::getCreateTime, query.getBeginTime(), query.getEndTime())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(0L, 0L);
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        Set<Long> chapterAndSectionIds = new HashSet<>();
        for (InteractionQuestion record : records) {
            userIds.add(record.getUserId());
            cIds.add(record.getCourseId());
            chapterAndSectionIds.add(record.getChapterId());
            chapterAndSectionIds.add(record.getSectionId());
        }
        //2.远程调用用户服务 获取用户信息
        List<UserDTO> userDTOS = userClient.queryUserByIds(userIds);
        if (CollUtils.isEmpty(userDTOS)) {
            throw new BizIllegalException("用户不存在");
        }
        Map<Long, UserDTO> userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        //3.远程调用课程服务 获取课程信息
        List<CourseSimpleInfoDTO> courseList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(courseList)) {
            throw new BizIllegalException("课程不存在");
        }
        Map<Long, CourseSimpleInfoDTO> courseDTOMap = courseList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        //4.远程调用章节服务 获取章节信息
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(chapterAndSectionIds);
        if (CollUtils.isEmpty(cataSimpleInfoDTOS)) {
            throw new BizIllegalException("课程不存在");
        }
        Map<Long, CataSimpleInfoDTO> cataSimpleInfoDTOMap = cataSimpleInfoDTOS.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, c -> c));

        //5.获取分类信息
        //6.封装vo
        ArrayList<QuestionAdminVO> voList = new ArrayList<>();
        for (InteractionQuestion record : records) {
            QuestionAdminVO adminVO = BeanUtils.copyBean(record, QuestionAdminVO.class);
            UserDTO userDTO = userDTOMap.get(record.getUserId());
            if (userDTO != null) {
                adminVO.setUserName(userDTO.getName());
            }
            CourseSimpleInfoDTO courseDTO = courseDTOMap.get(record.getCourseId());
            if (courseDTO != null) {
                adminVO.setCourseName(courseDTO.getName());
                List<Long> categoryIds = courseDTO.getCategoryIds();
                String categoryNames = categoryCache.getCategoryNames(categoryIds);
                adminVO.setCategoryName(categoryNames);
            }
            CataSimpleInfoDTO cataChapterDTO = cataSimpleInfoDTOMap.get(record.getChapterId());
            if (cataChapterDTO != null) {
                adminVO.setChapterName(cataChapterDTO.getName());
            }
            CataSimpleInfoDTO cataSectionDTO = cataSimpleInfoDTOMap.get(record.getSectionId());
            if (cataSectionDTO != null) {
                adminVO.setSectionName(cataSectionDTO.getName());
            }
            voList.add(adminVO);
        }
        return PageDTO.of(page, voList);
    }
}
