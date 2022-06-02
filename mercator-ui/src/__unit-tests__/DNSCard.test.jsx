import api from "../services/api";
import { cleanup, render, screen, waitFor } from "@testing-library/react";

import DNSCard from "../components/detailsCards/DNSCard";

describe("Testing DNSCard's rendering with and without correct data", () => {
    afterEach(cleanup);
    jest.mock("../services/api");
    api.get = jest.fn();

    const visitId = "some-visit-id-489";

    test("With correct data from API", async () => {
        await api.get.mockResolvedValue({
            status: 200,
            data: {
                _embedded: {
                    requests: [
                        {
                            crawlTimestamp: "2022-06-01T14:30:00.000Z",
                            domainName: "testdomain.be",
                            id: "489",
                            numOfResponses: 0,
                            ok: true,
                            prefix: "@",
                            problem: "Normally this would be null, but for testing purposes it's set to this string",
                            rcode: 0,
                            recordSignatures: [{}, {}],
                            recordType: "SOA",
                            responses: [{}, {}],
                            visitId: visitId,
                        },
                    ],
                },
            }
        });

        render(<DNSCard visitId={visitId} />);

        // Card title:
        const cardTitle = await waitFor(() => screen.findByText(/DNS crawl/i));
        expect(cardTitle).toBeInTheDocument();

        // rcode's data is kind of impossible to check.

        // Ok's data:
        const cardContentOk = await waitFor(() => screen.findByText(/true/i));
        expect(cardContentOk).toBeInTheDocument();

        // Problem's data:
        const cardContentProblem = await waitFor(() => screen.findByText(/Normally this would be null, but for testing purposes it's set to this string/i));
        expect(cardContentProblem).toBeInTheDocument();

        // Crawl timestamp's data: --> +2 hours? Why?
        const cardContentTechnologies = await waitFor(() => screen.findByText("01/06/2022 16:30:00"));
        expect(cardContentTechnologies).toBeInTheDocument();

        // Record data and Geo IP's data's "more info" button: (only exists if arrays are not empty)
        const moreInfoButton = await waitFor(() => screen.findByText(/More info/i));
        expect(moreInfoButton).toBeInTheDocument();
    });

    // TODO: Write a test with actual Responses and GeoIP's, and 'click' the "more info" button.

    test("When API returns code 500", async () => {
        await api.get.mockResolvedValue({
            status: 500,
            data: {}
        });
    
        render(<DNSCard visitId={visitId} />);

        const cardTitle = await waitFor(() => screen.findByText(/DNS crawl/i));
        expect(cardTitle).toBeInTheDocument();
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

    test("When API returns code 404", async () => {
        await api.get.mockResolvedValue({
            status: 404,
            data: {}
        });
    
        render(<DNSCard visitId={visitId} />);

        const cardTitle = await waitFor(() => screen.findByText(/DNS crawl/i));
        expect(cardTitle).toBeInTheDocument();
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

});