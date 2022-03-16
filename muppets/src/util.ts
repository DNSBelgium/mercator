import {v4 as uuid} from "uuid";

export function convertDate(d: Date) {
    function pad(s: string | number) { return (s < 10) ? "0" + s : s; }
    return [pad(d.getFullYear()), pad(d.getMonth()+1), pad(d.getDate())].join("/");
}

export function getDomainName(hostname: string) {
    // TODO: Handling more than .be ? Checking TLD ?
    return hostname.substring(hostname.lastIndexOf(".", hostname.lastIndexOf(".") - 1) + 1);
}

export function computePath(url: URL, id = uuid()) {
    return [
        getDomainName(url.hostname),
        convertDate(new Date()),
        url.protocol.replace(":", ""),
        url.hostname,
        url.pathname.replace(/^\/+|\/+$/g, "") || "index.html",
        id
    ].join("/");
}
