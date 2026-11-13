"use client"

import {useEffect, useRef} from "react";
import Link from "next/link";
import {usePathname, useRouter} from "next/navigation";
import {ArrowLeft, Home} from "lucide-react";
import {cn} from "cn";
import {Button, buttonVariants} from "@/components/ui/button";
import {stripLanguagePrefix} from "@/i18n/paths";
import {NotFoundMessages} from "@/i18n/not-found-messages";

type FieldNode = {
    x: number;
    y: number;
    vx: number;
    vy: number;
    radius: number;
    phase: number;
};

type Shockwave = {
    x: number;
    y: number;
    age: number;
};

const MIN_NODES = 93;
const MAX_NODES = 373;
const MAX_SPEED = 9;
const FOOTER_SIGNATURE = "langora · node field";

/**
 * 全屏节点场。
 * 鼠标是引力源：节点被吸向光标附近的目标轨道，同时受切向力形成旋涡，
 * 节点之间再互相排斥，所以会围绕光标聚拢、推挤、旋转。
 * 点击空白处释放冲击波，把节点炸开，随后它们会重新被吸回来。
 */
type NodeFieldProps = {
    className?: string;
};

const NodeField = ({className}: NodeFieldProps) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const context = canvas.getContext("2d");
        if (!context) return;

        const pointer = {x: 0, y: 0, active: false};

        let nodes: FieldNode[] = [];
        const shockwaves: Shockwave[] = [];
        let width = 0;
        let height = 0;
        let frameId = 0;
        let running = false;
        let lastTimestamp = 0;

        let lineColor = "#737373";
        let nodeColor = "#a3a3a3";
        let accentColor = "#171717";
        let nodeSprite: HTMLCanvasElement | null = null;
        let accentSprite: HTMLCanvasElement | null = null;

        // canvas 只认具体颜色，先探测浏览器能否解析主题变量（如 oklch），失败则回退
        const sanitizeColor = (input: string, fallback: string): string => {
            if (!input) return fallback;
            context.fillStyle = "#000001";
            context.fillStyle = input;
            return context.fillStyle === "#000001" ? fallback : input;
        };

        // 预渲染光点贴图，比每帧 createRadialGradient 便宜得多
        const buildSprite = (color: string): HTMLCanvasElement => {
            const sprite = document.createElement("canvas");
            sprite.width = 64;
            sprite.height = 64;

            const spriteContext = sprite.getContext("2d");
            if (!spriteContext) return sprite;

            const gradient = spriteContext.createRadialGradient(32, 32, 0, 32, 32, 32);
            gradient.addColorStop(0, color);
            gradient.addColorStop(0.3, color);
            gradient.addColorStop(1, "rgba(0, 0, 0, 0)");
            spriteContext.fillStyle = gradient;
            spriteContext.fillRect(0, 0, 64, 64);

            return sprite;
        };

        // 配色全部来自 shadcn 的 neutral token：普通节点用 muted-foreground，被捕获的节点用 foreground
        const resolveColors = () => {
            const styles = window.getComputedStyle(canvas);
            lineColor = sanitizeColor(styles.color, "#737373");
            nodeColor = sanitizeColor(styles.getPropertyValue("--muted-foreground").trim(), "#a3a3a3");
            accentColor = lineColor;
            nodeSprite = buildSprite(nodeColor);
            accentSprite = buildSprite(accentColor);
        };

        const seed = () => {
            const count = Math.round(
                Math.min(MAX_NODES, Math.max(MIN_NODES, (width * height) / 6750)),
            );

            nodes = Array.from({length: count}, () => {
                const angle = Math.random() * Math.PI * 2;
                const speed = 0.08 + Math.random() * 0.34;

                return {
                    x: Math.random() * width,
                    y: Math.random() * height,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed,
                    radius: 1.2 + Math.random() * 1.6,
                    phase: Math.random() * Math.PI * 2,
                };
            });
        };

        const resize = () => {
            const rect = canvas.getBoundingClientRect();
            const dpr = Math.min(window.devicePixelRatio || 1, 2);

            width = Math.max(1, rect.width);
            height = Math.max(1, rect.height);
            canvas.width = Math.round(width * dpr);
            canvas.height = Math.round(height * dpr);
            context.setTransform(dpr, 0, 0, dpr, 0, 0);

            seed();
        };

        const getInfluence = () => Math.min(380, Math.max(220, Math.min(width, height) * 0.44));

        const update = (dt: number, timestamp: number) => {
            const influence = getInfluence();
            const orbit = 48;

            for (const node of nodes) {
                // 呼吸式漂移，让没有鼠标时也不呆板
                node.vx += Math.cos(timestamp * 0.00045 + node.phase) * 0.004 * dt;
                node.vy += Math.sin(timestamp * 0.00038 + node.phase) * 0.004 * dt;

                if (pointer.active) {
                    const dx = pointer.x - node.x;
                    const dy = pointer.y - node.y;
                    const distance = Math.hypot(dx, dy);

                    if (distance < influence && distance > 0.001) {
                        const falloff = 1 - distance / influence;
                        const dirX = dx / distance;
                        const dirY = dy / distance;
                        const target = orbit + node.radius * 5;

                        // 离目标轨道太远就吸过来，太近就推开
                        const radial = (distance - target) * 0.0034 * falloff;
                        node.vx += dirX * radial * dt;
                        node.vy += dirY * radial * dt;

                        // 切向力形成旋涡
                        node.vx += -dirY * 0.075 * falloff * dt;
                        node.vy += dirX * 0.075 * falloff * dt;

                        const damping = Math.pow(0.9, dt);
                        node.vx *= damping;
                        node.vy *= damping;
                    }
                }

                // 轻微全局摩擦，冲击波过后会慢慢平静下来
                const friction = Math.pow(0.99, dt);
                node.vx *= friction;
                node.vy *= friction;

                const speed = Math.hypot(node.vx, node.vy);
                if (speed > MAX_SPEED) {
                    node.vx = (node.vx / speed) * MAX_SPEED;
                    node.vy = (node.vy / speed) * MAX_SPEED;
                }

                node.x += node.vx * dt;
                node.y += node.vy * dt;

                // 边界环绕
                const margin = 24;
                if (node.x < -margin) node.x = width + margin;
                else if (node.x > width + margin) node.x = -margin;
                if (node.y < -margin) node.y = height + margin;
                else if (node.y > height + margin) node.y = -margin;
            }

            // 节点之间互相挤开，避免在光标周围堆成一坨
            const minDistance = 32;
            for (let i = 0; i < nodes.length; i += 1) {
                const a = nodes[i];

                for (let j = i + 1; j < nodes.length; j += 1) {
                    const b = nodes[j];
                    const dx = b.x - a.x;
                    const dy = b.y - a.y;
                    const distance = Math.hypot(dx, dy);
                    if (distance >= minDistance || distance < 0.001) continue;

                    const force = ((1 - distance / minDistance) * 1.3 * dt) / distance;
                    const pushX = dx * force;
                    const pushY = dy * force;

                    a.vx -= pushX;
                    a.vy -= pushY;
                    b.vx += pushX;
                    b.vy += pushY;
                }
            }

            for (let i = shockwaves.length - 1; i >= 0; i -= 1) {
                shockwaves[i].age += dt * 0.022;
                if (shockwaves[i].age >= 1) shockwaves.splice(i, 1);
            }
        };

        const draw = (timestamp: number) => {
            context.clearRect(0, 0, width, height);

            const influence = getInfluence();
            const linkDistance = Math.min(150, Math.max(96, width * 0.075));

            // 光标周围的引力辉光
            if (pointer.active && accentSprite) {
                context.globalAlpha = 0.1;
                context.drawImage(accentSprite, pointer.x - 90, pointer.y - 90, 180, 180);
            }

            // 节点之间的连线
            context.lineWidth = 1;
            for (let i = 0; i < nodes.length; i += 1) {
                const a = nodes[i];

                for (let j = i + 1; j < nodes.length; j += 1) {
                    const b = nodes[j];
                    const dx = b.x - a.x;
                    const dy = b.y - a.y;
                    const distance = Math.hypot(dx, dy);
                    if (distance > linkDistance) continue;

                    context.globalAlpha = (1 - distance / linkDistance) * 0.18;
                    context.strokeStyle = lineColor;
                    context.beginPath();
                    context.moveTo(a.x, a.y);
                    context.lineTo(b.x, b.y);
                    context.stroke();
                }
            }

            // 被引力捕获的节点连向光标
            if (pointer.active) {
                for (const node of nodes) {
                    const distance = Math.hypot(node.x - pointer.x, node.y - pointer.y);
                    if (distance > influence * 0.9) continue;

                    context.globalAlpha = (1 - distance / (influence * 0.9)) * 0.4;
                    context.strokeStyle = accentColor;
                    context.beginPath();
                    context.moveTo(node.x, node.y);
                    context.lineTo(pointer.x, pointer.y);
                    context.stroke();
                }
            }

            // 光点
            for (const node of nodes) {
                const influenced = pointer.active
                    && Math.hypot(node.x - pointer.x, node.y - pointer.y) < influence;
                const sprite = influenced ? accentSprite : nodeSprite;
                const size = node.radius * (influenced ? 10 : 7);

                context.globalAlpha = influenced ? 0.95 : 0.7;

                if (sprite) {
                    context.drawImage(sprite, node.x - size / 2, node.y - size / 2, size, size);
                } else {
                    context.fillStyle = influenced ? accentColor : nodeColor;
                    context.beginPath();
                    context.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
                    context.fill();
                }
            }

            // 冲击波圆环
            for (const wave of shockwaves) {
                const eased = 1 - Math.pow(1 - wave.age, 3);

                context.globalAlpha = (1 - wave.age) * 0.7;
                context.strokeStyle = accentColor;
                context.lineWidth = 2;
                context.beginPath();
                context.arc(wave.x, wave.y, eased * 320, 0, Math.PI * 2);
                context.stroke();
                context.lineWidth = 1;
            }

            // 光标：旋转的引力环
            if (pointer.active) {
                context.save();
                context.translate(pointer.x, pointer.y);
                context.rotate(timestamp * 0.0004);
                context.globalAlpha = 0.8;
                context.strokeStyle = accentColor;
                context.lineWidth = 1;

                for (const start of [0, Math.PI]) {
                    context.beginPath();
                    context.arc(0, 0, 48, start, start + Math.PI * 0.58);
                    context.stroke();
                }

                context.restore();

                if (accentSprite) {
                    context.globalAlpha = 0.9;
                    context.drawImage(accentSprite, pointer.x - 10, pointer.y - 10, 20, 20);
                }
            }

            context.globalAlpha = 1;
        };

        const frame = (timestamp: number) => {
            if (!running) return;

            const dt = lastTimestamp ? Math.min(2.5, (timestamp - lastTimestamp) / 16.6667) : 1;
            lastTimestamp = timestamp;
            update(dt, timestamp);
            draw(timestamp);
            frameId = requestAnimationFrame(frame);
        };

        const start = () => {
            if (running) return;
            running = true;
            lastTimestamp = 0;
            frameId = requestAnimationFrame(frame);
        };

        const stop = () => {
            running = false;
            if (frameId) cancelAnimationFrame(frameId);
            frameId = 0;
        };

        const onPointerMove = (event: PointerEvent) => {
            const rect = canvas.getBoundingClientRect();
            const x = event.clientX - rect.left;
            const y = event.clientY - rect.top;

            pointer.x = x;
            pointer.y = y;
            pointer.active = x >= 0 && y >= 0 && x <= rect.width && y <= rect.height;
        };

        const onPointerOut = (event: PointerEvent) => {
            if (event.relatedTarget) return;
            pointer.active = false;
        };

        const onPointerDown = (event: PointerEvent) => {
            if (event.button !== 0) return;

            const target = event.target as Element | null;
            if (target?.closest("a, button, input, textarea, select, [role='button']")) return;

            const rect = canvas.getBoundingClientRect();
            const x = event.clientX - rect.left;
            const y = event.clientY - rect.top;
            if (x < 0 || y < 0 || x > rect.width || y > rect.height) return;

            shockwaves.push({x, y, age: 0});

            for (const node of nodes) {
                const dx = node.x - x;
                const dy = node.y - y;
                const distance = Math.hypot(dx, dy);
                if (distance >= 320 || distance < 0.001) continue;

                const force = (1 - distance / 320) * 22;
                node.vx += (dx / distance) * force;
                node.vy += (dy / distance) * force;
            }
        };

        const onVisibilityChange = () => {
            if (document.hidden) stop();
            else start();
        };

        const resizeObserver = new ResizeObserver(resize);
        const themeObserver = new MutationObserver(resolveColors);

        resolveColors();
        resize();
        resizeObserver.observe(canvas);
        themeObserver.observe(document.documentElement, {attributes: true, attributeFilter: ["class"]});

        window.addEventListener("pointermove", onPointerMove, {passive: true});
        window.addEventListener("pointerout", onPointerOut);
        window.addEventListener("pointerdown", onPointerDown);
        document.addEventListener("visibilitychange", onVisibilityChange);

        start();

        return () => {
            stop();
            resizeObserver.disconnect();
            themeObserver.disconnect();
            window.removeEventListener("pointermove", onPointerMove);
            window.removeEventListener("pointerout", onPointerOut);
            window.removeEventListener("pointerdown", onPointerDown);
            document.removeEventListener("visibilitychange", onVisibilityChange);
        };
    }, []);

    return (
        <canvas
            ref={canvasRef}
            aria-hidden="true"
            className={cn("text-foreground", className)}
        />
    );
};

type NotFoundPageProps = {
    /** 返回首页按钮（以及快捷键 H、无历史时的兜底）的目标地址 */
    homePath: string;
    /** 404 文案：三语小字典同步加载，保证错误页首帧即正确语言 */
    messages: NotFoundMessages;
};

/**
 * 404 页面。
 * 全屏节点场跟随鼠标聚拢、旋涡、互斥，点击空白释放冲击波；
 * 中间是玻璃质感的 404 文案与返回首页操作。
 */
export const NotFoundPage = ({homePath, messages}: NotFoundPageProps) => {
    // not-found 阶段 usePathname 可能返回 null
    const currentPathname = usePathname() ?? "/";
    const router = useRouter();

    // 地址里去掉语言前缀，展示 "GET /xxx" 这种终端风格的信息
    const pathname = stripLanguagePrefix(currentPathname) || "/";
    const displayPath = pathname.length > 42 ? `${pathname.slice(0, 42)}…` : pathname;

    // 彩蛋：按 H 回首页
    useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.altKey) return;

            const target = event.target as HTMLElement | null;
            if (target && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName))) {
                return;
            }

            if (event.key.toLowerCase() === "h") {
                event.preventDefault();
                router.push(homePath);
            }
        };

        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);
    }, [homePath, router]);

    const goBack = () => {
        // 直接从外部打开这个地址时没有上一页，兜底回首页
        if (window.history.length > 1) {
            router.back();
            return;
        }
        router.push(homePath);
    };

    return (
        <main className="nf-scene relative isolate flex min-h-svh flex-col overflow-hidden bg-background">
            {/* 全屏节点场 */}
            <NodeField className="pointer-events-none absolute inset-0 z-0 size-full"/>

            {/* 边缘淡出，让中心更聚焦 */}
            <div aria-hidden="true" className="nf-vignette pointer-events-none absolute inset-0 z-[1]"/>

            <div
                className="relative z-10 flex flex-1 flex-col items-center justify-center px-6 py-8 text-center sm:py-10">
                <div
                    className="nf-rise inline-flex items-center gap-2.5 rounded-full border border-border/70 bg-background px-3.5 py-1.5 font-mono text-[10px] tracking-[0.3em] text-muted-foreground uppercase">
                    <span className="relative flex size-1.5">
                        <span
                            className="absolute inline-flex size-full animate-ping rounded-full bg-foreground opacity-75"/>
                        <span className="relative inline-flex size-1.5 rounded-full bg-foreground"/>
                    </span>
                    {messages.badge}
                </div>

                <div className="relative mt-4 flex items-center justify-center">
                    {/* Apple 风玻璃板：真实模糊背后的节点场 */}
                    <div aria-hidden="true"
                         className="nf-glass pointer-events-none absolute inset-0 z-0 rounded-[2.25rem] sm:rounded-[2.75rem]">
                        <span className="nf-glass-blur nf-glass-blur-1"/>
                        <span className="nf-glass-blur nf-glass-blur-2"/>
                        <span className="nf-glass-blur nf-glass-blur-3"/>
                        <span className="nf-glass-tint"/>
                    </div>
                    <h1 className="nf-404 relative z-10 px-[0.22em] py-[0.26em] text-[clamp(5.5rem,18vw,15rem)] leading-[0.82] font-black tracking-[0.01em] select-none">
                        404
                    </h1>
                </div>

                <h2 className="nf-rise mt-4 text-2xl font-semibold tracking-tight text-balance sm:text-3xl [animation-delay:100ms]">
                    {messages.title}
                </h2>
                <p className="nf-rise mt-2 max-w-md text-sm leading-relaxed text-balance text-muted-foreground [animation-delay:160ms]">
                    {messages.description}
                    <br className="hidden sm:block"/>
                    <span className="text-muted-foreground/70">{messages.reassurance}</span>
                </p>

                <div className="nf-rise mt-4 w-full max-w-md [animation-delay:220ms]">
                    <div
                        className="flex items-center gap-2 rounded-xl border border-border/70 bg-background px-3.5 py-2.5 font-mono text-[11px] shadow-sm sm:text-xs">
                        <span className="shrink-0 text-muted-foreground/60">GET</span>
                        <span className="min-w-0 flex-1 truncate text-left text-muted-foreground" title={pathname}>
                            {displayPath}
                        </span>
                        <span
                            className="shrink-0 rounded-md border border-border/70 bg-muted px-1.5 py-0.5 font-semibold text-foreground">
                            404
                        </span>
                    </div>
                </div>

                <div className="nf-rise mt-6 flex flex-wrap items-center justify-center gap-3 [animation-delay:300ms]">
                    <Link
                        href={homePath}
                        className={cn(
                            buttonVariants({variant: "default", size: "lg"}),
                            "h-11 gap-2 rounded-full px-6 text-sm",
                        )}
                    >
                        <Home className="size-4"/>
                        {messages.home}
                    </Link>
                    <Button
                        variant="secondary"
                        size="lg"
                        onClick={goBack}
                        className="h-11 gap-2 rounded-full px-5 text-sm"
                    >
                        <ArrowLeft className="size-4"/>
                        {messages.back}
                    </Button>
                </div>

                <p className="nf-rise mt-6 flex items-center gap-2 rounded-full bg-background px-3 py-1 font-mono text-[10px] tracking-[0.18em] text-muted-foreground/70 uppercase [animation-delay:380ms]">
                    <span className="size-1 animate-pulse rounded-full bg-foreground"/>
                    {messages.interactionHint}
                </p>
            </div>

            <p className="pointer-events-none absolute bottom-5 left-1/2 z-10 -translate-x-1/2 font-mono text-[10px] tracking-[0.4em] text-muted-foreground/40 uppercase">
                {FOOTER_SIGNATURE}
            </p>
        </main>
    );
};
