"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface WildPet {
  id: number;
  mapId: number;
  templateId: number;
  minLevel: number;
  maxLevel: number;
  spawnRate: number;
}

const columns: Column<WildPet>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "mapId", header: "Bản đồ" },
  { key: "templateId", header: "Mẫu sủng vật" },
  { key: "level", header: "Cấp", render: (w) => `${w.minLevel} - ${w.maxLevel}` },
  { key: "spawnRate", header: "Tỉ lệ xuất hiện" },
];

export default function WildPetsPage() {
  return (
    <CrudResource<WildPet>
      title="Sủng vật hoang"
      description="Cấu hình sủng vật xuất hiện ngoài tự nhiên"
      basePath="/api/admin/game/wild-pets"
      rowKey={(w) => w.id}
      columns={columns}
      createLabel="Thêm spawn"
      fields={[
        { name: "mapId", label: "Bản đồ (mapId)", type: "number", required: true, defaultValue: 0 },
        { name: "templateId", label: "Mẫu sủng vật (templateId)", type: "number", required: true, defaultValue: 0 },
        { name: "minLevel", label: "Cấp tối thiểu", type: "number", defaultValue: 1 },
        { name: "maxLevel", label: "Cấp tối đa", type: "number", defaultValue: 1 },
        { name: "spawnRate", label: "Tỉ lệ xuất hiện", type: "number", defaultValue: 0 },
      ]}
    />
  );
}
