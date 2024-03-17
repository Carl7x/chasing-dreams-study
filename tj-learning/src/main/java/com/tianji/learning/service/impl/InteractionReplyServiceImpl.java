package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_CREATE_TIME;
import static com.tianji.common.constants.Constant.DATA_FIELD_NAME_LIKED_TIME;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author kyle
 * @since 2024-03-09
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    final InteractionQuestionMapper questionMapper;
    final UserClient userclient;

    @Override
    public void saveReply(ReplyDTO replyDTO) {
        //1.获取用户id
        Long userId = UserContext.getUser();
        //2.保存回答或评论
        InteractionReply reply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        this.save(reply);
        //3.判断是否是回答
        Long answerId = replyDTO.getAnswerId();

        if (answerId != null) {
            //3.1 不是回答 累计回答下的评论次数
            InteractionReply answer = this.getById(answerId);
            answer.setReplyTimes(answer.getReplyTimes() + 1);
            this.updateById(answer);
        } else {
            //3.2 是回答 修改问题表最新回答id 累加问题下的回答次数
            Long questionId = replyDTO.getQuestionId();
            InteractionQuestion question = questionMapper.selectById(questionId);
            question.setLatestAnswerId(reply.getId());
            question.setAnswerTimes(question.getAnswerTimes() + 1);
            //4.判断是否是学生回答 学生回答则将问题表中该问题的status设置为未查看
            if (replyDTO.getIsStudent()) {
                question.setStatus(QuestionStatus.UN_CHECK);
            }
            questionMapper.updateById(question);
        }
    }

    @Override
    public PageDTO<ReplyVO> queryReplyVOPage(ReplyPageQuery query) {
        //1.校验questionId和answerId
        if (query.getQuestionId() == null && query.getAnswerId() == null) {
            throw new BadRequestException("问题id和回答id不能都为空");
        }
        Page<InteractionReply> page = this.lambdaQuery()
                .eq(query.getQuestionId() != null, InteractionReply::getQuestionId, query.getQuestionId())
                .eq(InteractionReply::getAnswerId, query.getAnswerId() == null ? 0L : query.getAnswerId())
                .eq(InteractionReply::getHidden, false)
                .page(query.toMpPage(
                        new OrderItem(DATA_FIELD_NAME_LIKED_TIME, false),
                        new OrderItem(DATA_FIELD_NAME_CREATE_TIME, true)
                ));
        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(0L, 0L);
        }
        //3.填充其他字段
        HashSet<Long> uIds = new HashSet<>();
        HashSet<Long> targetReplyIds = new HashSet<>();

        for (InteractionReply record : records) {
            if (!record.getAnonymity()) {
                uIds.add(record.getUserId());
                uIds.add(record.getTargetUserId());
            }
            if (record.getTargetReplyId() != null && record.getTargetReplyId() > 0) {
                targetReplyIds.add(record.getTargetReplyId());
            }
        }
        if (targetReplyIds.size() > 0) {
            List<InteractionReply> targetReplies = listByIds(targetReplyIds);
            Set<Long> targetUserIds = targetReplies.stream()
                    .filter(Predicate.not(InteractionReply::getAnonymity))
                    .map(InteractionReply::getUserId)
                    .collect(Collectors.toSet());
            uIds.addAll(targetUserIds);
        }

        List<UserDTO> userDTOS = userclient.queryUserByIds(uIds);
        Map<Long, UserDTO> userDTOMap = new HashMap<>();
        if (userDTOS != null) {
            userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, c -> c));
        }
        List<ReplyVO> voList = new ArrayList<>();
        for (InteractionReply record : records) {
            ReplyVO replyVO = BeanUtils.copyBean(record, ReplyVO.class);
            if(!record.getAnonymity()){
                UserDTO userDTO = userDTOMap.get(record.getUserId());
                if(userDTO!=null){
                    replyVO.setUserIcon(userDTO.getIcon());
                    replyVO.setUserName(userDTO.getName());
                    replyVO.setUserType(userDTO.getType());
                }
            }
            UserDTO targetUserDTO = userDTOMap.get(record.getTargetUserId());
            if(targetUserDTO!=null){
                replyVO.setTargetUserName(targetUserDTO.getName());
            }
            voList.add(replyVO);
        }
        return PageDTO.of(page,voList);
    }
}
