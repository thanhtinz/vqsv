"use client";

import { ReactNode } from "react";

interface FormFieldProps {
  label: string;
  htmlFor?: string;
  hint?: ReactNode;
  required?: boolean;
  children: ReactNode;
}

export default function FormField({
  label,
  htmlFor,
  hint,
  required,
  children,
}: FormFieldProps) {
  return (
    <div className="mb-3">
      <label
        htmlFor={htmlFor}
        className="mb-1 block text-xs font-medium text-gray-300"
      >
        {label}
        {required && <span className="text-red-400"> *</span>}
      </label>
      {children}
      {hint && <p className="mt-1 text-[11px] text-gray-500">{hint}</p>}
    </div>
  );
}

const baseInput =
  "w-full rounded-md border border-surface-border bg-surface px-3 py-2 text-sm text-gray-100 placeholder-gray-500 outline-none focus:border-brand-light focus:ring-1 focus:ring-brand-light";

export function TextInput(
  props: React.InputHTMLAttributes<HTMLInputElement>
) {
  const { className = "", ...rest } = props;
  return <input className={`${baseInput} ${className}`} {...rest} />;
}

export function TextArea(
  props: React.TextareaHTMLAttributes<HTMLTextAreaElement>
) {
  const { className = "", ...rest } = props;
  return (
    <textarea className={`${baseInput} font-mono ${className}`} {...rest} />
  );
}

export function Select(
  props: React.SelectHTMLAttributes<HTMLSelectElement>
) {
  const { className = "", children, ...rest } = props;
  return (
    <select className={`${baseInput} ${className}`} {...rest}>
      {children}
    </select>
  );
}
