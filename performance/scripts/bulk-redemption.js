import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

// =========================================================
// CONFIGURATION
// =========================================================

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const DEVICE_PREFIX = __ENV.DEVICE_PREFIX || 'PERF-DEVICE';

const TARGET_RATE = Number(__ENV.TARGET_RATE || 500);
const DURATION = __ENV.DURATION || '5m';

const DATASET_SIZE = Number(__ENV.DATASET_SIZE || 200000);

const PRE_VUS = Number(__ENV.PRE_VUS || 300);
const MAX_VUS = Number(__ENV.MAX_VUS || 1500);

// =========================================================
// K6 OPTIONS
// =========================================================

export const options = {
    scenarios: {
        bulk_redemption: {
            executor: 'constant-arrival-rate',

            rate: TARGET_RATE,
            timeUnit: '1s',
            duration: DURATION,

            preAllocatedVUs: PRE_VUS,
            maxVUs: MAX_VUS,

            gracefulStop: '30s',
        },
    },

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        dropped_iterations: ['count==0'],
        checks: ['rate>0.99'],
    },
};

// =========================================================
// MAIN TEST
// =========================================================

export default function () {
    // Each iteration gets a unique index.
    const index = exec.scenario.iterationInTest;

    // Avoid generating IDs outside the prepared dataset.
    if (index >= DATASET_SIZE) {
        console.error(`Dataset exhausted: iteration=${index}`);
        return;
    }

    // Generate matching voucher/customer identifiers.
    const suffix = String(index);
    const customerId = `PERF-BULK-${suffix}`;
    const voucherCode = `PERF-BULK-${suffix}`;
    const fingerprint = `${DEVICE_PREFIX}-${suffix}`;

    const url = `${BASE_URL}/redemptions`;

    const payload = JSON.stringify({
        voucherCode: voucherCode,
        customerId: customerId,

        cartSubtotal: 100000,

        customerSegments: [],

        items: [
            {
                productId: 'PERF-PRODUCT-001',
                quantity: 1,
                unitPrice: 100000,
            },
        ],
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Device-Fingerprint': fingerprint,
        },

        tags: {
            endpoint: 'bulk-redemption',
        },
    };

    const response = http.post(url, payload, params);

    check(response, {
        'status is successful': (r) => r.status === 200,
        'response time below 500ms': (r) => r.timings.duration < 500,
    });

}

export function handleSummary(data) {
    return {
        '/results/bulk-redemption-summary.json':
            JSON.stringify(data, null, 2),
    };
}