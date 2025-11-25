package com.easylive.web.task;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.service.StatisticsInfoService;
import com.easylive.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Repeatable;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SysTask {
    @Resource
    private StatisticsInfoService statisticsInfoService;
    @Resource
    private AppConfig appConfig;
    @Scheduled(cron = "0 0 0 * * ?")
    public void statisticsInfoTask() {
        statisticsInfoService.statisticsData();
    }
    
    /**
     * 定时删除临时文件夹中过期的文件夹。
     * 
     * 该方法每分钟执行一次（由 @Scheduled(cron = "0 *
//     * 用于清理存放临时文件的主目录下，所有2天前及更早的以日期命名的子文件夹。
//     *
//     * 具体逻辑如下：
//     * 1. 计算临时文件夹的路径，该路径由项目根目录、文件存储主文件夹、临时文件夹这三部分拼接而成。
//     * 2. 读取临时文件夹下的所有子文件（通常子文件夹以日期字符串命名）。
//     * 3. 如果临时文件夹不存在或为空，则直接返回。
//     * 4. 获取2天前的日期，并格式化为 “yyyyMMdd” 的字符串，再转为整数，例如“20240605”。
//     * 5. 遍历目录下所有文件（文件夹），尝试将文件夹名转为整数（假定为日期）。
//     *    如果该日期小于等于前面计算得到的2天前的日期，说明该文件夹已过期，需要删除。
//     * 6. 调用FileUtils.deleteDirectory删除对应的文件夹，若删除失败则记录日志。
//     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void delTempFile() {
        // 拼接一个临时文件夹的绝对路径
        String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP;
        File folder = new File(tempFolderName);
        // 获取临时目录下所有文件或文件夹
        File[] listFile = folder.listFiles();
        // 如果文件夹不存在或没有内容，提前返回
        if (listFile == null) {
            return;
        }
        // 获取2天前的日期（格式化为yyyyMMdd，例如："20240605"），并转为小写字母（虽然此处实际无大小写差异）
        String twodaysAgo = DateUtil.format(DateUtil.getDayAgo(2), DateTimePatternEnum.YYYYMMDD.getPattern()).toLowerCase();
        // 将2天前的日期字符串转为整数，后续用于比较
        Integer dayInt = Integer.parseInt(twodaysAgo);
        // 遍历临时目录中的每一个文件夹
        for (File file : listFile) {
            // 假定每一个文件夹名都是日期（如"20240605"），尝试将其名转为整数
            Integer fileDate = Integer.parseInt(file.getName());
            // 如果该文件夹的日期小于等于2天前（说明已过期）
            if (fileDate <= dayInt) {
                try {
                    // 删除该文件夹及其内容
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    // 如果删除失败，记录日志
                    log.info("删除临时文件失败", e);
                }
            }
        }
    }
}
