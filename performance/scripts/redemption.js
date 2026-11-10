import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VOUCHER_CODE = __ENV.VOUCHER_CODE || 'PERF-GENERIC-001';

const CUSTOMER_PREFIX = __ENV.CUSTOMER_PREFIX || 'PERF';
const DEVICE_PREFIX = __ENV.DEVICE_PREFIX || 'PERF-DEVICE';

const MODE = __ENV.MODE || 'ramp';
const TARGET_RATE = Number(__ENV.TARGET_RATE || 500);
const DURATION = __ENV.DURATION || '5m';

const successful = new Counter('redemption_success');
const rejected = new Counter('redemption_rejected');
const serverErrors = new Counter('redemption_server_errors');
const unexpected = new Counter('redemption_unexpected');

const successRate = new Rate('redemption_success_rate');
const businessLatency = new Trend('redemption_latency');

const BATCH_MODE = (__ENV.BATCH_MODE || 'single').toLowerCase();
const BATCH_COUNT = __ENV.BATCH_COUNT;
const VOUCHER_CODES = Array.from(
    { length: BATCH_COUNT },
    (_, index) => `PERF-MULTI-${String(index + 1).padStart(3, '0')}`
);

if (!['single', 'distributed'].includes(BATCH_MODE)) {
    throw new Error(`Invalid BATCH_MODE: ${BATCH_MODE}`);
}

if (BATCH_MODE === 'distributed' && VOUCHER_CODES.length < 2) {
    throw new Error(
        'Distributed mode requires at least 2 VOUCHER_CODES'
    );
}

export const options = {
    summaryTrendStats: [
        'avg',
        'min',
        'med',
        'max',
        'p(90)',
        'p(95)',
        'p(99)',
    ],

    scenarios: {
        redemption_load: MODE === 'constant'
            ? {
                executor: 'constant-arrival-rate',
                rate: TARGET_RATE,
                timeUnit: '1s',
                duration: DURATION,
                preAllocatedVUs: Number(__ENV.PRE_VUS || 100),
                maxVUs: Number(__ENV.MAX_VUS || 1000),
            }
            : {
                executor: 'ramping-arrival-rate',
                startRate: 50,
                timeUnit: '1s',
                stages: [
                    { target: 100, duration: '1m' },
                    { target: 200, duration: '2m' },
                    { target: 350, duration: '2m' },
                    { target: 500, duration: '2m' },
                    { target: 500, duration: DURATION },
                ],
                preAllocatedVUs: Number(__ENV.PRE_VUS || 100),
                maxVUs: Number(__ENV.MAX_VUS || 1000),
            },
    },

    thresholds: {
        http_req_duration: [
            'p(95)<500',
            'p(99)<1000',
        ],
        http_req_failed: [
            'rate<0.001',
        ],
        dropped_iterations: [
            'count==0',
        ],
        redemption_success_rate: [
            'rate>0.99',
        ],
    },
};

export default function () {
    const id = exec.scenario.iterationInTest;

    const customerId = `${CUSTOMER_PREFIX}-${id}`;
    const fingerprint = `${DEVICE_PREFIX}-${id}`;

    const voucherCode = BATCH_MODE === 'distributed'
        ? VOUCHER_CODES[id % VOUCHER_CODES.length]
        : VOUCHER_CODE;

    const payload = JSON.stringify({
        voucherCode: voucherCode,
        customerId: customerId,
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
                'X-Device-Fingerprint': fingerprint,
            },
            tags: {
                endpoint: 'redemptions',
                scenario: MODE,
            },
            timeout: '10s',
        }
    );

    const status = response.status;

    const isSuccess = status === 200;
    const isBusinessRejection = [404, 409, 422, 429].includes(status);
    const isServerError = status >= 500;

    if (isSuccess) {
        successful.add(1, {status: String(status)});
    } else if (isBusinessRejection) {
        rejected.add(1, {status: String(status)});
    } else if (isServerError) {
        serverErrors.add(1, {status: String(status)});
    } else {
        unexpected.add(1, {status: String(status)});
    }

    successRate.add(isSuccess);
    businessLatency.add(response.timings.duration);

    check(response, {
        'HTTP status is 200': (r) => r.status === 200,
        'response is valid JSON': (r) => {
            try {
                return r.json('data') !== undefined;
            } catch (_) {
                return false;
            }
        },
    });
}

export function handleSummary(data) {
    return {
        '/results/redemption-summary.json':
            JSON.stringify(data, null, 2),
    };
}
