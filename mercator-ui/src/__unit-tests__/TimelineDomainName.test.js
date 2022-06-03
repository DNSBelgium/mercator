import api from "../services/api";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import {BrowserRouter as Router} from 'react-router-dom';

import TimelineDomainName from "../components/timelineCards/TimelineDomainName";

// Setting up the useNavigate hook.
const mockedUsedNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
   ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockedUsedNavigate,
}));

// Setting up the useParams hook.
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useParams: () => ({
      domain: 'dnsbelgium.be',
      page: 1,
    }),
    useRouteMatch: () => ({ url: 'http://localhost:3000/dnsbelgium.be/1' }),
}));

const visitId = "some-visit-id-489";

describe("Testing Timeline table's rendering with and without correct data", () => {
    afterEach(cleanup);
    jest.mock("../services/api");
    api.get = jest.fn();

    test("Timeline table should render correctly with correct data", async () => {
        await api.get.mockResolvedValue({
            status: 200,
            data: {
                amountOfPages: 1,
                amountOfRecords: 1,
                dtos: [
                    {
                        crawlStatus: { dns: true, muppets: true, smtp: true, wappalyzer: true, visitId: visitId },
                        domainName: "dnsbelgium.be",
                        requestTimestamp: "2022-06-02T12:00:00.000Z",
                        screenshotKey: "some-screenshot-key-123",
                        visitId: visitId,
                    }, 
                ],
                hasNext: false,
                hasPrevious: false,
            }
        });

        render(
            <Router>
                <TimelineDomainName />
            </Router>
        );

        // Table gets rendered
        const table = await waitFor(() => screen.findByRole("table"));
        expect(table).toBeInTheDocument();

        // Page has correct domain
        const domainName = await waitFor(() => screen.findByText("dnsbelgium.be"));
        expect(domainName).toBeInTheDocument();

        // Number of records
        const records = await waitFor(() => screen.findByText("Number of records: 1"));
        expect(records).toBeInTheDocument();

        // Note: Actual crawl timestamp is (currently) a Bootstrap Link element.

        // Check for "Copy Visit Id" button to assert at least 1 row is rendered.
        const copyVisitIdButton = await waitFor(() => screen.findByText("Copy Visit Id"));
        expect(copyVisitIdButton).toBeInTheDocument();

        // Asserting the visitId exists by grabbing the data-testid tag from the "Copy Visit Id" button.
        const visitIdTag = await waitFor(() => screen.findByTestId(visitId));
        expect(visitIdTag).toBeInTheDocument();
    });

    test("Timeline table should not render when given faulty data", async () => {
        await api.get.mockResolvedValue({
            status: 200,
            data: {}
        });

        render(
            <Router>
                <TimelineDomainName />
            </Router>
        );
        
        // Error message gets rendered
        const error = await waitFor(() => screen.findByText(/Apologies, something went wrong./i));
        expect(error).toBeInTheDocument();

    });

    // TODO: Figure out why I cannot trigger the handleExResponse function.

});