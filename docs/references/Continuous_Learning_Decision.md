# 不斷學習 (Continuous Learning) 與記憶體架構評估

本文件旨在分析並規劃 Agent 如何具備「自我學習與不斷進化」的能力，並比較不同實作機制的優劣，做為 Team Work 系統長期記憶發展的決策依據。

## 1. 「不斷學習」的核心機制

如 OpenClaw 的設計理念，Agent 的進化並非仰賴重新訓練模型成本極高的 Fine-tuning，而是依賴 **長效記憶 (Long-term Memory)** + **檢索實境 (RAG)** 的設計。其核心運作流程如下：

1.  **經驗捕獲 (Capture)**：當任務結束，或是 Agent 發覺有重要設定、錯誤經驗、使用者偏好時，主動呼叫 `SaveMemory` Tool。
2.  **知識儲存 (Store)**：將這些經驗以結構化或非結構化文字寫入持久化儲存庫。
3.  **動態喚醒 (Recall)**：在下一次執行新任務或新對話前，Gateway 或 Master Agent 會透過語意搜尋，把前次類似任務的「筆記本」翻出來。
4.  **Prompt 注入 (Inject)**：將找出的關聯記憶，動態安插到 System Prompt 裡：「*注意：根據歷史經驗，User 偏好用 Tailwind CSS，且某某套件的 v2 API 會失效，請改用 v3*」。

如此一來，模型不變，但「每次的起跑點」會因為帶著豐富的前置記憶而越來越聰明。

---

## 2. 實作方案評估：純 Markdown 檔案 vs 向量資料庫 (Vector DB)

### 方案 A：純文字 / Markdown 檔案架構 (如早期 OpenClaw `MEMORY.md`)

*   **實作方式**：提供 Agent 一個讀寫特定資料夾內檔案的權限 (如 `memory/使用者ID/user_profile.md` 或 `memory/專案ID/tech_stack.md`)。
*   **優點**：
    *   **實作門檻極低**：只要有 File System IO 權限即可。
    *   **人類可讀性高**：開發者或使用者可以直接打開 Markdown 修改、刪除錯誤記憶，如同編輯維基百科。
*   **缺點**：
    *   **搜尋能力薄弱**：隨著記憶增多，只能依賴關鍵字全文檢索 (如 `grep`)。如果文字描述是「使用者喜歡暗色系」，搜尋「黑夜模式」可能就找不到關聯筆記。
    *   **Context Window 負擔**：如果把整個 `MEMORY.md` 塞給模型，容易耗盡 Token 且可能干擾當前任務。
    *   **併發讀寫問題**：多個 Agent 若同時想更新經驗筆記，容易發生檔案 Lock 或覆寫衝突。

### 方案 B：向量資料庫架構 (如 pgvector 或 Milvus) - 👑 **推薦方案**

*   **實作方式**：
    1. Agent 呼叫 `SaveMemory` 傳入心得總結。
    2. Spring Boot 收到後，透過 Embedding Model (如 `text-embedding-3-small`) 將文字轉為向量 (Vector)。
    3. 存入 PostgreSQL 的 `pgvector` 擴充套件，或專門的 Vector DB。
    4. 下次任務執行前，用任務目標的 Embedding 進行「相似性搜尋 (Cosine Similarity Search)」，撈出 Top-K 最相關的 3~5 條筆記，注入 Prompt。
*   **優點**：
    *   **語意搜尋 (Semantic Search)**：能理解上下文概念，而非死板的字串比對。「暗黑風格」與「黑色背景」會被視為高度相似，精準命中過往經驗。
    *   **精準控制 Context 大小**：RAG (檢索增強生成) 只會提取最相關的那幾句話，大大節省 Prompt Token 長度，並提高模型注意力 (Attention)。
    *   **擴充性極佳**：非常適合多租戶系統。每條記憶打上 `tenant_id` 或 `project_id` 標籤 (Metadata)，就能完美隔離不同專案與使用者的知識庫。
*   **缺點**：
    *   **基礎設施成本**：需啟用 PostgreSQL 上的 `pgvector` 或架設 ChromaDB/Milvus。
    *   **需要 Embedding 步驟**：存入與讀取時都需要額外呼叫一次 Embedding API，稍微增加網路延遲與 API 費用。

---

## 3. Team Work 系統的建議架構

綜合考量我們系統主打的 **多租戶 (Multi-tenant)** 與 **複雜 Agent 協作** 兩個特性，**強烈建議採用「方案 B：向量資料庫 (使用 PostgreSQL + pgvector)」**。

### 理由與優勢整合：
1.  **基礎設施復用**：我們目前的架構圖中已經有了 PostgreSQL 作為 State DB。PostgreSQL 只要啟用 `pgvector` extension，就能直接變身為強大的向量資料庫，**不須額外引入新的服務**。
2.  **Spring AI 原生支援**：Spring AI 具備極為成熟的 `PgVectorStore` 實作。實作 RAG 喚醒記憶只需幾行程式碼配置。
3.  **大腦功能分工明確**：
    *   **Redis Chat Memory**：負責本局對話的「**短期工作記憶**」(如你上一句問什麼)，快取性質，會過期清理。
    *   **PostgreSQL / pgvector**：負責橫跨專案與時間線的「**長期知識記憶與使用者畫像**」，作為經驗的永久沉澱池。

## 4. 進化的雙引擎：記憶 (Memory) + 技能工具 (Skills)

除了上述提到的「長期記憶」讓 Agent 累積**經驗**之外，正如 OpenClaw 展現的能力，**動態的技能擴充與安裝** 則是賦予 Agent 新的**手腳與能力**。真正的「不斷學習」是由這兩者共同驅動的雙引擎：

1.  **記憶體 (大腦經驗)**：透過 Vector DB 記住「過去跌倒的教訓」與「使用者的開發習慣」。
2.  **技能庫 (延伸手腳)**：透過 MCP (Model Context Protocol) 整合與 Skills Directory 動態增減工具。

### 技能如何參與進化迴圈？
在我們的 **Team Work 架構**中，Agent 不僅能寫死在程式碼裡的工具，更能透過 `Skills Directory (.md)` 與 `MCP Integration` 達成：
*   **動態安裝能力**：當 Agent 遇到無法解決的問題（例如：需要爬取特定格式的網頁），它可以透過類似 `install_skill` 的機制，將外部的擴展套件（像 Firecrawl MCP Server）動態引入到它目前的可用工具清單中。
*   **技能微調 (Skill Adjustment)**：如同更新 Prompt，如果一個特定的 Skill (例如：建立 Spring Boot 專案的流程) 被發現有瑕疵，Agent 或 Master Administrator 可以修改那個 Skill 的 `.md` 定義檔，修正 SOP。下一次再呼叫這個技能時，它表現就會進化。

### 結論
改用向量資料庫不僅是「更好的做法」，對於一個要具備「多租戶與不斷進化」的系統來說，它是必然走向的最終型態。它能讓 Agent 從「死讀書的打字機」進化成能「聯想過往經驗的資深工程師」。而配合 **MCP 技能的動態安插**，更讓這位工程師能隨時為自己配備最新型號的工具箱，完美實現系統的無限進化。
