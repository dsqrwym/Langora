export interface ApiResponse<T> {
    status: number;
    code: string;
    message: string;
    traceId: string | null;
    data: T | null;
}
