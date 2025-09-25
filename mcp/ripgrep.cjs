#!/usr/bin/env node
// Minimalny serwer MCP (JSON-RPC 2.0) dla ripgrep, CommonJS – bez zależności.
// Na STDOUT TYLKO odpowiedzi z "id". Logi -> STDERR.

const { spawn } = require("node:child_process");
const readline = require("node:readline");

const NAME = "mcp-ripgrep";
const VERSION = "1.0.1";
const WORKDIR = process.env.WORKDIR || process.cwd();

const rl = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });

// (opcjonalnie) zablokuj przypadkowe logi na STDOUT:
console.log = () => {};

function send(id, result) {
  process.stdout.write(JSON.stringify({ jsonrpc: "2.0", id, result }) + "\n");
}
function sendError(id, message, code = -32000, data = {}) {
  process.stdout.write(JSON.stringify({ jsonrpc: "2.0", id, error: { code, message, data } }) + "\n");
}
function log(...args) { process.stderr.write(args.map(String).join(" ") + "\n"); }

const toolDef = {
  name: "ripgrep.search",
  description: "Szybkie wyszukiwanie w repo przy użyciu `rg`. Zwraca dopasowania (plik, linia, tekst).",
  inputSchema: {
    type: "object",
    additionalProperties: false,
    properties: {
      query: { type: "string" },
      cwd:   { type: "string", description: "Katalog roboczy (opcjonalnie)" },
      globs: { type: "array", items: { type: "string" }, description: "Dodatkowe -g globs" },
      json:  { type: "boolean", description: "Użyj `rg --json` (domyślnie false)" }
    },
    required: ["query"]
  }
};

async function handleRipgrepSearch(args) {
  const query = args?.query;
  if (!query) return { error: { message: "missing query" } };

  const cwd = args?.cwd || WORKDIR;
  const globs = Array.isArray(args?.globs) ? args.globs : [];
  const useJson = !!args?.json;

  const base = globs.flatMap(g => ["-g", g]);
  const rgArgs = useJson
    ? ["--json", ...base, query]
    : ["-n", "--no-heading", "--color", "never", ...base, query];

  const proc = spawn("rg", rgArgs, { cwd });
  let out = "", err = "";
  await new Promise(resolve => {
    proc.stdout.on("data", d => (out += d.toString()));
    proc.stderr.on("data", d => (err += d.toString()));
    proc.on("close", () => resolve());
  });

  let matches;
  if (useJson) {
    matches = out.split("\n").filter(Boolean).map(line => {
      try { return JSON.parse(line); } catch { return null; }
    }).filter(e => e && e.type === "match").map(e => ({
      file: e.data?.path?.text ?? "",
      line: e.data?.line_number ?? 0,
      text: (e.data?.lines?.text ?? "").replace(/\r?\n$/, "")
    }));
  } else {
    matches = out.split("\n").filter(Boolean).map(line => {
      const [file, lineNo, ...rest] = line.split(":");
      return { file, line: Number(lineNo || 0), text: rest.join(":") };
    });
  }

  return {
    content: [{
      type: "text",
      text: JSON.stringify({ cwd, count: matches.length, matches }, null, 2)
    }]
  };
}

async function handleMessage(msg) {
  const { id, method, params } = msg || {};

  if (method === "initialize") {
    return send(id, {
      protocolVersion: "2024-11-05",
      serverInfo: { name: NAME, version: VERSION },
      capabilities: { tools: {} }
    });
  }
  if (method === "ping") return send(id, { ok: true });
  if (method === "shutdown") { send(id, { ok: true }); process.exit(0); }

  if (method === "capabilities/list") return send(id, { capabilities: { tools: {} } });
  if (method === "tools/list") return send(id, { tools: [toolDef] });

  if (method === "tools/call") {
    const name = params?.name;
    const args = params?.arguments || {};
    if (name !== "ripgrep.search") return sendError(id, "unknown tool", -32601);
    try {
      const res = await handleRipgrepSearch(args);
      if (res?.error) return sendError(id, res.error.message);
      return send(id, res);
    } catch (e) {
      log("ripgrep error:", e?.message || e);
      return sendError(id, "ripgrep failed", -32000, { message: String(e?.message || e) });
    }
  }

  return sendError(id, "unknown method");
}

rl.on("line", async line => {
  if (!line.trim()) return;
  let msg; try { msg = JSON.parse(line); } catch { return; }
  handleMessage(msg);
});
