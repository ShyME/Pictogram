import { vi } from 'vitest';

type Listener = (event: MessageEvent | CloseEvent) => void;

export class FakeWebSocket {
  static instances: FakeWebSocket[] = [];

  readonly url: string;
  readonly protocols: string[];
  readonly sent: string[] = [];
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

  send(data: string): void {
    this.sent.push(data);
  }

  close(): void {
    this.closed = true;
  }

  open(): void {
    this.dispatch('open', new MessageEvent('open'));
  }

  receive(data: string): void {
    this.dispatch('message', new MessageEvent('message', { data }));
  }

  simulateDrop(code?: number): void {
    this.dispatch('close', new CloseEvent('close', code === undefined ? {} : { code }));
  }

  private dispatch(type: string, event: MessageEvent | CloseEvent): void {
    const listeners = this.listeners.get(type) ?? [];
    for (const listener of listeners) listener(event);
  }
}

export function stubWebSocket(): typeof FakeWebSocket {
  FakeWebSocket.instances = [];
  vi.stubGlobal('WebSocket', FakeWebSocket);
  return FakeWebSocket;
}
