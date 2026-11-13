"use client"

import {NotFoundBoundary} from "@/components/pages/not-found-boundary";

/**
 * not-found.tsx 默认会被 NextJs 作为 404 页面
 * @constructor
 */
export default function NotFound() {
    return <NotFoundBoundary/>;
}
