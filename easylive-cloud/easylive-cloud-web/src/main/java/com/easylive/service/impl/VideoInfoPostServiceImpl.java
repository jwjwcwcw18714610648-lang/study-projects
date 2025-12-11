package com.easylive.service.impl;

import com.easylive.component.EsSearchComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.UploadingFileDto;
import com.easylive.entity.enums.*;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.*;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.*;
import com.easylive.service.VideoInfoPostService;
import com.easylive.utils.CopyTools;
import com.easylive.utils.FFmpegUtils;
import com.easylive.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 视频信息 业务接口实现
 */
@Slf4j
@Service("videoInfoPostService")
public class VideoInfoPostServiceImpl implements VideoInfoPostService {

    @Resource
    private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

    @Resource
    private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;

    @Resource
    private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

    @Resource
    private VideoInfoFileMapper<VideoInfoFile, VideoInfoFileQuery> videoInfoFileMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FFmpegUtils fFmpegUtils;
    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    private UserInfoMapper userInfoMapper;

    //把指定目录下按序号命名的所有分片文件（0、1、2…）顺序合并成一个完整文件，并可选择是否删除源分片。
    public static void union(String dirPath, String toFilePath, boolean delSource) throws BusinessException {
        // 1. 入参：分片目录、目标完整文件路径、是否删除源分片；抛出业务异常方便上层事务回滚
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        try (RandomAccessFile writeFile = new RandomAccessFile(targetFile, "rw")) {
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            throw new BusinessException("合并文件" + dirPath + "出错了");
        } finally {
            if (delSource) {
                for (int i = 0; i < fileList.length; i++) {
                    fileList[i].delete();
                }
            }
        }
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public List<VideoInfoPost> findListByParam(VideoInfoPostQuery param) {
        return this.videoInfoPostMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(VideoInfoPostQuery param) {
        return this.videoInfoPostMapper.selectCount(param);
    }

    /**
     * 审核视频
     * @param videoId 视频ID
     * @param status 视频审核状态
     * @param reason 审核不通过原因或备注
     * @throws IOException
     */
    @Override
    public void auditVideo(String videoId, Integer status, String reason) throws IOException {
        // 1. 根据传入的status找到对应的视频状态枚举
        VideoStatusEnum videoStatusEnum = VideoStatusEnum.getByStatus(status);
        // 如果状态不存在，说明状态不合法，抛出异常
        if (videoStatusEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 2. 构造只更新status状态的VideoInfoPost对象
        VideoInfoPost videoInfoPost = new VideoInfoPost();
        videoInfoPost.setStatus(status); // 只修改状态字段，其他字段不作修改

        // 3. 构造条件，仅更新status为“待审核”并且videoId相同的数据
        VideoInfoPostQuery videoInfoPostQuery = new VideoInfoPostQuery();
        videoInfoPostQuery.setStatus(VideoStatusEnum.STATUS2.getStatus());  // STATUS2代表待审核
        videoInfoPostQuery.setVideoId(videoId);

        // 4. 根据条件批量更新视频审核状态
        Integer audioCount = videoInfoPostMapper.updateByParam(videoInfoPost, videoInfoPostQuery);
        // 如果未更新任何数据，说明审核失败，抛出异常
        if (audioCount == 0) {
            throw new BusinessException("审核失败，请稍后重试");
        }

        // 5. 更新视频文件的update_type为NO_UPDATE，防止后续被同步覆盖
        VideoInfoFilePost videoInfoFilePost = new VideoInfoFilePost();
        videoInfoFilePost.setUpdateType(VideoFileUpdateTypeEnum.NO_UPDATE.getStatus());
        VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
        filePostQuery.setVideoId(videoId);
        this.videoInfoFilePostMapper.updateByParam(videoInfoFilePost, filePostQuery);

        // 6. 如果status为“审核不通过”，直接结束方法，不再写入正式表
        if (VideoStatusEnum.STATUS4 == videoStatusEnum) {
            return;
        }

        // 7. 从发布表获取最新审核后的视频详情
        VideoInfoPost infoPost = this.videoInfoPostMapper.selectByVideoId(videoId);

        // 8. 查询正式表是否有历史记录，用于判定是否是首次过审
        VideoInfo dbVideoInfo = this.videoInfoMapper.selectByVideoId(videoId);
        if (dbVideoInfo == null) {
            // 首次通过审核
            SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
            // TODO: 给用户加硬币奖励
            userInfoMapper.updateCoinCountInfo(infoPost.getUserId(), sysSettingDto.getPostVideoCoinCount());
        }

        // 9. 将发布表中的视频数据复制到正式表，并执行插入或更新操作
        VideoInfo videoInfo = CopyTools.copy(infoPost, VideoInfo.class);
        this.videoInfoMapper.insertOrUpdate(videoInfo);

        // 10. 清空正式文件存储表的当前视频所有文件记录，准备覆盖
        VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
        videoInfoFileQuery.setVideoId(videoId);
        this.videoInfoFileMapper.deleteByParam(videoInfoFileQuery);

        // 11. 查询发布表的所有视频文件信息
        VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
        videoInfoFilePostQuery.setVideoId(videoId);
        List<VideoInfoFilePost> videoInfoFilePostList = this.videoInfoFilePostMapper.selectList(videoInfoFilePostQuery);

        // 12. 复制发布表的视频文件列表到正式表结构对象
        List<VideoInfoFile> videoInfoFileList = CopyTools.copyList(videoInfoFilePostList, VideoInfoFile.class);

        // 13. 批量写入正式文件表
        this.videoInfoFileMapper.insertBatch(videoInfoFileList);

        // 14. 获取Redis中待删除的文件路径列表（分片等历史临时文件）
        List<String> filePathList = redisComponent.getDelFileList(videoId);

        // 15. 遍历删除所有待删除临时文件夹或文件
        if (filePathList != null) {
            for (String path : filePathList) {
                File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + path);
                if (file.exists()) {
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        log.error("删除文件失败", e);
                    }
                }
            }
        }

        // 16. 清除Redis中删除路径的缓存
        redisComponent.cleanDelFileList(videoId);

        // 17. 将视频信息同步到ES索引中，便于搜索
        esSearchComponent.saveDoc(videoInfo);
    }
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void transferVideoFile4Db(String videoId, String uploadId, String userId, VideoInfoFilePost updateFilePost) {
        //更新文件状态
        videoInfoFilePostMapper.updateByUploadIdAndUserId(updateFilePost, uploadId, userId);
        //更新视频信息
        VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
        fileQuery.setVideoId(videoId);
        fileQuery.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
        Integer failCount = videoInfoFilePostMapper.selectCount(fileQuery);
        if (failCount > 0) {
            VideoInfoPost videoUpdate = new VideoInfoPost();
            videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
            videoInfoPostMapper.updateByVideoId(videoUpdate, videoId);
            return;
        }
        fileQuery.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
        Integer transferCount = videoInfoFilePostMapper.selectCount(fileQuery);
        if (transferCount == 0) {
            Integer duration = videoInfoFilePostMapper.sumDuration(videoId);
            VideoInfoPost videoUpdate = new VideoInfoPost();
            videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
            videoUpdate.setDuration(duration);
            videoInfoPostMapper.updateByVideoId(videoUpdate, videoId);
        }
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<VideoInfoPost> list = this.findListByParam(param);
        PaginationResultVO<VideoInfoPost> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }
    @Override
    public void recommendVideo(String videoId) {
        VideoInfo videoInfo = videoInfoMapper.selectByVideoId(videoId);
        if (videoInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        Integer recommendType = null;
        if (VideoRecommendTypeEnum.RECOMMEND.getType().equals(videoInfo.getRecommendType())) {
            recommendType = VideoRecommendTypeEnum.NO_RECOMMEND.getType();
        } else {
            recommendType = VideoRecommendTypeEnum.RECOMMEND.getType();
        }
        VideoInfo updateInfo = new VideoInfo();
        updateInfo.setRecommendType(recommendType);
        videoInfoMapper.updateByVideoId(updateInfo, videoId);
    }
    /**
     * 新增
     */
    @Override
    public Integer add(VideoInfoPost bean) {
        return this.videoInfoPostMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<VideoInfoPost> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.videoInfoPostMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<VideoInfoPost> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.videoInfoPostMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(VideoInfoPost bean, VideoInfoPostQuery param) {
        StringTools.checkParam(param);
        return this.videoInfoPostMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(VideoInfoPostQuery param) {
        StringTools.checkParam(param);
        return this.videoInfoPostMapper.deleteByParam(param);
    }

    /**
     * 根据VideoId获取对象
     */
    @Override
    public VideoInfoPost getVideoInfoPostByVideoId(String videoId) {
        return this.videoInfoPostMapper.selectByVideoId(videoId);
    }

    /**
     * 根据VideoId修改
     */
    @Override
    public Integer updateVideoInfoPostByVideoId(VideoInfoPost bean, String videoId) {
        return this.videoInfoPostMapper.updateByVideoId(bean, videoId);
    }

    /**
     * 根据VideoId删除
     */
    @Override
    public Integer deleteVideoInfoPostByVideoId(String videoId) {
        return this.videoInfoPostMapper.deleteByVideoId(videoId);
    }

    /**
     * 保存视频投稿或编辑视频信息的主要方法。
     * 
     * 1. 判断分P数目是否超出系统限制，超出则抛异常。
     * 2. 判断是新增投稿还是编辑：
     *    - 新投稿：未传 videoId，
     *        - 新生成 videoId、设置状态为“转码中”，插入视频表。
     *    - 编辑：传了videoId，
     *        - 校验视频是否存在，且状态不是“待审核/转码中”，否则抛异常。
     *        - 查询当前用户下该视频的所有分P投稿（dbInfoFileList）。
     *        - uploadFileMap 用于快速根据上传id定位本次前端分P。
     *        - 比较库和前端，每发现一个库中存在但前端未传入的分P，计入 deleteFileList，待删除。
     *        - 若分P仅改名，updateFileName=true。
     *        - 统计需要添加的新文件（前端未带fileId）。
     *        - 判断视频元信息是否被编辑，需要重新审核状态。
     *        - 更新数据库表中的视频状态和时间。
     * 3. 删除前端被移除的分P：先删db记录，后推送磁盘异步删除。
     * 4. 针对所有分P补充索引、videoId、userId等字段，对于新加分P随机生成fileId，并标记为待转码。
     * 5. 批量插入/更新所有分P到库。
     * 6. 新增的分P统一加入转码队列，等待后续处理。
     */
    @Override
    public void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> uploadFileList) {
        // 1. 校验分P数量
        if (uploadFileList.size() > redisComponent.getSysSettingDto().getVideoCount()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600); // 超过最大上传限制
        }

        // 2. 判断是“编辑”还是“新投稿”
        if (!StringTools.isEmpty(videoInfoPost.getVideoId())) {
            // 编辑视频：videoId 不为空
            VideoInfoPost videoInfoPostDb = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
            if (videoInfoPostDb == null) {
                throw new BusinessException(ResponseCodeEnum.CODE_600); // 原始视频不存在
            }
            // 编辑时状态校验，不允许“待审核”或“转码中”修改内容
            if (ArrayUtils.contains(
                    new Integer[]{VideoStatusEnum.STATUS0.getStatus(), VideoStatusEnum.STATUS2.getStatus()},
                    videoInfoPostDb.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        // 公共变量初始化
        Date curDate = new Date();
        String videoId = videoInfoPost.getVideoId();
        List<VideoInfoFilePost> deleteFileList = new ArrayList<>(); // 待删除分P
        List<VideoInfoFilePost> addFileList = uploadFileList;       // 待新增分P（初值等于全部上传分P）

        if (StringTools.isEmpty(videoId)) {
            // 新投稿（videoId为空）
            videoId = StringTools.getRandomString(Constants.LENGTH_10); // 生成视频id
            videoInfoPost.setVideoId(videoId);
            videoInfoPost.setCreateTime(curDate);
            videoInfoPost.setLastUpdateTime(curDate);
            videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus()); // 初始状态为“转码中”
            this.videoInfoPostMapper.insert(videoInfoPost);
        } else {
            // 已有videoId：编辑流程
            VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
            filePostQuery.setVideoId(videoId);
            filePostQuery.setUserId(videoInfoPost.getUserId());
            // 查询所有已存在分P
            List<VideoInfoFilePost> dbInfoFileList = this.videoInfoFilePostMapper.selectList(filePostQuery);

            // 前端最新分P列表，用 Map 便于查找（key:uploadId）
            Map<String, VideoInfoFilePost> uploadFileMap = uploadFileList.stream()
                    .collect(Collectors.toMap(
                            VideoInfoFilePost::getUploadId, 
                            Function.identity(), 
                            (data1, data2) -> data2
                    ));

            Boolean updateFileName = false;
            // 遍历数据库分P列表
            for (VideoInfoFilePost fileInfo : dbInfoFileList) {
                VideoInfoFilePost updateFile = uploadFileMap.get(fileInfo.getUploadId());
                if (updateFile == null) {
                    // 数据库有、前端没传——删除
                    deleteFileList.add(fileInfo);
                } else if (!updateFile.getFileName().equals(fileInfo.getFileName())) {
                    // 只是改了文件名
                    updateFileName = true;
                }
            }

            // 重新统计本次需要新转码的分P（前端未带fileId的为新增上传）
            addFileList = uploadFileList.stream().filter(item -> item.getFileId() == null)
                    .collect(Collectors.toList());
            videoInfoPost.setLastUpdateTime(curDate); // 更新时间戳

            // 判断“视频文字内容/分类/简介”等是否被编辑（触发待审核）
            Boolean changeVideoInfo = this.changeVideoInfo(videoInfoPost);

            // 状态变更：如有新上传，转码中。如只改文本信息/文件名，待审核。
            if (!addFileList.isEmpty()) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
            } else if (changeVideoInfo || updateFileName) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS2.getStatus());
            }
            // 更新视频信息
            this.videoInfoPostMapper.updateByVideoId(videoInfoPost, videoInfoPost.getVideoId());
        }

        // 3. 删除前端已移除分P：删库记录+异步删文件
        if (!deleteFileList.isEmpty()) {
            // 批量删除分P在表中的记录
            List<String> delFileIdList = deleteFileList.stream()
                    .map(VideoInfoFilePost::getFileId)
                    .collect(Collectors.toList());
            this.videoInfoFilePostMapper.deleteBatchByFileId(delFileIdList, videoInfoPost.getUserId());
            // 推到删除队列，异步删磁盘文件
            List<String> delFilePathList = deleteFileList.stream()
                    .map(VideoInfoFilePost::getFilePath)
                    .collect(Collectors.toList());
            redisComponent.addFile2DelQueue(videoId, delFilePathList);
        }

        // 4. 批量插入/更新所有分P
        Integer index = 1;
        for (VideoInfoFilePost videoInfoFile : uploadFileList) {
            videoInfoFile.setFileIndex(index++);
            videoInfoFile.setVideoId(videoId);
            videoInfoFile.setUserId(videoInfoPost.getUserId());
            if (videoInfoFile.getFileId() == null) {
                // 新增分P（未分配fileId时）
                videoInfoFile.setFileId(StringTools.getRandomString(Constants.LENGTH_10 * 2));
                videoInfoFile.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
                videoInfoFile.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
            }
        }
        this.videoInfoFilePostMapper.insertOrUpdateBatch(uploadFileList);

        // 5. 新增分P统一入“转码队列”
        if (!addFileList.isEmpty()) {
            for (VideoInfoFilePost file : addFileList) {
                file.setUserId(videoInfoPost.getUserId());
                file.setVideoId(videoId);
            }
            redisComponent.addFile2TransferQueue(addFileList);
        }
    }

    private boolean changeVideoInfo(VideoInfoPost videoInfoPost) {
        // 1. 根据传入对象的 videoId 去数据库查出原来的记录（基准数据）
        VideoInfoPost dbInfo = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());

        // 2. 逐字段比对“会影响审核”的元数据：封面、标题、标签、简介
        //    只要任意一个字段与数据库不一致，就认为用户做了实质性修改
        // 简介变化
        return !videoInfoPost.getVideoCover().equals(dbInfo.getVideoCover())     // 封面 URL 变化
                || !videoInfoPost.getVideoName().equals(dbInfo.getVideoName()) // 视频标题变化
                || !videoInfoPost.getTags().equals(dbInfo.getTags())           // 标签内容变化
                || !videoInfoPost.getIntroduction().equals(dbInfo.getIntroduction());  // 3. 检测到变动，返回 true，外层会把状态置为“待审核”

        // 4. 所有字段完全一致，认为没有改动，返回 false，状态保持不变
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferVideoFile(VideoInfoFilePost videoInfoFilePost) throws IOException {
        //入参 投稿时生成在videoinfoilepost表中的分片文件
        VideoInfoFilePost updateFilePost = new VideoInfoFilePost();//生成一个对象 便于进行数据库操作 用于 finally 回写数据库（状态/时长/大小/路径）
        File tempFile;
        File targetFile;
        try {
            UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(videoInfoFilePost.getUserId(), videoInfoFilePost.getUploadId());
            String tempFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath();//得到分片所在的文件夹C:\webser\easylive\file\temp\不唯一路径块
            tempFile = new File(tempFilePath);
            String targetFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_VIDEO + fileDto.getFilePath();//根据上传id确定目标路径
            //此乃正式目录

            targetFile = new File(targetFilePath);
            if (!targetFile.exists()) {
                targetFile.mkdirs();//不存在就生成
            }
            FileUtils.copyDirectory(tempFile, targetFile);//将temp中的分片文件copy到目标路径
            //将temp 目录中的视频 copy 到目标目录
            FileUtils.forceDelete(tempFile);
            //强制删除整个树
            redisComponent.delVideoFileInfo(videoInfoFilePost.getUserId(), videoInfoFilePost.getUploadId());//删除redis中的upload视频缓存
            //清理缓存

            String completeVideo = targetFilePath + Constants.TEMP_VIDEO_NAME;//生成完整的视频目标路径
            union(targetFilePath, completeVideo, true);//# 15. 按序号合并 *.part → 单文件；deleteParts=true → 合并后删除分片；IO/校验失败 → 回滚
            Integer duration = fFmpegUtils.getVideoInfoDuration(completeVideo);//获取时长大小
            updateFilePost.setDuration(duration);
            updateFilePost.setFileSize(new File(completeVideo).length());
            updateFilePost.setFilePath(Constants.FILE_VIDEO + fileDto.getFilePath());
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.SUCCESS.getStatus());
            this.convertVideo2Ts(completeVideo);//生成m3u8+ts


        } catch (Exception e) {
            log.error("文件转码失败", e);
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());

        } finally {
            videoInfoFilePostMapper.updateByUploadIdAndUserId(updateFilePost, videoInfoFilePost.getUploadId(), videoInfoFilePost.getUserId());//更新mysql数据库的存储 变化主要是时长 大小 路径以及状态
            VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
            fileQuery.setVideoId(videoInfoFilePost.getVideoId());
            fileQuery.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
            Integer failCount = videoInfoFilePostMapper.selectCount(fileQuery);
            //查询当前videoid 下面转码失败的数量
            if (failCount > 0) {
                VideoInfoPost videoUpdate = new VideoInfoPost();
                videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
                videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFilePost.getVideoId());
                //将失败的video根据videoid更新状态
                return;
            }
            fileQuery.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
            Integer transferCount = videoInfoFilePostMapper.selectCount(fileQuery);
            //查询转码中视频个数
            // 5. 如果还有分片正在转码，则什么都不做，直接返回，等待后续分片回调
            if (transferCount > 0) {
                return;
            }
        }
        Integer duration = videoInfoFilePostMapper.sumDuration(videoInfoFilePost.getVideoId());
        //将所有分p的时长汇总
        VideoInfoPost videoUpdate = new VideoInfoPost();
        videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
        videoUpdate.setDuration(duration);
        videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFilePost.getVideoId());
    }

    private void convertVideo2Ts(String videoFilePath) {
        File videoFile = new File(videoFilePath);
        //创建同名切片目录
        File tsFolder = videoFile.getParentFile();
        String codec = fFmpegUtils.getVideoCodec(videoFilePath);
        //转码
        if (Constants.VIDEO_CODE_HEVC.equals(codec)) {
            String tempFileName = videoFilePath + Constants.VIDEO_CODE_TEMP_FILE_SUFFIX;
            new File(videoFilePath).renameTo(new File(tempFileName));
            fFmpegUtils.convertHevc2Mp4(tempFileName, videoFilePath);
            new File(tempFileName).delete();
        }

        //视频转为ts
        fFmpegUtils.convertVideo2Ts(tsFolder, videoFilePath);

        //删除视频文件
        videoFile.delete();
}}


