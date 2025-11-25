package com.easylive.web.controller;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.enums.VideoOrderTypeEnum;
import com.easylive.entity.po.UserFocus;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.query.UserFocusQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserInfoVO;
import com.easylive.service.UserActionService;
import com.easylive.service.UserFocusService;
import com.easylive.service.UserInfoService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.CopyTools;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/uhome")
public class UHomeController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserFocusService userFocusService;

    @Resource
    private UserActionService userActionService;

    @RequestMapping("/getUserInfo")

    public ResponseVO getUserInfo(@NotEmpty String userId) {//传入的是浏览的页面的页面的主任
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();//拿到当前的token
        UserInfo userInfo = userInfoService.getUserDetailInfo(null == tokenUserInfoDto ? null : tokenUserInfoDto.getUserId(), userId);//传入当前的用户的userid 如果未登录也可以查看 传入null即可
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);//把查到的信息copy进入返回结果类中
        return getSuccessResponseVO(userInfoVO);
    }


    @RequestMapping("/updateUserInfo")//修改个人的各个参数

    public ResponseVO updateUserInfo(@NotEmpty @Size(max = 20) String nickName, //昵称，非空且不超过20字符
                                     @NotEmpty @Size(max = 100) String avatar, //头像URL，非空且不超过100字符
                                     @NotNull Integer sex, //性别，0/1/2，非空
                                     String birthday, //生日，可为空，格式yyyy-MM-dd
                                     @Size(max = 150) String school, //学校，最长150字符，可空
                                     @Size(max = 80) String personIntroduction, //个人简介，最长80字符，可空
                                     @Size(max = 300) String noticeInfo) { //公告信息，最长300字符，可空
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); //从请求头token解析当前登录用户
        UserInfo userInfo = new UserInfo(); //组装待更新实体
        userInfo.setUserId(tokenUserInfoDto.getUserId()); //仅设置当前用户ID，防止横向越权
        userInfo.setNickName(nickName);
        userInfo.setAvatar(avatar);
        userInfo.setSex(sex);
        userInfo.setBirthday(birthday);
        userInfo.setSchool(school);
        userInfo.setPersonIntroduction(personIntroduction);
        userInfo.setNoticeInfo(noticeInfo);//填充进用户对象中
        userInfoService.updateUserInfo(userInfo, tokenUserInfoDto); //持久化并刷新缓存/令牌信息

        return getSuccessResponseVO(null); //统一返回成功，无附加数据
    }

    @RequestMapping("/saveTheme")

    public ResponseVO saveTheme(Integer theme) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserInfo userInfo = new UserInfo();
        userInfo.setTheme(theme);
        userInfoService.updateUserInfoByUserId(userInfo, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/focus")
    public ResponseVO focus(@NotEmpty String focusUserId){
        userFocusService.focusUser(getTokenUserInfoDto().getUserId(),focusUserId);//参数1 ：关注的人 参数2 ：被关注的人
        return getSuccessResponseVO(null);
    }
   @RequestMapping("/cancelFocus")
    public ResponseVO cancelFocus(@NotEmpty String focusUserId){
        userFocusService.cancelFocus(getTokenUserInfoDto().getUserId(),focusUserId);
        return getSuccessResponseVO(null);
   }
    @RequestMapping("/loadFocusList")

    public ResponseVO loadFocusList(Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserFocusQuery focusQuery = new UserFocusQuery();
        focusQuery.setUserId(tokenUserInfoDto.getUserId());
        focusQuery.setQueryType(Constants.ZERO);
        focusQuery.setPageNo(pageNo);
        focusQuery.setOrderBy("focus_time desc");
        PaginationResultVO resultVO = userFocusService.findListByPage(focusQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/loadFansList")

    public ResponseVO loadFansList(Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserFocusQuery focusQuery = new UserFocusQuery();
        focusQuery.setFocusUserId(tokenUserInfoDto.getUserId());
        focusQuery.setQueryType(1);
        focusQuery.setPageNo(pageNo);
        focusQuery.setOrderBy("focus_time desc");
        PaginationResultVO resultVO = userFocusService.findListByPage(focusQuery);
        return getSuccessResponseVO(resultVO);
    }
    @RequestMapping("/loadVideoList")

    public ResponseVO loadVideoList(@NotEmpty String userId, Integer type, Integer pageNo, String videoName, Integer orderType) {//这个界面任何人都可查看 传入被观察者的userid
        VideoInfoQuery infoQuery = new VideoInfoQuery();
        if (type != null) {
            infoQuery.setPageSize(PageSize.SIZE10.getSize());//设置只能查看前十个视频
        }
        VideoOrderTypeEnum videoOrderTypeEnum = VideoOrderTypeEnum.getByType(orderType);//得知是根据什么排序
        if (videoOrderTypeEnum == null) {//默认按发布时间排序
            videoOrderTypeEnum = VideoOrderTypeEnum.CREATE_TIME;
        }
        infoQuery.setOrderBy(videoOrderTypeEnum.getField() + " desc");//填入条件
        infoQuery.setVideoNameFuzzy(videoName);//被观察者的昵称
        infoQuery.setPageNo(pageNo);//页数
        infoQuery.setUserId(userId);//被观测者的userid
        PaginationResultVO resultVO = videoInfoService.findListByPage(infoQuery);
        return getSuccessResponseVO(resultVO);
    }
    @RequestMapping("/loadUserCollection")

    public ResponseVO loadUserCollection(@NotEmpty String userId, Integer pageNo) {//传入被观测者的userid
        UserActionQuery actionQuery = new UserActionQuery();
        actionQuery.setActionType(UserActionTypeEnum.VIDEO_COLLECT.getType());//需要查询当前用户收藏的视频
        actionQuery.setUserId(userId);
        actionQuery.setPageNo(pageNo);
        actionQuery.setQueryVideoInfo(true);//需要设置需要关联查询视频详细信息
        actionQuery.setOrderBy("action_time desc");
        PaginationResultVO resultVO = userActionService.findListByPage(actionQuery);
        return getSuccessResponseVO(resultVO);
    }
}
