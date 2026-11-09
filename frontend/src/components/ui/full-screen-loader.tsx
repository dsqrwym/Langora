import { cn } from "cn"

import { Spinner } from "@/components/ui/spinner"

type FullScreenLoaderProps = {
  label?: string
  className?: string
}

/**
 * 全屏玻璃加载层。
 */
function FullScreenLoader({
  label = "Is loading...",
  className,
}: FullScreenLoaderProps) {
  return (
    <div
      data-slot="full-screen-loader"
      role="status"
      aria-live="polite"
      aria-busy="true"
      className={cn(
        "fixed inset-0 z-50 grid cursor-wait place-items-center bg-background/60 backdrop-blur-md backdrop-saturate-150 select-none",
        className
      )}
    >
      <div className="flex flex-col items-center gap-3">
        <Spinner aria-hidden="true" className="size-6 text-foreground" />
        <p className="text-sm text-muted-foreground">{label}</p>
      </div>
    </div>
  )
}

export { FullScreenLoader }
