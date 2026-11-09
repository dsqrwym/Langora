import {useEffect} from 'react'
import {bootstrapSession} from "@/lib/auth";
import {Route, Routes, useParams} from "react-router-dom";
import {RedirectIfAuthed} from "@/components/auth/redirect-if-authed.tsx";
import LoginPage from "@/components/pages/admin/admin-login-page.tsx";
import {RequireAuth} from "@/components/auth/require-auth.tsx";
import {DashboardPage} from "@/components/pages/admin/admin-dashbord-page.tsx";
import {RootRedirect} from "@/routes/root-redirect.tsx";
import {LocaleGate} from "@/routes/locale-gate.tsx";
import {NotFoundPage} from "@/components/pages/not-found-page.tsx";
import {DEFAULT_LANGUAGE, isSupportedLanguage} from "@/i18n/languages.ts";
import {buildLocalizedPath} from "@/i18n/paths.ts";

/**
 * admin 路径下的 404：首页按钮指回后台仪表盘。
 */
const AdminNotFoundPage = () => {
    const {lang} = useParams();

    return (
        <NotFoundPage
            homePath={buildLocalizedPath(
                isSupportedLanguage(lang) ? lang : DEFAULT_LANGUAGE,
                "/admin",
            )}
        />
    );
};

function App() {
    useEffect(() => {
        // 启动时问一次后端："我还登录着吗"
        // StrictMode 会跑两遍，但 bootstrapSession 用模块级 Promise 去重了
        void bootstrapSession();
    }, []);

    return (
        <Routes>
            {/** 根路径 添加 lang */}
            <Route path="/" element={<RootRedirect/>}/>

            {/** 所有页面都要在 /:lang 下  */}
            <Route path="/:lang" element={<LocaleGate/>}>
                <Route path="admin">
                    {/* 只有在"没登录"时才允许进入的组 */}
                    <Route element={<RedirectIfAuthed/>}>
                        <Route path="login" element={<LoginPage/>}/>
                    </Route>

                    {/* 需要登录才能进入的组。新增页面放进这里就自动受保护 */}
                    <Route element={<RequireAuth/>}>
                        <Route index element={<DashboardPage/>}/>
                    </Route>
                </Route>

                {/** admin 下的 404 回到后台首页 */}
                <Route path="admin/*" element={<AdminNotFoundPage/>}/>

                {/** 其余路径的 404 回到公网首页 */}
                <Route path="*" element={<NotFoundPage homePath="/"/>}/>
            </Route>
        </Routes>
    );
}

export default App
