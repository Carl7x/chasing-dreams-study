package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author kyle
 * @since 2024-03-09
 */
@Api("互动问题相关接口")
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    @ApiOperation("新增互动问题")
    @PostMapping
    public void saveQuestions(@Validated @RequestBody QuestionFormDTO questionFormDTO){
        questionService.saveQuestion(questionFormDTO);
    }

    @ApiOperation("修改互动问题")
    @PutMapping("{id}")
    public void updateQuestions(@PathVariable Long id,
            @RequestBody QuestionFormDTO questionFormDTO){
        questionService.updateQuestion(id,questionFormDTO);
    }

    @ApiOperation("分页查询互动问题-用户端")
    @GetMapping("page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery questionPageQuery){
        return questionService.queryQuestionPage(questionPageQuery);
    }

    @ApiOperation("查询互动问题详情-用户端")
    @GetMapping("{id}")
    public QuestionVO queryQuestionById(@PathVariable Long id){
        return questionService.queryQuestionById(id);
    }
}
