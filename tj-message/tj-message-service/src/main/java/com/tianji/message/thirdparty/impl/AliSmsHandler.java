package com.tianji.message.thirdparty.impl;

import com.tianji.api.dto.sms.SmsInfoDTO;
import com.tianji.message.thirdparty.ISmsHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("aliYun")
@Slf4j
public class AliSmsHandler implements ISmsHandler {
    @Override
    public void send(SmsInfoDTO platformSmsInfoDTO) {
        //第三方发送短信验证码
        log.info("aliYun平台，短信发送成功 ...");
        log.info("platformSmsInfoDTO：{}", platformSmsInfoDTO);
    }
}