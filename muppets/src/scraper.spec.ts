import * as Scraper from './scraper';
import * as Websnapper from './websnapper';
import { ScraperParams } from './scraper';
import { expect } from 'chai';
import { v4 as uuid } from 'uuid';


describe('Scraper Tests', function () {
    this.timeout(30000);

    // const mockUuid = '1fe39e26-9d20-4cc0-8696-fe7887a3dfbc';
    // sinon.stub(uuid, 'v4').returns(mockUuid);
    //
    it('dns_png', () => {
        let params: ScraperParams = {
            url: 'https://kinepolis.be/',
            visitId: uuid(),
            saveHar: true,
            saveHtml: true,
            saveScreenshot: true,
            screenshotOptions: {
                fullPage: true,
                encoding: "binary",
                type: "png",
                captureBeyondViewport: true
            },
            browserOptions: {
                ignoreHTTPSErrors: true
            },
            retries: 0,
        };
        return Scraper.websnap(params).then(scraperResult => {
            if(scraperResult!=undefined){
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024+" kB")
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024/1024+" mB")
            }
            // @ts-ignore
            return Websnapper.uploadToS3(scraperResult).then(result => {
                console.log(result.errors)
                expect(result.errors).to.be.empty;
            })
        });
    });
    //
    // it('dns_webp', () => {
    //     let params: ScraperParams = {
    //         url: 'https://dnsbelgium.be',
    //         visitId: uuid(),
    //         saveHar: true,
    //         saveHtml: true,
    //         saveScreenshot: true,
    //         screenshotOptions: {
    //             fullPage: true,
    //             encoding: "binary",
    //             type: "webp",
    //             captureBeyondViewport: true,
    //             quality: 100
    //         },
    //         browserOptions: {
    //             ignoreHTTPSErrors: true
    //         },
    //         retries: 0,
    //     };
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         // @ts-ignore
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });
    //
    // it('standaard_png_should come out in webp', () => {
    //
    //     let params: ScraperParams = {
    //         url: 'https://standaard.be',
    //         visitId: uuid(),
    //         saveHar: true,
    //         saveHtml: true,
    //         saveScreenshot: true,
    //         screenshotOptions: {
    //             fullPage: true,
    //             encoding: "binary",
    //             type: "png",
    //             captureBeyondViewport: true
    //         },
    //         browserOptions: {
    //             ignoreHTTPSErrors: true
    //         },
    //         retries: 0,
    //     };
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });

    it('testHtmlFile', () => {

        let params: ScraperParams = {
            url: 'https://kinepolis.be',
            visitId: uuid(),
            saveHar: true,
            saveHtml: true,
            saveScreenshot: true,
            screenshotOptions: {
                fullPage: true,
                encoding: "binary",
                type: "png",
                captureBeyondViewport: true
            },
            browserOptions: {
                ignoreHTTPSErrors: true
            },
            retries: 0,
        };
        return Scraper.websnap(params).then(scraperResult => {
            if(scraperResult!=undefined){
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024+" kB")
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024/1024+" mB")
            }

            return Websnapper.uploadToS3(scraperResult).then(result => {
                console.log(result.errors)
                expect(result.errors).to.be.empty;
            })
        });
    });

    // it('vrt', () => {
    //     params.url ='https://vrt.be';
    //     params.visitId = uuid();
    //
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });
    //
    // it('omloophetnieuwsblad', () => {
    //     params.url ='https://omloophetnieuwsblad.be';
    //     params.visitId = uuid();
    //
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });
    //
    // it('hln', () => {
    //     params.url ='https://hln.be';
    //     params.visitId = uuid();
    //
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });
    //
    // it('coolblue', () => {
    //     params.url ='https://coolblue.be';
    //     params.visitId = uuid();
    //
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });
    //
    // it('fnac', () => {
    //     params.url ='https://fnac.be';
    //     params.visitId = uuid();
    //
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });

    //
    // it('zalando', () => {
    //     params.url ='https://zalando.be';
    //     params.visitId = uuid();
    //
    //     return Scraper.websnap(params).then(scraperResult => {
    //         if(scraperResult!=undefined){
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024+" kB")
    //             // @ts-ignore
    //             console.log(scraperResult.screenshotData.length/1024/1024+" mB")
    //         }
    //         return Websnapper.uploadToS3(scraperResult).then(result => {
    //             console.log(result.errors)
    //             expect(result.errors).to.be.empty;
    //         })
    //     });
    // });

});
