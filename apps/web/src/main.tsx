import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { App } from "./App";
import "./styles.css";

// Immediate theme initialization to prevent flash
const initialTheme = localStorage.getItem("veil_theme") || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
document.documentElement.setAttribute("data-theme", initialTheme);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);
