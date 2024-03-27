package com.tianji.learning.mq.msg;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * @description:
 * @Author：kyle
 * @gitee: https://gitee.com/kyle20251
 * @Package：com.tianji.learning.mq.msg
 * @Project：tianji
 * @Date：2024/3/24 9:13
 * @Filename：m
 */

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SignInMessage {
    private Long userId;
    private Integer points;
}