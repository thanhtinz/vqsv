import Link from "next/link";

export function Footer() {
  return (
    <footer className="mt-16 border-t border-white/10 bg-bg-soft">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand font-black text-bg">
              V
            </span>
            <span className="font-extrabold text-white">VQSV</span>
          </div>
          <p className="mt-3 text-sm text-white/50">
            Vương Quốc Sủng Vật — thế giới sủng vật huyền ảo trong tầm tay.
            Thu phục, nuôi dưỡng và chinh phục mọi đấu trường.
          </p>
        </div>

        <div>
          <h3 className="mb-3 text-sm font-semibold text-white">Trò chơi</h3>
          <ul className="space-y-2 text-sm text-white/60">
            <li>
              <Link href="/tin-tuc" className="hover:text-brand">
                Tin tức
              </Link>
            </li>
            <li>
              <Link href="/su-kien" className="hover:text-brand">
                Sự kiện
              </Link>
            </li>
            <li>
              <Link href="/bxh" className="hover:text-brand">
                Bảng xếp hạng
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <h3 className="mb-3 text-sm font-semibold text-white">Người chơi</h3>
          <ul className="space-y-2 text-sm text-white/60">
            <li>
              <Link href="/nap" className="hover:text-brand">
                Nạp thẻ
              </Link>
            </li>
            <li>
              <Link href="/webshop" className="hover:text-brand">
                Webshop
              </Link>
            </li>
            <li>
              <Link href="/giftcode" className="hover:text-brand">
                Giftcode
              </Link>
            </li>
            <li>
              <Link href="/tai-khoan" className="hover:text-brand">
                Tài khoản
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <h3 className="mb-3 text-sm font-semibold text-white">Hỗ trợ</h3>
          <ul className="space-y-2 text-sm text-white/60">
            <li>
              <a
                href="https://github.com/"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-brand"
              >
                Tải game (GitHub)
              </a>
            </li>
            <li>
              <Link href="/login" className="hover:text-brand">
                Đăng nhập
              </Link>
            </li>
            <li>
              <Link href="/register" className="hover:text-brand">
                Đăng ký
              </Link>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-white/10 px-4 py-4 text-center text-xs text-white/40">
        © {new Date().getFullYear()} Vương Quốc Sủng Vật (VQSV). Mọi quyền được
        bảo lưu.
      </div>
    </footer>
  );
}

export default Footer;
