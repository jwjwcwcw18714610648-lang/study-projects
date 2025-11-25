package com.easylive.web.component;

import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
@Slf4j
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {
    @Resource
    private RedisComponent redisComponent;
    //每次监听到redis 中存在键 过期 就会触发这个方法
    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();//将过期的key 还原出来
        if (!key.startsWith(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE_PREIFX + Constants.REDIS_KEY_VIDEO_PLAY_COUNT_USER_PREFIX)) {
            return;//startsWith = “是不是以…开头”。即判断key的格式是否是关于在线人数的
        }
        //监听 在线用户过期的key
        Integer userKeyIndex = key.indexOf(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_USER_PREFIX) + Constants.REDIS_KEY_VIDEO_PLAY_COUNT_USER_PREFIX.length();//找到 fileId 在过期 key 里的起始位置——也就是 "user:" 这段前缀结束之后的下标。
        String fileId = key.substring(userKeyIndex, userKeyIndex + 20);//这行代码的任务：从过期 key 里“抠”出 20 位的 fileId。
        redisComponent.decrementPlayOnlineCount(String.format(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE, fileId));//传入构造好的键 并调用rediscomment方法进行减去1
    }
}
