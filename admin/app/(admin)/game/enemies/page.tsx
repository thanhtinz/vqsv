"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface Enemy {
  id: number;
  name: string;
  spriteId: number;
  level: number;
  hp: number;
  atk: number;
  def: number;
  spd: number;
  expReward: number;
  goldReward: number;
  mapId: number;
}

const columns: Column<Enemy>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên" },
  { key: "level", header: "Cấp" },
  { key: "hp", header: "HP" },
  { key: "atk", header: "ATK" },
  { key: "def", header: "DEF" },
  { key: "spd", header: "SPD" },
  { key: "expReward", header: "EXP" },
  { key: "goldReward", header: "Vàng" },
  { key: "mapId", header: "Bản đồ" },
];

export default function EnemiesPage() {
  return (
    <CrudResource<Enemy>
      title="Quái"
      description="Quản lý quái vật trong game"
      basePath="/api/admin/game/enemies"
      rowKey={(e) => e.id}
      columns={columns}
      createLabel="Tạo quái"
      fields={[
        { name: "name", label: "Tên", required: true },
        { name: "spriteId", label: "Sprite ID", type: "number", defaultValue: 0 },
        { name: "level", label: "Cấp", type: "number", defaultValue: 1 },
        { name: "hp", label: "HP", type: "number", defaultValue: 0 },
        { name: "atk", label: "ATK", type: "number", defaultValue: 0 },
        { name: "def", label: "DEF", type: "number", defaultValue: 0 },
        { name: "spd", label: "SPD", type: "number", defaultValue: 0 },
        { name: "expReward", label: "EXP thưởng", type: "number", defaultValue: 0 },
        { name: "goldReward", label: "Vàng thưởng", type: "number", defaultValue: 0 },
        { name: "mapId", label: "Bản đồ (mapId)", type: "number", defaultValue: 0 },
      ]}
    />
  );
}
