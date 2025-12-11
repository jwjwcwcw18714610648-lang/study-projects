package com.easylive.api.provider;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.service.UserActionService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/userAction")
public class UserActionApi {

    @Resource
    private UserActionService userActionService;

    @RequestMapping("/getUserActionList")
    List<UserAction> getUserActionList(@RequestBody UserActionQuery actionQuery) {
        return userActionService.findListByParam(actionQuery);
    }
}
