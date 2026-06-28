"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { Loading } from "./Loading";

/**
 * Wraps protected pages. Redirects to /login (with a redirect query)
 * once we know the user is not authenticated.
 */
export function AuthGuard({
  children,
  redirectTo = "/login",
}: {
  children: React.ReactNode;
  redirectTo?: string;
}) {
  const { isAuthenticated, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && !isAuthenticated) {
      router.replace(redirectTo);
    }
  }, [ready, isAuthenticated, router, redirectTo]);

  if (!ready) {
    return <Loading label="Đang kiểm tra đăng nhập..." />;
  }

  if (!isAuthenticated) {
    return <Loading label="Đang chuyển tới trang đăng nhập..." />;
  }

  return <>{children}</>;
}

export default AuthGuard;
