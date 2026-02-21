const fs = require('fs');
const { createCanvas } = require('canvas');

// Canvas dimensions
const width = 1920;
const height = 1080;
const canvas = createCanvas(width, height);
const ctx = canvas.getContext('2d');

// --- Philosophy: "Metabolic Grid" ---
// Colors
const bg = '#0D1117';
const gridColor = 'rgba(255, 255, 255, 0.03)';
const gatewayColor = '#2E7D32';    // Glow Base
const agentColor = '#E65100';      // Spawning
const mcpColor = '#00838F';        // Tools
const textColor = 'rgba(255, 255, 255, 0.85)';
const titleColor = '#FFFFFF';

// Background
ctx.fillStyle = bg;
ctx.fillRect(0, 0, width, height);

// Draw Clinical Grid
ctx.lineWidth = 1;
ctx.strokeStyle = gridColor;
for (let i = 0; i < width; i += 40) {
    ctx.beginPath(); ctx.moveTo(i, 0); ctx.lineTo(i, height); ctx.stroke();
}
for (let j = 0; j < height; j += 40) {
    ctx.beginPath(); ctx.moveTo(0, j); ctx.lineTo(width, j); ctx.stroke();
}

// Function to draw glowing nodes (glassmorphism feel)
function drawNode(x, y, w, h, baseColor, title, subtitle) {
    ctx.save();

    // Outer glow
    ctx.shadowColor = baseColor;
    ctx.shadowBlur = 30;
    ctx.fillStyle = 'rgba(25, 25, 30, 0.6)';
    ctx.strokeStyle = baseColor;
    ctx.lineWidth = 2;

    // Rounded Rect
    const radius = 12;
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + w - radius, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + radius);
    ctx.lineTo(x + w, y + h - radius);
    ctx.quadraticCurveTo(x + w, y + h, x + w - radius, y + h);
    ctx.lineTo(x + radius, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
    ctx.fill();
    ctx.stroke();

    ctx.shadowBlur = 0; // Reset

    // Text
    ctx.fillStyle = titleColor;
    ctx.font = '300 18px "Segoe UI", sans-serif';
    ctx.fillText(title, x + 20, y + 35);

    ctx.fillStyle = textColor;
    ctx.font = '100 12px "Segoe UI", monospace';
    ctx.fillText(subtitle, x + 20, y + 55);

    ctx.restore();
}

// Function to draw data connections
function drawConnection(x1, y1, x2, y2, color, dashed = false) {
    ctx.save();
    ctx.globalCompositeOperation = 'screen';
    ctx.strokeStyle = color;
    ctx.lineWidth = 1.5;
    ctx.shadowColor = color;
    ctx.shadowBlur = 10;

    if (dashed) ctx.setLineDash([5, 5]);

    ctx.beginPath();
    ctx.moveTo(x1, y1);

    // Smooth bezier curve
    const cpX = x1 + (x2 - x1) / 2;
    ctx.bezierCurveTo(cpX, y1, cpX, y2, x2, y2);
    ctx.stroke();
    ctx.restore();
}

// Draw Title
ctx.fillStyle = titleColor;
ctx.font = '200 36px "Segoe UI", sans-serif';
ctx.letterSpacing = "4px";
ctx.fillText("TEAM WORK // METABOLIC GRID", 80, 80);
ctx.fillStyle = mcpColor;
ctx.font = '400 14px "Segoe UI", monospace';
ctx.fillText("ARCHITECTURE_MAP.V1 // NANO BANANA PRO", 80, 110);


// ------------ Nodes Layout ------------
// Frontend (Left)
drawNode(100, 450, 280, 120, '#1565C0', 'UI Dashboard', '【多租戶】個人 Agent 看板\n【大盤】中央監控儀表板');

// Gateway (Center Left)
drawNode(480, 300, 280, 160, gatewayColor, 'Spring Boot Gateway', 'REST API / Event Router\nRedis Streams (Sub)');
// ---------- Databases ----------
// Postgres
drawNode(380, 550, 260, 90, '#6A1B9A', 'State DB (Postgres)', '任務狀態 & pgvector(長期記憶)');
// Redis Hub
drawNode(680, 640, 380, 120, '#D32F2F', '⚡ Redis (資料與事件中樞)', '» Streams (多租戶 Event Bus)\n» Chat Memory (高效上下文)');

// Agent Pool (Center Right)
drawNode(880, 200, 320, 380, agentColor, 'Agent Execution Pool', 'Virtual Threads / Async');
// Master Agent inside pool
drawNode(910, 270, 260, 80, '#FF8F00', 'Master Agent', 'Depth 0 // ChatClient');
// Child Agent inside pool
drawNode(910, 390, 260, 80, '#FF5252', 'Sub Agent Spawning', 'Depth 1..3 // Delegate');

// Tooling (Right)
drawNode(1340, 270, 280, 160, mcpColor, '🧩 Tool Integration', '內建 / MCP / Skills (.md)');
drawNode(1340, 480, 280, 90, '#880E4F', 'External Services', 'GitHub / Firecrawl / Notion');

// ------------ Connections ------------
// UI -> Gateway
drawConnection(340, 480, 480, 380, '#1565C0');

// Gateway -> Postgres
drawConnection(540, 460, 540, 550, gatewayColor);

// Gateway -> Agents (派發任務)
drawConnection(760, 380, 880, 350, gatewayColor);

// Agents -> MCP / Tooling
drawConnection(1200, 310, 1340, 310, agentColor);
drawConnection(1200, 430, 1340, 380, agentColor);

// MCP -> External
drawConnection(1480, 430, 1480, 480, mcpColor);

// Agents -> Redis Hub (寫入記憶與發布事件)
drawConnection(1000, 580, 950, 640, '#D32F2F');
// Gateway -> Redis Hub (讀寫記憶與訂閱事件)
drawConnection(680, 460, 750, 640, '#D32F2F', true);

// Decorational Particles (Data flow marks)
ctx.fillStyle = '#FFFFFF';
ctx.shadowBlur = 10;
ctx.shadowColor = '#FFFFFF';
ctx.beginPath(); ctx.arc(410, 440, 3, 0, Math.PI * 2); ctx.fill();
ctx.beginPath(); ctx.arc(820, 380, 3, 0, Math.PI * 2); ctx.fill();
ctx.beginPath(); ctx.arc(820, 490, 3, 0, Math.PI * 2); ctx.fill();
ctx.beginPath(); ctx.arc(1270, 350, 3, 0, Math.PI * 2); ctx.fill();


// Export
const buffer = canvas.toBuffer('image/png');
fs.writeFileSync('e:/github/TeamWork/docs/development/team_work_architecture.png', buffer);
console.log('PNG successfully created at e:/github/TeamWork/docs/development/team_work_architecture.png');
