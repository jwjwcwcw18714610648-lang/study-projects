package com.easylive.admin.controller;

import com.easylive.component.RedisComponent;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Validated
@Slf4j
@RestController
@RequestMapping("/setting")
public class SettingController extends ABaseController {

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/getSetting")
    public ResponseVO getSetting() {
        return getSuccessResponseVO(redisComponent.getSysSettingDto());
    }


    @RequestMapping("/saveSetting")
    public ResponseVO saveSetting(SysSettingDto sysSettingDto) {
        redisComponent.saveSettingDto(sysSettingDto);
        return getSuccessResponseVO(null);
    }
}
