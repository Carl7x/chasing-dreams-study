package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author kyle
 * @since 2024-03-17
 */
@Api("点赞相关接口")
@RestController
@RequestMapping("/liked-record")
public class LikedRecordController {
    @Autowired
    private ILikedRecordService likedRecordService;

    @ApiOperation("点赞或取消")
    @PostMapping
    public void addLikeRecord(@RequestBody @Validated LikeRecordFormDTO likeRecordFormDTO){
        likedRecordService.addLikeRecord(likeRecordFormDTO);
    }

    @ApiOperation("批量查询点赞状态")
    @GetMapping("list")
    public Set<Long> getLikesByBizIds(@RequestParam List<Long> bizIds){
         return likedRecordService.getLikesByBizIds(bizIds);
    }
}
