process.env.TZ = 'UTC';

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest, onCall, HttpsError } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const firebase = require("firebase-admin");
firebase.initializeApp();

const firestore = firebase.firestore();
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
        logger.info("Manually function triggered", { structuredData: true });
        await notificationsFunction();
        response.send("Done");
    }
);

exports.shareBirthday = onCall(async (request) => {
    const email = request.auth?.token?.email;

    if (!email) {
        throw new HttpsError(
            "invalid-argument",
            "The function must be called by an authenticated user with an email."
        );
    }

    const birthdayId = request.data.birthdayId;
    const birthday = (await firestore.collection("birthdays").doc(birthdayId).get()).data();

    if (!birthday || birthday.owner !== request.auth.uid) {
        throw new HttpsError("not-found", "Birthday not found");
    }

    const doc = await firestore.collection("share_birthday").add({
        birthdayId,
        email,
    });

    return doc.id;
});
