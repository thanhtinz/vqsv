"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface Npc {
  id: number;
  name: string;
  spriteId: number;
  npcType: string;
  mapId: number;
  posX: number;
  posY: number;
  dialogKey: string;
}

const columns: Column<Npc>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên" },
  { key: "npcType", header: "Loại" },
  { key: "mapId", header: "Bản đồ" },
  { key: "pos", header: "Toạ độ", render: (n) => `(${n.posX}, ${n.posY})` },
  { key: "dialogKey", header: "Khoá hội thoại" },
];

export default function NpcsPage() {
  return (
    <CrudResource<Npc>
      title="NPC"
      description="Quản lý NPC trong game"
      basePath="/api/admin/game/npcs"
      rowKey={(n) => n.id}
      columns={columns}
      createLabel="Tạo NPC"
      fields={[
        { name: "name", label: "Tên", required: true },
        { name: "spriteId", label: "Sprite ID", type: "number", defaultValue: 0 },
        { name: "npcType", label: "Loại NPC", placeholder: "vd: SHOP, QUEST" },
        { name: "mapId", label: "Bản đồ (mapId)", type: "number", required: true, defaultValue: 0 },
        { name: "posX", label: "Toạ độ X", type: "number", defaultValue: 0 },
        { name: "posY", label: "Toạ độ Y", type: "number", defaultValue: 0 },
        { name: "dialogKey", label: "Khoá hội thoại" },
      ]}
    />
  );
}
