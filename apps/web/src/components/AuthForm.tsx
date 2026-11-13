import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api";
import { ErrorBanner } from "./States";

interface AuthFormProps {
  isRegister?: boolean;
}

export function AuthForm({ isRegister = false }: AuthFormProps) {
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [theme, setTheme] = useState<"light" | "dark">(() => {
    return (document.documentElement.getAttribute("data-theme") as "light" | "dark") || "light";
  });

  const toggleTheme = () => {
    const next = theme === "dark" ? "light" : "dark";
    setTheme(next);
    document.documentElement.setAttribute("data-theme", next);
    localStorage.setItem("veil_theme", next);
  };

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true);
    setError("");

    try {
      if (isRegister) {
        await api.register(data);
      } else {
        await api.login(data);
      }
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Authentication failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-container">
      <form onSubmit={handleSubmit} className="auth-card">
        <div className="auth-header">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <p className="eyebrow">VEIL MESSENGER</p>
            <button
              type="button"
              className="btn-icon"
              onClick={toggleTheme}
              title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
              aria-label="Toggle theme"
            >
              {theme === "dark" ? "☀️" : "🌙"}
            </button>
          </div>
          <h1>{isRegister ? "Create your account" : "Welcome back"}</h1>
          <p className="muted">
            {isRegister
              ? "Start private, real-time one-to-one conversations."
              : "Sign in to access your conversations and messages."}
          </p>
        </div>

        {error && <ErrorBanner message={error} onDismiss={() => setError("")} />}

        <div className="form-fields">
          {isRegister && (
            <>
              <label>
                Full Name
                <input
                  name="name"
                  type="text"
                  required
                  placeholder="Alex Rivers"
                  minLength={2}
                  maxLength={60}
                  autoComplete="name"
                />
              </label>

              <label>
                Username
                <input
                  name="username"
                  type="text"
                  required
                  placeholder="alex_rivers"
                  pattern="[A-Za-z0-9_]{3,30}"
                  title="3-30 letters, numbers, or underscores"
                  autoComplete="username"
                />
              </label>
            </>
          )}

          <label>
            Email Address
            <input
              name="email"
              type="email"
              required
              placeholder="alex@example.com"
              autoComplete="email"
            />
          </label>

          <label>
            Password
            <input
              name="password"
              type="password"
              required
              minLength={8}
              placeholder="Minimum 8 characters"
              autoComplete={isRegister ? "new-password" : "current-password"}
            />
          </label>
        </div>

        <button className="btn-primary full-width" type="submit" disabled={busy}>
          {busy ? "Please wait…" : isRegister ? "Create Account" : "Sign In"}
        </button>

        <p className="auth-footer muted small">
          {isRegister ? (
            <>
              Already have an account? <Link to="/login">Sign in</Link>
            </>
          ) : (
            <>
              New to Private Messenger? <Link to="/register">Create an account</Link>
            </>
          )}
        </p>
      </form>
    </main>
  );
}
