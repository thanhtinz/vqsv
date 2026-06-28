"use client";

import { useCallback, useEffect, useState } from "react";
import type { Account } from "./types";

const TOKEN_KEY = "vqsv_token";
const ACCOUNT_KEY = "vqsv_account";
const AUTH_EVENT = "vqsv-auth-change";

function isBrowser(): boolean {
  return typeof window !== "undefined";
}

export function getToken(): string | null {
  if (!isBrowser()) return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function getAccount(): Account | null {
  if (!isBrowser()) return null;
  const raw = window.localStorage.getItem(ACCOUNT_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Account;
  } catch {
    return null;
  }
}

function emitChange(): void {
  if (!isBrowser()) return;
  window.dispatchEvent(new Event(AUTH_EVENT));
}

export function setToken(token: string, account: Account): void {
  if (!isBrowser()) return;
  window.localStorage.setItem(TOKEN_KEY, token);
  window.localStorage.setItem(ACCOUNT_KEY, JSON.stringify(account));
  emitChange();
}

export function setAccount(account: Account): void {
  if (!isBrowser()) return;
  window.localStorage.setItem(ACCOUNT_KEY, JSON.stringify(account));
  emitChange();
}

export function clearToken(): void {
  if (!isBrowser()) return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(ACCOUNT_KEY);
  emitChange();
}

export interface UseAuth {
  token: string | null;
  account: Account | null;
  isAuthenticated: boolean;
  ready: boolean;
  login: (token: string, account: Account) => void;
  logout: () => void;
  refreshAccount: (account: Account) => void;
}

/**
 * React hook exposing the current auth state from localStorage.
 * `ready` flips to true after the first client-side read so guarded
 * pages can avoid flashing while hydrating.
 */
export function useAuth(): UseAuth {
  const [token, setTokenState] = useState<string | null>(null);
  const [account, setAccountState] = useState<Account | null>(null);
  const [ready, setReady] = useState(false);

  const sync = useCallback(() => {
    setTokenState(getToken());
    setAccountState(getAccount());
  }, []);

  useEffect(() => {
    sync();
    setReady(true);
    const onChange = () => sync();
    window.addEventListener(AUTH_EVENT, onChange);
    window.addEventListener("storage", onChange);
    return () => {
      window.removeEventListener(AUTH_EVENT, onChange);
      window.removeEventListener("storage", onChange);
    };
  }, [sync]);

  const login = useCallback((t: string, a: Account) => {
    setToken(t, a);
  }, []);

  const logout = useCallback(() => {
    clearToken();
  }, []);

  const refreshAccount = useCallback((a: Account) => {
    setAccount(a);
  }, []);

  return {
    token,
    account,
    isAuthenticated: !!token,
    ready,
    login,
    logout,
    refreshAccount,
  };
}
