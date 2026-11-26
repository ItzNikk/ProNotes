import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const PORT = 3000;
const ROOT = '/app/applet';

function findApkPath() {
  const candidates = [
    path.join(ROOT, '.build-outputs', 'app-debug.apk'),
    path.join(ROOT, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'),
  ];
  for (const p of candidates) {
    if (fs.existsSync(p)) return p;
  }
  return null;
}

const HTML = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Pro Notes Studio</title>
  <style>
    :root {
      --bg: #0f1319;
      --panel: #171d26;
      --panel-2: #1e2632;
      --border: rgba(255, 255, 255, 0.14);
      --text: #f1f5f9;
      --muted: #94a3b8;
      --accent: #1a84ff;
      --emerald: #10b981;
      --amber: #f59e0b;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: var(--bg);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 22px;
      background: var(--panel);
      border-bottom: 1px solid var(--border);
      gap: 12px;
      flex-wrap: wrap;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 700;
      font-size: 18px;
    }
    .badge {
      font-size: 11px;
      padding: 3px 8px;
      border-radius: 6px;
      background: rgba(26, 132, 255, 0.18);
      border: 1px solid rgba(26, 132, 255, 0.45);
      color: #60a5fa;
      font-weight: 600;
    }
    .nav-tabs {
      display: flex;
      gap: 6px;
    }
    .nav-btn {
      background: transparent;
      color: var(--muted);
      border: 1px solid var(--border);
      padding: 7px 14px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
    }
    .nav-btn.active {
      background: var(--accent);
      color: #fff;
      border-color: #60a5fa;
    }
    .apk-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: var(--emerald);
      color: #fff;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 700;
      border: 1px solid rgba(255,255,255,0.28);
    }
    main {
      flex: 1;
      padding: 20px;
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
    }
    .view { display: none; }
    .view.active { display: block; }
    .tag-strip {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    .tag-chip {
      padding: 5px 12px;
      border-radius: 7px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      border: 1px solid var(--border);
      background: var(--panel);
      color: var(--muted);
    }
    .tag-chip.active {
      background: rgba(26, 132, 255, 0.22);
      border-color: var(--accent);
      color: #fff;
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
      gap: 16px;
    }
    .card {
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .card h3 { font-size: 15px; }
    .card p { font-size: 12px; color: var(--muted); line-height: 1.45; }
    .tags-row { display: flex; gap: 6px; flex-wrap: wrap; }
    .pill {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 5px;
      background: rgba(16, 185, 129, 0.16);
      border: 1px solid rgba(16, 185, 129, 0.4);
      color: #6ee7b7;
      font-weight: 600;
    }
    /* Interactive Canvas Preview */
    .studio-frame {
      position: relative;
      background: #131720;
      border: 1px solid var(--border);
      border-radius: 14px;
      height: 540px;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .toolbar-dock {
      position: absolute;
      top: 14px;
      left: 50%;
      transform: translateX(-50%);
      background: #1b2230;
      border: 1.5px solid rgba(255, 255, 255, 0.26);
      border-radius: 14px;
      padding: 6px 10px;
      display: flex;
      align-items: center;
      gap: 8px;
      z-index: 20;
      user-select: none;
    }
    .tool-slot {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      border: 1.5px solid rgba(255,255,255,0.28);
      background: rgba(255,255,255,0.04);
      color: #fff;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 12px;
      font-weight: 700;
      position: relative;
    }
    .tool-slot.active {
      background: #1a84ff;
      border-color: #ffffff;
    }
    .color-bar {
      width: 16px;
      height: 4px;
      border-radius: 4px;
      margin-top: 3px;
      border: 1px solid #fff;
    }
    .page-sheet {
      width: 760px;
      max-width: 92%;
      height: 420px;
      background: #fcfaf5;
      color: #1e293b;
      border-radius: 10px;
      padding: 24px;
      position: relative;
      box-shadow: 0 12px 32px rgba(0,0,0,0.45);
    }
    .write-box {
      margin-top: 16px;
      border: 2px dashed #3d6cf4;
      background: rgba(61, 108, 244, 0.07);
      border-radius: 12px;
      padding: 14px;
      position: relative;
    }
    .write-box-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      font-weight: 700;
      color: #1e3a8a;
      margin-bottom: 8px;
    }
    .write-box textarea {
      width: 100%;
      border: none;
      background: transparent;
      font-size: 18px;
      color: #0f172a;
      resize: none;
      outline: none;
      line-height: 34px;
      background-image: repeating-linear-gradient(transparent, transparent 33px, rgba(61,108,244,0.25) 34px);
    }
  </style>
</head>
<body>
  <header>
    <div class="brand">
      <span>Pro Notes</span>
      <span class="badge">Workspace Preview</span>
    </div>
    <div class="nav-tabs">
      <button class="nav-btn active" onclick="showTab('editor', this)">Canvas & Write Box</button>
      <button class="nav-btn" onclick="showTab('library', this)">Library & #Tags</button>
      <button class="nav-btn" onclick="showTab('reader', this)">Reader Mode</button>
    </div>
    <a class="apk-btn" href="/ProNotes-debug.apk" download="ProNotes-debug.apk">
      Download Android APK
    </a>
  </header>

  <main>
    <section id="view-editor" class="view active">
      <div class="studio-frame" id="studioFrame">
        <div class="toolbar-dock" id="toolbarDock">
          <button class="nav-btn" style="padding:4px 8px;font-size:11px;" onclick="cycleDock()">Dock: <span id="dockLabel">TOP</span></button>
          <div class="tool-slot" onclick="selectTool(this)">Pen<span class="color-bar" style="background:#1a84ff;"></span></div>
          <div class="tool-slot" onclick="selectTool(this)">High<span class="color-bar" style="background:#facc15;border-color:#0f172a;"></span></div>
          <div class="tool-slot active" onclick="selectTool(this)">Text<span class="color-bar" style="background:#10b981;"></span></div>
          <div class="tool-slot" onclick="selectTool(this)">Erase</div>
          <div class="tool-slot" onclick="selectTool(this)">Shape</div>
        </div>

        <div class="page-sheet">
          <div style="font-weight:700;font-size:16px;color:#334155;">Lecture 04 — Linear Algebra & Eigenvalues</div>
          <div class="write-box">
            <div class="write-box-header">
              <span>ON-CANVAS WRITE BOX</span>
              <span>AUTO-CONVERTS HANDWRITING IN PLACE</span>
            </div>
            <textarea rows="5" placeholder="Write inside this box with stylus or type here — handwriting automatically converts to neat formatted text in place...">A matrix A has eigenvalue λ if Av = λv for a non-zero eigenvector v.
Characteristic equation: det(A - λI) = 0.</textarea>
          </div>
        </div>
      </div>
    </section>

    <section id="view-library" class="view">
      <div class="tag-strip">
        <span class="tag-chip active" onclick="filterTag('all', this)">All Tags</span>
        <span class="tag-chip" onclick="filterTag('Important', this)">#Important</span>
        <span class="tag-chip" onclick="filterTag('Study', this)">#Study</span>
        <span class="tag-chip" onclick="filterTag('Lecture', this)">#Lecture</span>
        <span class="tag-chip" onclick="filterTag('Exam', this)">#Exam</span>
        <span class="tag-chip" onclick="filterTag('Math', this)">#Math</span>
      </div>
      <div class="grid" id="notebookGrid">
        <div class="card" data-tags="Guide,Study,Important">
          <h3>Welcome to Pro Notes</h3>
          <p>Interactive Write Box editing, repositionable bordered toolbar, #Tag filtering, and Reader Mode.</p>
          <div class="tags-row">
            <span class="pill">#Important</span>
            <span class="pill">#Study</span>
            <span class="pill">#Guide</span>
          </div>
        </div>
        <div class="card" data-tags="Lecture,Math,Exam">
          <h3>Linear Algebra & Differential Equations</h3>
          <p>Eigenvalues, Jordan canonical forms, and practice problems.</p>
          <div class="tags-row">
            <span class="pill">#Lecture</span>
            <span class="pill">#Math</span>
            <span class="pill">#Exam</span>
          </div>
        </div>
        <div class="card" data-tags="Study,Important">
          <h3>Organic Chemistry Reaction Mechanisms</h3>
          <p>Nucleophilic substitution, elimination pathways, and stereochemistry flashcards.</p>
          <div class="tags-row">
            <span class="pill">#Study</span>
            <span class="pill">#Important</span>
          </div>
        </div>
      </div>
    </section>

    <section id="view-reader" class="view">
      <div class="card" style="max-width:780px;margin:0 auto;padding:24px;">
        <h3>Reader Mode</h3>
        <p>Includes 4 selectable Page Turn Animations (3D Page Curl, 3D Spine Flip, Smooth Slide, Instant), 3 Turn Speeds (Fast, Natural, Relaxed), 6 Paper Themes (Paper, Warm Ivory, Sepia, Forest Sage, Night, OLED Pitch Black), Page Margins, Brightness & Ink Contrast sliders, and synthesized paper-flip audio.</p>
      </div>
    </section>
  </main>

  <script>
    function showTab(id, btn) {
      document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
      document.getElementById('view-' + id).classList.add('active');
      document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    }
    function selectTool(el) {
      document.querySelectorAll('.tool-slot').forEach(s => s.classList.remove('active'));
      el.classList.add('active');
    }
    const docks = ['TOP', 'RIGHT', 'BOTTOM', 'LEFT'];
    let dockIdx = 0;
    function cycleDock() {
      dockIdx = (dockIdx + 1) % docks.length;
      const d = docks[dockIdx];
      document.getElementById('dockLabel').textContent = d;
      const bar = document.getElementById('toolbarDock');
      bar.style.top = ''; bar.style.bottom = ''; bar.style.left = ''; bar.style.right = '';
      bar.style.transform = '';
      bar.style.flexDirection = 'row';
      if (d === 'TOP') { bar.style.top = '14px'; bar.style.left = '50%'; bar.style.transform = 'translateX(-50%)'; }
      if (d === 'BOTTOM') { bar.style.bottom = '14px'; bar.style.left = '50%'; bar.style.transform = 'translateX(-50%)'; }
      if (d === 'LEFT') { bar.style.left = '14px'; bar.style.top = '50%'; bar.style.transform = 'translateY(-50%)'; bar.style.flexDirection = 'column'; }
      if (d === 'RIGHT') { bar.style.right = '14px'; bar.style.top = '50%'; bar.style.transform = 'translateY(-50%)'; bar.style.flexDirection = 'column'; }
    }
    function filterTag(tag, chip) {
      document.querySelectorAll('.tag-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      document.querySelectorAll('#notebookGrid .card').forEach(card => {
        const tags = (card.getAttribute('data-tags') || '').split(',');
        card.style.display = (tag === 'all' || tags.includes(tag)) ? 'flex' : 'none';
      });
    }
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  const url = (req.url || '/').split('?')[0];
  if (url.endsWith('.apk') || url === '/download-apk') {
    const apk = findApkPath();
    if (!apk) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK not found');
      return;
    }
    const stat = fs.statSync(apk);
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': stat.size,
      'Content-Disposition': 'attachment; filename="ProNotes-debug.apk"',
    });
    fs.createReadStream(apk).pipe(res);
    return;
  }

  if (url === '/health' || url === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', apkAvailable: Boolean(findApkPath()) }));
    return;
  }

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(HTML);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('Pro Notes preview & APK server listening on http://0.0.0.0:' + PORT);
});
