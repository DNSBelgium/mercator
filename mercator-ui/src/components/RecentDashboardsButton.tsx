import {useState, useRef, useEffect} from "react";
import {Button, ButtonGroup, Dropdown, Spinner} from "react-bootstrap";

interface DashboardEntry {
    label: string;
    url: string;
}

function RecentDashboardsButton() {
    const [recentDashboards, setRecentDashboards] = useState<DashboardEntry[] | null>(null);

    async function dashboardExists(month: number, year: number): Promise<DashboardEntry | null> {
        const label = `${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}`;
        const url = (window as any)._env_.REACT_APP_MUPPETS_HOST + `/dashboard/${label}/mercator_full_crawl_dashboard.html`;
        const response = await fetch(url,{ method: "HEAD" } );
        return response.status === 200 ? { label, url } : null;
    }

    async function fetchRecentDashboards(): Promise<DashboardEntry[]> {
        // Max amount of dashboards that are shown (including most recent one)
        const DASHBOARD_LIMIT = 3;
        // Max time in months that is looked back upon
        const MAX_MONTHS_BACK = 5;
        const now = new Date();
        const currentMonthZeroIndexed = now.getMonth();
        const currentYear = now.getFullYear();
        const recentDashboards: DashboardEntry[] = [];
        for (let monthDiff = 0; (monthDiff < MAX_MONTHS_BACK) && (recentDashboards.length < DASHBOARD_LIMIT); monthDiff++) {
            const month = (currentMonthZeroIndexed - monthDiff) % 12;
            const year = (currentMonthZeroIndexed - monthDiff) > 0 ? currentYear : currentYear-1;

            const dashboardOption = await dashboardExists(month, year);
            if (dashboardOption !== null)
                recentDashboards.push(dashboardOption);
        }
        return recentDashboards;
    }

    useEffect(() => {
        (async () => {
            if (recentDashboards === null) {
                setRecentDashboards(await fetchRecentDashboards());
            }
        })();
    });

    function buttonContents() {
        if (recentDashboards === null)
        return (
            <Spinner
                as="span"
                animation="border"
                size="sm"
                role="status"
                aria-hidden="true"
            ></Spinner>
        );
        else
            return (
                <span>Dashboard ( { recentDashboards[0].label }</span>
            );
    }

    function dropdownContents() {
        if (recentDashboards === null)
            return <Dropdown.Menu></Dropdown.Menu>;

        // TODO: expand
        return (
           <Dropdown.Menu>
                <Dropdown.Item href={recentDashboards[0].url} target="_blank">Version of { recentDashboards[0].label }</Dropdown.Item>
           </Dropdown.Menu>
        );

    }

    const recentDashboardsButton = () => {
        return (
            <Dropdown as={ButtonGroup}>
                      <Button disabled={recentDashboards !== null} href={recentDashboards !== null ? recentDashboards[0].url : "#"}>
                          { buttonContents() }
                      </Button>
                      <Dropdown.Toggle split />
                { dropdownContents() }
            </Dropdown>
        );
    };

    return recentDashboardsButton();
}

export default RecentDashboardsButton;