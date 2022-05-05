import { useState } from 'react';
import { Routes, Route } from 'react-router-dom';

import Details from './components/Details';
import NavigationBar from './components/NavigationBar';

import './App.scss';
import 'bootstrap/dist/css/bootstrap.min.css';
import TimelineDomainName from './components/timelineCards/TimelineDomainName';

function App() {
  const [search, setSearch] = useState(null);
  const [page, setPage] = useState(0);

  return (
      <div className="App">
        <NavigationBar setSearch={setSearch}/>

        <Routes>

          <Route path="/*" element={<TimelineDomainName search={search} setSearch={setSearch} page={page} setPage={setPage}/>} />
          <Route path="/details/:visitId" element={<Details />} />

        </Routes>
      </div>
  );
}

export default App;
