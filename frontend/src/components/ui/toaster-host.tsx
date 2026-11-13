"use client"

import * as React from "react";
import {Toaster} from "sonner";
import {useResolvedTheme} from "@/lib/theme";

export const ToasterHost = () => {
    const resolvedTheme = useResolvedTheme();
    return (
        <Toaster
            theme={resolvedTheme}
            className="toaster group"
            richColors
        />
    );
}