package com.easylive.web;

import com.easylive.component.EsSearchComponent;
import com.easylive.redis.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component("initRun")// // 告诉Spring：这是启动安检员，项目启动后马上叫他来干活
public class InitRun implements ApplicationRunner {// ApplicationRunner：Spring Boot启动完成后自动执行

    private static final Logger logger = LoggerFactory.getLogger(InitRun.class);

    @Resource
    private DataSource dataSource;// 数据库连接池（检查数据库能不能连上）

    @Resource
    private RedisUtils redisUtils;// Redis工具（检查Redis能不能存东西）

    @Resource
    private EsSearchComponent esSearchComponent;// ES搜索组件（检查ES索引建好了没）
    /**
     * run()：安检员的工作清单
     * 按顺序检查：数据库 → Redis → Elasticsearch
     * 任何一步失败，整个系统就启动失败
     */
    @Override
    public void run(ApplicationArguments args) {

        Connection connection = null;// 数据库连接对象
        Boolean startSuccess = true;// 启动成功标志，默认成功
        try {
            // =========== 检查1：数据库 ===========
            // 尝试从连接池拿一个连接，如果拿不到说明数据库挂了
            connection = dataSource.getConnection();
            // 如果上面这行没报错，说明数据库正常
            // =========== 检查2：Redis ===========
            // 尝试从Redis读一个"test"数据，如果Redis挂了会抛异常
            redisUtils.get("test");
            // 如果上面这行没报错，说明Redis正常
            // =========== 检查3：Elasticsearch ===========
            // 检查ES的视频索引有没有建好，没建就新建
            esSearchComponent.createIndex();
            // 如果上面这行没报错，说明ES正常
            logger.error("服务启动成功，可以开始愉快的开发了");
        } catch (SQLException e) {
            logger.error("数据库配置错误，请检查数据库配置");
            startSuccess = false;
        } catch (Exception e) {
            logger.error("服务启动失败", e);
            startSuccess = false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (!startSuccess) {
                System.exit(0);
            }
        }
    }
}
