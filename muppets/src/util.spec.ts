import { convertDate, getDomainName } from './util.js';
import { expect } from 'chai';

describe('Util functions', () => {

    it('convertDate should format a date as yyyy-mm-dd', () => {
        const date = new Date(2019, 11, 25);
        expect(convertDate(date)).to.equal('2019/12/25');
    });

    it('getDomainName should return the domainname from a given url', () => {
        expect(getDomainName("google.be")).to.equal("google.be");
        expect(getDomainName("www.google.be")).to.equal("google.be");
        expect(getDomainName("many.sub.domain.google.be")).to.equal("google.be");
    });

});
