import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const stylesheet = fileURLToPath(new URL("../../frontend/src/app/styles.css", import.meta.url));
const css = readFileSync(stylesheet, "utf8");
const variables = new Map(
  [...css.matchAll(/--([\w-]+):\s*([^;]+);/g)].map((match) => [match[1], match[2].trim()]),
);

const sourcePalette = {
  "spark-dark": "#000000",
  "spark-light": "#FFFFFF",
  "spark-grey-dark": "#797979",
  "spark-grey-light": "#A9A9A9",
  "spark-blue": "#0050F0",
  "spark-orange": "#F68B1F",
  "spark-navy": "#002C63",
  "spark-red-orange": "#F95922",
  "spark-red": "#DA2010",
  "spark-yellow": "#FEC800",
  "spark-template-hlink": "#65B2E8",
  "spark-template-followed-hlink": "#1EB950",
};

function resolve(name, seen = new Set()) {
  if (seen.has(name)) throw new Error(`Circular token reference: ${name}`);
  const value = variables.get(name);
  if (!value) throw new Error(`Missing token: ${name}`);
  if (/^#[\da-f]{6}$/i.test(value)) return value.toUpperCase();
  const reference = /^var\(--([\w-]+)\)$/.exec(value)?.[1];
  if (!reference) throw new Error(`Unsupported token value for ${name}: ${value}`);
  return resolve(reference, new Set([...seen, name]));
}

function luminance(hex) {
  const channels = hex.slice(1).match(/../g).map((part) => {
    const srgb = Number.parseInt(part, 16) / 255;
    return srgb <= 0.04045 ? srgb / 12.92 : ((srgb + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
}

function contrast(first, second) {
  const [lighter, darker] = [luminance(first), luminance(second)].sort((a, b) => b - a);
  return (lighter + 0.05) / (darker + 0.05);
}

const pairs = [
  ["body text", "text", "surface", 4.5],
  ["secondary text", "muted", "surface", 4.5],
  ["heading", "heading", "surface", 4.5],
  ["header brand", "spark-template-hlink", "spark-navy", 4.5],
  ["header visited brand", "spark-template-followed-hlink", "spark-navy", 4.5],
  ["primary action", "on-primary", "primary", 4.5],
  ["primary hover", "spark-light", "primary-dark", 4.5],
  ["secondary action", "primary", "surface", 4.5],
  ["danger action", "spark-light", "danger", 4.5],
  ["danger hover", "spark-light", "danger-hover", 4.5],
  ["input border", "border", "surface", 3],
  ["content link", "link", "surface", 4.5],
  ["visited link", "link-visited", "surface", 4.5],
  ["table header", "spark-light", "spark-navy", 4.5],
  ["pending status", "text", "status-pending-bg", 4.5],
  ["approved status", "text", "status-approved-bg", 4.5],
  ["rejected status", "spark-light", "status-rejected-bg", 4.5],
  ["cancelled status", "text", "status-cancelled-bg", 4.5],
  ["error message", "error-ink", "error-bg", 4.5],
  ["conflict message", "conflict-ink", "conflict-bg", 4.5],
];

let failures = 0;
for (const [name, expected] of Object.entries(sourcePalette)) {
  if (resolve(name) !== expected) {
    failures += 1;
    process.stderr.write(`Source palette mismatch: ${name}\n`);
  }
}
for (const [name, foreground, background, minimum] of pairs) {
  const ratio = contrast(resolve(foreground), resolve(background));
  const pass = ratio >= minimum;
  process.stdout.write(`${pass ? "PASS" : "FAIL"} ${name}: ${ratio.toFixed(2)}:1 (minimum ${minimum}:1)\n`);
  if (!pass) failures += 1;
}
process.stdout.write(`Source colors: ${Object.keys(sourcePalette).length}; contrast pairs: ${pairs.length}; failures: ${failures}\n`);
if (failures) process.exitCode = 1;
