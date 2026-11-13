import {notFound} from "next/navigation";

/**
 * NextJs 规则 page.tsx 显示页面而在本[...notFound] 捕获 [lang] 下所有未匹配的子路径，统一返回 404。
 */
export default function CatchAllNotFound() {
    // notFound 会抛出 404
    notFound();
}
