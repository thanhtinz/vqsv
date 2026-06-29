// Shared API types for VQSV web

export type Role = "USER" | "ADMIN" | string;

export interface Account {
  id: number;
  username: string;
  email?: string | null;
  role: Role;
  balanceXu: number;
  totalTopup: number;
  status: string;
}

export interface AuthResponse {
  token: string;
  account: Account;
}

export interface NewsItem {
  id: number;
  title: string;
  slug: string;
  summary: string;
  bannerUrl?: string | null;
  category?: string | null;
  publishedAt: string;
}

export interface NewsDetail extends NewsItem {
  body: string;
}

export interface EventItem {
  id: number;
  title: string;
  body: string;
  bannerUrl?: string | null;
  startsAt: string;
  endsAt: string;
  active: boolean;
}

export interface ServerItem {
  id: number;
  code: string;
  name: string;
  status: string;
  crossGroup?: string | null;
  playerCount: number;
}

export interface LeaderboardRow {
  rank: number;
  name: string;
  level: number;
  exp: number;
  serverCode: string;
}

export interface TopupPackage {
  id: number;
  name: string;
  priceVnd: number;
  xuAmount: number;
  bonusXu: number;
  totalXu: number;
}

export interface ShopProduct {
  id: number;
  name: string;
  description?: string | null;
  iconId?: number | null;
  priceXu: number;
  stock: number;
}

export interface CharacterItem {
  id: number;
  serverId: number;
  serverName: string;
  name: string;
  level: number;
  kimTien: number;
  huyChuong: number;
}

export interface TransactionItem {
  id: number;
  kind: string;
  amountVnd: number;
  xu: number;
  status: string;
  provider?: string | null;
  detail?: string | null;
  createdAt: string;
}

export interface RedeemResult {
  success: boolean;
  message: string;
  reward?: string | null;
}

export interface TopupOrderResult {
  transactionId: number | string;
  amountVnd: number;
  payUrl: string;
  status: string;
  provider?: string;
  bankAccount?: string | null;
  bankCode?: string | null;
  accountHolder?: string | null;
  transferContent?: string | null;
  qrUrl?: string | null;
}

export interface ShopBuyResult {
  success: boolean;
  message: string;
  reward?: string | null;
}
