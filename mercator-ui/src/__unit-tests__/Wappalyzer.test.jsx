import api from "../services/api";
import { cleanup, render, screen, waitFor } from "@testing-library/react";

import Wappalyzer from "../components/detailsCards/Wappalyzer";

describe("Testing Wappalyzer card's rendering with and without correct data", () => {
    afterEach(cleanup);
    jest.mock("../services/api");
    api.get = jest.fn();

    const visitId = "some-visit-id-489";

    test("With correct data from API", async () => {
        await api.get.mockResolvedValue({
            status: 200,
            data: {
                domainName: "testdomain.be",
                error: null,
                ok: true,
                technologies: [
                    {
                        categories: [
                            {
                                id: "489",
                                name: "Security",
                                slug: "security",
                            },
                        ],
                        name: "Company",
                    }
                ],
                visitId: visitId,
                url: "http://testdomain.be",
                urls: []
            }
        });

        render(<Wappalyzer visitId={visitId} />);

        // Card title
        const cardTitle = await waitFor(() => screen.findByText("Wappalyzer"));
        expect(cardTitle).toBeInTheDocument();

        // Url data:
        const urlData = await waitFor(() => screen.findByText("http://testdomain.be"));
        expect(urlData).toBeInTheDocument();

        // Ok data:
        const okData = await waitFor(() => screen.findByText(/true/i));
        expect(okData).toBeInTheDocument();

        // TODO: Figure out how to check the technologies data.
    });

    test("When API returns code 500", async () => {
        await api.get.mockResolvedValue({
            status: 500,
            data: {}
        });
    
        render(<Wappalyzer visitId={visitId} />);
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

    test("When API returns code 404", async () => {
        await api.get.mockResolvedValue({
            status: 404,
            data: {}
        });
    
        render(<Wappalyzer visitId={visitId} />);
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

});