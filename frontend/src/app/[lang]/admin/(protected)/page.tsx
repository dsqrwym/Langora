"use client"
import {useState} from "react";
import {Button} from "@base-ui/react";
import {useAuth} from "@/features/auth";
import {logoutAdmin} from "@/features/auth/session/auth-session";

export default function DashboardPage() {
    const {user} = useAuth();
    const [error, setError] = useState<string | null>(null);
    const [pending, setPending] = useState(false);

    const onLogout = async () => {
        setPending(true);
        setError(null);
        try {
            await logoutAdmin();
            // 同样什么都不做：status → anonymous，<RequireAuth> 自动跳 /login
        } catch {
            setError("退出失败，请重试。");
            setPending(false);
        }
    };

    return (
        <div className="mx-auto flex min-h-svh w-full max-w-3xl flex-col gap-8 p-8">
            <header className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold">
                        欢迎回来，{user?.username ?? "朋友"}
                    </h1>
                    <p className="text-sm text-muted-foreground">{user?.email}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                        角色：{user?.roles.join("、") || "—"}
                    </p>
                </div>
                <Button onClick={onLogout} disabled={pending}>
                    {pending ? "退出中…" : "退出登录"}
                </Button>
            </header>

            {error ? <p>{error}</p> : null}

            <section className="rounded-xl border p-6">
                <h2 className="text-lg font-medium">下一步</h2>
                <p className="mt-2 text-sm text-muted-foreground">
                    把 Vite 模板清掉，开始写真正的业务页面。
                </p>
            </section>
        </div>
    );
};