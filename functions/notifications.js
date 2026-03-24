const logger = require("firebase-functions/logger");
const { getFirestore } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const { getMessaging } = require("firebase-admin/messaging");
const { getRemoteConfig } = require("firebase-admin/remote-config");

const db = getFirestore();

const NOTIFICATION_TITLES = {
    en: {
        today: (name) => `Today is the birthday of ${name}`,
        todayAge: (name, age) => `Today ${name} turns ${age}`,
        future: (name) => `In 7 days is the birthday of ${name}`,
        futureAge: (name, age) => `In 7 days ${name} turns ${age}`,
    },
    es: {
        today: (name) => `Hoy es el cumpleaños de ${name}`,
        todayAge: (name, age) => `Hoy ${name} cumple ${age}`,
        future: (name) => `En 7 días es el cumpleaños de ${name}`,
        futureAge: (name, age) => `En 7 días ${name} cumple ${age}`,
    },
    fr: {
        today: (name) => `Aujourd'hui c'est l'anniversaire de ${name}`,
        todayAge: (name, age) => `Aujourd'hui ${name} fête ses ${age} ans`,
        future: (name) => `Dans 7 jours c'est l'anniversaire de ${name}`,
        futureAge: (name, age) => `Dans 7 jours ${name} fête ses ${age} ans`,
    },
    pt: {
        today: (name) => `Hoje é o aniversário de ${name}`,
        todayAge: (name, age) => `Hoje ${name} faz ${age} anos`,
        future: (name) => `Em 7 dias é o aniversário de ${name}`,
        futureAge: (name, age) => `Em 7 dias ${name} faz ${age} anos`,
    },
    de: {
        today: (name) => `Heute hat ${name} Geburtstag`,
        todayAge: (name, age) => `Heute wird ${name} ${age}`,
        future: (name) => `In 7 Tagen hat ${name} Geburtstag`,
        futureAge: (name, age) => `In 7 Tagen wird ${name} ${age}`,
    },
    it: {
        today: (name) => `Oggi è il compleanno di ${name}`,
        todayAge: (name, age) => `Oggi ${name} compie ${age} anni`,
        future: (name) => `Tra 7 giorni è il compleanno di ${name}`,
        futureAge: (name, age) => `Tra 7 giorni ${name} compie ${age} anni`,
    },
};

function getTitle(lang, noYear, isFuture, name, age) {
    const strings = NOTIFICATION_TITLES[lang] || NOTIFICATION_TITLES.en;
    if (noYear) {
        return isFuture ? strings.future(name) : strings.today(name);
    }
    return isFuture ? strings.futureAge(name, age) : strings.todayAge(name, age);
}

module.exports.notificationsFunction = async function notificationsFunction() {
    const config = await getConfig();

    for await (const { users, timezone } of getAllCurrentUsers(config)) {
        for (const user of users) {
            if (user.enable_notifications === false) {
                continue;
            }

            const snapshot = await db.collection("birthdays").where("owner", "==", user.user_id).get();
            const birthdays = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));

            const authedUser = await getAuth().getUser(user.user_id);

            for (const birthday of birthdays) {
                const offset = timezone * 60 * 60 * 1000;
                const birthDate = new Date(birthday.birth.toDate().getTime() + offset);
                const today = new Date(Date.now() + offset);

                const day = birthDate.getDate();
                const month = birthDate.getMonth() + 1;
                const todayDay = today.getDate();
                const todayMonth = today.getMonth() + 1;

                const future7Days = new Date(Date.now() + offset);
                future7Days.setDate(future7Days.getDate() + 7);

                const isSameDay = todayDay === day && todayMonth === month;
                const isSameDay7Days = future7Days.getDate() === day && (future7Days.getMonth() + 1) === month;

                if (isSameDay) {
                    logger.info(`Notification: "${authedUser.displayName}" - ${birthday.personName} (${birthday.id})`);
                    await sendNotification(birthday, user, false);
                }

                if (isSameDay7Days) {
                    logger.info(`7-day notice: "${authedUser.displayName}" - ${birthday.personName} (${birthday.id})`);
                    await sendNotification(birthday, user, true);
                }
            }
        }
    }
};

async function sendNotification(birthday, user, isFuture) {
    const noYear = !!birthday.noYear;
    const year = birthday.birth.toDate().getFullYear();
    const name = birthday.personName;
    const age = new Date().getFullYear() - year;
    const lang = user.lang || "en";

    const title = getTitle(lang, noYear, isFuture, name, age);

    const description =
        (birthday.notes ? `(${birthday.notes}) ` : "") +
        birthday.birth.toDate().toLocaleDateString(lang, { day: "numeric", month: "long" });

    return await getMessaging().send({
        token: user.token,
        notification: { title, body: description },
        android: {
            notification: {
                clickAction: "OPEN_BIRTHDAY",
            },
        },
        data: {
            birthday_id: birthday.id,
        },
    });
}

async function getConfig() {
    const template = await getRemoteConfig().getTemplate();

    const updateTimes = JSON.parse(template.parameters["daily_update_time"].defaultValue.value);
    const defaultTime = template.parameters["default_daily_update_time"].defaultValue.value;

    return { updateTimes, defaultTime };
}

async function getUsers(option, timezone, config) {
    const registrations = (
        await db
            .collection("fcm_tokens")
            .where("daily_update_time", "==", option)
            .where("timezone", "==", timezone)
            .get()
    ).docs.map((doc) => ({ token: doc.id, ...doc.data() }));

    if (option === config.defaultTime) {
        const usersWithDefaultTime = (
            await db.collection("fcm_tokens").where("timezone", "==", timezone).get()
        ).docs
            .map((doc) => ({ token: doc.id, ...doc.data() }))
            .filter((user) => !user.daily_update_time);

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
    const combinations = await getCurrentOptionTimezoneCombinations(config);

    for (const combo of combinations) {
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
