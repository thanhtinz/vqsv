"use client";

import { Suspense, useState, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { apiPost, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Input } from "@/components/Input";
import Button from "@/components/Button";
import { ErrorMessage, Loading } from "@/components/Loading";
import type { AuthResponse } from "@/lib/types";

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const { login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const redirectTo = params.get("redirect") || "/tai-khoan";

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await apiPost<AuthResponse>("/api/web/auth/login", {
        username: username.trim(),
        password,
      });
      login(res.token, res.account);
      router.push(redirectTo);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "Đăng nhập thất bại. Vui lòng thử lại."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-md flex-col px-4 py-16">
      <h1 className="text-3xl font-black text-white">Đăng nhập</h1>
      <p className="mt-2 text-sm text-white/60">
        Đăng nhập để nạp thẻ, nhận giftcode và quản lý nhân vật.
      </p>

      <form onSubmit={onSubmit} className="mt-8 flex flex-col gap-4">
        {error && <ErrorMessage message={error} />}
        <Input
          id="username"
          label="Tên đăng nhập"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <Input
          id="password"
          label="Mật khẩu"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <Button type="submit" disabled={loading} className="mt-2 w-full">
          {loading ? "Đang đăng nhập..." : "Đăng nhập"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-white/60">
        Chưa có tài khoản?{" "}
        <Link href="/register" className="font-semibold text-brand">
          Đăng ký ngay
        </Link>
      </p>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<Loading label="Đang tải..." />}>
      <LoginForm />
    </Suspense>
  );
}
