// wasm-wasi-collector.mjs — collect wasm coverage for the wasmWasi target.
// The wasmWasi test mjs sets up WASI and exports startUnitTests (does NOT auto-call it),
// so this collector imports it, runs the tests, then collects %DebugCollectWasmCoverage.
//
// Usage: node --experimental-wasi --allow-natives-syntax --wasm-code-coverage --no-wasm-lazy-compilation \
//          wasm-wasi-collector.mjs <test.mjs> <out.json>
import { writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const mjs = process.argv[2];
const OUT = process.argv[3];
if (!mjs || !OUT) { console.error('usage: wasm-wasi-collector.mjs <test.mjs> <out.json>'); process.exit(2); }

const { startUnitTests } = await import(pathToFileURL(mjs).href);
startUnitTests();   // run the wasmWasi tests under WASI

const cov = %DebugCollectWasmCoverage();
writeFileSync(OUT, JSON.stringify(cov));
const mod = cov[cov.length - 1] || [];
let zero = 0, nonzero = 0;
for (const r of mod) { if (r.count === 0) zero++; else nonzero++; }
console.log(`[wasm-wasi-collector] modules=${cov.length} ranges in last module=${mod.length} covered=${nonzero} uncovered=${zero}`);
