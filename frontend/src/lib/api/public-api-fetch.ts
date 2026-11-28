import {createApiFetch} from "@/lib/api/api-fetch";
import {API_BASE} from "@/lib/api/api-config";
import {PUBLIC_CACHE_TAGS} from "@/lib/api/api-constants";

export const publicApiFetch = createApiFetch({
    baseUrl: API_BASE,
    cache: { tags: [PUBLIC_CACHE_TAGS.ALL] },
    credentials: "omit"
});