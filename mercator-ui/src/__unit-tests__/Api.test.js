import api from "../services/api";
import { cleanup } from "@testing-library/react";

describe("Example of how to setup the API for testing", () => {
    afterEach(cleanup);
    jest.mock("../services/api");
    api.get = jest.fn();

    test("API is setup correctly for testing", async () => {

        api.get.mockResolvedValue({
            status: 200,
            data: {
                foo: "bar"
            }
        });
    
        const url = `/some/url`;
        const result = await api.get(url);
        // console.log(result);
        
    });

});

