# TeamWork 專案已知問題與避坑指南 (Known Issues & Workarounds)

本文件紀錄開發過程中遇到的環境、框架升級或不穩定相依性問題，並提供相應的解決方案或 Workaround，以避免後續開發任務踩到相同的坑。

## 1. Spring Boot 4.0.x (Milestone) 與 WebMvcTest / Jackson 3 依賴黑洞

**發生時間**: Phase 1.3 (REST API 開發階段)
**相關技術**: Spring Boot 4.0.0-M系列, Spring Web MVC, Jackson 3, Maven

### 描述 (Issue Description)
Spring Boot 4 大幅重構了測試套件結構 (`spring-boot-test-autoconfigure`)，並且將原生的 JSON 處理核心預設切換為 **Jackson 3** (`tools.jackson`)。
這導致了在實作 Controller 單元測試時，會遇到以下依賴找不到的連鎖崩潰問題：
1. `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` 找不到（官方將路徑改為 `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`）
2. 即使路徑修正且加入了 Context7 官方最新推薦的 `MockMvcTester` 寫法，Maven 的 `spring-boot-starter-test` 在 M 版依舊無法正確拉取所有 `autoconfigure` 細部 jar 包，導致 `AutoConfigureMockMvc` 等標籤拋出 `Cannot find symbol`。
3. `com.fasterxml.jackson.databind.ObjectMapper` 無法使用，需改用 `tools.jackson.databind.ObjectMapper`，但在缺少底層 AutoConfigure 的情況下，MockMvc 難以正確序列化/反序列化 Request。

### 解決方案 / Workaround
**絕對不要在 Controller 單元測試中使用 `@WebMvcTest` 或依賴 Spring 容器啟動！**

為了避開框架升級過渡期的不穩定，所有的 API 介面單元測試應統一採用 **純 Mockito (POJO) 測試**。
這不僅完全免疫 Spring Boot 依賴黑箱問題，測試執行速度還能提升百倍。

#### Workaround 範例寫法：

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

// 1. 使用 MockitoExtension 而非 SpringBootTest / WebMvcTest
@ExtendWith(MockitoExtension.class)
class MyControllerTest {

    // 2. Mock 掉底層服務
    @Mock
    private MyService myService;

    // 3. 直接注入 Controller 本體，不透過 Spring Context
    @InjectMocks
    private MyController myController;

    @Test
    void testApi() {
        // Arrange...
        given(myService.doSomething()).willReturn(mockResult);

        // Act (直接呼叫 Controller 方法)
        ResponseEntity<MyResponse> response = myController.callApi(new MyRequest());

        // Assert (針對 ResponseEntity 做斷言)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEqualTo("expected");
    }
}
```

> **後續任務原則**：
> 凡涉及 Spring Context 的 `Integration Test` (如 DB 操作) 使用 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` 配合 `TestRestTemplate`。
> 凡涉及單一類別邏輯 (如 Controller, Service) 的 `Unit Test`，一律使用 `@ExtendWith(MockitoExtension.class)`。
