"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface GameMap {
  id: number;
  name: string;
  width: number;
  height: number;
  tilesetId: number;
  bgmId: number;
  isPvp: boolean;
  minLevel: number;
}

const columns: Column<GameMap>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên bản đồ" },
  { key: "size", header: "Kích thước", render: (m) => `${m.width} x ${m.height}` },
  { key: "tilesetId", header: "Tileset" },
  { key: "bgmId", header: "BGM" },
  { key: "isPvp", header: "PvP", render: (m) => (m.isPvp ? "Có" : "Không") },
  { key: "minLevel", header: "Cấp tối thiểu" },
];

export default function MapsPage() {
  return (
    <CrudResource<GameMap>
      title="Bản đồ"
      description="Quản lý bản đồ trong game"
      basePath="/api/admin/game/maps"
      rowKey={(m) => m.id}
      columns={columns}
      createLabel="Tạo bản đồ"
      fields={[
        { name: "name", label: "Tên bản đồ", required: true },
        { name: "width", label: "Chiều rộng", type: "number", defaultValue: 0 },
        { name: "height", label: "Chiều cao", type: "number", defaultValue: 0 },
        { name: "tilesetId", label: "Tileset ID", type: "number", defaultValue: 0 },
        { name: "bgmId", label: "BGM ID", type: "number", defaultValue: 0 },
        { name: "minLevel", label: "Cấp tối thiểu", type: "number", defaultValue: 0 },
        { name: "isPvp", label: "Cho phép PvP", type: "checkbox", defaultValue: false },
      ]}
    />
  );
}
