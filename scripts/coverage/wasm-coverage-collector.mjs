// wasm-coverage-collector.mjs
// Experimental: collect V8 WebAssembly coverage via the Inspector API.
// Node's NODE_V8_COVERAGE does NOT cover wasm modules by default — there is no CLI flag
// (Node 24 removed/never exposed --experimental-wasm-code-coverage). The only way to get
// wasm block coverage is to drive the V8 inspector's Profiler.startPreciseCoverage with
// {detailed: true, callCount: false}, which includes wasm function/block ranges.
//
// This script:
//   1. spawns the wasm test executable under node --inspect-brk
//   2. attaches via inspector, starts precise (detailed) coverage
//   3. lets the test run, then takes coverage and writes it as V8-coverage JSON
//   4. c8 can then consume that JSON (with wasm source-map remap)
//
// NOTE: this is best-effort and records whether remap to .kt actually works.
import * as inspector from 'node:inspector'
import { spawn } from 'node:child_process'
import { writeFileSync, mkdirSync } from 'node:fs'

const target = process.argv[2]        // .mjs test executable
const outDir = process.argv[3]        // where to write coverage JSON
const wasmRegex = process.argv[4] || '\\.wasm$'

if (!target || !outDir) {
  console.error('usage: wasm-coverage-collector.mjs <test.mjs> <out-dir> [wasm-regex]')
  process.exit(2)
}
mkdirSync(outDir, { recursive: true })

// Spawn node with inspector paused at start, so we can attach & enable coverage before any wasm runs.
const child = spawn(process.execPath, ['--inspect-brk=0', target], {
  stdio: ['ignore', 'inherit', 'inherit'],
  env: { ...process.env },
})

let port = 0
child.stderr.on('data', (b) => {
  const m = /Debugger listening on .*:(\d+)/.exec(b.toString())
  if (m) port = +m[1]
})

// wait for the inspector port
await new Promise((res) => {
  const t = setInterval(() => { if (port) { clearInterval(t); res() } }, 50)
})

const session = new inspector.Session()
session.connect({ port })

const post = (method, params = {}) => new Promise((r) => session.post(method, params, (err, res) => r(res)))

await post('Profiler.enable')
await post('Profiler.startPreciseCoverage', { callCount: false, detailed: true })  // detailed=true => block + wasm

// resume the paused-at-brk program
await post('Runtime.runIfWaitingForDebugger')

await new Promise((res) => child.on('exit', res))

const { result } = await post('Profiler.takePreciseCoverage')
await post('Profiler.stopPreciseCoverage')
await post('Profiler.disable')

// Filter to entries that look like our wasm / mjs; write in NODE_V8_COVERAGE shape so c8 can read.
const filtered = result.filter((r) => new RegExp(wasmRegex).test(r.url) || r.url.endsWith('.mjs') || r.url.endsWith('.wasm'))
const payload = { result: filtered }
const out = `${outDir}/coverage-wasm-${process.pid}-${Date.now()}.json`
writeFileSync(out, JSON.stringify(payload))
console.log(`[wasm-collector] wrote ${out} (${filtered.length} entries)`)
const wasmEntries = filtered.filter((r) => r.url.endsWith('.wasm'))
console.log(`[wasm-collector] wasm module entries: ${wasmEntries.length}`)
if (wasmEntries.length) {
  console.log(`[wasm-collector] first wasm url: ${wasmEntries[0].url}`)
  console.log(`[wasm-collector] functions in first wasm entry: ${wasmEntries[0].functions?.length ?? 0}`)
}
session.disconnect()
