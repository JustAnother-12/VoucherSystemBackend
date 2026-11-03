import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VOUCHER_CODE = __ENV.VOUCHER_CODE || 'PERF-GENERIC-001';

const MODE = __ENV.MODE || 'invalid_voucher';

const expectedStatus = {
    invalid_voucher: 404,
    invalid_condition: 422,
};

const rejected = new Counter('failure_expected');
const unexpected = new Counter('failure_unexpected');

export const options = {
    scenarios: {
        failure_traffic: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.TARGET_RATE || 100),
            timeUnit: '1s',
            duration: __ENV.DURATION || '2m',
            preAllocatedVUs: 50,
            maxVUs: 300,
        },
    },
};

export default function () {
    const id = exec.scenario.iterationInTest;

    const payload = JSON.stringify({
        voucherCode: MODE === 'invalid_voucher'
            ? 'UNKNOWN-PERF-VOUCHER'
            : VOUCHER_CODE,

        customerId: `PERF-FAIL-${id}`,

        cartSubtotal: MODE === 'invalid_condition'
            ? 20.00
            : 100.00,

        customerSegments: [],

        items: [
            {
                productId: 'PRODUCT-01',
                quantity: 1,
                unitPrice: MODE === 'invalid_condition'
                    ? 20.00
                    : 100.00,
            },
        ],
    });

    const response = http.post(
        `${BASE_URL}/redemptions`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
                'X-Device-Fingerprint': `FAIL-${id}`,
            },
            timeout: '10s',
        }
    );

    const expected = expectedStatus[MODE];

    if (response.status === expected) {
        rejected.add(1);
    } else {
        unexpected.add(1);
    }

    check(response, {
        'expected business status': (r) => r.status === expected,
    });
}
