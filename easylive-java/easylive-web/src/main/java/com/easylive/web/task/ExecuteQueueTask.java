package com.easylive.web.task;

import com.easylive.component.EsSearchComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.VideoPlayInfoDto;
import com.easylive.entity.enums.SearchOrderTypeEnum;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.redis.RedisUtils;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import com.easylive.service.VideoPlayHistoryService;
import com.easylive.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ExecuteQueueTask {

    // 添加 volatile 标志位，用于控制循环结束
    private volatile boolean isRunning = true;

    private ExecutorService executorService = Executors.newFixedThreadPool(Constants.LENGTH_10);

    @Resource
    private VideoPlayHistoryService videoPlayHistoryService;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private VideoInfoService videoInfoService;
    @Resource
    private VideoInfoPostService videoInfoPostService;
    @Resource
    private EsSearchComponent esSearchComponent;

    @PostConstruct
    public void consumeTransferFileQueue() {
        executorService.execute(() -> {
            while (isRunning) { // 1. 使用标志位替代 true
                try {
                    // 拿到已经投稿的视频分片文件
                    VideoInfoFilePost videoInfoFile = (VideoInfoFilePost) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_TRANSFER);
                    if (videoInfoFile == null) {
                        Thread.sleep(1500);
                        continue;
                    }
                    // 下载→转码→切片→上传OSS→回调
                    videoInfoPostService.transferVideoFile(videoInfoFile);
                } catch (Exception e) {
                    // 2. 捕获异常，如果是由于关闭引起的，则优雅退出
                    if (!isRunning || (e.getMessage() != null && e.getMessage().contains("LettuceConnectionFactory was destroyed"))) {
                        log.info("应用正在关闭，停止转码任务消费。");
                        break;
                    }
                    log.error("处理转码任务异常", e);
                }
            }
        });
    }

    @PostConstruct
    public void consumeVideoPlayQueue() {
        executorService.execute(() -> {
            while (isRunning) { // 1. 使用标志位替代 true
                try {
                    VideoPlayInfoDto videoPlayInfoDto = (VideoPlayInfoDto) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY);
                    if (videoPlayInfoDto == null) {
                        Thread.sleep(1500);
                        continue;
                    }
                    videoInfoService.addReadCount(videoPlayInfoDto.getVideoId());
                    if (!StringTools.isEmpty(videoPlayInfoDto.getUserId())) {
                        videoPlayHistoryService.saveHistory(
                                videoPlayInfoDto.getUserId(),
                                videoPlayInfoDto.getVideoId(),
                                videoPlayInfoDto.getFileIndex());
                    }
                    redisComponent.recordVideoPlayCount(videoPlayInfoDto.getVideoId());
                    esSearchComponent.updateDocCount(
                            videoPlayInfoDto.getVideoId(),
                            SearchOrderTypeEnum.VIDEO_PLAY.getField(),
                            1);
                } catch (Exception e) {
                    // 2. 捕获异常，如果是由于关闭引起的，则优雅退出
                    if (!isRunning || (e.getMessage() != null && e.getMessage().contains("LettuceConnectionFactory was destroyed"))) {
                        log.info("应用正在关闭，停止播放记录消费。");
                        break;
                    }
                    log.error("获取视频播放文件队列信息失败", e);
                }
            }
        });
    }

    /**
     * 3. 添加销毁方法
     * 当 Spring 容器关闭时执行
     */
    @PreDestroy
    public void destroy() {
        log.info("正在停止 ExecuteQueueTask...");
        this.isRunning = false; // 停止 while 循环
        executorService.shutdown(); // 停止接收新任务
        try {
            // 等待正在执行的任务完成（例如正在转码的任务），最多等待 60 秒
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // 超时强制关闭
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        log.info("ExecuteQueueTask 已停止。");
    }
}