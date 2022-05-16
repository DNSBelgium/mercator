import { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';

import 'bootstrap/dist/css/bootstrap.min.css';

import './App.scss';
import Details from './components/Details';
import NavigationBar from './components/NavigationBar';
import TimelineDomainName from './components/timelineCards/TimelineDomainName';
import ClusterValidator from './components/ClusterValidator';
import Home from './components/Home';

function App() {
  // The following hooks are to increase routing possibilities between pages as well as multiple tabs support.
  const [clusterData, setClusterData] = useState([]); // Hook to contain ClusterValidator's data.

  return (
      <div className="App">
        <NavigationBar />

        <Routes>

          <Route path='/*' element={<Home />} />
          
          <Route path=":domain/:page" element={<TimelineDomainName />} />
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
