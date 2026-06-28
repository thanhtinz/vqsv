"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { apiPost, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { Input } from "@/components/Input";
import Button from "@/components/Button";
import { ErrorMessage } from "@/components/Loading";
import type { AuthResponse } from "@/lib/types";

export default function RegisterPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    if (password !== confirm) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }
    if (password.length < 6) {
      setError("Mật khẩu phải có ít nhất 6 ký tự.");
      return;
    }
    setLoading(true);
    try {
      const res = await apiPost<AuthResponse>("/api/web/auth/register", {
        username: username.trim(),
        password,
        email: email.trim() || undefined,
      });
      login(res.token, res.account);
      router.push("/tai-khoan");
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "Đăng ký thất bại. Vui lòng thử lại."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-md flex-col px-4 py-16">
      <h1 className="text-3xl font-black text-white">Đăng ký</h1>
      <p className="mt-2 text-sm text-white/60">
        Tạo tài khoản VQSV để bắt đầu hành trình của bạn.
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
          id="email"
          label="Email (không bắt buộc)"
          type="email"
          autoComplete="email"
          hint="Dùng để khôi phục tài khoản khi cần."
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <Input
          id="password"
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <Input
          id="confirm"
          label="Xác nhận mật khẩu"
          type="password"
          autoComplete="new-password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          required
        />
        <Button type="submit" disabled={loading} className="mt-2 w-full">
          {loading ? "Đang tạo tài khoản..." : "Đăng ký"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-white/60">
        Đã có tài khoản?{" "}
        <Link href="/login" className="font-semibold text-brand">
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
