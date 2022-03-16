import * as Scraper from './scraper';
import {ScraperParams} from './scraper';
import {expect} from 'chai';
import 'mocha';
import sinon from 'sinon';
import {v4 as uuid} from 'uuid';
import * as path from "path";
import {convertDate} from "./util";

describe('Scraper Tests', function () {
    this.timeout(5000);

    const mockUuid = '1fe39e26-9d20-4cc0-8696-fe7887a3dfbc';
    sinon.stub(uuid, 'v4').returns(mockUuid);

    it('convertDate should format a date as yyyy-mm-dd', () => {
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
            }
        };

        let folder = path.join("output", "dnsbelgium.be", convertDate(new Date()), 'https', 'dnsbelgium.be', 'index.html', mockUuid);
        return Scraper.websnap(params).then(scraperResult => {

            expect(scraperResult).to.have.property('hostname', 'dnsbelgium.be');
            expect(scraperResult).to.have.property('url', 'https://www.dnsbelgium.be/');
            expect(scraperResult).to.have.property('request', );
            expect(scraperResult.request).to.have.eql({...params});
            expect(scraperResult).to.have.property('referer', '');
            expect(scraperResult).to.have.property('htmlLength');
            expect(scraperResult).to.have.property('pageTitle');
            expect(scraperResult).to.have.property('metrics');
            expect(scraperResult).to.have.property('folder', folder);
            expect(scraperResult).to.have.property('harFile', path.join(folder, 'dnsbelgium.be.har'));
            expect(scraperResult).to.have.property('htmlFile', path.join(folder, 'index.html'));
            expect(scraperResult).to.have.property('screenshot', path.join(folder, 'screenshot.png'));

        });

    });

});
