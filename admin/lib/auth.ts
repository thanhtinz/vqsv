"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

const TOKEN_KEY = "vqsv_admin_token";
const ACCOUNT_KEY = "vqsv_admin_account";

export interface Account {
  id: number;
  username: string;
  email: string;
  role: string;
  balanceXu: number;
  totalTopup: number;
  status: string;
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function getAccount(): Account | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(ACCOUNT_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Account;
  } catch {
    return null;
  }
}

export function setAuth(token: string, account: Account): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(TOKEN_KEY, token);
  window.localStorage.setItem(ACCOUNT_KEY, JSON.stringify(account));
}

export function clearAuth(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(ACCOUNT_KEY);
}

export function isAdmin(account: Account | null): boolean {
  if (!account) return false;
  return account.role === "ADMIN" || account.role === "GM";
}

export function isAuthed(): boolean {
  return !!getToken() && isAdmin(getAccount());
}

/**
 * Hook bảo vệ trang admin. Nếu chưa đăng nhập hoặc không có quyền,
 * chuyển hướng về /login. Trả về { ready, account }.
 */
export function useAdminGuard() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [account, setAccount] = useState<Account | null>(null);

  useEffect(() => {
    const token = getToken();
    const acc = getAccount();
    if (!token || !isAdmin(acc)) {
      router.replace("/login");
      return;
    }
    setAccount(acc);
    setReady(true);
  }, [router]);

  return { ready, account };
}
