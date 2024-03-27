package com.tianji.api.client.remark;

import com.tianji.api.client.remark.fallback.RemarkClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.api.client.like
 * @Project：tianji
 * @Date：2024/3/18 15:57
 * @Filename：LikeClient
 */
@FeignClient(value = "remark-service",fallbackFactory = RemarkClientFallback.class)
public interface RemarkClient {

    @GetMapping("/likes/isLiked")
    Set<Long> isBizLiked(@RequestParam("bizIds") List<Long> bizIds);


}
