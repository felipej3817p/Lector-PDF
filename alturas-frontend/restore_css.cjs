const fs = require('fs');
const path = 'c:\\Users\\sen_atorres\\Downloads\\Lector-PDF-CODEX-PR4\\alturas-frontend\\src\\assets\\main.css';
const content = fs.readFileSync(path, 'utf8');

const header = `:root {
  color-scheme: light;
  --page-bg: #f7f8f9;
  --surface: #ffffff;
  --surface-soft: #f9fafb;
  --surface-raised: #ffffff;
  --text: #111827;
  --text-soft: #374151;
  --text-muted: #6b7280;
  --border: #e5e7eb;
  --border-strong: #d1d5db;
  --primary: #3ecf8e;
  --primary-strong: #249361;
  --primary-hover: #2ebc7f;
  --primary-soft: rgba(62, 207, 142, 0.12);
  --success: #168356;
  --success-soft: rgba(22, 131, 86, 0.12);
  --danger: #dc2626;
  --danger-soft: rgba(220, 38, 38, 0.1);
  --warning: #b45309;
  --warning-soft: rgba(180, 83, 9, 0.12);
  --info: #2563eb;
  --info-soft: rgba(37, 99, 235, 0.1);
  --purple: #8b5cf6;
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.04), 0 8px 28px rgba(15, 23, 42, 0.05);
  --shadow-md: 0 18px 50px rgba(15, 23, 42, 0.11);
  --radius-sm: 10px;
  --radius-md: 16px;
  --radius-lg: 22px;
  --sidebar-width: 220px;
}

:root[data-theme='dark'] {
  color-scheme: dark;
  --page-bg: #020617;
  --surface: #0b1120;
  --surface-soft: #111827;
  --surface-raised: #0f172a;
  --text: #f8fafc;
  --text-soft: #d1d5db;
  --text-muted: #94a3b8;
  --border: #1f2937;
  --border-strong: #334155;
  --primary: #3ecf8e;
  --primary-strong: #67e8b6;
  --primary-hover: #67e8b6;
  --primary-soft: rgba(62, 207, 142, 0.14);
  --success: #34d399;
  --success-soft: rgba(52, 211, 153, 0.14);
  --danger: #f87171;
  --danger-soft: rgba(248, 113, 113, 0.13);
  --warning: #fbbf24;
  --warning-soft: rgba(251, 191, 36, 0.14);
  --info: #60a5fa;
  --info-soft: rgba(96, 165, 250, 0.14);
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.35), 0 16px 40px rgba(0, 0, 0, 0.25);
  --shadow-md: 0 24px 70px rgba(0, 0, 0, 0.38);
}

html {
  box-sizing: border-box;
  min-height: 100%;
}

*,
*::before,
*::after {
  box-sizing: inherit;
}
`;

fs.writeFileSync(path, (header + content).replace(/^[\s\S]*?(?=html,)/, header));
