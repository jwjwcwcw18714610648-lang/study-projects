package com.easylive.entity.constants;

/**
 * Constants
 * 系统中用到的常量统一定义在这里，方便维护和复用。
 */
public class Constants {
    public static final Integer REDIS_KEY_EXPIRES_ONE_SECONDS = 1000;//1000毫秒
    public static  final  String IMAGE_THUMBNALL_SUFFIX="_thumbnail.jpg";
    /** Redis key 的统一前缀，避免和其他项目冲突 */
    public static final String REDIS_KEY_PREFIX = "esylive:";
    public static final Integer LENGTH_10=10;
    public static final Integer LENGTH_30=30;
    /** 存储验证码的 Redis key 前缀 */
    public static final String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "checkcode:";
    public static final String FILE_FOLDER="file/";
    public static final String FILE_FOLDER_TEMP="temp/";
    public static final String FILE_COVER="cover/";
    public static final String FILE_VIDEO="video/";
    public static final Integer HOUR_24 = 24;
    /** Redis 过期时间：一分钟（单位：毫秒，60000 = 60 * 1000） */
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60000;
    public static final String REDIS_KEY_UPLOADING_FILE = REDIS_KEY_PREFIX + "uploading:";
    /** Redis 过期时间：一天（分钟级计算：1 分钟 * 60 * 24） */
    public static final Integer REDIS_KEY_EXPIRES_ONE_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;
    public static final Integer ZERO=0;
    /** Web 端用户登录 token 存储的 Redis key */
    public static final String REDIS_KEY_TOKEN_WEB = REDIS_KEY_PREFIX + "token:web";
    public static final String REDIS_KEY_TOKEN_ADMIN = REDIS_KEY_PREFIX + "token_admin";
    public static final String TOKEN_WEB = "token_--web";
    public static final String TOKEN_ADMIN = "token_--admin";
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";
    public static  final  String REDIS_KEY_CATEGORY_LIST=REDIS_KEY_PREFIX +"category_list";
    //系统设置
    public static final String REDIS_KEY_SYS_SETTING = REDIS_KEY_PREFIX + "sysSetting:";

    //消息队列
    //转码的消息队列
    public static final String REDIS_KEY_QUEUE_TRANSFER = REDIS_KEY_PREFIX + "queue:transfer:";

    public static final String REDIS_KEY_QUEUE_VIDEO_PLAY = REDIS_KEY_PREFIX + "queue:video:play:";

    //删除文件的结合
    public static final String REDIS_KEY_FILE_DEL = REDIS_KEY_PREFIX + "file:list:del:";

    //视频在线
    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE_PREIFX = REDIS_KEY_PREFIX + "video:play:online:";

    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE = REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE_PREIFX + "count:%s";

    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_USER_PREFIX = "user:";

    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_USER = REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE_PREIFX + REDIS_KEY_VIDEO_PLAY_COUNT_USER_PREFIX + "%s:%s";


    public static final String REDIS_KEY_VIDEO_PLAY_COUNT = REDIS_KEY_PREFIX + "video:playcount:";


    public static final String REDIS_KEY_VIDEO_SEARCH_COUNT = REDIS_KEY_PREFIX + "video:search:";

    public static final String TEMP_COVER_NAME = "/cover.jpg";

    public static final String TS_NAME = "index.ts";

    public static final String M3U8_NAME = "index.m3u8";

    public static final String TEMP_VIDEO_NAME = "/temp.mp4";

    public static final String IMAGE_THUMBNAIL_SUFFIX = "_thumbnail.jpg";

    public static final Integer UPDATE_NICK_NAME_COIN = 5;

    public static final String  VIDEO_CODE_HEVC= "hevc";

    public static final String  VIDEO_CODE_TEMP_FILE_SUFFIX= "_temp";
    public static final Long MB_SIZE = 1024 * 1024L;
}
