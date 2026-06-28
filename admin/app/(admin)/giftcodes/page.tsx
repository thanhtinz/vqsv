"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";
import { REWARD_HINT, validateRewardJson } from "@/lib/reward";
import { formatDate } from "@/lib/format";

interface Giftcode {
  id: number;
  code: string;
  description: string;
  rewardJson: string;
  maxUses: number;
  usedCount: number;
  perAccount: number;
  serverId: number;
  startsAt: string;
  expiresAt: string;
  status: string;
}

const columns: Column<Giftcode>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "code", header: "Mã code" },
  { key: "description", header: "Mô tả" },
  {
    key: "uses",
    header: "Đã dùng / Tối đa",
    render: (g) => `${g.usedCount ?? 0} / ${g.maxUses ?? 0}`,
  },
  { key: "perAccount", header: "Mỗi TK" },
  { key: "serverId", header: "Máy chủ" },
  { key: "status", header: "Trạng thái" },
  {
    key: "expiresAt",
    header: "Hết hạn",
    render: (g) => formatDate(g.expiresAt),
  },
];

export default function GiftcodesPage() {
  return (
    <CrudResource<Giftcode>
      title="Giftcode"
      description="Tạo và quản lý mã quà tặng"
      basePath="/api/admin/giftcodes"
      rowKey={(g) => g.id}
      columns={columns}
      createLabel="Tạo giftcode"
      transformOut={(form) => ({
        ...form,
        rewardJson: validateRewardJson(form.rewardJson),
      })}
      fields={[
        { name: "code", label: "Mã code", required: true, placeholder: "vd: WELCOME2026" },
        { name: "description", label: "Mô tả" },
        {
          name: "rewardJson",
          label: "Phần thưởng",
          type: "reward",
          required: true,
          hint: REWARD_HINT,
        },
        { name: "maxUses", label: "Số lượt tối đa", type: "number", defaultValue: 100 },
        { name: "perAccount", label: "Giới hạn mỗi tài khoản", type: "number", defaultValue: 1 },
        { name: "serverId", label: "Máy chủ (0 = tất cả)", type: "number", defaultValue: 0 },
        {
          name: "status",
          label: "Trạng thái",
          type: "select",
          defaultValue: "ACTIVE",
          options: [
            { value: "ACTIVE", label: "ACTIVE" },
            { value: "DISABLED", label: "DISABLED" },
          ],
        },
        { name: "startsAt", label: "Bắt đầu", type: "datetime" },
        { name: "expiresAt", label: "Hết hạn", type: "datetime" },
      ]}
    />
  );
}
