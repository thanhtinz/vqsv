// Typed fetch helpers for the VQSV REST backend.

import { getToken } from "./auth";

export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

interface RequestOptions {
  /** Attach the bearer token from localStorage (client-side only). */
  auth?: boolean;
  /** Next.js fetch cache options for server components. */
  cache?: RequestCache;
  /** Revalidate seconds for ISR-style fetching on the server. */
  revalidate?: number;
  signal?: AbortSignal;
}

function buildHeaders(auth: boolean, hasBody: boolean): HeadersInit {
  const headers: Record<string, string> = {};
  if (hasBody) headers["Content-Type"] = "application/json";
  if (auth) {
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }
  return headers;
}

async function parseError(res: Response): Promise<string> {
  try {
    const data = await res.json();
    if (data && typeof data === "object") {
      if (typeof data.error === "string") return data.error;
      if (typeof data.message === "string") return data.message;
    }
  } catch {
    // ignore parse failure, fall through to status text
  }
  return res.statusText || `Lỗi máy chủ (${res.status})`;
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    throw new ApiError(await parseError(res), res.status);
  }
  if (res.status === 204) return undefined as unknown as T;
  const text = await res.text();
  if (!text) return undefined as unknown as T;
  return JSON.parse(text) as T;
}

function nextOpts(opts?: RequestOptions) {
  const init: RequestInit & { next?: { revalidate?: number } } = {};
  if (opts?.cache) init.cache = opts.cache;
  if (typeof opts?.revalidate === "number") {
    init.next = { revalidate: opts.revalidate };
  }
  if (opts?.signal) init.signal = opts.signal;
  return init;
}

export async function apiGet<T>(
  path: string,
  opts?: RequestOptions
): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "GET",
    headers: buildHeaders(opts?.auth ?? false, false),
    ...nextOpts(opts),
  });
  return handle<T>(res);
}

export async function apiPost<T>(
  path: string,
  body?: unknown,
  opts?: RequestOptions
): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: buildHeaders(opts?.auth ?? false, body !== undefined),
    body: body !== undefined ? JSON.stringify(body) : undefined,
    ...nextOpts(opts),
  });
  return handle<T>(res);
}
