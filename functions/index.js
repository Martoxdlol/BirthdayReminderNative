process.env.TZ = "UTC";

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest, onCall, HttpsError } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

const { notificationsFunction } = require("./notifications");

exports.scheduledHourlyNotificationSendingTask = onSchedule(
    { schedule: "1 * * * *", timeoutSeconds: 540 },
    async () => {
        await notificationsFunction();
    }
);

exports.manually = onRequest(
    { timeoutSeconds: 540 },
    async (request, response) => {
        logger.info("Manually function triggered");
        await notificationsFunction();
        response.send("Done");
    }
);

exports.shareBirthday = onCall(async (request) => {
    const email = request.auth?.token?.email;

    if (!email) {
        throw new HttpsError(
            "unauthenticated",
            "The function must be called by an authenticated user with an email."
        );
    }

    const birthdayId = request.data.birthdayId;
    const birthday = (await db.collection("birthdays").doc(birthdayId).get()).data();

    if (!birthday || birthday.owner !== request.auth.uid) {
        throw new HttpsError("not-found", "Birthday not found");
    }

    const doc = await db.collection("share_birthday").add({
        birthdayId,
        email,
    });

    return doc.id;
});
