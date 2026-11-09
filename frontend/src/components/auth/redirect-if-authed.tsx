import {useAuth} from "@/lib/auth";
import {Navigate, Outlet, useLocation} from "react-router-dom";
import {FullScreenLoader} from "@/components/ui/full-screen-loader.tsx";
import {buildLocalizedPath, getLanguageOrDefault} from "@/i18n/paths.ts";

export const RedirectIfAuthed = () => {
    const {status} = useAuth();
    const location = useLocation();

    const lang = getLanguageOrDefault(location.pathname);
    const rawFrom = (location.state as { from?: string } | null)?.from;
    // from 必须落在当前语言域内，防止跨语言/外域跳转
    const from = rawFrom && rawFrom.startsWith(`/${lang}/`)
        ? rawFrom
        : buildLocalizedPath(lang, "/admin");

    if (status === "checking") return <FullScreenLoader/>;
    if (status === "authenticated") return <Navigate to={from ?? "/"} replace/>;

    return <Outlet/>;
};
