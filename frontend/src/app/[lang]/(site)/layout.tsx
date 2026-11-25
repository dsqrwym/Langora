import React, {type PropsWithChildren} from "react";
import SiteHeader from "@/components/site/site-header";

export default async function SiteLayout({children}: PropsWithChildren) {
    const schoolName = "Example School"
    return <>
        <SiteHeader schoolName={schoolName}/>
        {children}
    </>;
}
