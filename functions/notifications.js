const logger = require("firebase-functions/logger");
const firebase = require("firebase-admin");

const firestore = firebase.firestore();

module.exports.notificationsFunction = async function notificationsFunction() {
    const config = await getConfig();

    for await (const { users, option, timezone } of getAllCurrentUsers(config)) {
        for (const user of users) {
            if (user.enable_notifications === false) {
                continue;
            }

            const snapshot = await firestore.collection("birthdays").where("owner", "==", user.user_id).get();
            const birthdays = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));

            const authedUser = await firebase.auth().getUser(user.user_id);

            for (const birthday of birthdays) {
                let date = birthday["birth"].toDate();

                const offset = timezone * 60 * 60 * 1000;

                date = new Date(date.getTime() + offset);
                const today = new Date(Date.now() + offset);

                const day = date.getDate();
                const month = date.getMonth() + 1;

                const todayDay = today.getDate();
                const todayMonth = today.getMonth() + 1;

                const future7Days = new Date();
                future7Days.setDate(future7Days.getDate() + 7);
                const future7DaysDay = future7Days.getDate();
                const future7DaysMonth = future7Days.getMonth() + 1;

                const isSameDay = todayDay === day && todayMonth === month;
                const isSameDay7Days = future7DaysDay === day && future7DaysMonth === month;

                if (isSameDay) {
                    logger.info(
                        `Sent notification to "${authedUser.displayName} <${authedUser.email}>. Birthday id: ${birthday.id}, (${birthday.personName})"`,
                        { structuredData: true }
                    );
                    sendNotification(birthday, user);
                }

                if (isSameDay7Days) {
                    logger.info(
                        `Sent notification to "${authedUser.displayName} <${authedUser.email}>. Birthday id: ${birthday.id}, (${birthday.personName})"`,
                        { structuredData: true }
                    );
                    sendNotification(birthday, user, true);
                }
            }
        }
    }
};

async function sendNotification(birthday, user, isFuture = false) {
    const noYear = !!birthday.noYear;
    const year = birthday.birth.toDate().getFullYear();
    const name = birthday.personName;
    const turns = new Date().getFullYear() - year;

    let title = "";

    if (noYear) {
        title = !isFuture
            ? `Today is the birthday of ${name}`
            : `In 7 days is the birthday of ${name}`;
    } else {
        title = !isFuture
            ? `Today ${name} turns ${turns}`
            : `In 7 days ${name} turns ${turns}`;
    }

    if (user.lang === "es") {
        if (noYear) {
            title = !isFuture
                ? `Hoy es el cumpleaños de ${name}`
                : `En 7 días es el cumpleaños de ${name}`;
        } else {
            title = !isFuture
                ? `Hoy ${name} cumple ${turns}`
                : `En 7 días ${name} cumple ${turns}`;
        }
    }

    const description =
        (birthday.notes ? `(${birthday.notes}) ` : "") +
        birthday.birth.toDate().toLocaleDateString(user.lang, { day: "numeric", month: "long" });

    return await firebase.messaging().send({
        token: user.token,
        notification: {
            title: title,
            body: description,
        },
        webpush: {
            fcmOptions: {
                link: `https://birthday-remainder-app.web.app/app/#/?birthday=${encodeURIComponent(birthday.id)}`,
            },
        },
        data: {
            birthday_id: birthday.id,
        },
    });
}

async function getConfig() {
    const template = await firebase.remoteConfig().getTemplate();

    /** @type {string[]} */
    const updateTimes = JSON.parse(template.parameters["daily_update_time"].defaultValue.value);
    /** @type {string} */
    const defaultTime = template.parameters["default_daily_update_time"].defaultValue.value;

    return { updateTimes, defaultTime };
}

async function getUsers(option, timezone, config) {
    const registrations = (
        await firestore
            .collection("fcm_tokens")
            .where("daily_update_time", "==", option)
            .where("timezone", "==", timezone)
            .get()
    ).docs.map((doc) => ({ token: doc.id, ...doc.data() }));

    if (option === config.defaultTime) {
        let usersWithDefaultTime = (
            await firestore.collection("fcm_tokens").where("timezone", "==", timezone).get()
        ).docs.map((doc) => ({ token: doc.id, ...doc.data() }));

        usersWithDefaultTime = usersWithDefaultTime.filter((user) => !user.daily_update_time);
        registrations.push(...usersWithDefaultTime);
    }

    return registrations;
}

async function getCurrentOption(config) {
    const utc0 = new Date();
    const hours = utc0.getHours();
    const minutes = utc0.getMinutes();
    const seconds = utc0.getSeconds();

    let currentTimeOption = { hour: hours, minute: minutes, second: seconds };

    for (const configTime of config.updateTimes) {
        const [configHour, configMinute, configSecond] = configTime.split(":").map(Number);

        if (
            hours > configHour ||
            (hours === configHour && minutes > configMinute) ||
            (hours === configHour && minutes === configMinute && seconds >= configSecond)
        ) {
            currentTimeOption = { hour: configHour, minute: configMinute, second: configSecond };
        }
    }

    return currentTimeOption;
}

async function* getAllCurrentUsers(config) {
    const timezoneOptionCombinations = await getCurrentOptionTimezoneCombinations(config);

    for (const combo of timezoneOptionCombinations) {
        const users = await getUsers(combo.option, combo.timezone, config);
        yield { ...combo, users };
    }
}

async function getCurrentOptionTimezoneCombinations(config) {
    const currentOption = await getCurrentOption(config);

    const combinations = [];

    for (let timezone = -12; timezone <= 14; timezone++) {
        combinations.push({
            option: formatLabel({
                hour: (currentOption.hour + timezone + 24) % 24,
                minute: currentOption.minute,
                second: currentOption.second,
            }),
            timezone,
        });
    }

    return combinations;
}

function formatLabel(option) {
    const hourStr = option.hour.toString().padStart(2, "0");
    const minuteStr = option.minute.toString().padStart(2, "0");
    const secondStr = option.second.toString().padStart(2, "0");

    if (secondStr === "00") {
        return `${hourStr}:${minuteStr}`;
    }

    return `${hourStr}:${minuteStr}:${secondStr}`;
}
