import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VOUCHER_CODE = __ENV.VOUCHER_CODE || 'PERF-GENERIC-001';

const MODE = __ENV.MODE || 'same_batch';
const RATE = Number(__ENV.TARGET_RATE || 100);
const DURATION = __ENV.DURATION || '1m';

const successes = new Counter('contention_success');
const conflicts = new Counter('contention_conflicts');
const otherResponses = new Counter('contention_other');

export const options = {
    scenarios: {
        contention: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: 100,
            maxVUs: 500,
        },
    },

    thresholds: {
        http_req_duration: ['p(95)<1000'],
        dropped_iterations: ['count==0'],
    },
};

export default function () {
    const iteration = exec.scenario.iterationInTest;

    const customerId = MODE === 'same_customer'
        ? 'PERF-SAME-CUSTOMER'
        : `PERF-CONTENTION-${iteration}`;

    const payload = JSON.stringify({
        voucherCode: VOUCHER_CODE,
        customerId,
        cartSubtotal: 100.00,
        customerSegments: [],
        items: [
            {
                productId: 'PRODUCT-01',
                quantity: 1,
                unitPrice: 100.00,
            },
        ],
    });

    const response = http.post(
        `${BASE_URL}/redemptions`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
                'X-Device-Fingerprint': `CONTENTION-${iteration}`,
            },
            timeout: '10s',
        }
    );

    if (response.status === 200) {
        successes.add(1);
    } else if (response.status === 409) {
        conflicts.add(1);
    } else {
        otherResponses.add(1);
    }

    check(response, {
        'request completed': (r) => r.status !== 0,
    });
}
