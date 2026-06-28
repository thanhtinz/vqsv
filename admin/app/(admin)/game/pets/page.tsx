"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface PetTemplate {
  id: number;
  name: string;
  spriteId: number;
  element: string;
  baseHp: number;
  baseAtk: number;
  baseDef: number;
  baseSpd: number;
  growthHp: number;
  growthAtk: number;
  growthDef: number;
  growthSpd: number;
  catchRate: number;
  evolveInto: number;
  evolveLv: number;
  description: string;
}

const columns: Column<PetTemplate>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên" },
  { key: "element", header: "Hệ" },
  { key: "baseHp", header: "HP" },
  { key: "baseAtk", header: "ATK" },
  { key: "baseDef", header: "DEF" },
  { key: "baseSpd", header: "SPD" },
  { key: "catchRate", header: "Tỉ lệ bắt" },
  { key: "evolveLv", header: "Cấp tiến hoá" },
];

export default function PetsPage() {
  return (
    <CrudResource<PetTemplate>
      title="Sủng vật (mẫu)"
      description="Quản lý mẫu sủng vật / quái"
      basePath="/api/admin/game/pets"
      rowKey={(p) => p.id}
      columns={columns}
      createLabel="Tạo sủng vật"
      fields={[
        { name: "name", label: "Tên", required: true },
        { name: "spriteId", label: "Sprite ID", type: "number", defaultValue: 0 },
        { name: "element", label: "Hệ", placeholder: "vd: Hoả, Thuỷ, Mộc" },
        { name: "baseHp", label: "HP cơ bản", type: "number", defaultValue: 0 },
        { name: "baseAtk", label: "ATK cơ bản", type: "number", defaultValue: 0 },
        { name: "baseDef", label: "DEF cơ bản", type: "number", defaultValue: 0 },
        { name: "baseSpd", label: "SPD cơ bản", type: "number", defaultValue: 0 },
        { name: "growthHp", label: "Tăng trưởng HP", type: "number", defaultValue: 0 },
        { name: "growthAtk", label: "Tăng trưởng ATK", type: "number", defaultValue: 0 },
        { name: "growthDef", label: "Tăng trưởng DEF", type: "number", defaultValue: 0 },
        { name: "growthSpd", label: "Tăng trưởng SPD", type: "number", defaultValue: 0 },
        { name: "catchRate", label: "Tỉ lệ bắt", type: "number", defaultValue: 0 },
        { name: "evolveInto", label: "Tiến hoá thành (ID)", type: "number", defaultValue: 0 },
        { name: "evolveLv", label: "Cấp tiến hoá", type: "number", defaultValue: 0 },
        { name: "description", label: "Mô tả", type: "textarea" },
      ]}
    />
  );
}
