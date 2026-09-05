import { vi } from 'vitest';

type Listener = () => void;

export class FakeWebSocket {
  static instances: FakeWebSocket[] = [];

  readonly url: string;
  readonly protocols: string[];
  closed = false;
  private readonly listeners = new Map<string, Set<Listener>>();

  constructor(url: string, protocols: string[]) {
    this.url = url;
    this.protocols = protocols;
    FakeWebSocket.instances.push(this);
  }

  addEventListener(type: string, listener: Listener): void {
    const forType = this.listeners.get(type) ?? new Set<Listener>();
    forType.add(listener);
    this.listeners.set(type, forType);
  }

  close(): void {
    this.closed = true;
  }

  open(): void {
    const listeners = this.listeners.get('open') ?? [];
    for (const listener of listeners) listener();
  }

  simulateDrop(): void {
    const listeners = this.listeners.get('close') ?? [];
    for (const listener of listeners) listener();
  }
}

export function stubWebSocket(): typeof FakeWebSocket {
  FakeWebSocket.instances = [];
  vi.stubGlobal('WebSocket', FakeWebSocket);
  return FakeWebSocket;
}
