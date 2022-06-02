import api from "../services/api";
import { cleanup, render, screen, waitFor } from "@testing-library/react";

import VATCard from "../components/detailsCards/VATCard";

describe("Testing VATCard's rendering with and without correct data", () => {
    afterEach(cleanup);
    jest.mock("../services/api");
    api.get = jest.fn();

    const visitId = "some-visit-id-489";

    test("With correct data from API", async () => {
        await api.get.mockResolvedValue({
            status: 200,
            data: {
                crawlFinished: "2022-06-01T15:00:00.045Z",
                crawlStarted: "2022-06-01T15:00:00.000Z",
                domainName: "testdomain.be",
                id: "489",
                matchingUrl: "https://testdomain.be/en/contact-us",
                startUrl: "http://testdomain.be/",
                vatValues: ["BE123456789"],
                visitId: visitId,
                visitedUrls: [
                    {
                        0: "https://testdomain.be/en/"
                    },
                    {
                        1: "https://testdomain.be/en/contact"
                    },
                ],
            }
        });

        render(<VATCard visitId={visitId} />);

        // Card title
        const cardTitle = await waitFor(() => screen.findByText(/VAT crawl/i));
        expect(cardTitle).toBeInTheDocument();

        // Id data
        const cardContentId = await waitFor(() => screen.findByText(/489/i));
        expect(cardContentId).toBeInTheDocument();

        // Crawl timestamp data --> +2 hours? Why?
        const cardContentCrawlTimestamp = await waitFor(() => screen.findByText(/01\/06\/2022 17:00:00/i));
        expect(cardContentCrawlTimestamp).toBeInTheDocument();

        // Crawl duration data
        const cardContentCrawlDuration = await waitFor(() => screen.findByText(/45 ms/i));
        expect(cardContentCrawlDuration).toBeInTheDocument();

        // VAT values data
        const cardContentVATValues = await waitFor(() => screen.findByText(/BE123456789/i));
        expect(cardContentVATValues).toBeInTheDocument();

        // URL data
        const cardContentUrl = await waitFor(() => screen.findByText(/http:\/\/testdomain.be\//i));
        expect(cardContentUrl).toBeInTheDocument();

        // Matching URL data
        const cardContentMatchingUrl = await waitFor(() => screen.findByText(/https:\/\/testdomain.be\/en\/contact-us/i));
        expect(cardContentMatchingUrl).toBeInTheDocument();

        // # URLs followed data
        const cardContentNumUrls = await waitFor(() => screen.findByText("2"));
        expect(cardContentNumUrls).toBeInTheDocument();

        // Visited URL 1's data
        const cardContentVisitedUrl1 = await waitFor(() => screen.findByText(/https:\/\/testdomain.be\/en\//i));
        expect(cardContentVisitedUrl1).toBeInTheDocument();

        // // Visited URL 2's data
        const cardContentVisitedUrl2 = await waitFor(() => screen.findByText(/https:\/\/testdomain.be\/en\/contact/i));
        expect(cardContentVisitedUrl2).toBeInTheDocument();

        // Note that Visited URLs don't get rendered until "more info" is clicked, however they do exist.
        // I do not yet know how to mock true rendering.
    });

    test("When API returns code 500", async () => {
        await api.get.mockResolvedValue({
            status: 500,
            data: {}
        });
    
        render(<VATCard visitId={visitId} />);

        const cardTitle = await waitFor(() => screen.findByText(/VAT crawl/i));
        expect(cardTitle).toBeInTheDocument();
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

    test("When API returns code 404", async () => {
        await api.get.mockResolvedValue({
            status: 404,
            data: {}
        });
    
        render(<VATCard visitId={visitId} />);

        const cardTitle = await waitFor(() => screen.findByText(/VAT crawl/i));
        expect(cardTitle).toBeInTheDocument();
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

});