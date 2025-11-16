package com.easylive.component;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.dto.VideoInfoEsDto;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.SearchOrderTypeEnum;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.SimplePage;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.UserInfoMapper;
import com.easylive.utils.CopyTools;
import com.easylive.utils.JsonUtils;
import com.easylive.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("esSearchUtils")
@Slf4j
public class EsSearchComponent {
//这个类就是一个专门管Elasticsearch的工具箱，把复杂的搜索、添加、删除操作变成简单方法调用。
    @Resource
    private AppConfig appConfig;//// 配置文件，存ES的索引名、地址等

    @Resource
    private RestHighLevelClient restHighLevelClient;// // ES的"遥控器"，用来给ES发命令

    @Resource
    private UserInfoMapper userInfoMapper;//  // 数据库查询工具，查用户信息用
    /**
     * 检查"视频仓库"有没有建好
     * @return true=建好了 false=没建
     */
    private Boolean isExistIndex() throws IOException {
        // 创建一个"查仓库"请求：名字叫啥？从配置文件里读
        GetIndexRequest getIndexRequest = new GetIndexRequest(appConfig.getEsIndexVideoName());
        // 用遥控器发请求给ES：大哥，你这有叫这个名字的仓库吗？
        // 返回true表示有，false表示没有
        return restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }
    /**
     * 创建视频仓库 + 定规则
     * 规则：哪些字段能搜？哪些字段不能搜？怎么分词？
     */
    public void createIndex() {
        try {  // 先检查仓库建没建，建了就直接返回，不重复造轮子
            Boolean existIndex = isExistIndex();
            if (existIndex) {
                return;
            }
            // 创建一个"建仓库"请求：名字叫啥？从配置文件里读
            CreateIndexRequest request = new CreateIndexRequest(appConfig.getEsIndexVideoName());
            // =========== 设置分词规则 ===========
            // 这串JSON就是告诉ES：我要自定义一个分词器，名字叫"comma"
            // 作用是：遇到逗号就切开（因为tags字段存的是"游戏,动漫,搞笑"这种格式）
//            {
//  "analysis": {              // 第一层：分析模块配置
//    "analyzer": {            // 第二层：定义分词器（可有多个）
//      "comma": {             // 第三层：分词器名字叫"comma"
//        "type": "pattern",   // 类型：基于正则表达式的分词器
//        "pattern": ","       // 正则表达式：逗号（,）
//      }
//    }
//  }
//}告诉ES：遇到逗号（,）就切开，把 "游戏,动漫,搞笑" 变成 ["游戏", "动漫", "搞笑"]
            request.settings(
                    "{\"analysis\": {\n" +
                            "      \"analyzer\": {\n" +
                            "        \"comma\": {\n" +
                            "          \"type\": \"pattern\",\n" +
                            "          \"pattern\": \",\"\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }}", XContentType.JSON);
            // =========== 设置字段规则（Mapping） ===========
            // 这串JSON就是给仓库里的每个字段贴标签：
            // videoId: 类型是text，但index=false（只存不搜，节省空间）
            // videoName: 类型是text，用ik_max_word分词器（中文智能分词）
            // tags: 类型是text，用自定义的comma分词器（按逗号切）
            // playCount: 类型是integer，index=false（只存不搜）
            // createTime: 类型是date，指定格式，index=false
            request.mapping(
                    "{\"properties\": {\n" +
                            "      \"videoId\":{\n" +
                            "        \"type\": \"text\",\n" +
                            "        \"index\": false\n" +
                            "      },\n" +
                            "      \"userId\":{\n" +
                            "        \"type\": \"text\",\n" +
                            "        \"index\": false\n" +
                            "      },\n" +
                            "      \"videoCover\":{\n" +
                            "        \"type\": \"text\",\n" +
                            "        \"index\": false\n" +
                            "      },\n" +
                            "      \"videoName\":{\n" +
                            "        \"type\": \"text\",\n" +
                            "        \"analyzer\": \"ik_max_word\"\n" +
                            "      },\n" +
                            "      \"tags\":{\n" +
                            "        \"type\": \"text\",\n" +
                            "        \"analyzer\": \"comma\"\n" +
                            "      },\n" +
                            "      \"playCount\":{\n" +
                            "        \"type\":\"integer\",\n" +
                            "        \"index\":false\n" +
                            "      },\n" +
                            "      \"danmuCount\":{\n" +
                            "        \"type\":\"integer\",\n" +
                            "        \"index\":false\n" +
                            "      },\n" +
                            "      \"collectCount\":{\n" +
                            "        \"type\":\"integer\",\n" +
                            "        \"index\":false\n" +
                            "      },\n" +
                            "      \"createTime\":{\n" +
                            "        \"type\":\"date\",\n" +
                            "        \"format\": \"yyyy-MM-dd HH:mm:ss\",\n" +
                            "        \"index\": false\n" +
                            "      }\n" +
                            " }}", XContentType.JSON);
            // 用遥控器发请求给ES：大哥，按这个规则给我建个仓库！
            CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            boolean acknowledged = createIndexResponse.isAcknowledged();// 检查ES有没有成功创建
            if (!acknowledged) {
                throw new BusinessException("初始化es失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("初始化es失败", e);
            throw new BusinessException("初始化es失败");
        }
    }
    private Boolean docExist(String id) throws IOException {
        GetRequest getRequest = new GetRequest(appConfig.getEsIndexVideoName(), id);//查询es该索引中是否有这个id
        // 执行查询
        GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        return response.isExists();

    }
    public void saveDoc(VideoInfo videoInfo) {
        try {
            if (docExist(videoInfo.getVideoId())) {//如果索引的唯一id 存在了则说明此次操作更新即可
                updateDoc(videoInfo);
            } else {
                VideoInfoEsDto videoInfoEsDto = CopyTools.copy(videoInfo, VideoInfoEsDto.class);
                videoInfoEsDto.setCollectCount(0);
                videoInfoEsDto.setPlayCount(0);
                videoInfoEsDto.setDanmuCount(0);
                IndexRequest request = new IndexRequest(appConfig.getEsIndexVideoName());
                request.id(videoInfo.getVideoId()).source(JsonUtils.convertObj2Json(videoInfoEsDto), XContentType.JSON);
                restHighLevelClient.index(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            log.error("新增视频到es失败", e);
            throw new BusinessException("保存失败");
        }
    }
    private void updateDoc(VideoInfo videoInfo) {
        try {
            //时间不更新
            videoInfo.setLastUpdateTime(null);
            videoInfo.setCreateTime(null);
            //此思路是通过反射机制获取videoinfo对象中的非空字段值 构建更新请求
            Map<String, Object> dataMap = new HashMap<>();// 准备存储需要更新的字段数据，key为字段名，value为字段值
            Field[] fields = videoInfo.getClass().getDeclaredFields();//通过反射获取videoinfo对象所有的声明字段
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());//构造对应的get方法 如getVideoId 该String方法是把传入的string首字母大写
                Method method = videoInfo.getClass().getMethod(methodName);//获取该方法的对象 它代表了你想要调用的那个方法（例如 getVideoId()）的元信息。
                Object object = method.invoke(videoInfo);/*method  ：前面通过反射获取到的 Method 对象，代表某个 getter 方法（如 getVideoId()）
invoke()  ：这是 Method 类的核心方法，作用是触发执行对应的方法
videoInfo  ：作为参数传入，表示在哪个实例对象上执行这个方法（相当于 videoInfo.getVideoId() 中的 videoInfo.）*/
                //object返回的是对应拼装的get方法的返回值
                //使用Object核心原因：通用性
                //   // 过滤条件：只保留非空字段
                //                // 如果是字符串类型，额外判断不为空字符串；非字符串类型则直接保留
                if (object != null && object instanceof java.lang.String && !StringTools.isEmpty(object.toString()) || object != null && !(object instanceof java.lang.String)) {
                    dataMap.put(field.getName(), object);
                }
            }
            if (dataMap.isEmpty()) {
                return;
            }
            UpdateRequest updateRequest = new UpdateRequest(appConfig.getEsIndexVideoName(), videoInfo.getVideoId());//创建es更新请求 指定索引名称 和文档id
            updateRequest.doc(dataMap);//设置更新字段内容
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);//发送更新请求
        } catch (Exception e) {
            log.error("新增视频到es失败", e);
            throw new BusinessException("保存失败");
        }
    }
    public void updateDocCount(String videoId, String fieldName, Integer count) {
        try {
            UpdateRequest updateRequest = new UpdateRequest(appConfig.getEsIndexVideoName(), videoId);
            Script script = new Script(ScriptType.INLINE, "painless", "ctx._source." + fieldName + " += params.count", Collections.singletonMap("count", count));
            //ctx._source  ：指向 ES 中存储的 JSON 文档（磁盘/索引中的数据）
            //params.count 是参数值（value），从 Java 变量 count 传入
            // Collections.singletonMap("count", count));脚本参数：将count值传给params.count
            updateRequest.script(script);//创建请求
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);//发送请求
        } catch (Exception e) {
            log.error("更新数量到es失败", e);
            throw new BusinessException("保存失败");
        }
    }
    public void delDoc(String videoId) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest(appConfig.getEsIndexVideoName(), videoId);//删除索引中的表id
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("从es删除视频失败", e);
            throw new BusinessException("删除视频失败");
        }

    }
    //es搜索组件
    /**
     * 视频搜索核心方法
     * 功能：根据关键词在ES中搜索视频，支持高亮、排序、分页，并关联用户昵称信息
     *
     * @param highlight  是否开启搜索关键词高亮
     * @param keyword    搜索关键词（在videoName和tags字段中匹配） 多条件查询 查videname和其标签
     * @param orderType  排序类型枚举值（如：播放数、发布时间等）
     * @param pageNo     页码（从1开始）
     * @param pageSize   每页条数
     * @return           分页封装后的视频列表，包含视频信息及发布者昵称
     */
    public PaginationResultVO<VideoInfo> search(Boolean highlight, String keyword, Integer orderType, Integer pageNo, Integer pageSize) {
        try {

            SearchOrderTypeEnum searchOrderTypeEnum = SearchOrderTypeEnum.getByType(orderType);//确定排序规则

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();//构建es查询构造器
            //关键字
            // 1. 查询条件：构建多字段分词查询，在videoName和tags两个字段中匹配keyword
            // multiMatchQuery会自动进行分词，支持模糊匹配和 relevance score 计算
            searchSourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "videoName", "tags"));

            if (highlight) {
                //高亮
                HighlightBuilder highlightBuilder = new HighlightBuilder();//构建高亮构造器
                highlightBuilder.field("videoName"); // 替换为你想要高亮的字段名
                highlightBuilder.preTags("<span class='highlight'>"); // 设置高亮前缀标签，搜索结果中的匹配词会被此HTML标签包裹
                highlightBuilder.postTags("</span>"); // 设置高亮后缀标签，用于闭合
                searchSourceBuilder.highlighter(highlightBuilder);//传入查询构造器
            }


            // 3. 排序逻辑：根据orderType选择排序字段，默认按相关性_score倒序
            if (orderType != null) {
                searchSourceBuilder.sort(searchOrderTypeEnum.getField(), SortOrder.DESC); // 第一个排序字段，升序
            }else{
                searchSourceBuilder.sort("_score", SortOrder.DESC); // 第一个排序字段，倒序
            }
            pageNo = pageNo == null ? 1 : pageNo; // 页码为空时默认第1页
            //分页查询
            pageSize = pageSize == null ? PageSize.SIZE20.getSize() : pageSize;// 每页条数为空时默认20条（PageSize.SIZE20.getSize()）
            searchSourceBuilder.size(pageSize);
            searchSourceBuilder.from((pageNo - 1) * pageSize); // 设置查询起始位置（ES采用from语法，需计算偏移量：(pageNo-1)*pageSize）

            SearchRequest searchRequest = new SearchRequest(appConfig.getEsIndexVideoName());// 5. 构建ES搜索请求：指定索引名称（从配置读取）
            searchRequest.source(searchSourceBuilder); // 将前面构建的查询DSL（JSON格式的查询语句，用于定义搜索、过滤、聚合等操作。）设置到请求中

            // 执行查询
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 处理查询结果
            SearchHits hits = searchResponse.getHits(); // 获取命中结果集对象，包含总条数和文档数组
            Integer totalCount = (int) hits.getTotalHits().value;// 从hits中提取总匹配文档数（用于前端分页计算）

            // 初始化视频列表和发布者ID列表（用于后续关联查询用户昵称）
            List<VideoInfo> videoInfoList = new ArrayList<>();

            List<String> userIdList = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                VideoInfo videoInfo = JsonUtils.convertJson2Obj(hit.getSourceAsString(), VideoInfo.class);   // 将JSON格式的文档源数据反序列化为VideoInfo对象
                // 如果开启了高亮且videoName字段有高亮结果，用高亮文本替换原标题
                // 注意：高亮文本是HTML片段，需前端配合渲染
                if (hit.getHighlightFields().get("videoName") != null) {
                    videoInfo.setVideoName(hit.getHighlightFields().get("videoName").fragments()[0].string());//那高亮片段与其他不高亮片段是如何拼接的？
                    /*不需要手动拼接，ES 已经帮你拼好了，直接返回的就是完整文本（或带省略号的片段）。
                核心原理：ES 自动合并
                当你在 videoName 字段上开启高亮时，ES 的工作流程是：
                定位关键词：在原始标题中找到所有匹配 Java 的位置
                包裹标签：在每个 Java 周围加上 <span class='highlight'>...</span>
                返回完整文本：将整个标题（包含标签）作为一个完整的字符串返回*/
                }
                videoInfoList.add(videoInfo);//添加到视频list

                userIdList.add(videoInfo.getUserId());//发布者id
            }
            UserInfoQuery userInfoQuery = new UserInfoQuery();
            userInfoQuery.setUserIdList(userIdList);
            // 9. 关联查询用户信息：通过MyBatis批量查询userIdList对应的用户详情
            List<UserInfo> userInfoList = userInfoMapper.selectList(userInfoQuery);
            // 将用户列表转换为Map，key为userId，value为UserInfo对象（去重，重复ID取后者）
            Map<String, UserInfo> userInfoMap = userInfoList.stream().collect(Collectors.toMap(item -> item.getUserId(), Function.identity(), (data1, data2) -> data2));
            videoInfoList.forEach(item -> {// 遍历 videoInfoList 中的每一个 VideoInfo 对象，item 代表当前循环的视频
                UserInfo userInfo = userInfoMap.get(item.getUserId());
                if (userInfo != null) { // 将查询到的昵称设置到视频对象中（前端可直接展示）
                    item.setNickName(userInfo.getNickName());
                }
            });
            // 创建分页计算器，根据总条数、页码、页大小计算总页数等
            SimplePage page = new SimplePage(pageNo, totalCount, pageSize);
            // 创建最终返回的VO对象，封装所有分页信息和视频数据
            PaginationResultVO<VideoInfo> result = new PaginationResultVO(totalCount, page.getPageSize(), page.getPageNo(), page.getPageTotal(), videoInfoList);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询视频到es失败", e);
            throw new BusinessException("查询失败");
        }
    }
}