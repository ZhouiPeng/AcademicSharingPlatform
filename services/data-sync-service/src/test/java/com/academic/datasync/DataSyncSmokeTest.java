package com.academic.datasync;

import org.junit.jupiter.api.Test;
import com.academic.datasync.client.AchievementServiceClient;
import com.academic.datasync.client.FileServiceClient;
import com.academic.datasync.service.impl.DataSyncServiceImpl;
import org.springframework.web.reactive.function.client.WebClient;

public class DataSyncSmokeTest {

    @Test
    public void pullFromOpenAlexSmoke() {
        // 使用默认的 WebClient.Builder（不会提供 Spring 容器）
        WebClient.Builder builder = WebClient.builder();

        // 创建一个简单的 AchievementServiceClient stub（不会实际调用远端）
        AchievementServiceClient achStub = new AchievementServiceClient(builder, "http://localhost:9999") {
            @Override
            public String createAchievement(String jsonPayload) {
                System.out.println("[ACH-STUB] createAchievement called with payload: " + jsonPayload);
                return "{\"code\":1}";
            }
        };

        // 创建一个简单的 FileServiceClient stub（打印并返回模拟的 fileId）
        // 使用真实的 FileServiceClient 指向本地运行的 file-service（http://localhost:8083）
        FileServiceClient fileStub = new FileServiceClient(builder, "http://localhost:8083");

        // 构造 DataSyncServiceImpl 并执行拉取方法（会真实请求 OpenAlex）
        DataSyncServiceImpl svc = new DataSyncServiceImpl(builder, achStub, fileStub);
        svc.pullFromPublicDb();

        System.out.println("DataSync smoke run finished.");
    }
}
