import api from "../services/api";
import { cleanup, render, screen, waitFor } from "@testing-library/react";

import DispatcherCard from "../components/detailsCards/DispatcherCard";

describe("Testing Dispatcher card's rendering with and without correct data", () => {
    afterEach(cleanup);
    jest.mock("../services/api");
    api.get = jest.fn();

    const visitId = "some-visit-id-489";
    
    test("When API returns correct data", async () => {
        await api.get.mockResolvedValue({
            status: 200,
            data: {
                visitId: visitId,
                domainName: "testdomain.be",
                labels: ["test-label"]
            }
        });
    
        render(<DispatcherCard visitId={visitId} />);
    
        const domainName = await waitFor(() => screen.findByText(/testdomain.be/i));
        expect(domainName).toBeInTheDocument();
        
        const visitIdText = await waitFor(() => screen.findByText(/Visit id:/i));
        expect(visitIdText).toBeInTheDocument();
    
        const visitIdValue = await waitFor(() => screen.findByText(visitId));
        expect(visitIdValue).toBeInTheDocument();
    
        const labelsText = await waitFor(() => screen.findByText(/Labels:/i));
        expect(labelsText).toBeInTheDocument();
    
        const labelsValue = await waitFor(() => screen.findByText(/test-label/i));
        expect(labelsValue).toBeInTheDocument();
    });
    
    test("When API returns code 500", async () => {
        api.get.mockResolvedValue({
            status: 500,
            data: {}
        });
    
        render(<DispatcherCard visitId={visitId} />);
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

    test("When API returns code 404", async () => {
        api.get.mockResolvedValue({
            status: 404,
            data: {}
        });
    
        render(<DispatcherCard visitId={visitId} />);
    
        const noDataText = await waitFor(() => screen.findByText(/no data for this visit/i));
        expect(noDataText).toBeInTheDocument();
    });

});