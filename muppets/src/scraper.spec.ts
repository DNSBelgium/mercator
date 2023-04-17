import * as Scraper from './scraper';
import * as Websnapper from './websnapper';
import { ScraperParams } from './scraper';
import { expect } from 'chai';
import { v4 as uuid } from 'uuid';
import config from "./config";

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

    it('dns should succeed', () => {
        return Scraper.websnap(params).then(scraperResult => {
            return Websnapper.uploadToS3(scraperResult).then(result => {
                console.log(result.errors)
                expect(result.screenshotSkipped).to.equal(false)
                expect(result.errors).to.be.empty;
            })
        });
    });

    it('dns should fail length > then max', () => {
        return Scraper.websnap(params).then(scraperResult => {
            //set max_content_length for testing
            config.max_content_length = 1;

            return Websnapper.uploadToS3(scraperResult).then(result => {
                console.log(result.errors)
                expect(result.errors).to.be.empty;
                expect(result.screenshotSkipped).to.be.equal(true)
            })
        });
    });
});
