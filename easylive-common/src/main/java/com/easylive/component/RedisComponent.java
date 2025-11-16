package com.easylive.component;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.constants.Constants; // 常量类
import com.easylive.entity.dto.UploadingFileDto;
import com.easylive.entity.dto.VideoPlayInfoDto;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.redis.RedisUtils; // Redis 工具
import com.easylive.utils.DateUtil;
import com.easylive.utils.StringTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.UUID;
import java.util.*;

@Component // Spring 组件
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils; // 注入 Redis 工具
    @Resource
    private AppConfig appConfig;
    public String saveCheckCode(String code) { // 保存验证码答案并返回 key
        String checkCodeKey = UUID.randomUUID().toString(); // 生成唯一 UUID
        redisUtils.setex( // 存入 Redis，带过期时间
                Constants.REDIS_KEY_CHECK_CODE + checkCodeKey, // 完整 key：前缀+UUID
                code, // 存验证码答案
                Constants.REDIS_KEY_EXPIRES_ONE_MIN * 10 // 过期时间：10 分钟
        );
        return checkCodeKey; // 返回给调用方
    }
    // 从 Redis 获取验证码
    public String getCheckCode(String checkCodeKey){
        // 拼接 Redis Key 并取出对应的值
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    // 删除 Redis 中的验证码
    public void cleanCheckCode(String checkCodeKey){
        // 拼接 Redis Key 并执行删除操作
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }
public void saveTokenInfo(TokenUserInfoDto tokenUserInfoDto){
    String token = UUID.randomUUID().toString();
    tokenUserInfoDto.setExpireAt(System.currentTimeMillis() + Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    tokenUserInfoDto.setToken(token);
    redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + token, tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
}
public void cleanToken(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
    }
    public TokenUserInfoDto getTokenInfo(String token) {
        return (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token);
        }
    public String saveTokenInfo4Admin(String account){
        String token = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_ADMIN+token,account,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
return token;
    }
    public String getLoginInfo4Admin(String token) {
        return (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }
    public void cleanToken4Admin(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }
    public void saveCategoryList(List<CategoryInfo> categoryInfoList){
        redisUtils.set(Constants.REDIS_KEY_CATEGORY_LIST,categoryInfoList);
    }
    public List<CategoryInfo> getCategoryList(){
       return(List<CategoryInfo>) redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST);
    }
    public String savePreVideoFileInfo(String userId,String fileName,Integer chunks){
        String uploadId= StringTools.getRandomString(15);
        UploadingFileDto fileDto=new UploadingFileDto();
        fileDto.setChunks(chunks);
        fileDto.setUploadId(uploadId);
        fileDto.setFileName(fileName);
        fileDto.setChunkIndex(0);
        String day= DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String filePath=day+"/"+userId+uploadId;
        String folder=appConfig.getProjectFolder()+Constants.FILE_FOLDER+Constants.FILE_FOLDER_TEMP+filePath;
        File folderFile = new File(folder);
        if(!folderFile.exists()){
            folderFile.mkdirs();
        }
        fileDto.setFilePath(filePath);
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE+userId+uploadId,fileDto,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
return uploadId;

    }
    public UploadingFileDto getUploadingVideoFile(String userId, String uploadId) {
        return (UploadingFileDto) redisUtils.get(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId);
    }
    public void updateVideoFileInfo(String userId, UploadingFileDto fileDto) {
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE + userId + fileDto.getUploadId(), fileDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY);
    }
    public SysSettingDto getSysSettingDto() {
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingDto == null) {
            sysSettingDto = new SysSettingDto();
        }
        return sysSettingDto;
    }
    public void delVideoFileInfo(String userId, String uploadId) {
        redisUtils.delete(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId);
    }
    /**
     * 将待删除的文件路径批量推入 Redis 队列，供后台异步清理任务消费
     */
    public void addFile2DelQueue(String videoId, List<String> fileIdList) {
        // 构造 Redis 列表 key：file_del:{videoId}  每个视频一条队列，避免全局锁
        String key = Constants.REDIS_KEY_FILE_DEL + videoId;

        // 批量左插（lpushAll）：
        // 1. 从左侧依次放入，保证先删的文件在列表右侧（先推后删）
        // 2. 设置过期时间为 7 天，防止因消费失败导致脏数据永久残留
        redisUtils.lpushAll(key, fileIdList, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    }
    public void addFile2TransferQueue(List<VideoInfoFilePost> fileList) {
        // 将任务批量左插到全局转码队列，不设置过期时间，永久保留直到被消费
        redisUtils.lpushAll(Constants.REDIS_KEY_QUEUE_TRANSFER, fileList, 0);
    }
    public void saveSettingDto(SysSettingDto sysSettingDto) {
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
    }

    public List<String> getDelFileList(String videoId) {
        return redisUtils.getQueueList(Constants.REDIS_KEY_FILE_DEL + videoId);
    }

    public void cleanDelFileList(String videoId) {
        redisUtils.delete(Constants.REDIS_KEY_FILE_DEL + videoId);

    }
    public Integer reportVideoPlayOnline(String fileId, String deviceId) {
        // 拼出“这个设备是否正在播这个视频”的 key，用于去重
        String userPlayOnlineKey = String.format(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_USER, fileId, deviceId);
        // 拼出“这个视频当前在线总人数”的 key
        String playOnlineCountKey = String.format(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE, fileId);//这个键对应的最后的结果

        // 如果这个设备还没上报过（key 不存在）
        if (!redisUtils.keyExists(userPlayOnlineKey)) {//如果当前的 userPlayOnlineKey还不存在 则给userPlayOnlineKey 添加到redis 中并进行续命
            // 给设备标记“在线”，有效期 8 s（短时间心跳）
            redisUtils.setex(userPlayOnlineKey, fileId, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 8);
            // 把视频在线总人数 +1，并给总 key 续期 10 s
            return redisUtils.incrementex(playOnlineCountKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 10).intValue();//同时playOnlineCountKey加上1
        }

        // 设备已存在，说明是心跳续命：给总在线数续 10 s
        redisUtils.expire(playOnlineCountKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 10);
        // 也给设备 key 续 8 s，防止过期
        redisUtils.expire(userPlayOnlineKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 8);

        // 取出最新的在线总人数返回
        Integer count = (Integer) redisUtils.get(playOnlineCountKey);
        return count == null ? 1 : count;//最少也得是一个人观看
    }
    public void decrementPlayOnlineCount(String key) {
        // 直接把传入的 key 对应值减 1，用于设备离线/停止播放时扣减在线人数
        redisUtils.decrement(key);
    }
    public void updateTokenInfo(TokenUserInfoDto tokenUserInfoDto) {
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + tokenUserInfoDto.getToken(), tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    }
    public void addKeywordCount(String keyword) {
        redisUtils.zaddCount(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT, keyword);// 添加到有序集合中
    }

    public List<String> getKeywordTop(Integer top) {
        return redisUtils.getZSetList(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT, top - 1);
    }
    public void addVideoPlay(VideoPlayInfoDto videoPlayInfoDto) {
        redisUtils.lpush(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY, videoPlayInfoDto, null);
    }
    public void recordVideoPlayCount(String videoId) {
        String date = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM_DD.getPattern());
        redisUtils.incrementex(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + date + ":" + videoId, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 2L);
    }//该方法用于记录视频每日播放量，通过 Redis 的原子自增操作实现高效的播放计数，并为 key 设置自动过期时间。

    public Map<String, Integer> getVideoPlayCount(String date) {
        //返回当日的所有的视频的播放量
        Map<String, Integer> videoPlayMap = redisUtils.getBatch(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + date);
        return videoPlayMap;
    }
}
