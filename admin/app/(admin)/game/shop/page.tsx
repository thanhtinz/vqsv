"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";
import { formatNumber } from "@/lib/format";

interface ShopEntry {
  id: number;
  item: { id: number; name: string };
  priceGold: number;
  priceMedal: number;
  sortOrder: number;
}

const columns: Column<ShopEntry>[] = [
  { key: "id", header: "ID", width: "60px" },
  {
    key: "item",
    header: "Vật phẩm",
    render: (s) =>
      s.item ? (
        <span className="text-white">
          {s.item.name}{" "}
          <span className="text-gray-500">#{s.item.id}</span>
        </span>
      ) : (
        "—"
      ),
  },
  { key: "priceGold", header: "Giá vàng", render: (s) => formatNumber(s.priceGold) },
  { key: "priceMedal", header: "Giá huy chương", render: (s) => formatNumber(s.priceMedal) },
  { key: "sortOrder", header: "Thứ tự" },
];

export default function GameShopPage() {
  return (
    <CrudResource<ShopEntry>
      title="Cửa hàng game"
      description="Quản lý vật phẩm bán trong cửa hàng game (vàng / huy chương)"
      basePath="/api/admin/game/shop"
      rowKey={(s) => s.id}
      columns={columns}
      editable
      upsertViaPost
      createLabel="Thêm vật phẩm"
      // GET trả về item lồng nhau; form dùng itemId
      transformIn={(row) => ({
        id: row.id,
        itemId: row.item?.id ?? "",
        priceGold: row.priceGold,
        priceMedal: row.priceMedal,
        sortOrder: row.sortOrder,
      })}
      transformOut={(form) => {
        const payload: any = {
          itemId: Number(form.itemId),
          priceGold: form.priceGold === "" ? undefined : Number(form.priceGold),
          priceMedal:
            form.priceMedal === "" ? undefined : Number(form.priceMedal),
          sortOrder: Number(form.sortOrder) || 0,
        };
        if (form.id) payload.id = Number(form.id);
        return payload;
      }}
      fields={[
        { name: "itemId", label: "ID vật phẩm", type: "number", required: true, defaultValue: "" },
        { name: "priceGold", label: "Giá vàng", type: "number", defaultValue: 0 },
        { name: "priceMedal", label: "Giá huy chương", type: "number", defaultValue: 0 },
        { name: "sortOrder", label: "Thứ tự", type: "number", defaultValue: 0 },
      ]}
    />
  );
}
