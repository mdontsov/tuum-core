import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

export const options = {
  scenarios: {
    transactions: {
      executor: 'constant-vus',
      vus: 40,
      duration: '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const headers = { headers: { 'Content-Type': 'application/json' } };
let accountId;

function initializeAccount() {
  const customer = `perf-vu-${exec.vu.idInTest}-${Date.now()}`;
  const accountResponse = http.post(`${baseUrl}/api/v1/accounts`, JSON.stringify({
    customerId: customer,
    country: 'EE',
    currencies: ['EUR'],
  }), headers);
  check(accountResponse, { 'account created': response => response.status === 201 });
  accountId = accountResponse.json('accountId');

  const depositResponse = http.post(`${baseUrl}/api/v1/transactions`, JSON.stringify({
    accountId,
    amount: 100000,
    currency: 'EUR',
    direction: 'IN',
    description: 'Performance test initial balance',
  }), headers);
  check(depositResponse, { 'initial deposit created': response => response.status === 201 });
}

export default function () {
  if (!accountId) initializeAccount();
  const response = http.post(`${baseUrl}/api/v1/transactions`, JSON.stringify({
    accountId,
    amount: 1,
    currency: 'EUR',
    direction: __ITER % 2 === 0 ? 'OUT' : 'IN',
    description: 'Performance test transaction',
  }), headers);
  check(response, { 'transaction created': result => result.status === 201 });
}
