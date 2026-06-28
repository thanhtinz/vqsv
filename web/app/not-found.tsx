import Button from "@/components/Button";

export default function NotFound() {
  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-24 text-center">
      <p className="text-6xl font-black text-brand">404</p>
      <h1 className="mt-4 text-2xl font-bold text-white">
        Không tìm thấy trang
      </h1>
      <p className="mt-2 text-white/60">
        Trang bạn tìm có thể đã bị xóa hoặc đường dẫn không chính xác.
      </p>
      <div className="mt-6">
        <Button href="/">Về trang chủ</Button>
      </div>
    </div>
  );
}
