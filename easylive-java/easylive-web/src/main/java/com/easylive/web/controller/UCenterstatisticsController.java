package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.query.StatisticsInfoQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.StatisticsInfoService;
import com.easylive.utils.DateUtil;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterstatisticsController extends ABaseController {

    @Resource
    private StatisticsInfoService statisticsInfoService;

    @RequestMapping("/getActualTimeStatisticsInfo")
    @GlobalInterceptor
    /**
     * 获取用户的实时统计信息
     * 1. 查询昨日的统计数据（关注数、评论数、点赞数等，按数据类型区分）
     * 2. 查询当前用户的实时总数信息（实际粉丝、累计点赞等）
     * 3. 聚合上述数据后返回给前端
     * @return ResponseVO 统一响应对象，包含昨日数据和总数据
     */
    public ResponseVO getActualTimeStatisticsInfo() {
        // 获取昨天的日期（格式：yyyy-MM-dd）
        String preDate = DateUtil.getBeforeDayDate(1);

        // 获取当前登陆用户的token信息（含用户ID）
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        // 构造统计数据查询条件：统计日期=昨日、用户ID=当前用户
        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setStatisticsDate(preDate);
        param.setUserId(tokenUserInfoDto.getUserId());

        // 查询该用户昨日的各项统计数据（按类型归类）
        List<StatisticsInfo> preDayData = statisticsInfoService.findListByParam(param);

        // 将统计数据转换为Map，key为数据类型（如1=关注、2=点赞等），value为数量
        Map<Integer, Integer> preDayDataMap = preDayData.stream()
                .collect(Collectors.toMap(
                        StatisticsInfo::getDataType,      // 数据类型
                        StatisticsInfo::getStatisticsCount, // 数据数量
                        (item1, item2) -> item2            // 有重复key时用后者覆盖
                ));

        // 获取当前用户的总量统计信息（如总粉丝数、总获赞数等），key为统计项英文名,value为数量
        Map<String, Integer> totalCountInfo = statisticsInfoService.getStatisticsInfoActualTime(tokenUserInfoDto.getUserId());

        // 封装返回结果
        Map<String, Object> result = new HashMap<>();
        // 昨日数据Map<数据类型, 数量>
        result.put("preDayData", preDayDataMap);
        // 总量数据Map<英文标识, 数量>
        result.put("totalCountInfo", totalCountInfo);

        // 返回统一成功响应
        return getSuccessResponseVO(result);
    }

    @RequestMapping("/getWeekStatisticsInfo")
    @GlobalInterceptor
    /**
     * 获取当前用户最近一周（7天）的某一统计类型的数据，按天返回。
     *
     * 工作原理详解：
     * 1. 获取当前用户信息，目的是拿到 userId。
     * 2. 构造一个最近7天的日期列表（如 ["2024-04-20", "2024-04-21", ... , "2024-04-26"]），保证顺序从早到晚。
     * 3. 构造查询参数：只查这7天，且数据类型、用户都是当前条件。
     * 4. 查询结果得到一组 `StatisticsInfo`，里面每条代表某一天的数据（可能某天没有，数据库就查不到该记录）。
     * 5. 把查询结果转成 Map，key=日期，方便等会快速查找每一天对应统计。注意：如果多条同日期，以最后一条为准。
     * 6. 遍历7天日期列表，每一天都补一条数据（如果没有则补零），保证返回的天数是连续的、没有缺失日期。
     * 7. 最终返回统一成功响应，内容是最近7天的数据列表，包含每天的统计数量和日期。
     *
     * @param dataType 统计数据类型（如1表示粉丝、2表示获赞等，后端自定义类型编码）
     * @return 最近7天该数据类型每天的统计数量（如果某天没有数据则为0）
     */
    public ResponseVO getWeekStatisticsInfo(Integer dataType) {
        // 1. 获取当前登录用户的信息（主要用于获取 userId）
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        // 2. 获取最近7天的日期字符串列表，格式通常为 "yyyy-MM-dd"
        // 例如 ["2024-04-20", ..., "2024-04-26"]
        List<String> dateList = DateUtil.getBeforeDates(7);

        // 3. 构造查询参数对象
        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setDataType(dataType);                                // 设置数据类型
        param.setUserId(tokenUserInfoDto.getUserId());              // 设置用户ID
        param.setStatisticsDateStart(dateList.get(0));              // 设置起始日期（7天前）
        param.setStatisticsDateEnd(dateList.get(dateList.size() - 1)); // 设置结束日期（今天）
        param.setOrderBy("statistics_date asc");                    // 结果按日期升序排列

        // 4. 查询该用户、指定类型、最近7天的所有统计数据
        List<StatisticsInfo> statisticsInfoList = statisticsInfoService.findListByParam(param);

        // 5. 把查询出来的结果转成一个 Map，key=每天的日期，value=数据库查到的那条 StatisticsInfo
        //    方便后面快速通过日期找数据，如果同一天有多条记录，选最后一条覆盖
        Map<String, StatisticsInfo> dataMap = statisticsInfoList.stream()
                .collect(Collectors.toMap(
                        item -> item.getStatisticsDate(),   // key: 统计日期
                        Function.identity(),                // value: 当前对象本身
                        (data1, data2) -> data2             // 冲突时用后者覆盖
                ));

        // 6. 结果列表，保证7天每一天都有统计对象（没数据的日子就放0）
        List<StatisticsInfo> resultDataList = new ArrayList<>();
        for (String date : dateList) {
            // 尝试取出该日期的真实数据
            StatisticsInfo dataItem = dataMap.get(date);
            if (dataItem == null) {
                // 如果那天没有数据，手动补一条，数量为0，仅设日期
                dataItem = new StatisticsInfo();
                dataItem.setStatisticsCount(0);
                dataItem.setStatisticsDate(date);
            }
            resultDataList.add(dataItem);
        }

        // 7. 封装统一成功返回
        return getSuccessResponseVO(resultDataList);
    }
}
