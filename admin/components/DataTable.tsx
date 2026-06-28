"use client";

import { ReactNode } from "react";

export interface Column<T> {
  /** key dùng cho React, có thể là field hoặc tên tuỳ ý */
  key: string;
  header: string;
  /** render giá trị ô; nếu bỏ qua sẽ lấy row[key] */
  render?: (row: T) => ReactNode;
  className?: string;
  width?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  /** cột thao tác (sửa/xoá...) render bên phải */
  actions?: (row: T) => ReactNode;
  empty?: string;
}

export default function DataTable<T extends Record<string, any>>({
  columns,
  rows,
  rowKey,
  actions,
  empty = "Không có dữ liệu",
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-xl border border-surface-border bg-surface-card">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-surface-border text-xs uppercase tracking-wide text-gray-400">
            {columns.map((c) => (
              <th
                key={c.key}
                className={`px-4 py-3 font-medium ${c.className || ""}`}
                style={c.width ? { width: c.width } : undefined}
              >
                {c.header}
              </th>
            ))}
            {actions && (
              <th className="px-4 py-3 text-right font-medium">Thao tác</th>
            )}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + (actions ? 1 : 0)}
                className="px-4 py-10 text-center text-gray-500"
              >
                {empty}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr
                key={rowKey(row)}
                className="border-b border-surface-border/60 last:border-0 hover:bg-surface-hover/50"
              >
                {columns.map((c) => (
                  <td
                    key={c.key}
                    className={`px-4 py-2.5 align-middle text-gray-200 ${
                      c.className || ""
                    }`}
                  >
                    {c.render ? c.render(row) : (row[c.key] ?? "—")}
                  </td>
                ))}
                {actions && (
                  <td className="px-4 py-2.5 text-right">
                    <div className="flex justify-end gap-1.5">
                      {actions(row)}
                    </div>
                  </td>
                )}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
