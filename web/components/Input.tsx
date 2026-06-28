import type { InputHTMLAttributes, SelectHTMLAttributes, ReactNode } from "react";

interface FieldProps {
  label?: string;
  hint?: string;
  error?: string;
  children: ReactNode;
  htmlFor?: string;
}

export function Field({ label, hint, error, children, htmlFor }: FieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label
          htmlFor={htmlFor}
          className="text-sm font-medium text-white/80"
        >
          {label}
        </label>
      )}
      {children}
      {hint && !error && <p className="text-xs text-white/40">{hint}</p>}
      {error && <p className="text-xs text-red-400">{error}</p>}
    </div>
  );
}

const controlCls =
  "w-full rounded-lg border border-white/10 bg-bg-soft px-3 py-2.5 text-sm text-white placeholder-white/30 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand";

interface InputProps
  extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: string;
  error?: string;
}

export function Input({ label, hint, error, id, ...rest }: InputProps) {
  return (
    <Field label={label} hint={hint} error={error} htmlFor={id}>
      <input id={id} className={controlCls} {...rest} />
    </Field>
  );
}

interface SelectProps
  extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  hint?: string;
  error?: string;
  children: ReactNode;
}

export function Select({
  label,
  hint,
  error,
  id,
  children,
  ...rest
}: SelectProps) {
  return (
    <Field label={label} hint={hint} error={error} htmlFor={id}>
      <select id={id} className={controlCls} {...rest}>
        {children}
      </select>
    </Field>
  );
}

export default Input;
