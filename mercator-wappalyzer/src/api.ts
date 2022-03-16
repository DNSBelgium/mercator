export interface WappalyzerRequest {
    visitId: string
    domainName: string
    url: string
}

export interface WappalyzerResponse {
    request: WappalyzerRequest
    wappalyzer: any
}

// {
//     "urls": {
//     "http://dnsbelgium.be/": {
//         "status": 301
//     },
//     "https://dnsbelgium.be/": {
//         "status": 301
//     },
//     "https://www.dnsbelgium.be/": {
//         "status": 200
//     }
// },
//     "technologies": [
//     {
//         "slug": "incapsula",
//         "name": "Incapsula",
//         "confidence": 100,
//         "version": null,
//         "icon": "Incapsula.png",
//         "website": "http://www.incapsula.com",
//         "cpe": null,
//         "categories": [
//             {
//                 "id": 31,
//                 "slug": "cdn",
//                 "name": "CDN"
//             },
//             {
//                 "id": 61,
//                 "slug": "saas",
//                 "name": "SaaS"
//             }
//         ]
//     }
// ]
// }
