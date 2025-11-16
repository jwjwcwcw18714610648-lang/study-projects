package com.easylive.aspect;

import com.easylive.annotation.RecordUserMessage;
import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.MessageTypeEnum;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.UserMessage;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.UserMessageMapper;
import com.easylive.redis.RedisUtils;
import com.easylive.service.UserMessageService;
import jdk.nashorn.internal.parser.Token;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Component("userMessageOperationAspect")
@Aspect
@Slf4j
public class UserMessageOperationAspect {
    @Resource
    private UserMessageMapper userMessageMapper;
    @Resource
    private UserMessageService userMessageService;
    private static final String PARAMETERS_VIDEO_ID = "videoId";

    private static final String PARAMETERS_ACTION_TYPE = "actionType";

    private static final String PARAMETERS_REPLY_COMMENTID = "replyCommentId";

    private static final String PARAMETERS_AUDIT_REJECT_REASON = "reason";

    private static final String PARAMETERS_CONTENT = "content";
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private RedisUtils redisUtils;
    @Around("@annotation(com.easylive.annotation.RecordUserMessage)")
    public ResponseVO interceptoDo(ProceedingJoinPoint point) throws Throwable {
        try{/*这段代码是一个**“环绕切面”，作用：
凡是被 @RecordUserMessage 注解标记的方法，在真正执行业务前后都会被 UserMessageOperationAspect 拦截；*/
            // 1. 先让目标方法跑起来，拿到原始返回值
            ResponseVO result= (ResponseVO) point.proceed();
            // 2. 通过反射拿到 Method 对象
            Method method=((MethodSignature) point.getSignature()).getMethod();
            // 3. 读取方法上的 @RecordUserMessage 注解
            RecordUserMessage recordUserMessage=method.getAnnotation(RecordUserMessage.class);//获取传入的信息
            if(recordUserMessage!=null){
                // 4. 如果注解存在，就把注解内容 + 方法参数 保存成用户消息
                saveUserMessage(recordUserMessage,point.getArgs(),method.getParameters());
            }
            // 5. 把原返回值原样返回，不影响业务
            return result;
        }catch (BusinessException e) {
            log.error("全局拦截器异常", e);
            throw e;
        } catch (Exception e) {
            log.error("全局拦截器异常", e);
            throw e;
        } catch (Throwable e) {
            log.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }

    }
    private void saveUserMessage(RecordUserMessage recordUserMessage, Object[] arguments, Parameter[] parameters){
// TODO:
        // 1. 从 annotation 里获取消息模板、消息类型等配置；
        // 2. 结合 arguments 里的真实参数值，渲染出最终文本；
        // 3. 把文本写入数据库或消息队列，完成“记录用户消息”。
        log.info("aop消息准备");
        String videoId = null;
        Integer actionType = null;
        Integer replyCommentId = null;
        String content = null;
        for (int i = 0; i < parameters.length; i++) {
            if (PARAMETERS_VIDEO_ID.equals(parameters[i].getName())) {
                videoId = (String) arguments[i];
            } else if (PARAMETERS_ACTION_TYPE.equals(parameters[i].getName())) {
                actionType = (Integer) arguments[i];
            } else if (PARAMETERS_REPLY_COMMENTID.equals(parameters[i].getName())) {
                replyCommentId = (Integer) arguments[i];
            } else if (PARAMETERS_AUDIT_REJECT_REASON.equals(parameters[i].getName())) {
                content = (String) arguments[i];
            } else if (PARAMETERS_CONTENT.equals(parameters[i].getName())) {
                content = (String) arguments[i];
            }
        }// 通过反射拿到的参数 进行取值
        MessageTypeEnum messageTypeEnum=recordUserMessage.messageType();//拿到注解中的数据
        if(UserActionTypeEnum.VIDEO_COLLECT.equals(messageTypeEnum)){
            messageTypeEnum=messageTypeEnum.COLLECTION;
        }// 根据反射真正解包的类型 来确认 因为点赞收藏 时默认注解中传的时点赞类型
        TokenUserInfoDto tokenUserInfoDto=getTokenUserInfoDto();
        //因为管理端不需要传userid 所以如下
        userMessageService.saveUserMessage(videoId, tokenUserInfoDto == null ? null : tokenUserInfoDto.getUserId(), messageTypeEnum, content, replyCommentId);
    }
    private TokenUserInfoDto getTokenUserInfoDto() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (Constants.TOKEN_WEB.equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        return token == null ? null : redisComponent.getTokenInfo(token); }
}
