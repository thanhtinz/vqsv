"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { apiPost } from "@/lib/api";
import { Account, isAdmin, setAuth } from "@/lib/auth";
import Button from "@/components/Button";
import FormField, { TextInput } from "@/components/FormField";
import Alert from "@/components/Alert";

interface LoginResponse {
  token: string;
  account: Account;
}

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    if (!username || !password) {
      setError("Vui lòng nhập tài khoản và mật khẩu");
      return;
    }
    setLoading(true);
    try {
      const res = await apiPost<LoginResponse>("/api/web/auth/login", {
        username,
        password,
      });
      if (!res.token || !res.account) {
        setError("Phản hồi đăng nhập không hợp lệ");
        return;
      }
      if (!isAdmin(res.account)) {
        setError("Tài khoản này không có quyền truy cập bảng quản trị");
        return;
      }
      setAuth(res.token, res.account);
      router.replace("/");
    } catch (err: any) {
      setError(err.message || "Đăng nhập thất bại");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-brand text-lg font-bold text-white">
            V
          </div>
          <h1 className="text-xl font-bold text-white">Vương Quốc Sủng Vật</h1>
          <p className="mt-1 text-sm text-gray-400">Đăng nhập quản trị</p>
        </div>

        <form
          onSubmit={onSubmit}
          className="rounded-xl border border-surface-border bg-surface-card p-6"
        >
          {error && <Alert kind="error">{error}</Alert>}

          <FormField label="Tài khoản" htmlFor="username" required>
            <TextInput
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Tên đăng nhập"
              autoComplete="username"
            />
          </FormField>

          <FormField label="Mật khẩu" htmlFor="password" required>
            <TextInput
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Mật khẩu"
              autoComplete="current-password"
            />
          </FormField>

          <Button
            type="submit"
            className="mt-2 w-full"
            disabled={loading}
          >
            {loading ? "Đang đăng nhập..." : "Đăng nhập"}
          </Button>

          <p className="mt-4 text-center text-[11px] text-gray-500">
            Chỉ tài khoản có quyền ADMIN hoặc GM mới truy cập được.
          </p>
        </form>
      </div>
    </div>
  );
}
