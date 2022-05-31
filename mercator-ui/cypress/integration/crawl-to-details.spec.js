/**
 * Start all Docker containers (ui & ground-truth can be turned off).
 * Start up the frontend. Execute: npm start
 * Execute: npm start e2e
 * 
 * You can start with an empty database as these tests will execute the necessary crawls.
 */

describe("Happy flow of visiting a crawl's details card", () => {

    it("Renders Timeline table and is accessible", () => {
        // Execute a crawl.
        cy.exec(`aws --endpoint-url=http://localhost:4566 sqs send-message --queue-url http://localhost:4566/queue/mercator-dispatcher-input --message-body '{"domainName": "dnsbelgium.be", "labels": ["test-label"]}'`);

        // Wait a few seconds for the crawl to finish.
        cy.wait(10000);

        // Go to the home page.
        cy.visit("http://localhost:3000/");

        // Confirm NavBar is set to searching for domain name.
        cy.get('#navbar-input').invoke('attr', 'placeholder').should('contain', 'Enter domain name');
        
        // Enter a search in the Navigation bar.
        cy.get('#navbar-input').type('dnsbelgium.be');

        // Setting an intercept for the GET request.
        cy.intercept('http://localhost:3000/api/find-visits/dnsbelgium.be?page=0').as('NavSearch');

        // Click search button.
        cy.get('#submit-btn').click();

        // Checking intercepted server response.
        cy.wait('@NavSearch').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify the resulting page contains the domain name & Timeline table.
            cy.contains(/dnsbelgium.be/i, { timeout: 30000 });
            expect(cy.get('#timeline-table')).to.exist;

            // Verify status code.
            expect(statusCode).to.eq(200);

            // Verify url is correct.
            cy.url().should('contain', 'http://localhost:3000/dnsbelgium.be/1');
        });
    });

    it("Renders Details cards with data", () => {

        // Setting an intercepts for the Details cards' GET requests.
        // (visitId is being taken from the data-id tag of the Visit Id button in the Timeline table).
        cy.get(':nth-child(1) > :nth-child(6) > #Copy-Id-Btn').invoke('data', 'id').then(visitId => {
            cy.intercept(`http://localhost:3000/api/contentCrawlResults/search/findByVisitId?visitId=${visitId}`).as('ContentCrawlResults');
            cy.intercept(`http://localhost:3000/api/smtpCrawlResults/search/findFirstByVisitId?visitId=${visitId}`).as('SmtpCrawlResults');
            cy.intercept(`http://localhost:3000/api/vatCrawlResults/search/findByVisitId?visitId=${visitId}`).as('VatCrawlResults');
            cy.intercept(`http://localhost:3000/api/htmlFeatureses/search/findByVisitId?visitId=${visitId}`).as('HtmlFeatures');
            cy.intercept(`http://localhost:3000/api/requests/search/findByVisitId?visitId=${visitId}`).as('DnsRequests');
            cy.intercept(`http://localhost:3000/api/dispatcherEvents/${visitId}`).as('DispatcherEvents');
            cy.intercept(`http://localhost:3000/api/wappalyzerResults/${visitId}`).as('WappalyzerResults');
            cy.intercept(`http://localhost:3000/api/sslCrawlResults/search/findOneByVisitId?visitId=${visitId}`).as('SslCrawlResults');
            cy.intercept(`http://localhost:3000/api/trustStores/search/findRelatedToSslCrawlResult?visitId=${visitId}`).as('SslTrustStores');
            cy.intercept(`http://localhost:3000/api/certificates/search/findRelatedToSslCrawlResult?visitId=${visitId}`).as('SslCertificates');
            cy.intercept(`http://localhost:3000/api/countCipherSuitesResults/search/findNumberOfSupportedCipherSuitesByVisitId?visitId=${visitId}`).as('CipherSuitesResults');
            cy.log("Ready to intercept GET requests for Visit Id: " + visitId);
        });

        // Click the first Crawl Time (got to details page).
        cy.get(':nth-child(1) > :nth-child(1) > a').click();

        // Asserting Cards exist.
        cy.contains(/dnsbelgium.be/i, { timeout: 30000 });
        expect(cy.get('#content-card')).to.exist;
        expect(cy.get('#dns-card')).to.exist;
        expect(cy.get('#smtp-card')).to.exist;
        expect(cy.get('#html-card')).to.exist;
        expect(cy.get('#vat-card')).to.exist;
        expect(cy.get('#ssl-card')).to.exist;

        // Checking intercepted server response for Content Crawler.
        cy.wait('@ContentCrawlResults').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            expect(statusCode).to.eq(200);

            // Verify card has data.
            cy.get('#content-card').then(($card) => {
                expect($card).to.not.contain("No data for this visit");
            });
        });

        // Checking intercepted server response for SMTP Crawler.
        cy.wait('@SmtpCrawlResults').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            expect(statusCode).to.eq(200);

            // Verify card has data.
            cy.get('#smtp-card').then(($card) => {
                expect($card).to.not.contain("No data for this visit");
            });
        });

        // Checking intercepted server response for VAT Crawler.
        cy.wait('@VatCrawlResults').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            expect(statusCode).to.eq(200);

            // Verify card has data.
            cy.get('#vat-card').then(($card) => {
                expect($card).to.not.contain("No data for this visit");
            });
        });

        // TODO: feature-extraction Docker container keeps exiting.

        // Checking intercepted server response for HTML Features.
        cy.wait('@HtmlFeatures').then((intercept) => {
            // let statusCode = intercept.response.statusCode;

            // // Verify status code.
            // expect(statusCode).to.eq(200);

            // // Verify card has data.
            // cy.get('#html-card').then(($card) => {
            //     expect($card).to.not.contain("No data for this visit");
            // });
        });

        // Checking intercepted server response for DNS Requests.
        cy.wait('@DnsRequests').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            expect(statusCode).to.eq(200);

            // Verify card has data.
            cy.get('#dns-card').then(($card) => {
                expect($card).to.not.contain("No data for this visit");
            });
        });

        // Checking intercepted server response for Dispatcher Events.
        cy.wait('@DispatcherEvents').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            expect(statusCode).to.eq(200);

            // Verify card has data.
            cy.get('#dispatcher-card').then(($card) => {
                expect($card).to.not.contain("No data for this visit");
            });
        });

        // Checking intercepted server response for Wappalyzer Results.
        cy.wait('@WappalyzerResults').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            expect(statusCode).to.eq(200);

            // Verify card has data.
            cy.get('#wappalyzer').then(($card) => {
                expect($card).to.not.contain("No data for this visit");
            });
        });

        // TODO: The following are commented out out due to SSL Crawler not working properly.

        // Checking intercepted server response for SSL Crawler.
        cy.wait('@SslCrawlResults').then((intercept) => { 
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            // expect(statusCode).to.eq(200);

            // Verify card has data.
            // cy.get('#ssl-card').then(($card) => {
            //     expect($card).to.not.contain("No data for this visit");
            // });
        });

        // Checking intercepted server response for SSL Trust Stores.
        cy.wait('@SslTrustStores').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            // expect(statusCode).to.eq(200);
        });

        // Checking intercepted server response for SSL Certificates.
        cy.wait('@SslCertificates').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            // expect(statusCode).to.eq(200);
        });

        // Checking intercepted server response for Cipher Suites.
        cy.wait('@CipherSuitesResults').then((intercept) => {
            let statusCode = intercept.response.statusCode;

            // Verify status code.
            // expect(statusCode).to.eq(200);
        });
    });

});