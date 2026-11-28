import React, {type PropsWithChildren} from "react";
import {RequireAuth} from "@/features/auth/components/require-auth";

export default async function AdminProtectedLayout({children}: PropsWithChildren) {
    return <RequireAuth>
        <main className="flex-1">{children}</main>
    </RequireAuth>
}
