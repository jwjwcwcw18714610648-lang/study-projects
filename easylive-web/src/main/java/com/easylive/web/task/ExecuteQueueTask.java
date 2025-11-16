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
import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class ExecuteQueueTask {
    private ExecutorService executorService= Executors.newFixedThreadPool(Constants.LENGTH_10);
    //固定长度为十个线程的线程池
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
    public void consumeTransferFileQueue(){
        executorService.execute(
                ()->{
                    while(true){
                        try {
                            VideoInfoFilePost videoInfoFile=(VideoInfoFilePost) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_TRANSFER);//拿到已经投稿的视频分片文件（从消息队列中）
                            if(videoInfoFile==null){
                                Thread.sleep(1500);//如果队列空 则睡眠一点五秒
                                continue;
                            }
                            //以下开始真正的完成任务：下载→转码→切片→上传OSS→回调（耗时几十秒到数分钟）
                            videoInfoPostService.transferVideoFile(videoInfoFile);
                        } catch (InterruptedException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
    }
    /**
     * 该方法用于后台异步消费Redis中的视频播放队列，实现对视频播放的统计处理。
     * 
     * 实现原理及步骤说明：
     * - @PostConstruct注解表示该方法会在Spring容器初始化当前Bean后自动执行，用于启动后台的消费任务。
     * - 内部通过线程池异步执行任务，避免阻塞主线程，确保高并发下也不会影响主流程。
     * - while(true) 实现常驻后台消费，持续监控队列。
     * - 具体处理流程：
     *   1. 使用redisUtils.rpop从指定的Redis队列（常量REDIS_KEY_QUEUE_VIDEO_PLAY）弹出一个视频播放信息对象（VideoPlayInfoDto）。该队列遵循先进先出原则。
     *   2. 如果队列为空，会让线程休眠1.5秒，避免空轮询造成CPU资源浪费。
     *   3. 队列有数据时，先调用videoInfoService.addReadCount方法，累计当前视频的播放总量（写入数据库）。
     *   4. 若videoPlayInfoDto中userId非空，则调用videoPlayHistoryService.saveHistory方法，将用户的该次播放记录进历史表。
     *   5. 接着每日播放量会通过redisComponent.recordVideoPlayCount记录到Redis，用于统计和报表分析。
     *   6. 最后，调用esSearchComponent.updateDocCount同步更新ElasticSearch中文档的播放次数（支持搜索排序）。
     * - 所有异常都会被捕获并记录日志，确保消费线程稳定运行不退出。
     */
    @PostConstruct
    public void consumeVideoPlayQueue() {
        executorService.execute(() -> {
            while (true) {
                try {
                    // 1. 从Redis队列获取一个视频播放信息对象
                    VideoPlayInfoDto videoPlayInfoDto = (VideoPlayInfoDto) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY);
                    if (videoPlayInfoDto == null) {
                        Thread.sleep(1500); // 队列暂时为空，暂停1.5s，降低轮询压力
                        continue;
                    }
                    // 2. 更新视频总播放次数（数据库实现）
                    videoInfoService.addReadCount(videoPlayInfoDto.getVideoId());
                    // 3. 如果有用户信息，保存该用户的播放历史
                    if (!StringTools.isEmpty(videoPlayInfoDto.getUserId())) {
                        videoPlayHistoryService.saveHistory(
                                videoPlayInfoDto.getUserId(), 
                                videoPlayInfoDto.getVideoId(), 
                                videoPlayInfoDto.getFileIndex());
                    }
                    // 4. 按天统计：记录当日视频播放量到Redis
                    redisComponent.recordVideoPlayCount(videoPlayInfoDto.getVideoId());
                    // 5. 同步ES搜索引擎的播放次数，用于搜索排序
                    esSearchComponent.updateDocCount(
                            videoPlayInfoDto.getVideoId(),
                            SearchOrderTypeEnum.VIDEO_PLAY.getField(),
                            1);
                } catch (Exception e) {
                    log.error("获取视频播放文件队列信息失败", e);
                }
            }
        });
    }
}
