package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author kyle
 * @since 2024-03-23
 */
@Api("积分排行榜相关接口")
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsBoardController {
    private final IPointsBoardService boardService;

    @ApiOperation("查询学霸积分榜")
    @GetMapping
    public PointsBoardVO queryPointsBoardList(PointsBoardQuery pointsBoardQuery){
        return boardService.queryPointsBoardList(pointsBoardQuery);
    }
}
