import * as React from "react";
import {Toaster} from "sonner";
import {useResolvedTheme} from "@/lib/theme";

const TOASTER_STYLE = {
    "--normal-bg": "var(--popover)",
    "--normal-text": "var(--popover-foreground)",
    "--normal-border": "var(--border)",
} as React.CSSProperties;

export const ToasterHost = () => {
    const resolvedTheme = useResolvedTheme();
    return (
        <Toaster
            theme={resolvedTheme}
            className="toaster group"
            style={TOASTER_STYLE}
        />
    );
}