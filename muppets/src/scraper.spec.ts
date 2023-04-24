import * as Scraper from './scraper';
import { ScraperParams } from './scraper';
import { s3UploadFile, uploadToS3 } from './websnapper';
import { expect } from 'chai';
import { v4 as uuid } from 'uuid';
import config from "./config";
import sinon from 'sinon';

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

describe('Scraper Tests', function () {
    this.timeout(30000);

    it('should upload files to S3 and return ScraperResult', async () => {

        const scraperWebsnapResult = await Scraper.websnap(params)
        const result = await uploadToS3(scraperWebsnapResult);

        console.log(result.errors);

        expect(result).to.deep.equal({
            ...scraperWebsnapResult,
            bucket: scraperWebsnapResult.bucket,
            screenshotFile: scraperWebsnapResult.screenshotFile,
            screenshotSkipped: false,
            htmlFile: scraperWebsnapResult.htmlFile,
            harFile: scraperWebsnapResult.harFile,
        });
        sinon.restore();
    });
});

describe('Scraper Tests', function () {
    this.timeout(30000);

    it('dns should succeed and upload', async () => {
        const s3UploadFileSpy = sinon.spy(s3UploadFile);

        const scraperWebsnapResult = await Scraper.websnap(params)
        const websnapperResult = await uploadToS3(scraperWebsnapResult,s3UploadFileSpy)

        console.log(`s3UploadFile was called ${await s3UploadFileSpy.callCount} times`);
        console.log(websnapperResult.errors);

        expect(s3UploadFileSpy.calledThrice).to.be.true;
        expect(s3UploadFileSpy.callCount).to.be.equal(3)
        expect(websnapperResult.screenshotSkipped).to.equal(false)
        expect(websnapperResult.errors).to.be.empty;

        sinon.restore();
    });
});

describe('Scraper Tests', function () {
    this.timeout(30000);

    it('dns should not upload screenshot due to html-file size but not retry because of it', async () => {
        const before_test_max_content_length = config.max_content_length
        config.max_content_length = 1;

        const s3UploadFileSpy = sinon.spy(s3UploadFile);

        const scraperWebsnapResult = await Scraper.websnap(params)
        const websnapperResult = await uploadToS3(scraperWebsnapResult,s3UploadFileSpy)

        console.log(`s3UploadFile was called ${await s3UploadFileSpy.callCount} times`);
        console.log(websnapperResult.errors);

        expect(s3UploadFileSpy.calledTwice).to.be.true;
        expect(s3UploadFileSpy.callCount).to.be.equal(2)
        expect(websnapperResult.screenshotSkipped).to.equal(true)
        expect(websnapperResult.errors).to.be.empty;

        config.max_content_length = before_test_max_content_length;
        sinon.restore();
    });
});

