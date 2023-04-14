import * as Scraper from './scraper';
import * as Websnapper from './websnapper';
import { ScraperParams } from './scraper';
import { expect } from 'chai';
import { v4 as uuid } from 'uuid';

describe('Scraper Tests', function () {
    this.timeout(30000);

    const mockUuid = '4004ab8c-d4a9-47a8-8b7b-e45648068899';
    let params: ScraperParams = {
        url: 'https://dnsbelgium.be',
        visitId: uuid(),
        saveHar: true,
        saveHtml: true,
        saveScreenshot: true,
        screenshotOptions: {
            fullPage: true,
            encoding: "binary",
            type: "png",
            captureBeyondViewport: true,
        },
        browserOptions: {
            ignoreHTTPSErrors: true
        },
        retries: 0,
    };

    it('dns', () => {
        return Scraper.websnap(params).then(scraperResult => {
            if(scraperResult!=undefined){
                // @ts-ignore
                console.log(scraperResult.screenshotData.length+" B")
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024+" KiB")
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024/1024+" MiB")
            }
            // @ts-ignore
            return Websnapper.uploadToS3(scraperResult).then(result => {
                console.log(result.errors)
                expect(result.errors).to.be.empty;
            })
        });
    });

    it('standaard_png_should come out in webp', () => {
        params.url='https://standaard.be';

        return Scraper.websnap(params).then(scraperResult => {
            if(scraperResult.screenshotData!=undefined){

                console.log(scraperResult.screenshotData.length/1024+" kB")

                console.log(scraperResult.screenshotData.length/1024/1024+" mB")

                scraperResult.screenshotData?.length+10*1024*1024;
            }
            return Websnapper.uploadToS3(scraperResult).then(result => {
                console.log(result.errors)
                console.log(result.screenshotType)
                expect(result.errors).to.contain("screenshot")
            })
        });
    });

    it('kinepolis', () => {
       params.url = 'kinepolis.be'

        return Scraper.websnap(params).then(scraperResult => {
            if(scraperResult!=undefined){
                // @ts-ignore
                console.log(scraperResult.screenshotData.length+" B")
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024+" KiB")
                // @ts-ignore
                console.log(scraperResult.screenshotData.length/1024/1024+" MiB")
            }
            return Websnapper.uploadToS3(scraperResult).then(result => {
                console.log(result.errors)
                expect(result.errors).to.be.empty;
            })
        });
    });
});
