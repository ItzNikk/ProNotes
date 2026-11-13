import { ImageItem, Page, PageItem, StickyItem, TextItem, UNITS_PER_MM } from '../types/models';

export interface ImportParseResult {
  title: string;
  pages: Page[];
  blockCount: number;
  sourceFormat: string;
}

const A4_W = Math.round(210 * UNITS_PER_MM);
const A4_H = Math.round(297 * UNITS_PER_MM);

const uid = (p: string) => `${p}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;

function makeBlankA4(paperTint = '#FFFFFF'): Page {
  return {
    id: uid('pg'),
    width: A4_W,
    height: A4_H,
    paper: 'RULED',
    paperColor: paperTint,
    items: [],
  };
}

interface ParsedBlock {
  text: string;
  kind: 'h1' | 'h2' | 'callout' | 'body' | 'bullet';
}

/** Converts HTML fragments (Apple Notes, Notion HTML export, web clips) into clean structured text. */
function htmlToStructuredText(html: string): string {
  const parser = typeof DOMParser !== 'undefined' ? new DOMParser() : null;
  if (!parser) {
    return html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
  }
  const doc = parser.parseFromString(html, 'text/html');
  const lines: string[] = [];
  const walk = (node: Node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      const txt = node.textContent?.replace(/\s+/g, ' ') || '';
      if (txt.trim()) lines.push(txt.trim());
      return;
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return;
    const el = node as HTMLElement;
    const tag = el.tagName.toLowerCase();
    if (tag === 'script' || tag === 'style') return;
    if (tag === 'h1') {
      lines.push(`# ${(el.textContent || '').trim()}`);
      return;
    }
    if (tag === 'h2' || tag === 'h3') {
      lines.push(`## ${(el.textContent || '').trim()}`);
      return;
    }
    if (tag === 'li') {
      lines.push(`• ${(el.textContent || '').trim()}`);
      return;
    }
    if (tag === 'blockquote') {
      lines.push(`> ${(el.textContent || '').trim()}`);
      return;
    }
    if (tag === 'br') {
      lines.push('');
      return;
    }
    const block = ['p', 'div', 'section', 'article', 'tr'].includes(tag);
    if (block) {
      const textOnly = (el.textContent || '').trim();
      if (el.children.length === 0 && textOnly) {
        lines.push(textOnly);
        return;
      }
    }
    for (const child of Array.from(el.childNodes)) walk(child);
    if (block) lines.push('');
  };
  walk(doc.body);
  return lines.join('\n').replace(/\n{3,}/g, '\n\n').trim();
}

/** Parses CSV text into clean readable rows. */
function csvToStructuredText(csv: string): string {
  const rows = csv
    .split(/\r?\n/)
    .map((r) => r.trim())
    .filter(Boolean);
  if (rows.length === 0) return '';
  const headers = rows[0].split(',').map((c) => c.replace(/^"|"$/g, '').trim());
  const out: string[] = [];
  if (headers.length > 0) out.push(`## ${headers.join(' · ')}`);
  for (let i = 1; i < rows.length; i++) {
    const cols = rows[i].split(',').map((c) => c.replace(/^"|"$/g, '').trim());
    const pairs = cols
      .map((val, idx) => (headers[idx] && val ? `${headers[idx]}: ${val}` : val))
      .filter(Boolean);
    if (pairs.length) out.push(`• ${pairs.join(' · ')}`);
  }
  return out.join('\n');
}

/** Parses Markdown / Obsidian / Notion / Keep plain text into typed blocks & paginated A4 pages. */
export function parseImportedText(
  title: string,
  rawText: string,
  paperTint = '#FFFFFF',
  sourceFormat = 'Notes Text'
): ImportParseResult {
  const cleanTitle = title.trim() || 'Imported Note';
  const normalized = rawText
    .replace(/\r\n/g, '\n')
    // Clean Obsidian wikilinks [[Page Name|Alias]] -> Alias / Page Name
    .replace(/\[\[([^\]|]+)\|([^\]]+)\]\]/g, '$2')
    .replace(/\[\[([^\]]+)\]\]/g, '$1')
    // Clean markdown checkboxes
    .replace(/^(\s*)[-*]\s+\[x\]\s+/gim, '$1☑ ')
    .replace(/^(\s*)[-*]\s+\[\s?\]\s+/gim, '$1☐ ');

  const rawLines = normalized.split('\n');
  const blocks: ParsedBlock[] = [];
  let paraBuf: string[] = [];

  const flushPara = () => {
    if (paraBuf.length === 0) return;
    blocks.push({ text: paraBuf.join(' ').trim(), kind: 'body' });
    paraBuf = [];
  };

  for (const raw of rawLines) {
    const line = raw.trimEnd();
    const trimmed = line.trim();
    if (!trimmed) {
      flushPara();
      continue;
    }
    // Skip YAML frontmatter delimiters from Obsidian
    if (trimmed === '---' && blocks.length === 0) continue;

    if (trimmed.startsWith('# ')) {
      flushPara();
      blocks.push({ text: trimmed.slice(2).trim(), kind: 'h1' });
    } else if (/^#{2,4}\s+/.test(trimmed)) {
      flushPara();
      blocks.push({ text: trimmed.replace(/^#{2,4}\s+/, '').trim(), kind: 'h2' });
    } else if (trimmed.startsWith('> ')) {
      flushPara();
      blocks.push({ text: trimmed.slice(2).replace(/^\[!(\w+)\]\s*/i, '$1: ').trim(), kind: 'callout' });
    } else if (/^([-*•]|☑|☐|\d+[.)])\s+/.test(trimmed)) {
      flushPara();
      const body = trimmed.replace(/^[-*]\s+/, '• ');
      blocks.push({ text: body, kind: 'bullet' });
    } else {
      paraBuf.push(trimmed);
    }
  }
  flushPara();

  if (blocks.length === 0) {
    blocks.push({ text: cleanTitle, kind: 'h1' });
  }

  // Paginate onto A4 pages cleanly
  const marginX = 95;
  const maxWidthChars = 66;
  const pages: Page[] = [];
  let page = makeBlankA4(paperTint);
  let y = 96;

  const ensureRoom = (neededH: number) => {
    if (y + neededH > A4_H - 90) {
      pages.push(page);
      page = makeBlankA4(paperTint);
      y = 96;
    }
  };

  const wrapText = (txt: string, maxChars: number): string[] => {
    const words = txt.split(/\s+/);
    const lines: string[] = [];
    let cur = '';
    for (const w of words) {
      if ((cur + ' ' + w).trim().length > maxChars) {
        if (cur) lines.push(cur);
        cur = w;
      } else {
        cur = cur ? `${cur} ${w}` : w;
      }
    }
    if (cur) lines.push(cur);
    return lines.length ? lines : [''];
  };

  blocks.forEach((b) => {
    if (b.kind === 'callout') {
      const wrapped = wrapText(b.text, 44).join('\n');
      const lineCount = wrapped.split('\n').length;
      const boxH = Math.max(120, Math.min(260, 46 + lineCount * 30));
      ensureRoom(boxH + 22);
      const sticky: StickyItem = {
        id: uid('st'),
        kind: 'sticky',
        x: marginX,
        y,
        w: A4_W - marginX * 2,
        h: boxH,
        color: '#FFF4C4',
        text: wrapped,
        fontSize: 22,
        font: 'SANS',
      };
      page.items.push(sticky);
      y += boxH + 26;
      return;
    }

    const size = b.kind === 'h1' ? 38 : b.kind === 'h2' ? 29 : 23;
    const chars = b.kind === 'h1' ? 42 : b.kind === 'h2' ? 52 : maxWidthChars;
    const wrapped = wrapText(b.text, chars);
    const estH = wrapped.length * size * 1.42 + (b.kind === 'h1' ? 18 : 12);
    ensureRoom(estH);

    const item: TextItem = {
      id: uid('tx'),
      kind: 'text',
      x: b.kind === 'bullet' ? marginX + 18 : marginX,
      y,
      text: wrapped.join('\n'),
      size,
      color: b.kind === 'h1' ? '#1E3A8A' : '#1B1F2A',
      font: 'SANS',
      bold: b.kind === 'h1' || b.kind === 'h2',
      createdAt: Date.now(),
    };
    page.items.push(item);
    y += estH;
  });

  if (page.items.length > 0 || pages.length === 0) pages.push(page);

  return {
    title: cleanTitle,
    pages,
    blockCount: blocks.length,
    sourceFormat,
  };
}

/** Extracts readable text runs from an uncompressed/literal PDF byte buffer safely offline. */
async function extractPdfTextPages(file: File): Promise<{ pageTexts: string[]; pageCount: number }> {
  const buf = await file.arrayBuffer();
  const bytes = new Uint8Array(buf);
  const decoder = new TextDecoder('latin1');
  const raw = decoder.decode(bytes);

  const pageMatches = raw.match(/\/Type\s*\/Page\b/g);
  const detectedPages = Math.max(1, pageMatches ? pageMatches.length : 1);

  const chunks: string[] = [];
  const literalRe = /\(([^()\\]{3,160})\)/g;
  let m: RegExpExecArray | null;
  while ((m = literalRe.exec(raw)) !== null && chunks.length < 360) {
    const seg = m[1].replace(/[^\x20-\x7E]/g, ' ').replace(/\s+/g, ' ').trim();
    if (seg.length >= 3 && /[A-Za-z0-9]/.test(seg) && !/^(Type|Font|Filter|FlateDecode|Length|Pages|Catalog)$/i.test(seg)) {
      chunks.push(seg);
    }
  }

  if (chunks.length === 0) {
    return {
      pageCount: detectedPages,
      pageTexts: [
        `# ${file.name.replace(/\.pdf$/i, '')}\n> Imported PDF (${detectedPages} page${detectedPages === 1 ? '' : 's'}). Compressed binary PDF streams cannot be decoded in pure browser mode — paste extracted text or annotate directly on these A4 sheets.`,
      ],
    };
  }

  const perPage = Math.max(10, Math.ceil(chunks.length / detectedPages));
  const out: string[] = [];
  for (let i = 0; i < chunks.length; i += perPage) {
    out.push(chunks.slice(i, i + perPage).join(' '));
  }
  return { pageCount: detectedPages, pageTexts: out };
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

function getImageDimensions(src: string): Promise<{ w: number; h: number }> {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve({ w: img.naturalWidth || 900, h: img.naturalHeight || 700 });
    img.onerror = () => resolve({ w: 900, h: 700 });
    img.src = src;
  });
}

/** Parses one or more external files (Markdown, TXT, HTML, CSV, JSON, Images, PDF) into Pro Notes pages. */
export async function parseImportedFiles(
  files: FileList | File[],
  paperTint = '#FFFFFF'
): Promise<ImportParseResult[]> {
  const arr = Array.from(files);
  const results: ImportParseResult[] = [];

  for (const file of arr) {
    const name = file.name || 'Imported File';
    const baseTitle = name.replace(/\.[^.]+$/, '');
    const lower = name.toLowerCase();

    if (file.type.startsWith('image/') || /\.(png|jpe?g|webp|gif)$/i.test(lower)) {
      const src = await readFileAsDataUrl(file);
      const dims = await getImageDimensions(src);
      const maxW = A4_W - 140;
      const maxH = A4_H - 180;
      const scale = Math.min(maxW / Math.max(1, dims.w), maxH / Math.max(1, dims.h));
      const w = Math.round(dims.w * scale);
      const h = Math.round(dims.h * scale);
      const imgItem: ImageItem = {
        id: uid('im'),
        kind: 'image',
        x: Math.round((A4_W - w) / 2),
        y: 110,
        w,
        h,
        src,
      };
      const page: Page = { ...makeBlankA4(paperTint), paper: 'BLANK', items: [imgItem] };
      results.push({ title: baseTitle, pages: [page], blockCount: 1, sourceFormat: 'Image' });
      continue;
    }

    if (file.type === 'application/pdf' || lower.endsWith('.pdf')) {
      const { pageTexts, pageCount } = await extractPdfTextPages(file);
      const allPages: Page[] = [];
      let totalBlocks = 0;
      pageTexts.forEach((txt, idx) => {
        const res = parseImportedText(
          `${baseTitle} · p.${idx + 1}`,
          `## ${baseTitle} (Page ${idx + 1}/${pageCount})\n${txt}`,
          paperTint,
          'PDF'
        );
        allPages.push(...res.pages);
        totalBlocks += res.blockCount;
      });
      results.push({
        title: baseTitle,
        pages: allPages.length ? allPages : [makeBlankA4(paperTint)],
        blockCount: totalBlocks,
        sourceFormat: 'PDF',
      });
      continue;
    }

    const rawText = await file.text();

    if (lower.endsWith('.html') || lower.endsWith('.htm')) {
      const structured = htmlToStructuredText(rawText);
      results.push(parseImportedText(baseTitle, structured, paperTint, 'HTML'));
      continue;
    }

    if (lower.endsWith('.csv')) {
      const structured = csvToStructuredText(rawText);
      results.push(parseImportedText(baseTitle, structured, paperTint, 'CSV'));
      continue;
    }

    if (lower.endsWith('.json')) {
      try {
        const json = JSON.parse(rawText);
        // Google Keep single note JSON: { title, textContent, listContent }
        if (json && typeof json === 'object' && !Array.isArray(json) && ('textContent' in json || 'listContent' in json)) {
          const kTitle = String(json.title || baseTitle);
          const listLines = Array.isArray(json.listContent)
            ? json.listContent.map((it: { text?: string; isChecked?: boolean }) => `${it.isChecked ? '☑' : '☐'} ${it.text || ''}`)
            : [];
          const body = [`# ${kTitle}`, String(json.textContent || ''), ...listLines].filter(Boolean).join('\n');
          results.push(parseImportedText(kTitle, body, paperTint, 'Google Keep'));
          continue;
        }
        // Generic JSON array of notes [{ title, body/content/text }]
        if (Array.isArray(json)) {
          for (let idx = 0; idx < Math.min(20, json.length); idx++) {
            const entry = json[idx];
            if (entry && typeof entry === 'object') {
              const t = String(entry.title || entry.name || `${baseTitle} ${idx + 1}`);
              const b = String(entry.textContent || entry.content || entry.body || entry.text || JSON.stringify(entry, null, 2));
              results.push(parseImportedText(t, b, paperTint, 'JSON Notes'));
            }
          }
          continue;
        }
        const pretty = Object.entries(json as Record<string, unknown>)
          .map(([k, v]) => `## ${k}\n${typeof v === 'string' ? v : JSON.stringify(v, null, 2)}`)
          .join('\n\n');
        results.push(parseImportedText(baseTitle, pretty, paperTint, 'JSON'));
      } catch {
        results.push(parseImportedText(baseTitle, rawText, paperTint, 'Text'));
      }
      continue;
    }

    const fmt = lower.endsWith('.md') || lower.endsWith('.markdown') ? 'Markdown' : 'Text';
    results.push(parseImportedText(baseTitle, rawText, paperTint, fmt));
  }

  return results;
}

export function buildOutlineItemsFromMindNodes(nodes: { id: string; parentId: string | null; label: string; notes?: string; tag?: string }[]): PageItem[] {
  const roots = nodes.filter((n) => !n.parentId);
  const childrenOf = (pid: string) => nodes.filter((n) => n.parentId === pid);
  const lines: string[] = [];
  const visit = (node: typeof nodes[number], depth: number) => {
    const prefix = depth === 0 ? '# ' : `${'  '.repeat(Math.max(0, depth - 1))}• `;
    const tag = node.tag ? `[${node.tag}] ` : '';
    const note = node.notes ? ` — ${node.notes}` : '';
    lines.push(`${prefix}${tag}${node.label}${note}`);
    childrenOf(node.id).forEach((c) => visit(c, depth + 1));
  };
  roots.forEach((r) => visit(r, 0));
  if (lines.length === 0) return [];
  const parsed = parseImportedText('Mind Map Outline', lines.join('\n'), '#FFFFFF', 'Mind Map');
  return parsed.pages[0]?.items ?? [];
}
