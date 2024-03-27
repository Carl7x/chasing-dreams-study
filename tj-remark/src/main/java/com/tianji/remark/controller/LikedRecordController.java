package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
@RequestMapping("/likes")
public class LikedRecordController {

//    @Autowired
//    @Qualifier("LikedRecordRedisServiceImpl")
//    private ILikedRecordService likedRecordService;

    @Resource(name = "LikedRecordRedisServiceImpl")
    private ILikedRecordService likedRecordService;

    @ApiOperation("点赞或取消")
    @PostMapping
    public void addLikeRecord(@RequestBody @Validated LikeRecordFormDTO likeRecordFormDTO){
        likedRecordService.addLikeRecord(likeRecordFormDTO);
    }

    @ApiOperation("批量查询点赞状态")
    @GetMapping("list")
    public Set<Long> getLikesByBizIds(@RequestParam("bizIds") List<Long> bizIds){
         return likedRecordService.getLikesByBizIds(bizIds);
    }

    @ApiOperation("查询用户是否点赞")
    @GetMapping("isLiked")
    public Set<Long> isBizLiked(@RequestParam("bizIds") List<Long> bizIds){
        return likedRecordService.isBizLiked(bizIds);
    }

}
