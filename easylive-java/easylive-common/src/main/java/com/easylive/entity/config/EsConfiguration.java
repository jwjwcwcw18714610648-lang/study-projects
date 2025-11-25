package com.easylive.entity.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

import javax.annotation.Resource;
// 告诉Spring：这是一个配置类，里面会定义一些工具 Bean
@Configuration
// 继承这个类后，Spring Data ES会自动帮我们配好很多东西，省得自己写
public class EsConfiguration extends AbstractElasticsearchConfiguration implements DisposableBean {

    // 从AppConfig配置文件中拿到ES的地址（如：localhost:9200）
    @Resource
    private AppConfig appConfig;

    // 用来保存创建好的ES客户端，方便后面销毁它
    private RestHighLevelClient client;

    /**
     *  @Bean：这个方法返回一个工具，Spring会把它放进工具箱，谁想用就@Resource来拿
     *  方法名就是工具的名字：elasticsearchClient
     */
    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {

        // 1. 从appConfig里取出ES地址，组装成配置对象
        // 相当于写上：要连哪个服务器、超时多久、账号密码是多少
        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(appConfig.getEsHostPort())  // "localhost:9200" 或 "服务器IP:9200"
                .build();

        // 2. 根据上面的配置，真正创建一个ES客户端（类似创建数据库连接）
        // 这个client内部有连接池，可以复用连接，速度快
        client = RestClients.create(clientConfiguration).rest();

        // 3. 把这个建好的客户端交给Spring管理，其他地方可以@Resource注入使用
        return client;
    }

    /**
     * 当项目关闭时（比如重启、下线），Spring会自动调用这个方法
     * 作用是：把ES客户端关掉，释放连接，避免资源泄漏
     *
     * 如果不关会怎样？就像开了水龙头不拧紧，会一直漏水（占用服务器资源）
     */
    @Override
    public void destroy() throws Exception {
        if (client != null) {
            client.close();  // 关闭连接池，释放所有资源
        }
    }
}
