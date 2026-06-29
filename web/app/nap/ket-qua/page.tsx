"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";

function Result() {
  const params = useSearchParams();
  const success = params.get("success") === "true";

  return (
    <div className="mx-auto max-w-lg px-4 py-16 text-center">
      <div
        className={`mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full text-3xl ${
          success ? "bg-emerald-500/15 text-emerald-400" : "bg-red-500/15 text-red-400"
        }`}
      >
        {success ? "✓" : "✕"}
      </div>
      <h1 className="text-2xl font-black text-white">
        {success ? "Nạp thành công!" : "Thanh toán chưa hoàn tất"}
      </h1>
      <p className="mt-2 text-white/60">
        {success
          ? "Xu đã được cộng vào tài khoản của bạn. Cảm ơn bạn đã ủng hộ game!"
          : "Giao dịch bị huỷ hoặc chưa được xác nhận. Nếu bạn đã bị trừ tiền, vui lòng liên hệ hỗ trợ."}
      </p>
      <div className="mt-8 flex justify-center gap-3">
        <Link
          href="/tai-khoan"
          className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-light"
        >
          Xem tài khoản
        </Link>
        <Link
          href="/nap"
          className="rounded-lg border border-white/15 px-5 py-2.5 text-sm font-semibold text-white/80 hover:border-brand/60 hover:text-brand"
        >
          Nạp tiếp
        </Link>
      </div>
    </div>
  );
}

export default function TopupResultPage() {
  return (
    <Suspense fallback={<div className="px-4 py-16 text-center text-white/60">Đang tải...</div>}>
      <Result />
    </Suspense>
  );
}
