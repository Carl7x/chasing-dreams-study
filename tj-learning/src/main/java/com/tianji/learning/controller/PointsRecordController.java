package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 前端控制器
 * </p>
 *
 * @author kyle
 * @since 2024-03-23
 */
@Api("积分相关接口")
@RestController
@RequestMapping("/points")
public class PointsRecordController {

    @Autowired
    private IPointsRecordService recordService;

    @ApiOperation("查询我的今日积分")
    @GetMapping("today")
    public List<PointsStatisticsVO> getTotalPoints(){
        List<PointsStatisticsVO> list = recordService.getTotalPoints();
        return list;
    }
}
