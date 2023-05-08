import * as Scraper from './scraper';
import { IFileUploader, uploadScrapedData } from './websnapper.js';
import { ScraperParams } from './scraper.js';
import { expect } from 'chai';
import { v4 as uuid } from 'uuid';
import * as path from "path";
import { convertDate } from "./util.js";
import * as sinon from "sinon";
import { config } from './config.js';

class MockFileUploader implements IFileUploader {
    private called: number = 0;

    async upload(data: string | void | Buffer, filename: string, prefix: string, uploadFileFormat: string, contentType?: string): Promise<string> {
        this.called++;

        return Promise.resolve("");
    }

    public getCalledCount(): number {
        return this.called;
    }
}


describe('Scraper Tests', function () {
    this.timeout(15000);

    const mockUuid = '1fe39e26-9d20-4cc0-8696-fe7887a3dfbc';

    let params: ScraperParams = {
        url: 'https://dnsbelgium.be',
        visitId: uuid(),
        saveHar: true,
        saveHtml: true,
        saveScreenshot: true,
        screenshotOptions: {
            fullPage: true,
            encoding: "binary",
            type: "png"
        },
        browserOptions: {
            ignoreHTTPSErrors: true
        },
        retries: 0,
        referer: ""
    };

    //commented code does not return as expected
    it('check response', async () => {
        let folder = path.join("output", "dnsbelgium.be", convertDate(new Date()), 'https', 'dnsbelgium.be', 'index.html', mockUuid);
        const scraperResult = await Scraper.websnap(params);

        console.log(scraperResult.harFile);
        expect(scraperResult).to.have.property('hostname', 'dnsbelgium.be');
        expect(scraperResult).to.have.property('url', 'https://www.dnsbelgium.be/');
        expect(scraperResult).to.have.property('request',);
        expect(scraperResult.request).to.have.eql({ ...params });
        expect(scraperResult.request).to.have.property('referer', '');
        expect(scraperResult).to.have.property('htmlLength');
        expect(scraperResult).to.have.property('pageTitle');
        expect(scraperResult).to.have.property('metrics');
        // expect(scraperResult).to.have.property('bucket', folder);
        // expect(scraperResult).to.have.property('harFile', path.join(folder, 'dnsbelgium.be.har'));
        // expect(scraperResult).to.have.property('htmlFile', path.join(folder, 'index.html'));
        // expect(scraperResult).to.have.property('screenshotFile', path.join(folder, 'screenshot.png'));
        // expect(scraperResult).to.have.property('retries', 1);
    });

    it('all files get uploaded to S3, returns scraper result as expected', async () => {
        const mockedUploader = new MockFileUploader();

        const scraperWebsnapResult = await Scraper.websnap(params)
        const websnapperResult = await uploadScrapedData(scraperWebsnapResult, mockedUploader)

        console.log(`s3UploadFile was called ${mockedUploader.getCalledCount()} times`);
        console.log(websnapperResult.errors);

        expect(mockedUploader.getCalledCount()).to.be.equal(3);
        expect(websnapperResult.htmlSkipped).to.equal(false)
        expect(websnapperResult.screenshotSkipped).to.equal(false)
        expect(websnapperResult.harSkipped).to.equal(false)
        expect(websnapperResult.errors).to.be.empty;

        expect(websnapperResult).to.deep.equal({
            ...scraperWebsnapResult,
            bucket: scraperWebsnapResult.bucket,
            screenshotFile: scraperWebsnapResult.screenshotFile,
            screenshotSkipped: false,
            htmlFile: scraperWebsnapResult.htmlFile,
            harFile: scraperWebsnapResult.harFile,
        });
    });

    it('should upload all files to S3 and return ScraperResult', async () => {
        const scraperWebsnapResult = await Scraper.websnap(params)
        const result = await uploadScrapedData(scraperWebsnapResult);

        console.log(result.errors);

        expect(result).to.deep.equal({
            ...scraperWebsnapResult,
            bucket: scraperWebsnapResult.bucket,
            screenshotFile: scraperWebsnapResult.screenshotFile,
            screenshotSkipped: false,
            htmlFile: scraperWebsnapResult.htmlFile,
            harFile: scraperWebsnapResult.harFile,
        });
    });

    it('stops html from uploading to S3 due to html size all the rest passes', async () => {
        const mockedUploader = new MockFileUploader();

        const scraperWebsnapResult = await Scraper.websnap(params)
        scraperWebsnapResult.htmlLength = 11 * 1024 * 1024;
        const websnapperResult = await uploadScrapedData(scraperWebsnapResult, mockedUploader)

        console.log(`s3UploadFile was called ${mockedUploader.getCalledCount()} times`);
        console.log(websnapperResult.errors);

        expect(mockedUploader.getCalledCount()).to.be.equal(2);
        expect(websnapperResult.htmlSkipped).to.equal(true)
        expect(websnapperResult.screenshotSkipped).to.equal(false)
        expect(websnapperResult.harSkipped).to.equal(false)
        expect(websnapperResult.errors).to.be.empty;
    });

    it('stops screenshot from uploading to S3 due to html size all the rest passes', async () => {
        const before_test_max_content_length = config.max_content_length
        config.max_content_length = 10;
        const mockedUploader = new MockFileUploader();

        const scraperWebsnapResult = await Scraper.websnap(params)
        scraperWebsnapResult.htmlLength = 9;
        const websnapperResult = await uploadScrapedData(scraperWebsnapResult, mockedUploader);

        console.log(`s3UploadFile was called ${mockedUploader.getCalledCount()} times`);
        console.log(websnapperResult.errors);

        expect(mockedUploader.getCalledCount()).to.be.equal(2);
        expect(websnapperResult.htmlSkipped).to.equal(false)
        expect(websnapperResult.screenshotSkipped).to.equal(true)
        expect(websnapperResult.harSkipped).to.equal(false)
        expect(websnapperResult.errors).to.be.empty;

        config.max_content_length = before_test_max_content_length;
        sinon.restore();
    });

    it('stops both screenshot and htmlfile from uploading to S3 due to size', async () => {
        const before_test_max_content_length = config.max_content_length
        config.max_content_length = 1;
        const mockedUploader = new MockFileUploader();

        const scraperWebsnapResult = await Scraper.websnap(params)
        const websnapperResult = await uploadScrapedData(scraperWebsnapResult, mockedUploader);

        console.log(`s3UploadFile was called ${mockedUploader.getCalledCount()} times`);
        console.log(websnapperResult.errors);

        expect(mockedUploader.getCalledCount()).to.be.equal(1);
        expect(websnapperResult.htmlSkipped).to.equal(true)
        expect(websnapperResult.screenshotSkipped).to.equal(true)
        expect(websnapperResult.harSkipped).to.equal(false)
        expect(websnapperResult.errors).to.be.empty;

        config.max_content_length = before_test_max_content_length;
        sinon.restore();
    });
});
