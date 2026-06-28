import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./lib/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        bg: {
          DEFAULT: "#0b0e16",
          soft: "#121624",
          card: "#161b2c",
        },
        brand: {
          DEFAULT: "#f5a623",
          dark: "#c97f12",
          light: "#ffce6b",
        },
        accent: {
          DEFAULT: "#7c4dff",
          light: "#a47bff",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "Segoe UI", "Roboto", "sans-serif"],
      },
      boxShadow: {
        glow: "0 0 30px rgba(245, 166, 35, 0.25)",
      },
      backgroundImage: {
        "hero-grad":
          "radial-gradient(120% 120% at 50% 0%, rgba(124,77,255,0.25) 0%, rgba(11,14,22,0) 55%)",
      },
    },
  },
  plugins: [],
};

export default config;
