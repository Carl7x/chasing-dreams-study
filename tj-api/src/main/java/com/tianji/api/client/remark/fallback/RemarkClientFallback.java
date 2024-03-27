package com.tianji.api.client.remark.fallback;

import com.tianji.api.client.remark.RemarkClient;
import com.tianji.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;

/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.api.client.remark.fallback
 * @Project：tianji
 * @Date：2024/3/18 16:25
 * @Filename：RemarkClientFallback
 */

/**
 * 需要让spring管理 注册bean
 */
@Slf4j
public class RemarkClientFallback implements FallbackFactory<RemarkClient> {
    @Override
    public RemarkClient create(Throwable cause) {
        log.error("点赞服务异常", cause);
        return new RemarkClient() {
            @Override
            public Set<Long> isBizLiked(List<Long> bizIds) {
                return CollUtils.emptySet();
            }
        };
    }
}
