import { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';

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

  return (
      <div className="App">
        <NavigationBar setSearch={setSearch} />

        <Routes>

          <Route path='/*' element={<Navigate to="/1" replace />} />
          
          <Route path="/:id" element={<TimelineDomainName search={search} />} />
          <Route path="/details/:visitId" element={<Details />} />
          <Route path="/cluster" element={<ClusterValidator clusterData={clusterData} setClusterData={setClusterData} />} />

        </Routes>
      </div>
  );
}

export default App;

/*

To the future Frontend Developer:
Read my files from bottom to top, they will probably make more sense.
- AroenvR

*/
