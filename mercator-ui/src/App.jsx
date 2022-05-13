import { useState } from 'react';
import { Routes, Route } from 'react-router-dom';

import Details from './components/Details';
import NavigationBar from './components/NavigationBar';

import './App.scss';
import 'bootstrap/dist/css/bootstrap.min.css';
import TimelineDomainName from './components/timelineCards/TimelineDomainName';
import ClusterValidator from './components/ClusterValidator';

function App() {
  // The following hooks are to increase routing possibilities between pages as well as multiple tabs support.
  const [clusterData, setClusterData] = useState([]); // Hook to contain ClusterValidator's data.
  const [search, setSearch] = useState(null); // Hook to hold NavigationBar's search input.
  const [page, setPage] = useState(0); // Hook to hold TimelineDomainName's current page.

  return (
      <div className="App">
        <NavigationBar setSearch={setSearch} setPage={setPage}/>

        <Routes>

          <Route path="/*" element={<TimelineDomainName search={search} page={page} setPage={setPage}/>} />
          <Route path="/details/:visitId" element={<Details />} />
          <Route path="/cluster" element={<ClusterValidator clusterData={clusterData} setClusterData={setClusterData} />} />

        </Routes>
      </div>
  );
}

export default App;
