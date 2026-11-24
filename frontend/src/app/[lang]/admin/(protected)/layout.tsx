import React, {type PropsWithChildren} from "react";
import {RequireAuth} from "@/features/auth/components/require-auth";

export default async function AdminProtectedLayout({children}: PropsWithChildren) {
    return <RequireAuth>{children}</RequireAuth>
}
