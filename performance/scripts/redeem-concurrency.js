//docker compose --env-file performance/.env -f performance/docker-compose.perf.yaml --profile loadtest run --rm -e CODE={CODE} -e BASE_URL=http://host.docker.internal:8080 k6 run /scripts/redeem-concurrency.js
import http from "k6/http";
import { Counter } from "k6/metrics";

const ok = new Counter("redeem_ok");
const conflict = new Counter("redeem_conflict");
const other = new Counter("redeem_other");

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const CODE = __ENV.CODE;

export const options = {
    scenarios: {
        burst: {
            executor: "per-vu-iterations",
            vus: 100,
            iterations: 1,
            maxDuration: "60s",
        },
    },
    thresholds: {
        redeem_ok: ["count==5"],
        redeem_conflict: ["count==95"],
        redeem_other: ["count==0"],
    },
};

export default function () {
    const customerId = `LOAD-${__VU}-${Date.now()}`;

    const res = http.post(
        `${BASE}/redemptions`,
        JSON.stringify({
            voucherCode: CODE,
            customerId: customerId,
            cartSubtotal: 100,
            customerSegments: [],
            items: [{ productId: "PRODUCT-01", quantity: 1, unitPrice: 100 }],
        }),
        {
            headers: {
                "Content-Type": "application/json",
                "X-Device-Fingerprint": `LOAD-DEVICE-${customerId}`,
            },
        },
    );

    if (res.status === 200) ok.add(1);
    else if (res.status === 409) conflict.add(1);
    else other.add(1);
}