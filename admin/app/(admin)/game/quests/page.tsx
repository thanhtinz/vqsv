"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface Quest {
  id: number;
  name: string;
  giverNpcId: number;
  description: string;
  objectiveType: string;
  objectiveTarget: number;
  objectiveCount: number;
  rewardGold: number;
  rewardExp: number;
  rewardItemId: number | null;
  requiredLevel: number;
  prerequisiteQuestId: number | null;
}

const columns: Column<Quest>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên" },
  { key: "giverNpcId", header: "NPC giao" },
  { key: "objectiveType", header: "Loại" },
  { key: "obj", header: "Mục tiêu", render: (q) => `${q.objectiveTarget} x${q.objectiveCount}` },
  { key: "reward", header: "Thưởng", render: (q) => `${q.rewardGold} xu / ${q.rewardExp} EXP` },
  { key: "requiredLevel", header: "Cấp YC" },
];

export default function QuestsPage() {
  return (
    <CrudResource<Quest>
      title="Nhiệm vụ"
      description="Quản lý nhiệm vụ do NPC giao"
      basePath="/api/admin/game/quests"
      rowKey={(q) => q.id}
      columns={columns}
      createLabel="Tạo nhiệm vụ"
      fields={[
        { name: "name", label: "Tên nhiệm vụ", required: true },
        { name: "giverNpcId", label: "NPC giao (id)", type: "number", required: true, defaultValue: 0 },
        { name: "description", label: "Mô tả", type: "textarea" },
        {
          name: "objectiveType",
          label: "Loại mục tiêu",
          type: "select",
          options: [
            { value: "KILL_MOB", label: "Diệt quái (target = id sủng vật)" },
            { value: "COLLECT_ITEM", label: "Thu thập (target = id vật phẩm)" },
            { value: "REACH_LEVEL", label: "Đạt cấp (target = cấp)" },
          ],
          defaultValue: "KILL_MOB",
        },
        { name: "objectiveTarget", label: "Mục tiêu (target id / cấp)", type: "number", defaultValue: 0 },
        { name: "objectiveCount", label: "Số lượng", type: "number", defaultValue: 1 },
        { name: "rewardGold", label: "Thưởng kim tiền", type: "number", defaultValue: 0 },
        { name: "rewardExp", label: "Thưởng EXP", type: "number", defaultValue: 0 },
        { name: "rewardItemId", label: "Thưởng vật phẩm (id, để trống nếu không)", type: "number" },
        { name: "requiredLevel", label: "Cấp yêu cầu", type: "number", defaultValue: 1 },
        { name: "prerequisiteQuestId", label: "Nhiệm vụ tiên quyết (id, tuỳ chọn)", type: "number" },
      ]}
    />
  );
}
