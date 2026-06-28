"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface Item {
  id: number;
  name: string;
  itemType: string;
  effectVal: number;
  iconId: number;
  description: string;
}

const columns: Column<Item>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên" },
  { key: "itemType", header: "Loại" },
  { key: "effectVal", header: "Giá trị hiệu ứng" },
  { key: "iconId", header: "Icon" },
];

export default function ItemsPage() {
  return (
    <CrudResource<Item>
      title="Vật phẩm"
      description="Quản lý vật phẩm trong game"
      basePath="/api/admin/game/items"
      rowKey={(i) => i.id}
      columns={columns}
      createLabel="Tạo vật phẩm"
      fields={[
        { name: "name", label: "Tên", required: true },
        { name: "itemType", label: "Loại vật phẩm", placeholder: "vd: POTION, EQUIP" },
        { name: "effectVal", label: "Giá trị hiệu ứng", type: "number", defaultValue: 0 },
        { name: "iconId", label: "Icon ID", type: "number", defaultValue: 0 },
        { name: "description", label: "Mô tả", type: "textarea" },
      ]}
    />
  );
}
