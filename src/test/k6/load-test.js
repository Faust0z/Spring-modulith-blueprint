import http from 'k6/http';
import { check, sleep } from 'k6';
import { jUnit, textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

export function handleSummary(data) {
  return {
    'k6-report.xml': jUnit(data),
    'summary.txt': textSummary(data, { indent: ' ', enableColors: false }),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

export const options = {
  scenarios: {
    stress: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 200,
      maxDuration: '10m',
    },
  },
  noCookiesReset: true,
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<5000'],
  },
};

const BASE_URL = 'https://b2b-system.onrender.com';

export function setup() {
  const loginPayload = JSON.stringify({ identifier: __ENV.DEMO_USER, password: __ENV.DEMO_PASSWORD });
  const loginParams = { headers: { 'Content-Type': 'application/json' } };
  const loginRes = http.post(`${BASE_URL}/v1/auth/login`, loginPayload, loginParams);

  if (loginRes.status !== 200) {
    throw new Error(`Setup failed [${loginRes.status}]: ${loginRes.body}`);
  }

  const auth = loginRes.json();
  const rawCookies = loginRes.headers['Set-Cookie'] || [];

  return {
    tenantId: auth.tenant.id,
    token: auth.token,
    cookies: Array.isArray(rawCookies) ? rawCookies : [rawCookies]
  };
}

export default function (data) {
  const params = {
    headers: {
      'X-tenant-id': data.tenantId,
      'Content-Type': 'application/json'
    }
  };

  if (data.token) {
    params.headers['Authorization'] = `Bearer ${data.token}`;
  } else if (data.cookies.length > 0) {
    params.headers['Cookie'] = data.cookies.join('; ');
  }

  const responses = http.batch([
    ['GET', `${BASE_URL}/v1/gyms/entry-logs`, null, params],
    ['GET', `${BASE_URL}/v1/gyms/members`, null, params],
    ['GET', `${BASE_URL}/v1/gyms/membership-sales`, null, params],
    ['GET', `${BASE_URL}/v1/gyms/worker-shifts`, null, params],
  ]);

  responses.forEach(res => {
    check(res, { 'domain status 200': (r) => r.status === 200 });
  });

  sleep(Math.random() * 3 + 2);
}