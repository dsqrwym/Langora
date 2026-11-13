import {cn} from "cn"

import {Spinner} from "@/components/ui/spinner"
import React from "react";

type FullScreenLoaderProps = {
    label?: string
    className?: string
}

/**
 * 全屏玻璃加载层。
 */
function FullScreenLoader({label = "Is loading...", className}: FullScreenLoaderProps) {
    return (
        <div
            role="status"
            aria-live="polite"
            aria-busy="true"
            className={cn(
                "fixed inset-0 z-50 flex flex-col items-center justify-center cursor-wait",
                "backdrop-blur-sm backdrop-saturate-150 bg-background/60 select-none",
                className
            )}
        >
            <div className="flex flex-col items-center gap-3">
                <Spinner aria-hidden="true" className="size-6 text-foreground"/>
                {/* 内联 CSS 定义伪元素动画 */}
                <style>{`
                @keyframes loadingDots {
                    0% { content: ""; }
                    25% { content: "."; }
                    50% { content: ".."; }
                    75%, 100% { content: "..."; }
                }
                .loading-ellipsis::after {
                    content: "";
                    display: inline-block;
                    text-align: left;
                    animation: loadingDots 1.5s steps(4, end) infinite;
                }
            `}</style>
                <div className="flex items-center">
                    <p className="text-sm text-muted-foreground">{label} <span className="loading-ellipsis"></span></p>
                </div>
            </div>
        </div>
    )
}

export {FullScreenLoader}
