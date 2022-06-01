import api from "../services/api";
import { cleanup, render, screen, waitFor } from "@testing-library/react";

import DispatcherCard from "../components/detailsCards/DispatcherCard";

describe("Testing Dispatcher card's rendering with and without data", () => {
    afterEach(cleanup);
    jest.mock("../services/api");
    api.get = jest.fn();

    const visitId = "5f0f8f8f-f8f8-f8f8-f8f8-f8f8f8f8f8f8";
    
    test("WITH data", async () => {
        await api.get.mockResolvedValue({
            status: 200,
            data: {
                visitId: visitId,
                domainName: "testDomain",
                labels: ["test-label"]
            }
        });
    
        render(<DispatcherCard visitId={visitId} />);
    
        const domainName = await waitFor(() => screen.findByText("testDomain"));
        expect(domainName).toBeInTheDocument();
        
        const visitIdText = await waitFor(() => screen.findByText(/Visit id:/i));
        expect(visitIdText).toBeInTheDocument();
    
        const visitIdValue = await waitFor(() => screen.findByText(visitId));
        expect(visitIdValue).toBeInTheDocument();
    
        const labelsText = await waitFor(() => screen.findByText(/Labels:/i));
        expect(labelsText).toBeInTheDocument();
    
        const labelsValue = await waitFor(() => screen.findByText("test-label"));
        expect(labelsValue).toBeInTheDocument();
    });
    
    test("WITHOUT data",  () => {
        api.get.mockResolvedValue({
            status: 500,
            data: {}
        });
    
        render(<DispatcherCard visitId={visitId} />);
    
        const noDataText = screen.getByText(/no data for this visit/i);
        expect(noDataText).toBeInTheDocument();
    });

});