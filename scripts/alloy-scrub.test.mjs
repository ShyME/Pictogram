// Guards the PII-scrubbing stages in alloy/config.alloy (ADR-0016): email addresses and IP
// addresses must not leave the box in shipped log lines. Reads the real `stage.replace`
// patterns out of the committed config — rather than hardcoding a parallel copy — so an
// edit to the config that weakens scrubbing fails this test instead of only being caught by
// manual inspection. Zero deps: `node --test`.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';
import assert from 'node:assert/strict';

const configPath = fileURLToPath(new URL('../alloy/config.alloy', import.meta.url));
const config = readFileSync(configPath, 'utf8');

// Undoes the Alloy (Go-style) string-literal escaping of `expression`/`replace` values —
// only `\\` -> `\` is needed for the patterns this file writes.
function unescape(raw) {
  return raw.replaceAll('\\\\', '\\');
}

// Pulls out each `expression = "..."` / `replace = "..."` pair directly, rather than
// bounding to a `stage.replace { ... }` block first — a `{2,}` quantifier inside the
// quoted expression contains a literal `}` that would close a naive block match early.
function scrubStages() {
  const pairs = [
    ...config.matchAll(
      /expression\s*=\s*"((?:[^"\\]|\\.)*)"\s*\n\s*replace\s*=\s*"((?:[^"\\]|\\.)*)"/g,
    ),
  ];
  assert.ok(pairs.length > 0, 'expected at least one expression/replace pair in alloy/config.alloy');
  return pairs.map(([, expression]) => new RegExp(unescape(expression), 'g'));
}

function scrub(line) {
  let out = line;
  for (const pattern of scrubStages()) out = out.replace(pattern, '[REDACTED]');
  return out;
}

test('an ECS log line carrying an email address is scrubbed', () => {
  const line = '{"@timestamp":"2026-09-17T10:23:45.123Z","message":"login by alice@example.com"}';
  assert.ok(!scrub(line).includes('alice@example.com'));
});

test('an ECS log line carrying an IPv4 address is scrubbed', () => {
  const line = '{"@timestamp":"2026-09-17T10:23:45.123Z","client.ip":"203.0.113.42"}';
  assert.ok(!scrub(line).includes('203.0.113.42'));
});

test('an uncompressed IPv6 address is scrubbed', () => {
  const line = '{"client.ip":"2001:0db8:0000:0000:0000:8a2e:0370:7334"}';
  assert.ok(!scrub(line).includes('2001:0db8:0000:0000:0000:8a2e:0370:7334'));
});

test('a "::"-compressed IPv6 address is scrubbed', () => {
  for (const ip of ['fe80::1', '::1', '2001:db8::8a2e:370:7334']) {
    const line = `{"client.ip":"${ip}"}`;
    assert.ok(!scrub(line).includes(ip), `${ip} was not scrubbed`);
  }
});

test('an ISO-8601 timestamp is not mistaken for an IPv6 address', () => {
  const line = '{"@timestamp":"2026-09-17T10:23:45.123Z","message":"ok"}';
  assert.equal(scrub(line), line);
});

test('a user UUID — the correlation field this ADR keeps instead of PII — survives scrubbing', () => {
  const line = '{"user.id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","message":"posted"}';
  assert.equal(scrub(line), line);
});

test('a semantic version number is not mistaken for an IPv4 address', () => {
  const line = '{"service.version":"1.19.2"}';
  assert.equal(scrub(line), line);
});
