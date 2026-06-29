"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface Skill {
  id: number;
  name: string;
  description: string | null;
  element: number;
  requiredLevel: number;
  spCost: number;
  power: number;
  effectId: number | null;
  behaviorFlag: number;
  targetCode: number;
}

const ELEMENTS = ["Mộc", "Thổ", "Thuỷ", "Hoả", "Ma", "Phong", "Điện"];

const columns: Column<Skill>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên" },
  { key: "element", header: "Hệ", render: (s) => ELEMENTS[s.element] ?? s.element },
  { key: "requiredLevel", header: "Cấp" },
  { key: "spCost", header: "SP" },
  { key: "power", header: "Lực" },
  { key: "behaviorFlag", header: "Hành vi", render: (s) => ["Sát thương", "Buff bản thân", "Gây trạng thái"][s.behaviorFlag] ?? s.behaviorFlag },
  { key: "effectId", header: "Hiệu ứng" },
];

export default function SkillsPage() {
  return (
    <CrudResource<Skill>
      title="Kỹ năng"
      description="Quản lý kỹ năng (lực, hệ, hiệu ứng trạng thái)"
      basePath="/api/admin/game/skills"
      rowKey={(s) => s.id}
      columns={columns}
      createLabel="Tạo kỹ năng"
      wide
      transformOut={(f) => ({
        ...f,
        element: Number(f.element),
        behaviorFlag: Number(f.behaviorFlag),
        targetCode: Number(f.targetCode),
        effectId:
          f.effectId === "" || f.effectId === null || f.effectId === undefined
            ? null
            : Number(f.effectId),
      })}
      fields={[
        { name: "name", label: "Tên kỹ năng", required: true },
        { name: "description", label: "Mô tả", type: "textarea" },
        {
          name: "element",
          label: "Hệ",
          type: "select",
          options: ELEMENTS.map((label, value) => ({ value, label: `${value} - ${label}` })),
          defaultValue: 0,
        },
        { name: "requiredLevel", label: "Cấp yêu cầu", type: "number", defaultValue: 1 },
        { name: "spCost", label: "SP tiêu hao", type: "number", defaultValue: 0 },
        { name: "power", label: "Lực (% sát thương, 0 = đòn cơ bản 100%)", type: "number", defaultValue: 0 },
        {
          name: "behaviorFlag",
          label: "Hành vi",
          type: "select",
          options: [
            { value: 0, label: "0 - Sát thương trực tiếp" },
            { value: 1, label: "1 - Buff bản thân" },
            { value: 2, label: "2 - Gây trạng thái cho địch" },
          ],
          defaultValue: 0,
        },
        {
          name: "effectId",
          label: "Hiệu ứng (theo hành vi)",
          type: "select",
          hint: "Gây trạng thái: 0=Đốt cháy, 1=Mê muội, 2=Quấn quanh, 3=Thực loại. Buff bản thân: 0=Tăng thủ, 1=Tăng công, 2=Phản đòn, 3=Hồi máu.",
          options: [
            { value: "", label: "(không)" },
            { value: 0, label: "0" },
            { value: 1, label: "1" },
            { value: 2, label: "2" },
            { value: 3, label: "3" },
          ],
        },
        {
          name: "targetCode",
          label: "Mục tiêu",
          type: "select",
          options: [
            { value: 0, label: "0 - Đối thủ" },
            { value: 1, label: "1 - Bản thân" },
          ],
          defaultValue: 0,
        },
      ]}
    />
  );
}
