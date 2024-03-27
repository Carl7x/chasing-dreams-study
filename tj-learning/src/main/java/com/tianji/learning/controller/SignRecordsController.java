package com.tianji.learning.controller;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.controller
 * @Project：tianji
 * @Date：2024/3/23 16:02
 * @Filename：SignRecordContrller
 */

import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签到相关接口
 */
@Api(tags = "签到相关接口")
@RestController("sign-records")
@RequiredArgsConstructor
public class SignRecordsController {
    private ISignRecordService service;

    @ApiOperation("签到")
    @PostMapping
    public SignResultVO addSignRecords(){
        SignResultVO resultVO = service.addSignRecords();
        return resultVO;
    }

    @ApiOperation("签到查询")
    @GetMapping
    public Integer[] getSignRecords(){
        Integer[] arr = service.getSignRecords();
        return arr;
    }
}
