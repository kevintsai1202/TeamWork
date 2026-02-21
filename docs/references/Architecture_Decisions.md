# 架構決策分析：Agent 通訊與上下文管理

在 Team Work 多 AI Agents 系統中，有兩個核心的架構決策需要拍板：**Agent 的回報機制**與**上下文 (Context) 的儲存方式**。

---

## 1. 上下文儲存：Redis 的優勢

將對話記憶與上下文 (Chat Memory) 存放在 Redis 是一個非常正確且極力推薦的作法。

*   **極致的讀寫效能**：Agent 在 Reason-Act 迴圈中，每次呼叫 LLM 皆需讀取整個對話紀錄。Redis 作為 In-Memory 資料庫，能將此 I/O 延遲降至最低。
*   **無狀態 Gateway (Stateless)**：若將 Memory 存在本機 (In-Memory)，Gateway 將受限於單機，無法輕易擴容。將 Memory 外部化至 Redis，任何一台 Gateway 節點都能接手處理 Agent 請求。
*   **狀態與記憶分離**：PostgreSQL (RDBMS) 負責高一致性的「任務生命週期」(Pending/Running/Success)，而 Redis 負責高頻繁、可設 TTL 拋棄的「對話內容」。
*   **高度透明與易於除錯**：開發者可隨時透過 Redis GUI 查看特定 TaskID 的鍵值，精準觀察 Agent 腦中正在思考什麼。

---

## 2. Agent 任務回報機制：Webhook vs Redis Pub/Sub

當一個耗時的背景 Agent (特別是由其他語言或微服務實作的 Agent) 完成任務時，該如何通知 Gateway？

### 選項 A：Webhook (HTTP Callback)
**運作方式**：Gateway 派發任務時，附帶一個回呼網址 (如 `https://gateway/api/webhook/{taskId}`)。Agent 算完後，發送一個 HTTP POST 回去。

*   **優點**：
    1.  **語言與框架無關**：任何會發送 HTTP Request 的程式都能當 Agent，極大化了系統的異質性包容度 (Python, Node.js, Go)。
    2.  **網路穿透性高**：走標準 80/443 Port，不受防火牆對特殊 Port 的限制。
    3.  **重試機制容易**：若 Gateway 當下重啟，Agent 端實作 HTTP Retry 即可保證送達。
*   **缺點**：如果 Gateway 藏在內網或 Agent 在沒有固定 IP 的環境，網路配置會較為複雜。

### 選項 B：Redis Pub/Sub (或 Redis Stream/List)
**運作方式**：所有的 Agent 與 Gateway 都連到同一個 Redis 伺服器。Agent 算完後，將結果發布 (Publish) 到特定的 Channel 或寫入 Stream，Gateway 負責監聽 (Subscribe)。

*   **優點**：
    1.  **完美解耦**：發布者與訂閱者完全不需要知道對方的 IP 或存活狀態。
    2.  **極低延遲**：基於 TCP 持久連線，訊息推播速度遠快於 HTTP。
    3.  **內網/架構內聚性高**：只要大家都能連上 Redis，不用煩惱跨網路 HTTP 路由的問題。
*   **缺點**：
    1.  **耦合於特定技術**：所有的微服務或跨語言 Agent 都必須載入 Redis 客戶端套件。
    2.  **Pub/Sub 的遺失風險**：經典的 Redis Pub/Sub 是「射後不理」(Fire and Forget)。如果 Gateway 剛好斷線個幾秒鐘，Agent 發出來的完成通知就會永遠遺失。 (解決方案：改用 Redis Streams 或 List 作為 Message Queue)。

### 💡 結論與建議 (單機/小型場景)

在最初的單機規劃下，若 Agent 全在 Java 虛擬執行緒內，走內部 Event 即可；若有外部 Agent，Webhook 實作最簡單。

---

## 3. 進階架構：多用戶與「中央儀表板」場景

根據最新的需求規劃：**系統為網頁版，支援少量多用戶 (每人有自己的 Team Work)，並且存在一個「中央監控儀表板」能綜觀全局。**

在這種**「多租戶 (Multi-Tenant) + 集中監控」**的架構下，**強烈建議使用 Redis Streams (或 Message Queue)** 作為 Agent 回報機制。

### 為什麼 Redis 壓倒性勝過 Webhook？

1. **中央事件匯流排 (Central Event Bus) 的完美契合**：
   * 在多用戶環境下，狀態更新不能只發給單一用戶。中央儀表板需要訂閱「所有人」的事件，而個別用戶只需訂閱「自己」的事件。
   * **作法**：所有的 Agent (不管為哪個用戶服務) 只要把狀態寫入同一個 Redis Stream (例如 `teamwork:events`)。Gateway 只要監聽這個 Stream，就能輕鬆將事件進行路由 (Routing) —— 把事件推播給中央儀表板的 WebSocket，同時根據 `userId` 推播給對應使用者的 WebSocket。
2. **Gateway 的無狀態擴展與高可用性**：
   * 使用 Webhook，Agent 必須指定 Gateway 的 IP 或 Domain。如果未來用戶變多，Gateway 分成多台機器，Webhook 的負載平衡與連線狀態追蹤會變得複雜。
   * 使用 Redis，Agent 只需要對 Redis 負責（寫入即完成任務）。Gateway 就算中途重啟，上線後也能從 Redis Stream 的上次讀取點繼續消化事件，**這是建構高可靠監控大盤的必備特性**。
3. **統一的資料流基礎設施**：
   * 我們已經決定使用 Redis 作為 Chat Memory。既然 Redis 已經存在於架構中，直接利用它的 Streams 或 Pub/Sub (建議 Streams 以防資料遺失) 來做事件傳遞，不需要額外架設 RabbitMQ 也能達到企業級的解耦效果。
