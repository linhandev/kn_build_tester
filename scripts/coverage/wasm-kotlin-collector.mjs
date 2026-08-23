// wasm-kotlin-collector.mjs
// Collect V8 WebAssembly coverage via the internal %DebugCollectWasmCoverage.
// Node's NODE_V8_COVERAGE does NOT cover wasm modules. The only way to get wasm
// block coverage is %DebugCollectWasmCoverage, which requires these node flags
// (Node 25+, V8 14.1+):
//   --allow-natives-syntax         (enables % internal functions)
//   --wasm-code-coverage           (enables V8 wasm block instrumentation)
//   --no-wasm-lazy-compilation     (force eager compile so uncalled funcs get instrumented)
//
// Usage: node --allow-natives-syntax --wasm-code-coverage --no-wasm-lazy-complication \
//          wasm-kotlin-collector.mjs <test-pkg-dir> <out.json>
//   <test-pkg-dir> = the kotlin/ dir of the wasm test package (../static/runUnitTests.mjs is resolved from it)
import { writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { resolve } from 'node:path';

const TEST_DIR = process.argv[2];
const OUT = process.argv[3];
if (!TEST_DIR || !OUT) {
  console.error('usage: wasm-kotlin-collector.mjs <test-pkg-dir> <out.json>');
  process.exit(2);
}

// 1. Run the Kotlin/Wasm tests (startUnitTests is the entry the runner exposes).
const entry = pathToFileURL(resolve(TEST_DIR, '..', 'static', 'runUnitTests.mjs')).href;
await import(entry);

// 2. Collect wasm coverage BEFORE the module is GC'd.
const cov = %DebugCollectWasmCoverage();
writeFileSync(OUT, JSON.stringify(cov));
const mod = cov[cov.length - 1] || [];
let zero = 0, nonzero = 0;
for (const r of mod) { if (r.count === 0) zero++; else nonzero++; }
console.log(`[wasm-kotlin-collector] modules=${cov.length} ranges in last module=${mod.length} covered=${nonzero} uncovered=${zero}`);
